import java.util.List;

public class Main {
    public static void main(String[] args) {
        List<Boolean> scripted = List.of(false, false, false, false, true, true, false, true);
        CircuitBreakerSystem.CircuitBreaker breaker = new CircuitBreakerSystem.CircuitBreaker(3, 2);
        CircuitBreakerSystem.DependencySimulator dependency = new CircuitBreakerSystem.DependencySimulator(scripted);
        CircuitBreakerSystem.GuardedClient client = new CircuitBreakerSystem.GuardedClient(breaker, dependency);

        System.out.println("tick results");
        for (int tick = 1; tick <= 10; tick += 1) {
            CircuitBreakerSystem.CallOutcome outcome = client.request();
            System.out.println(
                "  tick="
                    + tick
                    + " before="
                    + outcome.getStateBefore()
                    + " allowed="
                    + outcome.isAllowed()
                    + " result="
                    + outcome.getResult()
                    + " after="
                    + client.getBreaker().getState()
            );
        }

        System.out.println("\nsummary");
        System.out.println("  success=" + client.getSuccess());
        System.out.println("  failure=" + client.getFailure());
        System.out.println("  blocked=" + client.getBlocked());
        System.out.println("  final_state=" + client.getBreaker().getState());
    }
}
