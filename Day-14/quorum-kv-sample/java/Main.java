import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        String key = "user:42";
        QuorumKVStore store = new QuorumKVStore(
            Arrays.asList("replica-a", "replica-b", "replica-c"),
            2,
            2
        );

        System.out.println("initial write with all replicas available");
        System.out.println("  write success: " + store.write(key, "active"));
        printStates(store, key);

        System.out.println("\nbring replica-c down and write a newer value");
        store.setReplicaAvailability("replica-c", false);
        System.out.println("  write success: " + store.write(key, "suspended"));
        printStates(store, key);

        System.out.println("\nbring replica-c back and read the key");
        store.setReplicaAvailability("replica-c", true);
        QuorumKVStore.ReadResult readResult = store.read(key);
        System.out.println(
            "  read value="
                + readResult.getValue()
                + ", version="
                + readResult.getVersion()
                + ", repaired_replicas="
                + readResult.getRepairedReplicas()
        );
        printStates(store, key);

        System.out.println("\nforce quorum failure and attempt write");
        store.setReplicaAvailability("replica-b", false);
        store.setReplicaAvailability("replica-c", false);
        System.out.println("  write success: " + store.write(key, "closed"));
        printStates(store, key);
    }

    private static void printStates(QuorumKVStore store, String key) {
        for (String line : store.statesForKey(key)) {
            System.out.println("  " + line);
        }
    }
}
