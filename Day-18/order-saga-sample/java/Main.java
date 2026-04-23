import java.util.List;

public class Main {
    public static void main(String[] args) {
        OrderSaga.OrderSagaOrchestrator successSaga = new OrderSaga.OrderSagaOrchestrator(
            List.of(
                new OrderSaga.InventoryStep(),
                new OrderSaga.PaymentStep(false),
                new OrderSaga.ShippingStep(false)
            )
        );
        printResult("successful saga", successSaga);

        OrderSaga.OrderSagaOrchestrator failedSaga = new OrderSaga.OrderSagaOrchestrator(
            List.of(
                new OrderSaga.InventoryStep(),
                new OrderSaga.PaymentStep(false),
                new OrderSaga.ShippingStep(true)
            )
        );
        System.out.println();
        printResult("failed saga with compensation", failedSaga);
    }

    private static void printResult(String title, OrderSaga.OrderSagaOrchestrator orchestrator) {
        System.out.println(title);
        OrderSaga.SagaResult result = orchestrator.run();
        System.out.println("  status: " + result.getStatus());
        for (String line : result.getHistory()) {
            System.out.println("  " + line);
        }
    }
}
