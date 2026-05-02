import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        BackpressureSystem.BackpressureEngine engine = new BackpressureSystem.BackpressureEngine(
            10,
            3,
            0.6,
            0.9
        );

        List<List<String>> incomingBatches = List.of(
            List.of("m1", "m2", "m3", "m4"),
            List.of("m5", "m6", "m7", "m8", "m9", "m10", "m11"),
            List.of("m12", "m13", "m14", "m15", "m16", "m17"),
            List.of("m18", "m19", "m20"),
            List.of("m21", "m22", "m23", "m24", "m25")
        );

        System.out.println("tick results");
        int tick = 1;
        for (List<String> batch : incomingBatches) {
            Map<String, Integer> result = engine.tick(batch);
            System.out.println("  tick=" + tick + " in=" + batch.size() + " -> " + result);
            tick += 1;
        }

        System.out.println("\nsummary");
        System.out.println("  accepted=" + engine.getAccepted());
        System.out.println("  throttled=" + engine.getThrottled());
        System.out.println("  shed=" + engine.getShed());
        System.out.println("  processed=" + engine.getProcessed());
        System.out.println("  final_queue_depth=" + engine.getQueueDepth());
        System.out.println("  depth_history=" + engine.getDepthHistory());
    }
}
