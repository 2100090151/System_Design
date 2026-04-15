import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RetryQueue<T> {
    public static final class Job<T> {
        private final String jobId;
        private final T payload;
        private int attempts;

        private Job(String jobId, T payload) {
            this.jobId = jobId;
            this.payload = payload;
            this.attempts = 0;
        }

        public String getJobId() {
            return jobId;
        }

        public T getPayload() {
            return payload;
        }

        private int incrementAttempts() {
            attempts += 1;
            return attempts;
        }
    }

    private final int maxRetries;
    private final int capacity;
    private int counter;
    private final Deque<Job<T>> queue;
    private final Map<String, Job<T>> inflight;
    private final List<Job<T>> deadLetter;

    public RetryQueue(int maxRetries, int capacity) {
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries must be >= 0");
        }
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.maxRetries = maxRetries;
        this.capacity = capacity;
        this.counter = 0;
        this.queue = new ArrayDeque<>();
        this.inflight = new HashMap<>();
        this.deadLetter = new ArrayList<>();
    }

    public String enqueue(T payload) {
        if (queue.size() >= capacity) {
            return null;
        }

        counter += 1;
        Job<T> job = new Job<>("job-" + counter, payload);
        queue.addLast(job);
        return job.getJobId();
    }

    public Job<T> poll() {
        Job<T> job = queue.pollFirst();
        if (job == null) {
            return null;
        }
        inflight.put(job.getJobId(), job);
        return job;
    }

    public boolean ack(String jobId) {
        return inflight.remove(jobId) != null;
    }

    public String fail(String jobId) {
        Job<T> job = inflight.remove(jobId);
        if (job == null) {
            return "missing";
        }

        int attempts = job.incrementAttempts();
        if (attempts <= maxRetries) {
            queue.addLast(job);
            return "requeued";
        }

        deadLetter.add(job);
        return "dead-lettered";
    }

    public List<String> queueIds() {
        return queue.stream().map(Job::getJobId).collect(Collectors.toList());
    }

    public List<String> inflightIds() {
        return new ArrayList<>(inflight.keySet());
    }

    public List<String> deadLetterIds() {
        return deadLetter.stream().map(Job::getJobId).collect(Collectors.toList());
    }
}