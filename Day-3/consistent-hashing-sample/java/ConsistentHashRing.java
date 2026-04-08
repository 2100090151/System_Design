import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConsistentHashRing {
    private final int virtualNodes;
    private final List<Integer> ring;
    private final Map<Integer, String> owners;

    public ConsistentHashRing(int virtualNodes) {
        this.virtualNodes = virtualNodes;
        this.ring = new ArrayList<>();
        this.owners = new HashMap<>();
    }

    public synchronized void addNode(String node) {
        for (int replica = 0; replica < virtualNodes; replica++) {
            int key = hash(node + "#" + replica);
            if (!owners.containsKey(key)) {
                owners.put(key, node);
                ring.add(key);
            }
        }
        Collections.sort(ring);
    }

    public synchronized String getNode(String itemKey) {
        if (ring.isEmpty()) {
            throw new IllegalStateException("Hash ring is empty");
        }

        int key = hash(itemKey);
        int index = Collections.binarySearch(ring, key);
        if (index < 0) {
            index = -index - 1;
        }
        if (index == ring.size()) {
            index = 0;
        }
        return owners.get(ring.get(index));
    }

    public synchronized Map<String, Integer> distribution(List<String> keys) {
        Map<String, Integer> counts = new HashMap<>();
        for (String key : keys) {
            String owner = getNode(key);
            counts.put(owner, counts.getOrDefault(owner, 0) + 1);
        }

        List<String> sortedKeys = new ArrayList<>(counts.keySet());
        Collections.sort(sortedKeys);
        Map<String, Integer> ordered = new LinkedHashMap<>();
        for (String key : sortedKeys) {
            ordered.put(key, counts.get(key));
        }
        return ordered;
    }

    private static int hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return ((bytes[0] & 0xff) << 24)
                    | ((bytes[1] & 0xff) << 16)
                    | ((bytes[2] & 0xff) << 8)
                    | (bytes[3] & 0xff);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
