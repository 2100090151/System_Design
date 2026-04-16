import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PartitionedLog<T> {
    public static final class Message<T> {
        private final int partition;
        private final int offset;
        private final String key;
        private final T payload;

        private Message(int partition, int offset, String key, T payload) {
            this.partition = partition;
            this.offset = offset;
            this.key = key;
            this.payload = payload;
        }

        public int getPartition() {
            return partition;
        }

        public int getOffset() {
            return offset;
        }

        public String getKey() {
            return key;
        }

        public T getPayload() {
            return payload;
        }
    }

    private final String topic;
    private final List<List<Message<T>>> partitions;
    private final Map<String, int[]> committedOffsets;

    public PartitionedLog(String topic, int partitionCount) {
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("topic must be non-empty");
        }
        if (partitionCount <= 0) {
            throw new IllegalArgumentException("partitionCount must be positive");
        }

        this.topic = topic;
        this.partitions = new ArrayList<>();
        for (int i = 0; i < partitionCount; i += 1) {
            this.partitions.add(new ArrayList<>());
        }
        this.committedOffsets = new LinkedHashMap<>();
    }

    public String getTopic() {
        return topic;
    }

    public Message<T> produce(String key, T payload) {
        int partitionId = partitionForKey(key);
        List<Message<T>> partition = partitions.get(partitionId);
        int offset = partition.size();

        Message<T> message = new Message<>(partitionId, offset, key, payload);
        partition.add(message);
        return message;
    }

    public void registerGroup(String groupName) {
        committedOffsets.computeIfAbsent(groupName, ignored -> new int[partitions.size()]);
    }

    public List<Message<T>> poll(String groupName, int maxMessages) {
        if (maxMessages <= 0) {
            throw new IllegalArgumentException("maxMessages must be positive");
        }

        int[] committed = offsetsForGroup(groupName);
        int[] cursors = Arrays.copyOf(committed, committed.length);
        List<Message<T>> batch = new ArrayList<>();

        while (batch.size() < maxMessages) {
            boolean emitted = false;

            for (int partitionId = 0; partitionId < partitions.size(); partitionId += 1) {
                List<Message<T>> entries = partitions.get(partitionId);
                int cursor = cursors[partitionId];
                if (cursor >= entries.size()) {
                    continue;
                }

                batch.add(entries.get(cursor));
                cursors[partitionId] += 1;
                emitted = true;

                if (batch.size() >= maxMessages) {
                    break;
                }
            }

            if (!emitted) {
                break;
            }
        }

        return batch;
    }

    public void commit(String groupName, List<Message<T>> messages) {
        int[] committed = offsetsForGroup(groupName);
        for (Message<T> message : messages) {
            int nextOffset = message.getOffset() + 1;
            int partitionId = message.getPartition();
            if (nextOffset > committed[partitionId]) {
                committed[partitionId] = nextOffset;
            }
        }
    }

    public Map<Integer, Integer> lag(String groupName) {
        int[] committed = offsetsForGroup(groupName);
        Map<Integer, Integer> lag = new LinkedHashMap<>();

        for (int partitionId = 0; partitionId < partitions.size(); partitionId += 1) {
            int partitionSize = partitions.get(partitionId).size();
            lag.put(partitionId, partitionSize - committed[partitionId]);
        }

        return lag;
    }

    public Map<Integer, Integer> committedOffsets(String groupName) {
        int[] committed = offsetsForGroup(groupName);
        Map<Integer, Integer> offsets = new LinkedHashMap<>();
        for (int partitionId = 0; partitionId < committed.length; partitionId += 1) {
            offsets.put(partitionId, committed[partitionId]);
        }
        return offsets;
    }

    private int[] offsetsForGroup(String groupName) {
        int[] offsets = committedOffsets.get(groupName);
        if (offsets == null) {
            throw new IllegalArgumentException("unknown consumer group: " + groupName);
        }
        return offsets;
    }

    private int partitionForKey(String key) {
        try {
            byte[] digest = MessageDigest.getInstance("MD5").digest(key.getBytes(StandardCharsets.UTF_8));
            BigInteger value = new BigInteger(1, digest);
            return value.mod(BigInteger.valueOf(partitions.size())).intValue();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm unavailable", e);
        }
    }
}
