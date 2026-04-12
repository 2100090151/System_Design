from trace_context import TraceContext


def gateway() -> None:
    root = TraceContext.start_root()
    print(root.format_log("api-gateway", "received client request /checkout"))

    order_span = root.create_child()
    print(order_span.format_log("api-gateway", "calling order-service"))
    order_service(order_span.to_headers())


def order_service(headers: dict[str, str]) -> None:
    incoming = TraceContext.from_headers(headers)
    print(incoming.format_log("order-service", "validated order payload"))

    payment_span = incoming.create_child()
    print(payment_span.format_log("order-service", "calling payment-service"))
    payment_service(payment_span.to_headers())


def payment_service(headers: dict[str, str]) -> None:
    incoming = TraceContext.from_headers(headers)
    print(incoming.format_log("payment-service", "authorized payment"))


if __name__ == "__main__":
    gateway()
