import java.util.ArrayList;
import java.util.List;

public class RoundRobinLoadBalancer {
    private static class BackendServer {
        private final String name;
        private boolean healthy;

        private BackendServer(String name) {
            this.name = name;
            this.healthy = true;
        }
    }

    private final List<BackendServer> servers;
    private int index;

    public RoundRobinLoadBalancer() {
        this.servers = new ArrayList<>();
        this.index = 0;
    }

    public synchronized void addServer(String name) {
        servers.add(new BackendServer(name));
    }

    public synchronized void setHealth(String name, boolean healthy) {
        for (BackendServer server : servers) {
            if (server.name.equals(name)) {
                server.healthy = healthy;
                return;
            }
        }
        throw new IllegalArgumentException("Unknown server: " + name);
    }

    public synchronized String nextServer() {
        if (servers.isEmpty()) {
            throw new IllegalStateException("No backend servers registered");
        }

        int total = servers.size();
        for (int i = 0; i < total; i++) {
            BackendServer server = servers.get(index);
            index = (index + 1) % total;
            if (server.healthy) {
                return server.name;
            }
        }

        throw new IllegalStateException("No healthy backend servers available");
    }

    public synchronized String snapshot() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < servers.size(); i++) {
            BackendServer server = servers.get(i);
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(server.name).append(":").append(server.healthy ? "healthy" : "unhealthy");
        }
        sb.append("]");
        return sb.toString();
    }
}
