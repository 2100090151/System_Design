import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        RaftCluster cluster = new RaftCluster(List.of("n1", "n2", "n3", "n4", "n5"));

        boolean elected = cluster.electLeader("n1");
        System.out.println("leader election for n1: " + elected);
        printState("cluster after election", cluster);

        boolean committed = cluster.leaderAppend("set x=10");
        System.out.println("\nappend set x=10 committed: " + committed);

        cluster.setAvailability("n4", false);
        cluster.setAvailability("n5", false);
        printState("\ncluster after two follower failures", cluster);

        committed = cluster.leaderAppend("set y=20");
        System.out.println("\nappend set y=20 committed with 3/5 up: " + committed);

        cluster.setAvailability("n3", false);
        printState("\ncluster after third failure", cluster);

        committed = cluster.leaderAppend("set z=30");
        System.out.println("\nappend set z=30 committed with 2/5 up: " + committed);
    }

    private static void printState(String label, RaftCluster cluster) {
        System.out.println(label);
        for (Map<String, Object> row : cluster.clusterState()) {
            System.out.println(
                "  "
                    + row.get("node")
                    + " avail="
                    + row.get("available")
                    + " role="
                    + row.get("role")
                    + " term="
                    + row.get("term")
                    + " log="
                    + row.get("log_len")
                    + " commit="
                    + row.get("commit_index")
            );
        }
    }
}
