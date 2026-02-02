# 🧪 RequiredValidationPlugin

The `RequiredValidationPlugin` is a validation plugin used to ensure that a value is not empty.

## 🔧 Configuration

```yaml
- type: required
  name: myRequiredValidator
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
