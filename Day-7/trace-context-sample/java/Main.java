public class Main {
    public static void main(String[] args) {
        gateway();
    }

    private static void gateway() {
        TraceContext root = TraceContext.startRoot();
        System.out.println(root.formatLog("api-gateway", "received client request /checkout"));

        TraceContext orderSpan = root.createChild();
        System.out.println(orderSpan.formatLog("api-gateway", "calling order-service"));
        orderService(orderSpan.toHeaders());
    }

    private static void orderService(java.util.Map<String, String> headers) {
        TraceContext incoming = TraceContext.fromHeaders(headers);
        System.out.println(incoming.formatLog("order-service", "validated order payload"));

        TraceContext paymentSpan = incoming.createChild();
        System.out.println(paymentSpan.formatLog("order-service", "calling payment-service"));
        paymentService(paymentSpan.toHeaders());
    }

    private static void paymentService(java.util.Map<String, String> headers) {
        TraceContext incoming = TraceContext.fromHeaders(headers);
        System.out.println(incoming.formatLog("payment-service", "authorized payment"));
    }
}
