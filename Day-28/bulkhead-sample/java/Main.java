import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        Map<String, BulkheadSystem.BulkheadPool> pools = Map.of(
            "critical", new BulkheadSystem.BulkheadPool("critical", 5, 2),
            "best_effort", new BulkheadSystem.BulkheadPool("best_effort", 3, 1)
        );
        BulkheadSystem.BulkheadEngine engine = new BulkheadSystem.BulkheadEngine(pools);

        List<BulkheadSystem.Task> batch1 = List.of(
            new BulkheadSystem.Task("c-1", "critical"),
            new BulkheadSystem.Task("c-2", "critical"),
            new BulkheadSystem.Task("b-1", "best_effort"),
            new BulkheadSystem.Task("b-2", "best_effort"),
            new BulkheadSystem.Task("b-3", "best_effort"),
            new BulkheadSystem.Task("b-4", "best_effort")
        );

        List<BulkheadSystem.Task> batch2 = List.of(
            new BulkheadSystem.Task("c-3", "critical"),
            new BulkheadSystem.Task("c-4", "critical"),
            new BulkheadSystem.Task("c-5", "critical"),
            new BulkheadSystem.Task("c-6", "critical"),
            new BulkheadSystem.Task("b-5", "best_effort")
        );

        System.out.println("submit batch 1");
        for (String line : engine.submitBatch(batch1)) {
            System.out.println(line);
        }

        System.out.println("\ntick 1");
        for (String line : engine.tick()) {
            System.out.println(line);
        }

        System.out.println("\nsubmit batch 2");
        for (String line : engine.submitBatch(batch2)) {
            System.out.println(line);
        }

        System.out.println("\ntick 2");
        for (String line : engine.tick()) {
            System.out.println(line);
        }

        System.out.println("\nfinal metrics");
        Map<String, Map<String, Integer>> metrics = engine.metrics();
        for (String lane : List.of("critical", "best_effort")) {
            System.out.println(lane + " " + metrics.get(lane));
        }
    }
}
