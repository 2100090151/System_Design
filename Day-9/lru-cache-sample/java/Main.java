public class Main {
    public static void main(String[] args) {
        LRUCache<String, String> cache = new LRUCache<>(3);

        System.out.println("put A=alpha | evicted: " + cache.put("A", "alpha"));
        printState(cache, "  state:");

        System.out.println("put B=bravo | evicted: " + cache.put("B", "bravo"));
        printState(cache, "  state:");

        System.out.println("put C=charlie | evicted: " + cache.put("C", "charlie"));
        printState(cache, "  state:");

        System.out.println("get A -> " + cache.get("A"));
        printState(cache, "  state after touching A:");

        System.out.println("put D=delta | evicted: " + cache.put("D", "delta"));
        printState(cache, "  state:");

        System.out.println("get B -> " + cache.get("B"));
        System.out.println("get C -> " + cache.get("C"));
        printState(cache, "  final state:");
    }

    private static void printState(LRUCache<String, String> cache, String label) {
        System.out.println(label + " " + cache.snapshot());
    }
}
