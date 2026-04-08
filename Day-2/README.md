# Day 2: Networking, Scalability, and Delivery

Day 2 focuses on the infrastructure concepts that sit underneath most distributed systems.

It covers:
- IP addressing and why every machine needs a routable identity
- OSI model as a troubleshooting and communication framework
- TCP vs UDP trade-offs for reliability and latency
- DNS for name resolution
- Load balancing and clustering for scale and redundancy
- Caching, CDN, and proxies for performance and control
- Availability, scalability, and storage fundamentals

In short, Day 2 is about how requests move through a system and how we keep that path fast and resilient.

![Day 2](./Day2.png)

## Core ideas

### IP, OSI, TCP/UDP
- `IP` gives devices an address on a network.
- `OSI` gives us a layered mental model for where failures happen.
- `TCP` is reliable and ordered, but comes with more overhead.
- `UDP` is lightweight and low-latency, but delivery is not guaranteed.

### DNS
- Converts human-readable names like `example.com` into IP addresses.
- Uses resolvers, root servers, TLD servers, and authoritative name servers.
- Caching matters because repeated lookups are expensive at scale.

### Load balancing and clustering
- `Load balancing` spreads requests across servers.
- `Clustering` coordinates multiple nodes to improve availability or throughput.
- Common load-balancing algorithms include round-robin, weighted round-robin, and least connections.

### Caching, CDN, and proxy
- `Caching` reduces read latency and database pressure.
- `CDN` moves static content closer to the user.
- `Forward proxy` sits in front of clients.
- `Reverse proxy` sits in front of servers and can also handle TLS, caching, and routing.

### Availability and scalability
- Availability is usually discussed in “nines”.
- Vertical scaling adds more power to one machine.
- Horizontal scaling adds more machines and is typically better for large distributed systems.

### Storage
- `File storage` is path-based and simple for shared files.
- `Block storage` is low-level and good for attached volumes.
- `Object storage` is highly scalable and common for blobs and media.
- RAID, NAS, and HDFS matter when reliability and throughput are part of the discussion.

## Day-2 sample: Round-Robin Load Balancer

This repository includes a small load balancer sample in both Python and Java.

### Functional requirements
- Register backend servers
- Mark a server healthy or unhealthy
- Route each incoming request to the next healthy server

### High-level components
- `Client`: sends requests
- `Load balancer`: picks the next healthy backend
- `Backend servers`: handle the request
- `Health state`: prevents routing to unhealthy nodes

### Data flow
1. Backend servers are registered with the load balancer
2. A request arrives
3. The load balancer picks the next healthy server in round-robin order
4. If a server is unhealthy, it is skipped
5. The selected backend handles the request

## Project structure

```text
Day-2/
  README.md
  load-balancer-sample/
    python/
      load_balancer.py
      demo.py
    java/
      RoundRobinLoadBalancer.java
      Main.java
```

## Run the sample

### Python

```powershell
cd load-balancer-sample\python
python demo.py
```

### Java

```powershell
cd load-balancer-sample\java
javac Main.java RoundRobinLoadBalancer.java
java Main
```

## Interview takeaways
- DNS reduces human complexity, not system complexity
- Load balancers remove hotspots but can also become a bottleneck if not replicated
- Caches improve reads but create invalidation and consistency trade-offs
- CDN and reverse proxies are often the first big performance multipliers in internet-scale systems
- Availability and scalability decisions are usually trade-offs, not free wins

## Next improvements
- Add weighted round-robin support
- Add active health checks
- Track latency and error rate per backend
- Add sticky sessions and request tracing
- Extend the sample into a reverse-proxy simulation
