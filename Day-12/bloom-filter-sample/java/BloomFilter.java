import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.BitSet;

public class BloomFilter {
    private final int sizeBits;
    private final int hashCount;
    private final BitSet bits;
    private int insertions;

    public BloomFilter(int sizeBits, int hashCount) {
        if (sizeBits <= 0) {
            throw new IllegalArgumentException("sizeBits must be positive");
        }
        if (hashCount <= 0) {
            throw new IllegalArgumentException("hashCount must be positive");
        }

        this.sizeBits = sizeBits;
        this.hashCount = hashCount;
        this.bits = new BitSet(sizeBits);
        this.insertions = 0;
    }

    public void add(String item) {
        for (int index : indexes(item)) {
            bits.set(index);
        }
        insertions += 1;
    }

    public boolean mightContain(String item) {
        for (int index : indexes(item)) {
            if (!bits.get(index)) {
                return false;
            }
        }
        return true;
    }

    public double fillRatio() {
        return (double) bits.cardinality() / sizeBits;
    }

    public double estimatedFalsePositiveRate() {
        return Math.pow(1 - Math.exp((-1.0 * hashCount * insertions) / sizeBits), hashCount);
    }

    private int[] indexes(String item) {
        if (item == null || item.isBlank()) {
            throw new IllegalArgumentException("item must be non-empty");
        }

        int[] indexes = new int[hashCount];
        for (int seed = 0; seed < hashCount; seed += 1) {
            indexes[seed] = indexForSeed(item, seed);
        }
        return indexes;
    }

    private int indexForSeed(String item, int seed) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest((seed + ":" + item).getBytes(StandardCharsets.UTF_8));
            long value = ByteBuffer.wrap(hashed, 0, Long.BYTES).getLong();
            return (int) Math.floorMod(value, sizeBits);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}
