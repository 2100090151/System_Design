import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class BackpressureSystem {
    public static final class WorkItem {
        private final String id;

        public WorkItem(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    public static final class BoundedQueue {
        private final int capacity;
        private final Queue<WorkItem> items = new ArrayDeque<>();

        public BoundedQueue(int capacity) {
            if (capacity <= 0) {
                throw new IllegalArgumentException("capacity must be positive");
            }
            this.capacity = capacity;
        }

        public boolean enqueue(WorkItem item) {
            if (items.size() >= capacity) {
                return false;
            }
            items.add(item);
            return true;
        }

        public List<WorkItem> dequeueMany(int count) {
            List<WorkItem> result = new ArrayList<>();
            while (count > 0 && !items.isEmpty()) {
                result.add(items.remove());
                count -= 1;
            }
            return result;
        }

        public int depth() {
            return items.size();
        }

        public int capacity() {
            return capacity;
        }
    }

    public static final class AdmissionController {
        private final double throttleWatermark;
        private final double shedWatermark;

        public AdmissionController(double throttleWatermark, double shedWatermark) {
            if (!(0 < throttleWatermark && throttleWatermark < shedWatermark && shedWatermark < 1)) {
                throw new IllegalArgumentException("watermarks must satisfy 0 < throttle < shed < 1");
            }
            this.throttleWatermark = throttleWatermark;
            this.shedWatermark = shedWatermark;
        }

        public String decide(BoundedQueue queue, int itemIndex) {
            double fillRatio = (double) queue.depth() / queue.capacity();
            if (fillRatio >= shedWatermark) {
                return "SHED";
            }
            if (fillRatio >= throttleWatermark) {
                return itemIndex % 2 == 0 ? "ACCEPT" : "THROTTLE";
            }
            return "ACCEPT";
        }
    }

    public static final class BackpressureEngine {
        private final BoundedQueue queue;
        private final AdmissionController controller;
        private final int consumerRate;

        private int accepted = 0;
        private int throttled = 0;
        private int shed = 0;
        private int processed = 0;
        private final List<Integer> depthHistory = new ArrayList<>();

        public BackpressureEngine(
            int queueCapacity,
            int consumerRatePerTick,
            double throttleWatermark,
            double shedWatermark
        ) {
            if (consumerRatePerTick <= 0) {
                throw new IllegalArgumentException("consumerRatePerTick must be positive");
            }
            this.queue = new BoundedQueue(queueCapacity);
            this.controller = new AdmissionController(throttleWatermark, shedWatermark);
            this.consumerRate = consumerRatePerTick;
        }

        public Map<String, Integer> tick(List<String> incomingIds) {
            int acceptedNow = 0;
            int throttledNow = 0;
            int shedNow = 0;

            for (int i = 0; i < incomingIds.size(); i += 1) {
                String decision = controller.decide(queue, i);
                WorkItem item = new WorkItem(incomingIds.get(i));
                if ("ACCEPT".equals(decision)) {
                    if (queue.enqueue(item)) {
                        acceptedNow += 1;
                    } else {
                        shedNow += 1;
                    }
                } else if ("THROTTLE".equals(decision)) {
                    throttledNow += 1;
                } else {
                    shedNow += 1;
                }
            }

            int processedNow = queue.dequeueMany(consumerRate).size();

            accepted += acceptedNow;
            throttled += throttledNow;
            shed += shedNow;
            processed += processedNow;
            depthHistory.add(queue.depth());

            Map<String, Integer> result = new LinkedHashMap<>();
            result.put("accepted_now", acceptedNow);
            result.put("throttled_now", throttledNow);
            result.put("shed_now", shedNow);
            result.put("processed_now", processedNow);
            result.put("queue_depth", queue.depth());
            return result;
        }

        public int getAccepted() {
            return accepted;
        }

        public int getThrottled() {
            return throttled;
        }

        public int getShed() {
            return shed;
        }

        public int getProcessed() {
            return processed;
        }

        public int getQueueDepth() {
            return queue.depth();
        }

        public List<Integer> getDepthHistory() {
            return depthHistory;
        }
    }
}
