from solid_alert_system import (
    Alert,
    AlertAuditService,
    AlertDispatcher,
    EmailChannel,
    InMemoryAlertRepository,
    PagerDutyChannel,
    Severity,
    SmsChannel,
)


def main() -> None:
    repository = InMemoryAlertRepository()
    channels = [EmailChannel(), SmsChannel(), PagerDutyChannel()]
    dispatcher = AlertDispatcher(writer=repository, channels=channels)

    alerts = [
        Alert(alert_id="a-101", severity=Severity.INFO, message="new deployment completed"),
        Alert(alert_id="a-102", severity=Severity.WARNING, message="retry rate increased"),
        Alert(alert_id="a-103", severity=Severity.CRITICAL, message="payment API is unavailable"),
    ]

    for alert in alerts:
        routed = dispatcher.dispatch(alert)
        print(f"dispatch {alert.alert_id} ({alert.severity.value}) -> {routed}")

    print("stored alerts:", [alert.alert_id for alert in repository.list_all()])

    for channel in channels:
        print(f"{channel.name} sent:", channel.sent_ids())

    audit = AlertAuditService(reader=repository)
    print("critical count:", audit.critical_count())


if __name__ == "__main__":
    main()
