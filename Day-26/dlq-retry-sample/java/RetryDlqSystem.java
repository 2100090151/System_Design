import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class RetryDlqSystem {
    public static final class WorkMessage {
        private final String messageId;
        private final String payload;
        private final int attempt;

        public WorkMessage(String messageId, String payload, int attempt) {
            this.messageId = messageId;
            this.payload = payload;
            this.attempt = attempt;
        }

        public String getMessageId() {
            return messageId;
        }

        public String getPayload() {
            return payload;
        }

        public int getAttempt() {
            return attempt;
        }
    }

    public static final class Worker {
        public boolean process(WorkMessage message) {
            if ("ok".equals(message.getPayload())) {
                return true;
            }
            if ("flaky".equals(message.getPayload())) {
                return message.getAttempt() >= 3;
            }
            if ("poison".equals(message.getPayload())) {
                return false;
            }
            return false;
        }
    }

    public static final class DeadLetterQueue {
        private final List<WorkMessage> messages = new ArrayList<>();

        public void add(WorkMessage message) {
            messages.add(message);
        }

        public List<WorkMessage> getMessages() {
            return messages;
        }
    }

    public static final class RetryEngine {
        private final Worker worker;
        private final int maxAttempts;
        private final Deque<WorkMessage> queue = new ArrayDeque<>();
        private final DeadLetterQueue dlq = new DeadLetterQueue();

        private int successCount = 0;
        private int retryCount = 0;
        private int dlqCount = 0;

        public RetryEngine(Worker worker, int maxAttempts) {
            if (maxAttempts < 1) {
                throw new IllegalArgumentException("maxAttempts must be >= 1");
            }
            this.worker = worker;
            this.maxAttempts = maxAttempts;
        }

        public void enqueue(WorkMessage message) {
            queue.addLast(message);
        }

        public List<String> run() {
            List<String> history = new ArrayList<>();

            while (!queue.isEmpty()) {
                WorkMessage message = queue.removeFirst();
                boolean ok = worker.process(message);

                if (ok) {
                    successCount += 1;
                    history.add("success " + message.getMessageId() + " attempt=" + message.getAttempt());
                    continue;
                }

                if (message.getAttempt() >= maxAttempts) {
                    dlq.add(message);
                    dlqCount += 1;
                    history.add("dlq " + message.getMessageId() + " attempt=" + message.getAttempt());
                    continue;
                }

                WorkMessage next = new WorkMessage(
                    message.getMessageId(),
                    message.getPayload(),
                    message.getAttempt() + 1
                );
                queue.addLast(next);
                retryCount += 1;
                history.add("retry " + message.getMessageId() + " next_attempt=" + next.getAttempt());
            }

            return history;
        }

        public DeadLetterQueue getDlq() {
            return dlq;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getRetryCount() {
            return retryCount;
        }

        public int getDlqCount() {
            return dlqCount;
        }
    }
}
