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

entities:
  - name: account
    route: accounts
    provider: ExampleDatabase
    tasks:
      # ... task definitions
    access:
      create:
        table: ACCOUNT_TABLE_NAME
      update:
        table: ACCOUNT_TABLE_NAME
      delete:
        table: ACCOUNT_TABLE_NAME
      findAll:
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
          column: USERNAME
```

### Configuration Fields

| Key                                         | Required | Description                                                               |
| ------------------------------------------- | -------- | ------------------------------------------------------------------------- |
| `providers[].name`                          | ✅       | Unique identifier for the database provider                               |
| `providers[].type`                          | ✅       | Must be `database` to activate this plugin                                |
| `providers[].url`                           | ✅       | JDBC connection URL (PostgreSQL format: `jdbc:postgresql://host:port/db`) |
| `providers[].username`                      | ✅       | Database authentication username                                          |
| `providers[].password`                      | ✅       | Database authentication password                                          |
| `entities[].provider`                       | ✅       | Reference to the database provider name                                   |
| `entities[].access.table`                   | ✅       | Target database table name for this entity                                |
| `entities[].attributes[].access.column`     | ✅       | Target database column name for this attribute                            |
| `entities[].attributes[].access.primaryKey` | ✅       | Indicates if this attribute is a primary key                              |

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
