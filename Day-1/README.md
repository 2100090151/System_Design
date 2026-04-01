# Day 1: What Is System Design?

System design is the process of planning a software system before building it.

It defines:
- Architecture (major components and their responsibilities)
- Interfaces (how services/components talk to each other)
- Data design (how data is stored, read, and updated)
- Scalability and reliability strategy
- Security and maintainability considerations

In short, system design is the blueprint that helps a system meet business and technical requirements.

![Day 1](./Day1.png)

## Why it matters

Good design decisions made early are hard to replace later. Strong system design helps you:
- avoid bottlenecks
- scale to more users safely
- handle failures gracefully
- keep systems easier to evolve

## Day-1 sample: URL Shortener

This repository includes a small URL shortener sample in both Python and Java.

### Functional requirements
- Accept a long URL
- Return a short URL
- Redirect/resolve short URL to original URL

### High-level components
- `Frontend`: accepts URL input
- `Backend service`: generates short code and stores mapping
- `Database`: persists `short_code -> long_url`
- `Redirect service`: looks up code and returns original URL

### Data flow
1. User submits long URL
2. Backend generates short code (like `b`)
3. Mapping is stored
4. User opens short URL
5. Service resolves code and redirects to original URL

## Project structure

```text
Day-1/
  README.md
  url-shortener-sample/
    python/
      url_shortener.py
      demo.py
    java/
      URLShortener.java
      Main.java
```

## Run the sample

### Python

```powershell
cd url-shortener-sample\python
python demo.py
```

### Java

```powershell
cd url-shortener-sample\java
javac Main.java URLShortener.java
java Main
```

## Next improvements (interview/real system)
- Use a real database (PostgreSQL, MySQL, or DynamoDB)
- Add collision-safe distributed ID generation
- Add cache (Redis) for hot links
- Add expiry support and custom aliases
- Add analytics (click count, geo, device)
- Add rate limiting and abuse protection
