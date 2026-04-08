import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        List<String> keys = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            keys.add("user:" + i);
        }

        ConsistentHashRing ring = new ConsistentHashRing(20);
        ring.addNode("shard-a");
        ring.addNode("shard-b");
        ring.addNode("shard-c");

        System.out.println("Initial routing:");
        for (int i = 0; i < 8; i++) {
            String key = keys.get(i);
            System.out.println("  " + key + " -> " + ring.getNode(key));
        }
        System.out.println("Distribution: " + ring.distribution(keys));

        ring.addNode("shard-d");
        System.out.println();
        System.out.println("After adding shard-d:");
        for (int i = 0; i < 8; i++) {
            String key = keys.get(i);
            System.out.println("  " + key + " -> " + ring.getNode(key));
        }
        System.out.println("Distribution: " + ring.distribution(keys));
    }
}
