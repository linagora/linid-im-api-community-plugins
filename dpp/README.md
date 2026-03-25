# 📦 DPP - Database Provider Plugin

The Database Provider Plugin (DPP) is responsible for managing relational database access through configurable datasources within the LinID Identity Manager API ecosystem.

---

## 🚀 Overview

This plugin enables dynamic persistence of entities to PostgreSQL databases using **jOOQ** for SQL construction and execution. It supports configuration-driven entity mapping without requiring static entity definitions, allowing flexible database integration through YAML configuration.

⚠️ **Current Scope**: PostgreSQL only (extensible architecture for future)

---

## 📋 Configuration Model

The plugin integrates with the global YAML configuration:

```yaml
providers:
  - name: ExampleDatabase
    type: database
    url: jdbc:postgresql://DATABASE_HOST:DATABASE_PORT/DATABASE_NAME
    username: DATABASE_USER
    password: DATABASE_PASSWORD
    maximumPoolSize: 10
    idleTimeout: 600000
    connectionTimeout: 30000

entities:
  - name: account
    route: accounts
    provider: ExampleDatabase
    tasks:
      - type: entity-mapper
        phases: ["afterCreate", "afterUpdate", "afterFindById", "afterFindAll"]
        mapping:
          username: "{{ entity.LOGIN }}"
    access:
      create:
        table: ACCOUNT_TABLE_NAME
        assignmentFieldExpressions:
          name:
            expression: "{0}"
            parameters:
              - "{{ 'Jean' ~ ' Dupont' }}"
            dependsOn: []
          period:
            expression: "tstzrange({0}::timestamptz, {1}::timestamptz, '[)')"
            parameters:
              - "2026-01-24 17:00:00+01"
              - "2026-12-25 17:00:00+01"
            dependsOn: []
      update:
        table: ACCOUNT_TABLE_NAME
      delete:
        table: ACCOUNT_TABLE_NAME
      findAll:
        table: ACCOUNT_TABLE_NAME
      findById:
        table: ACCOUNT_TABLE_NAME
    attributes:
      - name: id
        type: String
        required: false
        input: Text
        access:
          primaryKey: true
          column: ID
      - name: username
        type: String
        required: true
        input: Text
        access:
          column: LOGIN
```

### Configuration Fields

| Key                                                       | Required | Description                                                               |
| --------------------------------------------------------- | -------- | ------------------------------------------------------------------------- |
| `providers[].name`                                        | ✅       | Unique identifier for the database provider                               |
| `providers[].type`                                        | ✅       | Must be `database` to activate this plugin                                |
| `providers[].url`                                         | ✅       | JDBC connection URL (PostgreSQL format: `jdbc:postgresql://host:port/db`) |
| `providers[].username`                                    | ✅       | Database authentication username                                          |
| `providers[].password`                                    | ✅       | Database authentication password                                          |
| `providers[].maximumPoolSize`                             | ❌       | Maximum number of connections in the pool (default: 10)                   |
| `providers[].idleTimeout`                                 | ❌       | Maximum idle time for connections in the pool (default: 600000 ms)        |
| `providers[].connectionTimeout`                           | ❌       | Maximum time to wait for a connection from the pool (default: 30000 ms)   |
| `entities[].provider`                                     | ✅       | Reference to the database provider name                                   |
| `entities[].access.table`                                 | ✅       | Target database table name for this entity                                |
| `entities[].access.assignmentFieldExpressions`            | ❌       | List of expressions for assigning values                                  |
| `entities[].access.assignmentFieldExpressions.expression` | ❌       | Template expression for assigning values                                  |
| `entities[].access.assignmentFieldExpressions.parameters` | ❌       | Parameters for the Jinja template expression                              |
| `entities[].access.assignmentFieldExpressions.dependsOn`  | ❌       | Dependencies for the Jinja template expression                            |
| `entities[].access.retrievingFieldExpressions`            | ❌       | List of expressions for retrieving values                                  |
| `entities[].access.retrievingFieldExpressions.expression` | ❌       | Template expression for retrieving values                                  |
| `entities[].access.retrievingFieldExpressions.parameters` | ❌       | Parameters for the Jinja template expression                              |
| `entities[].access.retrievingFieldExpressions.dependsOn`  | ❌       | Dependencies for the Jinja template expression                            |
| `entities[].attributes[].access.column`                   | ✅       | Target database column name for this attribute                            |
| `entities[].attributes[].access.primaryKey`               | ❌       | Indicates if this attribute is a primary key                              |

## 🛠 Behavior

- The plugin delegates entity mapping to the service layer. Post-processing (e.g., entity mapping via [`EntityMapperTaskPlugin`](../emtp/README.md)) should use the generic service-level phases (`afterCreate`, `afterUpdate`, `afterFindById`, `afterFindAll`) provided by the corelib.

---

## 🏗️ Architecture

### Connection Pooling

- **Pool Provider**: HikariCP
- **Pool Scope**: One pool per configured database provider
- **Configuration**:
  - Maximum pool size
  - Idle timeout
  - Connection timeout

### Dynamic Mapping

1. Entity → Table mapping via `entities[].access.table`
2. Attribute → Column mapping via `entities[].attributes[].access.column`
3. Only declared attributes are persisted
4. No static JPA entities required

### jOOQ Integration

- All queries constructed via `DSLContext`
- Dynamic table/field resolution using `DSL.name(...)`
- Bind parameters for all values
- Type-safe SQL construction

---

## 🛠 Building the Project

To compile and build the plugin:

```bash
mvn clean install
```

---

## 🧪 Running Tests

Run tests using:

```bash
mvn test
```

---
