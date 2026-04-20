from raft_cluster import RaftCluster


def print_state(label: str, cluster: RaftCluster) -> None:
    print(label)
    for row in cluster.cluster_state():
        print(
            "  "
            + f"{row['node']} "
            + f"avail={row['available']} "
            + f"role={row['role']} "
            + f"term={row['term']} "
            + f"log={row['log_len']} "
            + f"commit={row['commit_index']}"
        )


def main() -> None:
    cluster = RaftCluster(["n1", "n2", "n3", "n4", "n5"])

    elected = cluster.elect_leader("n1")
    print("leader election for n1:", elected)
    print_state("cluster after election", cluster)

    committed = cluster.leader_append("set x=10")
    print("\nappend set x=10 committed:", committed)

    cluster.set_availability("n4", False)
    cluster.set_availability("n5", False)
    print_state("\ncluster after two follower failures", cluster)

    committed = cluster.leader_append("set y=20")
    print("\nappend set y=20 committed with 3/5 up:", committed)

    cluster.set_availability("n3", False)
    print_state("\ncluster after third failure", cluster)

    committed = cluster.leader_append("set z=30")
    print("\nappend set z=30 committed with 2/5 up:", committed)


if __name__ == "__main__":
    main()
