from merkle_tree import MerkleTree


def print_proof(label: str, proof: list) -> None:
    print(label)
    for step in proof:
        side = "left" if step.sibling_on_left else "right"
        print(f"  sibling on {side}: {step.sibling_hash[:16]}...")


def main() -> None:
    replica_a = ["order-1001", "order-1002", "order-1003", "order-1004"]
    replica_b = ["order-1001", "order-1002", "order-9999", "order-1004"]

    tree_a = MerkleTree(replica_a)
    tree_b = MerkleTree(replica_b)

    print("replica A root:", tree_a.root_hash)
    print("replica B root:", tree_b.root_hash)
    print("roots match:", tree_a.root_hash == tree_b.root_hash)

    proof = tree_a.proof_for(2)
    print_proof("\nproof for replica A value order-1003", proof)

    verified = MerkleTree.verify_proof("order-1003", proof, tree_a.root_hash)
    print("\nproof verifies against replica A root:", verified)

    mismatch = MerkleTree.verify_proof("order-1003", proof, tree_b.root_hash)
    print("same proof verifies against replica B root:", mismatch)


if __name__ == "__main__":
    main()
