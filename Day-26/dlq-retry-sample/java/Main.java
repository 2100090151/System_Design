import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        RetryDlqSystem.RetryEngine engine = new RetryDlqSystem.RetryEngine(new RetryDlqSystem.Worker(), 3);

        engine.enqueue(new RetryDlqSystem.WorkMessage("msg-ok-1", "ok", 1));
        engine.enqueue(new RetryDlqSystem.WorkMessage("msg-flaky-1", "flaky", 1));
        engine.enqueue(new RetryDlqSystem.WorkMessage("msg-poison-1", "poison", 1));

        for (String line : engine.run()) {
            System.out.println(line);
        }

        System.out.println("success_count: " + engine.getSuccessCount());
        System.out.println("retry_count: " + engine.getRetryCount());
        System.out.println("dlq_count: " + engine.getDlqCount());

        List<String> dlqIds = new ArrayList<>();
        for (RetryDlqSystem.WorkMessage m : engine.getDlq().getMessages()) {
            dlqIds.add(m.getMessageId());
        }
        System.out.println("dlq_ids: " + dlqIds);
    }
}
