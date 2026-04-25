from cqrs_order_system import CommandService, Projector, QueryService, ReadStore, WriteStore


def print_order(prefix: str, order) -> None:
    if order is None:
        print(prefix, "<not available in read model>")
        return
    print(
        prefix,
        f"id={order.order_id} customer={order.customer_id} amount={order.amount_cents} status={order.status}",
    )


def main() -> None:
    write_store = WriteStore()
    read_store = ReadStore()

    commands = CommandService(write_store)
    projector = Projector(write_store, read_store)
    queries = QueryService(read_store)

    commands.create_order("order-200", "cust-1", 4999)
    print_order("before projection:", queries.get_order("order-200"))

    for line in projector.project_next(batch_size=10):
        print(line)
    print_order("after create projection:", queries.get_order("order-200"))

    commands.confirm_order("order-200")
    print_order("after confirm, before projection:", queries.get_order("order-200"))

    for line in projector.project_next(batch_size=10):
        print(line)
    print_order("after confirm projection:", queries.get_order("order-200"))

    print("projector last_sequence:", projector.last_sequence)
    print("event count:", len(write_store.events))


if __name__ == "__main__":
    main()
