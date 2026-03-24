# 🌐 Entity Mapper Task Plugin (emtp)

The `emtp` module provides a task plugin for mapping values from the execution context into dynamic entities. It is part of the LinID task engine and allows flexible mapping configuration using Jinja templates.

## EntityMapperTaskPlugin

The `EntityMapperTaskPlugin` is a task plugin that populates entity attributes dynamically based on the execution context. It retrieves values defined in the task configuration and assigns them to corresponding fields in the dynamic entity.

### ✅ Use Case

Use this plugin when you need to:

* Map values from the execution context to entity attributes.
* Dynamically compute entity fields using templates.
* Simplify the integration of external data or precomputed values into entities.

### 🔧 Configuration

#### Minimal Example

```yaml
task:
  - name: X
    type: entity-mapper
    mapping:
      id: '{{context.response.id}}'
```

### Configuration Fields

| Key       | Required | Description                                       |
| --------- | -------- |---------------------------------------------------|
| `name`    | ✅       | Name of the task                                  |
| `type`    | ✅       | Must be `entity-mapper`                           |
| `mapping` | ✅       | Entity fields mapping, supports Jinja templating. |

### 🛠 Behavior

* For each entry in `mapping`, the plugin renders the Jinja template using the `JinjaService` with the current execution context and entity.
* The result is assigned to the corresponding attribute in the `DynamicEntity`.
* If the `mapping` option is missing, the plugin throws an `ApiException` with a descriptive error message.

### Example Mapping

```yaml
task:
  - name: MapResponseId
    type: entity-mapper
    mapping:
      id: '{{context.response.id}}'
      name: '{{context.response.name}}'
      status: '{{context.response.status}}'
```

In this example, the entity's `id`, `name`, and `status` attributes will be populated dynamically based on the values in `context.response`.

### 🔑 Notes

* Templates support any values available in the `TaskExecutionContext` and the entity itself.
* This plugin is purely a **task plugin**; it does not call external APIs or transform responses—use in combination with other plugins (like HTTP or JSON parsing) if needed.
* Missing mappings or invalid templates will result in a runtime exception.
