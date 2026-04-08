public class Main {
    public static void main(String[] args) {
        RoundRobinLoadBalancer balancer = new RoundRobinLoadBalancer();
        balancer.addServer("api-1");
        balancer.addServer("api-2");
        balancer.addServer("api-3");

        System.out.println("Initial backends: " + balancer.snapshot());
        System.out.println("Routing 6 requests:");
        for (int i = 1; i <= 6; i++) {
            System.out.println("  request-" + i + " -> " + balancer.nextServer());
        }

        balancer.setHealth("api-2", false);
        System.out.println();
        System.out.println("After marking api-2 unhealthy: " + balancer.snapshot());
        System.out.println("Routing 6 more requests:");
        for (int i = 7; i <= 12; i++) {
            System.out.println("  request-" + i + " -> " + balancer.nextServer());
        }
    }
}
