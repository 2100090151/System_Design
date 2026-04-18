import java.util.List;

public class Main {
    public static void main(String[] args) {
        List<String> replicaA = List.of("order-1001", "order-1002", "order-1003", "order-1004");
        List<String> replicaB = List.of("order-1001", "order-1002", "order-9999", "order-1004");

        MerkleTree treeA = new MerkleTree(replicaA);
        MerkleTree treeB = new MerkleTree(replicaB);

        System.out.println("replica A root: " + treeA.getRootHash());
        System.out.println("replica B root: " + treeB.getRootHash());
        System.out.println("roots match: " + treeA.getRootHash().equals(treeB.getRootHash()));

        List<MerkleTree.ProofStep> proof = treeA.proofFor(2);
        printProof("\nproof for replica A value order-1003", proof);

        boolean verified = MerkleTree.verifyProof("order-1003", proof, treeA.getRootHash());
        System.out.println("\nproof verifies against replica A root: " + verified);

        boolean mismatch = MerkleTree.verifyProof("order-1003", proof, treeB.getRootHash());
        System.out.println("same proof verifies against replica B root: " + mismatch);
    }

    private static void printProof(String label, List<MerkleTree.ProofStep> proof) {
        System.out.println(label);
        for (MerkleTree.ProofStep step : proof) {
            String side = step.isSiblingOnLeft() ? "left" : "right";
            System.out.println("  sibling on " + side + ": " + step.getSiblingHash().substring(0, 16) + "...");
        }
    }
}
