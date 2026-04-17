public class Main {
    public static void main(String[] args) {
        BloomFilter bloom = new BloomFilter(128, 3);

        String[] insertedItems = {
            "user:1001",
            "user:1002",
            "user:1003",
            "order:9001",
            "order:9002"
        };

        System.out.println("adding items");
        for (String item : insertedItems) {
            bloom.add(item);
            System.out.println("  added " + item);
        }

        System.out.println("\nmembership checks");
        printCheck(bloom, "user:1001");
        printCheck(bloom, "user:5000");
        printCheck(bloom, "order:9002");
        printCheck(bloom, "order:9999");

        System.out.println("\nfilter stats");
        System.out.printf("  fill ratio: %.2f%%%n", bloom.fillRatio() * 100);
        System.out.printf(
            "  estimated false positive rate: %.4f%%%n",
            bloom.estimatedFalsePositiveRate() * 100
        );
    }

    private static void printCheck(BloomFilter bloom, String item) {
        String verdict = bloom.mightContain(item) ? "might be present" : "definitely not present";
        System.out.printf("%12s -> %s%n", item, verdict);
    }
}
