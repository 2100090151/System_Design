import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class MerkleTree {
    public static final class ProofStep {
        private final String siblingHash;
        private final boolean siblingOnLeft;

        private ProofStep(String siblingHash, boolean siblingOnLeft) {
            this.siblingHash = siblingHash;
            this.siblingOnLeft = siblingOnLeft;
        }

        public String getSiblingHash() {
            return siblingHash;
        }

        public boolean isSiblingOnLeft() {
            return siblingOnLeft;
        }
    }

    private final List<String> values;
    private final List<List<String>> levels;

    public MerkleTree(List<String> values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("values must be non-empty");
        }
        for (String value : values) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("values must not contain empty items");
            }
        }

        this.values = new ArrayList<>(values);
        this.levels = buildLevels(values);
    }

    public String getRootHash() {
        return levels.get(levels.size() - 1).get(0);
    }

    public List<ProofStep> proofFor(int index) {
        if (index < 0 || index >= values.size()) {
            throw new IndexOutOfBoundsException("index out of range");
        }

        List<ProofStep> proof = new ArrayList<>();
        int currentIndex = index;

        for (int levelIndex = 0; levelIndex < levels.size() - 1; levelIndex += 1) {
            List<String> level = levels.get(levelIndex);
            int siblingIndex = currentIndex % 2 == 0 ? currentIndex + 1 : currentIndex - 1;
            if (siblingIndex >= level.size()) {
                siblingIndex = currentIndex;
            }

            proof.add(new ProofStep(level.get(siblingIndex), siblingIndex < currentIndex));
            currentIndex /= 2;
        }

        return proof;
    }

    public static boolean verifyProof(String value, List<ProofStep> proof, String expectedRoot) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("value must be non-empty");
        }

        String currentHash = leafHash(value);
        for (ProofStep step : proof) {
            if (step.isSiblingOnLeft()) {
                currentHash = parentHash(step.getSiblingHash(), currentHash);
            } else {
                currentHash = parentHash(currentHash, step.getSiblingHash());
            }
        }
        return currentHash.equals(expectedRoot);
    }

    private static List<List<String>> buildLevels(List<String> values) {
        List<List<String>> levels = new ArrayList<>();
        List<String> leaves = new ArrayList<>();
        for (String value : values) {
            leaves.add(leafHash(value));
        }
        levels.add(leaves);

        while (levels.get(levels.size() - 1).size() > 1) {
            List<String> current = levels.get(levels.size() - 1);
            List<String> nextLevel = new ArrayList<>();

            for (int index = 0; index < current.size(); index += 2) {
                String left = current.get(index);
                String right = index + 1 < current.size() ? current.get(index + 1) : current.get(index);
                nextLevel.add(parentHash(left, right));
            }

            levels.add(nextLevel);
        }

        return levels;
    }

    private static String leafHash(String value) {
        return sha256Hex("leaf:" + value);
    }

    private static String parentHash(String left, String right) {
        return sha256Hex("node:" + left + ":" + right);
    }

    private static String sha256Hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte current : digest) {
                builder.append(String.format("%02x", current));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}
