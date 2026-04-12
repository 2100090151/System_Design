import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record TraceContext(String traceId, String spanId, String parentSpanId) {
    public static TraceContext startRoot() {
        return new TraceContext(newId(), shortId(), null);
    }

    public static TraceContext fromHeaders(Map<String, String> headers) {
        return new TraceContext(
                headers.get("x-trace-id"),
                headers.get("x-span-id"),
                headers.get("x-parent-span-id")
        );
    }

    public TraceContext createChild() {
        return new TraceContext(traceId, shortId(), spanId);
    }

    public Map<String, String> toHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-trace-id", traceId);
        headers.put("x-span-id", spanId);
        if (parentSpanId != null) {
            headers.put("x-parent-span-id", parentSpanId);
        }
        return headers;
    }

    public String formatLog(String service, String message) {
        String parent = parentSpanId == null ? "-" : parentSpanId;
        return String.format(
                "[service=%s] [trace_id=%s] [span_id=%s] [parent_span_id=%s] %s",
                service,
                traceId,
                spanId,
                parent,
                message
        );
    }

    private static String newId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static String shortId() {
        return newId().substring(0, 16);
    }
}
