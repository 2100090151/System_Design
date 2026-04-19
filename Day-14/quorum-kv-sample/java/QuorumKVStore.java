import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuorumKVStore {
    public static final class ReadResult {
        private final String value;
        private final Integer version;
        private final int repairedReplicas;

        private ReadResult(String value, Integer version, int repairedReplicas) {
            this.value = value;
            this.version = version;
            this.repairedReplicas = repairedReplicas;
        }

        public String getValue() {
            return value;
        }

        public Integer getVersion() {
            return version;
        }

        public int getRepairedReplicas() {
            return repairedReplicas;
        }
    }

    private static final class VersionedValue {
        private final int version;
        private final String value;

        private VersionedValue(int version, String value) {
            this.version = version;
            this.value = value;
        }
    }

    private static final class Replica {
        private final String name;
        private boolean available;
        private final Map<String, VersionedValue> store;

        private Replica(String name) {
            this.name = name;
            this.available = true;
            this.store = new HashMap<>();
        }

        private void setAvailable(boolean available) {
            this.available = available;
        }

        private VersionedValue read(String key) {
            return store.get(key);
        }

        private void applyWrite(String key, int version, String value) {
            store.put(key, new VersionedValue(version, value));
        }
    }

    private final List<Replica> replicas;
    private final int readQuorum;
    private final int writeQuorum;
    private final Map<String, Integer> nextVersionByKey;

    public QuorumKVStore(List<String> replicaNames, int readQuorum, int writeQuorum) {
        if (replicaNames == null || replicaNames.isEmpty()) {
            throw new IllegalArgumentException("replicaNames must be non-empty");
        }
        if (readQuorum <= 0 || writeQuorum <= 0) {
            throw new IllegalArgumentException("readQuorum and writeQuorum must be positive");
        }
        if (readQuorum > replicaNames.size() || writeQuorum > replicaNames.size()) {
            throw new IllegalArgumentException("quorum values must be <= number of replicas");
        }

        this.replicas = new ArrayList<>();
        for (String replicaName : replicaNames) {
            replicas.add(new Replica(replicaName));
        }
        this.readQuorum = readQuorum;
        this.writeQuorum = writeQuorum;
        this.nextVersionByKey = new HashMap<>();
    }

    public void setReplicaAvailability(String replicaName, boolean available) {
        Replica replica = findReplica(replicaName);
        replica.setAvailable(available);
    }

    public boolean write(String key, String value) {
        validateKeyValue(key, value);
        List<Replica> availableReplicas = availableReplicas();
        if (availableReplicas.size() < writeQuorum) {
            return false;
        }

        int version = nextVersionByKey.getOrDefault(key, 0) + 1;
        nextVersionByKey.put(key, version);
        for (Replica replica : availableReplicas) {
            replica.applyWrite(key, version, value);
        }
        return true;
    }

    public ReadResult read(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key must be non-empty");
        }

        List<Replica> availableReplicas = availableReplicas();
        if (availableReplicas.size() < readQuorum) {
            throw new IllegalStateException("read quorum not met");
        }

        VersionedValue latest = null;
        for (Replica replica : availableReplicas) {
            VersionedValue current = replica.read(key);
            if (current == null) {
                continue;
            }
            if (latest == null || current.version > latest.version) {
                latest = current;
            }
        }

        if (latest == null) {
            return new ReadResult(null, null, 0);
        }

        int repaired = 0;
        for (Replica replica : availableReplicas) {
            VersionedValue current = replica.read(key);
            if (current == null || current.version < latest.version) {
                replica.applyWrite(key, latest.version, latest.value);
                repaired += 1;
            }
        }

        return new ReadResult(latest.value, latest.version, repaired);
    }

    public List<String> statesForKey(String key) {
        List<String> lines = new ArrayList<>();
        for (Replica replica : replicas) {
            VersionedValue current = replica.read(key);
            String valueRepresentation = current == null ? "None" : current.value;
            String versionRepresentation = current == null ? "-" : Integer.toString(current.version);

            lines.add(
                replica.name
                    + ": available="
                    + replica.available
                    + ", version="
                    + versionRepresentation
                    + ", value="
                    + valueRepresentation
            );
        }
        return lines;
    }

    private List<Replica> availableReplicas() {
        List<Replica> available = new ArrayList<>();
        for (Replica replica : replicas) {
            if (replica.available) {
                available.add(replica);
            }
        }
        return available;
    }

    private Replica findReplica(String replicaName) {
        for (Replica replica : replicas) {
            if (replica.name.equals(replicaName)) {
                return replica;
            }
        }
        throw new IllegalArgumentException("unknown replica: " + replicaName);
    }

    private static void validateKeyValue(String key, String value) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key must be non-empty");
        }
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("value must be non-empty");
        }
    }
}
