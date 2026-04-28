# Day 23: Schema Registry, Compatibility Checks, and Contract Safety

Day 23 focuses on how distributed systems evolve message schemas safely across producers and consumers.

It covers:
- schema registry fundamentals
- versioned contract management
- backward compatibility checks
- producer/consumer upgrade ordering
- contract validation at publish time
- operational trade-offs in schema governance

In short, Day 23 is about preventing breaking message changes from silently corrupting event-driven systems.

![Day 23](./Day23.png)

[Day 23 PDF](./System_Design_Day_23.pdf)

## Core ideas

### Why schema governance matters
- Producers and consumers usually deploy independently.
- Unchecked schema changes can break downstream parsing at runtime.
- A `schema registry` centralizes contracts and compatibility rules.

### Versioned contracts
- Each subject (topic/event type) has ordered schema versions.
- Producers publish with a schema version identifier.
- Consumers resolve payload shape using registry metadata.

### Compatibility checks
- Backward compatibility means new schema can read old data safely.
- A common safe rule: required fields should not be removed or type-changed.
- Additional optional fields are typically compatible.

### Publish-time safety
- Producer should validate payload against registered schema.
- Unknown fields, missing required fields, or wrong types are rejected early.
- Fail-fast behavior prevents invalid data from entering the stream.

### Trade-offs
- Registry improves reliability but adds governance overhead.
- Strict checks slow uncontrolled changes but reduce production incidents.
- Teams need clear evolution policy and migration playbooks.

## Day-23 sample: Schema Registry Compatibility Simulator

This repository includes a small schema registry sample in both Python and Java.

### Functional requirements
- Register schema versions under a subject
- Validate new versions for backward compatibility
- Reject incompatible schema updates
- Validate outgoing messages against schema
- Demonstrate safe producer evolution from v1 to v2

### High-level components
- `SchemaField`: field metadata (name, type, required)
- `SchemaRegistry`: version storage and compatibility checks
- `MessageValidator`: payload validation against a version
- `ProducerSimulator`: sends events only when validation passes

### Data flow
1. Team registers initial schema version for a subject
2. Team proposes a new schema version
3. Registry runs compatibility checks against latest version
4. Producer validates payload before publish
5. Valid payload is accepted; invalid payload is rejected with a clear error

## Project structure

```text
Day-23/
  README.md
  schema-registry-sample/
    python/
      schema_registry.py
      demo.py
    java/
      SchemaRegistrySystem.java
      Main.java
```

## Run the sample

### Python

```powershell
cd schema-registry-sample\python
python demo.py
```

### Java

```powershell
cd schema-registry-sample\java
javac Main.java SchemaRegistrySystem.java
java Main
```

## Interview takeaways
- Schema compatibility is a runtime safety concern, not just documentation
- Backward compatibility enables safer rolling upgrades
- Publish-time validation catches contract violations early
- Contract evolution should be explicit and reviewed
- Registry strategy pairs strongly with event-driven and data platform reliability

## Next improvements
- Add forward and full compatibility modes
- Add default values and nullable semantics
- Add schema deprecation lifecycle and ownership metadata
- Add CI contract checks for producer pull requests
- Integrate with Avro/Protobuf and real registry APIs
