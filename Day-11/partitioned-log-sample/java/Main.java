import java.util.List;

public class Main {
    public static void main(String[] args) {
        PartitionedLog<String> log = new PartitionedLog<>("orders", 3);

        String[][] events = {
            {"user-1", "order-1001-created"},
            {"user-2", "order-2001-created"},
            {"user-1", "order-1001-paid"},
            {"user-3", "order-3001-created"},
            {"user-2", "order-2001-cancelled"}
        };

        for (String[] event : events) {
            PartitionedLog.Message<String> message = log.produce(event[0], event[1]);
            System.out.println(
                "produce -> p"
                    + message.getPartition()
                    + "@"
                    + message.getOffset()
                    + " key="
                    + message.getKey()
                    + " payload="
                    + message.getPayload()
            );
        }

        log.registerGroup("billing");
        log.registerGroup("analytics");

        System.out.println("billing lag before poll: " + log.lag("billing"));

        List<PartitionedLog.Message<String>> billingFirst = log.poll("billing", 4);
        printBatch("billing poll #1 (before commit)", billingFirst);
        System.out.println("billing committed offsets: " + log.committedOffsets("billing"));

        log.commit("billing", billingFirst.subList(0, Math.min(2, billingFirst.size())));
        System.out.println("billing committed after partial commit: " + log.committedOffsets("billing"));
        System.out.println("billing lag after partial commit: " + log.lag("billing"));

        List<PartitionedLog.Message<String>> billingSecond = log.poll("billing", 4);
        printBatch("billing poll #2 (uncommitted messages replay)", billingSecond);

        log.commit("billing", billingSecond);
        System.out.println("billing committed after full commit: " + log.committedOffsets("billing"));
        System.out.println("billing lag final: " + log.lag("billing"));

        List<PartitionedLog.Message<String>> analyticsFirst = log.poll("analytics", 3);
        printBatch("analytics poll #1 (independent group)", analyticsFirst);
        System.out.println("analytics committed offsets: " + log.committedOffsets("analytics"));
    }

    private static void printBatch(String label, List<PartitionedLog.Message<String>> batch) {
        System.out.println(label);
        if (batch.isEmpty()) {
            System.out.println("  (empty)");
            return;
        }

        for (PartitionedLog.Message<String> message : batch) {
            System.out.println(
                "  p"
                    + message.getPartition()
                    + "@"
                    + message.getOffset()
                    + " key="
                    + message.getKey()
                    + " payload="
                    + message.getPayload()
            );
        }
    }
}
