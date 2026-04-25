public class Main {
    private static void printOrder(String prefix, CqrsOrderSystem.OrderReadModel order) {
        if (order == null) {
            System.out.println(prefix + " <not available in read model>");
            return;
        }
        System.out.println(
            prefix
                + " id="
                + order.getOrderId()
                + " customer="
                + order.getCustomerId()
                + " amount="
                + order.getAmountCents()
                + " status="
                + order.getStatus()
        );
    }

    public static void main(String[] args) {
        CqrsOrderSystem.WriteStore writeStore = new CqrsOrderSystem.WriteStore();
        CqrsOrderSystem.ReadStore readStore = new CqrsOrderSystem.ReadStore();

        CqrsOrderSystem.CommandService commands = new CqrsOrderSystem.CommandService(writeStore);
        CqrsOrderSystem.Projector projector = new CqrsOrderSystem.Projector(writeStore, readStore);
        CqrsOrderSystem.QueryService queries = new CqrsOrderSystem.QueryService(readStore);

        commands.createOrder("order-200", "cust-1", 4999);
        printOrder("before projection:", queries.getOrder("order-200"));

        for (String line : projector.projectNext(10)) {
            System.out.println(line);
        }
        printOrder("after create projection:", queries.getOrder("order-200"));

        commands.confirmOrder("order-200");
        printOrder("after confirm, before projection:", queries.getOrder("order-200"));

        for (String line : projector.projectNext(10)) {
            System.out.println(line);
        }
        printOrder("after confirm projection:", queries.getOrder("order-200"));

        System.out.println("projector last_sequence: " + projector.getLastSequence());
        System.out.println("event count: " + writeStore.getEvents().size());
    }
}
