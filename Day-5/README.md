# Day 5: Reliability, Security, and Platform Operations

Day 5 focuses on the operational and security topics that become critical once distributed systems move into production.

It covers:
- Geohashing and quadtrees
- Circuit breaker
- Rate limiting
- Service discovery and service mesh
- SLA, SLO, and SLI
- Disaster recovery
- Virtual machines and containers
- OAuth 2.0 and OpenID Connect (OIDC)
- Single Sign-On (SSO)
- SSL, TLS, and mTLS

In short, Day 5 is about keeping services discoverable, resilient, observable, and secure while they run at scale.

![Day 5](./Day5.png)

## Core ideas

### Geohashing and quadtrees
- `Geohashing` converts latitude and longitude into compact strings that preserve locality.
- `Quadtrees` partition 2D space recursively and are useful for range queries and location-based systems.
- Both are common when systems need to search nearby drivers, stores, or users efficiently.

### Circuit breaker and rate limiting
- A `circuit breaker` prevents repeated calls to a failing dependency and helps stop cascading failures.
- `Rate limiting` protects APIs and shared resources from abuse or sudden traffic spikes.
- Common rate-limiting algorithms include leaky bucket, token bucket, fixed window, sliding log, and sliding window.

### Service discovery
- In dynamic systems, service instances come and go frequently.
- `Service discovery` helps clients or load balancers find healthy service instances.
- `Service mesh` adds secure, observable, and policy-driven service-to-service communication.

### Reliability and recovery
- `SLA` defines business promises to users.
- `SLO` defines the measurable target for a service.
- `SLI` is the observed metric used to measure whether the objective is being met.
- `Disaster recovery` planning uses ideas like backup, RTO, RPO, cold sites, and hot sites.

### VMs and containers
- `Virtual machines` virtualize hardware and include a full guest OS.
- `Containers` virtualize at the OS level and package the app with its dependencies.
- Containers are lighter and faster to deploy, while VMs offer stronger isolation.

### Authentication and transport security
- `OAuth 2.0` handles authorization to protected resources.
- `OIDC` adds identity and authentication on top of OAuth 2.0.
- `SSO` lets users authenticate once and access multiple systems.
- `TLS` encrypts data in transit, while `mTLS` authenticates both sides of the connection.

## Day-5 sample: Token Bucket Rate Limiter

This repository includes a small token bucket rate limiter sample in both Python and Java.

### Functional requirements
- Allow a limited number of requests per time window
- Refill capacity over time
- Reject requests when no tokens are available
- Keep the implementation simple enough for interview explanation

### High-level components
- `Client`: sends requests
- `Rate limiter`: checks capacity before allowing the request
- `Token bucket`: stores the current available tokens
- `Clock/refill logic`: restores tokens over time

### Data flow
1. A request reaches the rate limiter
2. The limiter calculates how many tokens should be refilled based on elapsed time
3. If a token is available, the request is allowed and one token is consumed
4. If no token is available, the request is rejected
5. As time passes, tokens refill up to the configured capacity

## Project structure

```text
Day-5/
  README.md
  rate-limiter-sample/
    python/
      token_bucket.py
      demo.py
    java/
      TokenBucketRateLimiter.java
      Main.java
```

## Run the sample

### Python

```powershell
cd rate-limiter-sample\python
python demo.py
```

### Java

```powershell
cd rate-limiter-sample\java
javac Main.java TokenBucketRateLimiter.java
java Main
```

## Interview takeaways
- Reliability patterns like circuit breakers and rate limiters are defensive tools, not optional extras
- Service discovery becomes necessary once service locations are no longer static
- SLOs only matter if SLIs are measurable and actionable
- OAuth, OIDC, TLS, and mTLS solve different parts of the trust and identity problem
- Containers improve deployment speed, but they do not remove the need for good operational design

## Next improvements
- Add per-user and per-IP buckets
- Add distributed storage using Redis
- Return retry-after values for rejected requests
- Add burst versus sustained-rate scenarios
- Extend the sample into an API gateway rate-limiting middleware
