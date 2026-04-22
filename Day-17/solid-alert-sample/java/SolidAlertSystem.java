import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SolidAlertSystem {
    public enum Severity {
        INFO,
        WARNING,
        CRITICAL
    }

    public static final class Alert {
        private final String alertId;
        private final Severity severity;
        private final String message;

        public Alert(String alertId, Severity severity, String message) {
            if (alertId == null || alertId.isBlank()) {
                throw new IllegalArgumentException("alertId must be non-empty");
            }
            this.alertId = alertId;
            this.severity = severity;
            this.message = message;
        }

        public String getAlertId() {
            return alertId;
        }

        public Severity getSeverity() {
            return severity;
        }

        public String getMessage() {
            return message;
        }
    }

    public interface AlertWriter {
        void save(Alert alert);
    }

    public interface AlertReader {
        List<Alert> listAll();
    }

    public interface AlertChannel {
        String name();

        boolean supports(Alert alert);

        void send(Alert alert);

        List<String> sentIds();
    }

    public static final class InMemoryAlertRepository implements AlertWriter, AlertReader {
        private final List<Alert> alerts = new ArrayList<>();

        @Override
        public void save(Alert alert) {
            alerts.add(alert);
        }

        @Override
        public List<Alert> listAll() {
            return new ArrayList<>(alerts);
        }
    }

    private abstract static class BaseChannel implements AlertChannel {
        private final String name;
        private final List<String> sent = new ArrayList<>();

        private BaseChannel(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public void send(Alert alert) {
            sent.add(alert.getAlertId());
        }

        @Override
        public List<String> sentIds() {
            return new ArrayList<>(sent);
        }
    }

    public static final class EmailChannel extends BaseChannel {
        public EmailChannel() {
            super("email");
        }

        @Override
        public boolean supports(Alert alert) {
            return true;
        }
    }

    public static final class SmsChannel extends BaseChannel {
        public SmsChannel() {
            super("sms");
        }

        @Override
        public boolean supports(Alert alert) {
            return alert.getSeverity() == Severity.WARNING || alert.getSeverity() == Severity.CRITICAL;
        }
    }

    public static final class PagerDutyChannel extends BaseChannel {
        public PagerDutyChannel() {
            super("pager-duty");
        }

        @Override
        public boolean supports(Alert alert) {
            return alert.getSeverity() == Severity.CRITICAL;
        }
    }

    public static final class AlertDispatcher {
        private final AlertWriter writer;
        private final List<AlertChannel> channels;

        public AlertDispatcher(AlertWriter writer, List<AlertChannel> channels) {
            if (channels == null || channels.isEmpty()) {
                throw new IllegalArgumentException("channels must be non-empty");
            }
            this.writer = writer;
            this.channels = new ArrayList<>(channels);
        }

        public List<String> dispatch(Alert alert) {
            writer.save(alert);
            List<String> routed = new ArrayList<>();
            for (AlertChannel channel : channels) {
                if (channel.supports(alert)) {
                    channel.send(alert);
                    routed.add(channel.name());
                }
            }
            return routed;
        }
    }

    public static final class AlertAuditService {
        private final AlertReader reader;

        public AlertAuditService(AlertReader reader) {
            this.reader = reader;
        }

        public long criticalCount() {
            return reader.listAll().stream().filter(alert -> alert.getSeverity() == Severity.CRITICAL).count();
        }

        public List<String> allAlertIds() {
            return reader.listAll().stream().map(Alert::getAlertId).collect(Collectors.toList());
        }
    }
}
