# 📨 KafkaPlugin (`kap`)

The `kap` module provides a Kafka publishing task plugin for LinID:

- **KafkaPublishTaskPlugin** — a `TaskPlugin` that publishes messages to Apache Kafka.

## ✅ Use Case

Use this plugin when you need to:

- Send events to Kafka topics as part of the entity lifecycle (e.g., after user creation).
- Construct dynamic payloads and headers from the execution context using Jinja templates.
- Use flexible Kafka connection options including SSL and SASL authentication.

## 🔧 Configuration

```yaml
- type: kafka-publish
  connection:
    brokers: ['kafka:9092']
    clientId: 'linid-task-plugin'
    ssl:
      enabled: false
    sasl:
      mechanism: plain
      username: 'kafka_user'
      password: 'kafka_pass'
  topic: '{{context.kafka.topic}}'
  key: '{{context.user.id}}'
  payload: >
    {
      "eventType": "{{context.eventType}}",
      "timestamp": "{{context.now}}",
      "user": {
        "id": "{{context.user.id}}",
        "email": "{{context.user.email}}"
      }
    }
  headers: >
    [
      {"name": "correlationId", "value": "{{context.requestId}}"},
      {"name": "source", "value": "linid"}
    ]
  options:
    partition: null
    compression: gzip
    acks: all
    timestamp: null
    timeoutMs: 30000
    normalizeWhitespace: true
    retry:
      attempts: 3
      backoffMs: 1000
```

### Configuration Fields

| Key          | Required | Description                                                            |
| ------------ | -------- | ---------------------------------------------------------------------- |
| `connection` | ✅       | Kafka connection configuration (brokers, clientId, ssl, sasl).         |
| `topic`      | ✅       | Target Kafka topic (supports Jinja templates).                         |
| `key`        | ❌       | Message key (supports Jinja templates). If omitted, no key is set.     |
| `payload`    | ✅       | Message payload (supports Jinja templates).                            |
| `headers`    | ❌       | List of `{name, value}` headers (supports Jinja templates).            |
| `options`    | ❌       | Advanced Kafka options (partition, compression, acks, timeout, retry). |

### Connection Fields

| Key        | Required | Description                                                |
| ---------- | -------- | ---------------------------------------------------------- |
| `brokers`  | ✅       | List of Kafka broker addresses.                            |
| `clientId` | ❌       | Client identifier for the Kafka producer.                  |
| `ssl`      | ❌       | SSL configuration (`enabled`, truststore, keystore).       |
| `sasl`     | ❌       | SASL authentication (`mechanism`, `username`, `password`). |

### Options Fields

| Key                   | Default | Description                                                             |
| --------------------- | ------- | ----------------------------------------------------------------------- |
| `partition`           | `null`  | Target partition or `null` for round-robin.                             |
| `compression`         | `none`  | Compression type: `none`, `gzip`, `snappy`, `lz4`, `zstd`.              |
| `acks`                | `all`   | Acknowledgment mode: `0`, `1`, `all`.                                   |
| `timestamp`           | `null`  | Optional message timestamp or `null` for broker-assigned.               |
| `timeoutMs`           | —       | Maps to Kafka `delivery.timeout.ms`. If omitted, Kafka default applies. |
| `retry`               | —       | Retry configuration mapped to native Kafka producer properties.         |
| `retry.attempts`      | —       | Maps to Kafka `retries`. If omitted, Kafka default applies.             |
| `retry.backoffMs`     | —       | Maps to Kafka `retry.backoff.ms`. If omitted, Kafka default applies.    |
| `normalizeWhitespace` | `false` | If `true`, collapse consecutive whitespace in the rendered payload.     |

## 🛠 Behavior

1. The plugin reads the connection, topic, key, payload, headers, and options from the task configuration.
2. It resolves all Jinja templates using the execution context (`{{context.xxx}}`) and the entity attributes (`{{entity.xxx}}`).
3. If `normalizeWhitespace` is enabled, consecutive whitespace in the rendered payload is collapsed into single spaces.
4. It constructs a Kafka `ProducerRecord` with the resolved values, applying partition and timestamp if configured.
5. It sends the message **asynchronously** using a cached `KafkaProducer` instance. Retry logic is handled natively by the Kafka producer via `retries`, `delivery.timeout.ms`, and `retry.backoff.ms` properties.

- If a required option (`connection`, `topic`, `payload`) is missing, the plugin throws an error with status `500`.
- On send failure, the error is logged. The send is asynchronous so errors do not block the calling thread.
- Kafka producers are cached per connection and producer-level options (compression, acks) for reuse across invocations.
- Cached producers are automatically closed on application shutdown via `@PreDestroy`.

## 📖 Examples

### Publish user creation event

```yaml
tasks:
  - type: kafka-publish
    phases:
      - afterCreate
    connection:
      brokers: ['kafka:9092']
    topic: 'user-events'
    key: '{{context.user.id}}'
    payload: >
      {"action": "created", "userId": "{{context.user.id}}"}
```

### Publish with SASL authentication and compression

```yaml
tasks:
  - type: kafka-publish
    phases:
      - afterUpdate
    connection:
      brokers: ['kafka1:9093', 'kafka2:9093']
      clientId: 'linid-publisher'
      ssl:
        enabled: true
      sasl:
        mechanism: plain
        username: 'producer'
        password: 'secret'
    topic: 'audit-events'
    payload: >
      {"action": "updated", "entity": "{{context.entityName}}"}
    options:
      compression: gzip
      acks: all
```

## 🧷 Important Notes

- The plugin type identifier is `kafka-publish`.
- Supported SASL mechanisms: `PLAIN`, `SCRAM-SHA-256`, `SCRAM-SHA-512`.
- Templating uses Jinja (via `JinjaService`) to dynamically inject context values (`{{context.xxx}}`) and entity attributes (`{{entity.xxx}}`) into topic, key, payload, and headers.
- Kafka producers are thread-safe and cached per connection configuration for optimal performance.
