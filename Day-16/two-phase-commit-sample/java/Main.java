import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        TwoPhaseCommitCoordinator coordinator =
            new TwoPhaseCommitCoordinator(List.of("inventory", "payment", "shipping"));

        Map<String, String> operations = new LinkedHashMap<>();
        operations.put("inventory", "reserve sku=book-1 qty=1");
        operations.put("payment", "charge user=42 amount=499");
        operations.put("shipping", "create shipment user=42");

        String txId = "tx-100";
        TwoPhaseCommitCoordinator.Decision decision = coordinator.execute(txId, operations);
        System.out.println(txId + " decision: " + decision);
        printTxState(coordinator, txId);

        txId = "tx-101";
        coordinator.participant("payment").setRejectForTx(txId, true);
        decision = coordinator.execute(txId, operations);
        System.out.println("\n" + txId + " decision (payment votes NO): " + decision);
        printTxState(coordinator, txId);
        coordinator.participant("payment").setRejectForTx(txId, false);

        txId = "tx-102";
        coordinator.participant("shipping").setAvailable(true);
        decision = coordinator.execute(txId, operations, List.of("shipping"));
        System.out.println("\n" + txId + " decision (shipping misses decision broadcast): " + decision);
        printTxState(coordinator, txId);

        coordinator.participant("shipping").setAvailable(true);
        coordinator.reconcileParticipant("shipping", txId);
        System.out.println("\n" + txId + " after shipping recovery + reconcile:");
        printTxState(coordinator, txId);
    }

    private static void printTxState(TwoPhaseCommitCoordinator coordinator, String txId) {
        for (String line : coordinator.statesForTx(txId)) {
            System.out.println("  " + line);
        }
    }
}
