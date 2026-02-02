# 🧪 RequiredValidationPlugin

The `RequiredValidationPlugin` is a validation plugin used to ensure that a value is not empty.

## 🔧 Configuration

### Minimal Example

```yaml
- type: required
  name: myRequiredValidator
```

This will validate that the value is not `null` and not an empty string.

### Full Example with Entity Attributes

```yaml
entities:
  - name: user
    attributes:
      - name: id
        type: UUID
        validations:
          - name: idRequired
            type: required
```

### Configuration Fields

| Key    | Required | Description                                      |
| ------ | -------- | ------------------------------------------------ |
| `type` | ✅        | Must be set to `"required"` for this plugin.    |
| `name` | ✅        | A unique name for this validation rule.         |


## 🛠 Behavior

* It checks if the value is `null`.
* It checks if the value is an empty string (`""`).
* If any of these conditions are true, a validation error is raised.
* For all other cases, validation passes successfully.
