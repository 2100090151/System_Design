import java.util.List;

public class Main {
    public static void main(String[] args) {
        SolidAlertSystem.InMemoryAlertRepository repository = new SolidAlertSystem.InMemoryAlertRepository();
        List<SolidAlertSystem.AlertChannel> channels = List.of(
            new SolidAlertSystem.EmailChannel(),
            new SolidAlertSystem.SmsChannel(),
            new SolidAlertSystem.PagerDutyChannel()
        );

        SolidAlertSystem.AlertDispatcher dispatcher = new SolidAlertSystem.AlertDispatcher(repository, channels);

        List<SolidAlertSystem.Alert> alerts = List.of(
            new SolidAlertSystem.Alert("a-101", SolidAlertSystem.Severity.INFO, "new deployment completed"),
            new SolidAlertSystem.Alert("a-102", SolidAlertSystem.Severity.WARNING, "retry rate increased"),
            new SolidAlertSystem.Alert("a-103", SolidAlertSystem.Severity.CRITICAL, "payment API is unavailable")
        );

        for (SolidAlertSystem.Alert alert : alerts) {
            List<String> routed = dispatcher.dispatch(alert);
            System.out.println("dispatch " + alert.getAlertId() + " (" + alert.getSeverity() + ") -> " + routed);
        }

        SolidAlertSystem.AlertAuditService audit = new SolidAlertSystem.AlertAuditService(repository);
        System.out.println("stored alerts: " + audit.allAlertIds());

        for (SolidAlertSystem.AlertChannel channel : channels) {
            System.out.println(channel.name() + " sent: " + channel.sentIds());
        }

        System.out.println("critical count: " + audit.criticalCount());
    }
}
