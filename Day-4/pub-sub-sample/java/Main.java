import java.util.LinkedHashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        EventBus bus = new EventBus();

        bus.subscribe("order.created", (topic, payload) ->
                System.out.println("[notification-service] topic=" + topic + " payload=" + payload));
        bus.subscribe("order.created", (topic, payload) ->
                System.out.println("[analytics-service] topic=" + topic + " payload=" + payload));
        bus.subscribe("order.created", (topic, payload) ->
                System.out.println("[inventory-service] topic=" + topic + " payload=" + payload));
        bus.subscribe("user.signed_up", (topic, payload) ->
                System.out.println("[analytics-service] topic=" + topic + " payload=" + payload));

        System.out.println("Topic snapshot: " + bus.topicSnapshot());

        Map<String, Object> orderCreated = new LinkedHashMap<>();
        orderCreated.put("order_id", "ORD-1001");
        orderCreated.put("user_id", "U-42");
        orderCreated.put("amount", 1499);

        int delivered = bus.publish("order.created", orderCreated);
        System.out.println("Delivered to " + delivered + " subscribers");

        Map<String, Object> userSignedUp = new LinkedHashMap<>();
        userSignedUp.put("user_id", "U-99");
        userSignedUp.put("plan", "free");

        delivered = bus.publish("user.signed_up", userSignedUp);
        System.out.println("Delivered to " + delivered + " subscribers");
    }
}
