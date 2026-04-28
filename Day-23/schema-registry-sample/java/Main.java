import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        SchemaRegistrySystem.SchemaRegistry registry = new SchemaRegistrySystem.SchemaRegistry();
        String subject = "order-created";

        SchemaRegistrySystem.SchemaVersion v1 = registry.register(
            subject,
            List.of(
                new SchemaRegistrySystem.SchemaField("order_id", "string", true),
                new SchemaRegistrySystem.SchemaField("amount_cents", "int", true)
            )
        );
        System.out.println("registered " + subject + " schema v" + v1.getVersion());

        SchemaRegistrySystem.SchemaVersion v2 = registry.register(
            subject,
            List.of(
                new SchemaRegistrySystem.SchemaField("order_id", "string", true),
                new SchemaRegistrySystem.SchemaField("amount_cents", "int", true),
                new SchemaRegistrySystem.SchemaField("currency", "string", false)
            )
        );
        System.out.println("registered " + subject + " schema v" + v2.getVersion() + " (backward compatible)");

        SchemaRegistrySystem.ProducerSimulator producer = new SchemaRegistrySystem.ProducerSimulator(registry, subject);

        Map<String, Object> payloadV2 = new LinkedHashMap<>();
        payloadV2.put("order_id", "ord-1001");
        payloadV2.put("amount_cents", 1599);
        payloadV2.put("currency", "USD");
        System.out.println(producer.publish(2, payloadV2));

        Map<String, Object> payloadV1 = new LinkedHashMap<>();
        payloadV1.put("order_id", "ord-1002");
        payloadV1.put("amount_cents", 2099);
        System.out.println(producer.publish(1, payloadV1));

        try {
            registry.register(
                subject,
                List.of(
                    new SchemaRegistrySystem.SchemaField("order_id", "string", true)
                )
            );
        } catch (IllegalArgumentException err) {
            System.out.println("incompatible schema rejected: " + err.getMessage());
        }

        try {
            Map<String, Object> badPayload = new LinkedHashMap<>();
            badPayload.put("order_id", "ord-1003");
            System.out.println(producer.publish(2, badPayload));
        } catch (IllegalArgumentException err) {
            System.out.println("invalid message rejected: " + err.getMessage());
        }
    }
}
