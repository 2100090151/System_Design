import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BulkheadSystem {
    public static final class Task {
        private final String taskId;
        private final String lane;

        public Task(String taskId, String lane) {
            this.taskId = taskId;
            this.lane = lane;
        }

        public String getTaskId() {
            return taskId;
        }

        public String getLane() {
            return lane;
        }
    }

    public static final class BulkheadPool {
        private final String lane;
        private final int queueCapacity;
        private final int workersPerTick;
        private final Deque<Task> queue = new ArrayDeque<>();

        private int accepted = 0;
        private int rejected = 0;
        private int processed = 0;

        public BulkheadPool(String lane, int queueCapacity, int workersPerTick) {
            this.lane = lane;
            this.queueCapacity = queueCapacity;
            this.workersPerTick = workersPerTick;
        }

        public boolean submit(Task task) {
            if (queue.size() >= queueCapacity) {
                rejected += 1;
                return false;
            }
            queue.addLast(task);
            accepted += 1;
            return true;
        }

        public List<String> processTick() {
            List<String> history = new ArrayList<>();
            for (int i = 0; i < workersPerTick; i++) {
                if (queue.isEmpty()) {
                    break;
                }
                Task task = queue.removeFirst();
                processed += 1;
                history.add("processed " + task.getTaskId() + " lane=" + lane);
            }
            return history;
        }

        public int getAccepted() {
            return accepted;
        }

        public int getRejected() {
            return rejected;
        }

        public int getProcessed() {
            return processed;
        }

        public int queueDepth() {
            return queue.size();
        }
    }

    public static final class BulkheadRouter {
        private final Map<String, BulkheadPool> pools;

        public BulkheadRouter(Map<String, BulkheadPool> pools) {
            this.pools = pools;
        }

        public boolean route(Task task) {
            BulkheadPool pool = pools.get(task.getLane());
            if (pool == null) {
                return false;
            }
            return pool.submit(task);
        }
    }

    public static final class BulkheadEngine {
        private final Map<String, BulkheadPool> pools;
        private final BulkheadRouter router;

        public BulkheadEngine(Map<String, BulkheadPool> pools) {
            this.pools = pools;
            this.router = new BulkheadRouter(pools);
        }

        public List<String> submitBatch(List<Task> tasks) {
            List<String> history = new ArrayList<>();
            for (Task task : tasks) {
                boolean ok = router.route(task);
                history.add((ok ? "accepted " : "rejected ") + task.getTaskId() + " lane=" + task.getLane());
            }
            return history;
        }

        public List<String> tick() {
            List<String> history = new ArrayList<>();
            List<String> lanes = new ArrayList<>(pools.keySet());
            lanes.sort(String::compareTo);
            for (String lane : lanes) {
                history.addAll(pools.get(lane).processTick());
            }
            return history;
        }

        public Map<String, Map<String, Integer>> metrics() {
            Map<String, Map<String, Integer>> result = new HashMap<>();
            for (Map.Entry<String, BulkheadPool> entry : pools.entrySet()) {
                BulkheadPool p = entry.getValue();
                Map<String, Integer> lane = new HashMap<>();
                lane.put("accepted", p.getAccepted());
                lane.put("rejected", p.getRejected());
                lane.put("processed", p.getProcessed());
                lane.put("queue_depth", p.queueDepth());
                result.put(entry.getKey(), lane);
            }
            return result;
        }
    }
}
