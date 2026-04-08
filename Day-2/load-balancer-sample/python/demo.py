from load_balancer import RoundRobinLoadBalancer


def main() -> None:
    balancer = RoundRobinLoadBalancer()
    for name in ("api-1", "api-2", "api-3"):
        balancer.add_server(name)

    print("Initial backends:", balancer.snapshot())
    print("Routing 6 requests:")
    for i in range(1, 7):
        print(f"  request-{i} -> {balancer.next_server()}")

    balancer.set_health("api-2", False)
    print("\nAfter marking api-2 unhealthy:", balancer.snapshot())
    print("Routing 6 more requests:")
    for i in range(7, 13):
        print(f"  request-{i} -> {balancer.next_server()}")


if __name__ == "__main__":
    main()
