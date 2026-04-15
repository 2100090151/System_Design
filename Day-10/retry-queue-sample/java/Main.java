public class Main {
    public static void main(String[] args) {
        RetryQueue<String> queue = new RetryQueue<>(1, 2);

        System.out.println("enqueue invoice-101 -> " + queue.enqueue("invoice-101"));
        System.out.println("enqueue invoice-102 -> " + queue.enqueue("invoice-102"));
        System.out.println("enqueue invoice-103 (capacity exceeded) -> " + queue.enqueue("invoice-103"));
        printState(queue, "state after enqueue");

        RetryQueue.Job<String> first = queue.poll();
        if (first != null) {
            System.out.println("poll -> " + first.getJobId() + " " + first.getPayload());
            System.out.println("ack -> " + queue.ack(first.getJobId()));
        }
        printState(queue, "state after first job success");

        RetryQueue.Job<String> second = queue.poll();
        if (second != null) {
            System.out.println("poll -> " + second.getJobId() + " " + second.getPayload());
            System.out.println("fail -> " + queue.fail(second.getJobId()));
        }
        printState(queue, "state after first failure");

        RetryQueue.Job<String> retry = queue.poll();
        if (retry != null) {
            System.out.println("poll retry -> " + retry.getJobId() + " " + retry.getPayload());
            System.out.println("fail again -> " + queue.fail(retry.getJobId()));
        }
        printState(queue, "final state");
    }

    private static void printState(RetryQueue<String> queue, String label) {
        System.out.println(label);
        System.out.println("  queue: " + queue.queueIds());
        System.out.println("  inflight: " + queue.inflightIds());
        System.out.println("  dlq: " + queue.deadLetterIds());
    }
}