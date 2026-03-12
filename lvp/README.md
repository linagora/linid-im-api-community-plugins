# 📋 ListValidationPlugin

The `ListValidationPlugin` is a validation plugin used to ensure that a value is included in a predefined list of allowed values.

## ✅ Use Case

Use this plugin when:

* You need to validate that a value belongs to a specific set of allowed values
* You want to enforce list-based constraints (e.g., status codes, categories, types)
* You want to ensure data consistency with controlled vocabularies

## 🔧 Configuration

### Minimal Example

```yaml
- type: list
  name: statusValidator
  allowedValues: ["ACTIVE", "INACTIVE", "PENDING"]
```

This will validate that the value is one of: `ACTIVE`, `INACTIVE`, or `PENDING`.

### Full Example with global validation

```yaml
validations:
  - type: list
    name: statusValidation
    allowedValues: ["ACTIVE", "INACTIVE", "SUSPENDED", "PENDING"]

entities:
  - name: user
    attributes:
      - name: status
        type: STRING
        validations:
          - name: statusValidation
```

This will validate that the value is one of: `ACTIVE`, `INACTIVE`, `SUSPENDED` or `PENDING`. The validation is applied to the `status` attribute of the `user` entity and can be reused across multiple attributes or entities.

### Full Example with Entity Attributes

```yaml
entities:
  - name: user
    attributes:
      - name: status
        type: STRING
        validations:
          - name: statusValidation
            type: list
            allowedValues: ["ACTIVE", "INACTIVE", "SUSPENDED", "PENDING"]
```

This will validate that the value is one of: `ACTIVE`, `INACTIVE`, `SUSPENDED` or `PENDING`. The validation is defined directly within the attribute configuration, making it self-contained and not reusable across other attributes or entities.

### Configuration Fields

| Key             | Required | Description                                                                 |
| --------------- | -------- | --------------------------------------------------------------------------- |
| `allowedValues` | ✅        | Array of strings representing the allowed values for validation.            |

## 🛠 Behavior

* The plugin retrieves the value to validate from the context (as defined by the hosting engine).
* It checks whether the value is present in the configured `allowedValues` list.
* The comparison is **case-sensitive**.
* If the value is not in the list, a validation error is returned.
* If the value is null or an empty string no error is returned, this checks should be done by the `RequiredValidationPlugin` if needed.

## 🧷 Notes

* All values in the `allowedValues` array are treated as strings.
* The validation is **case-sensitive** (e.g., `"ACTIVE"` ≠ `"active"`).
* If the `allowedValues` option is missing or invalid, a configuration error will be raised at startup.
* `null` values and empty strings are accepted by the validation.
