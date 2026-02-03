# 🧪 RegexValidationPlugin

The `RegexValidationPlugin` is a validation plugin used to ensure that a value matches a specified regular expression pattern.

## ✅ Use Case

Use this plugin when:

* You need to validate that a value matches a specific regex pattern
* You want the validation to optionally ignore case sensitivity
* You want to easily enforce custom format rules in your flows

## 🔧 Configuration

### Minimal Example

```yaml
- type: regex
  name: myRegexValidator
  pattern: ^[a-z0-9_-]{3,16}$
```

This will validate that the value contains 3 to 16 characters consisting of lowercase letters, digits, hyphens, or underscores.

### Full Example

```yaml
- type: regex
  name: myRegexValidator
  pattern: ^hello.*world$
  insensitive: true
```

This will validate values against the regex `^hello.*world$`, ignoring case (e.g., `Hello my World`, `hello WORLD`, etc.).

### Full Example with Entity Attributes

```yaml
entities:
  - name: user
    attributes:
      - name: id
        type: UUID
        validations:
          - name: idRegexValidation
            type: regex
            pattern: ^[a-f0-9-]{36}$
            insensitive: false
```

### Configuration Fields

| Key           | Required | Description                                                                  |
| ------------- | -------- | ---------------------------------------------------------------------------- |
| `pattern`     | ✅        | The regular expression used to validate the value.                           |
| `insensitive` | ❌        | Boolean flag to enable case-insensitive matching (`true`). Default: `false`. |

## 🛠 Behavior

* The plugin retrieves the value to validate from the context (as defined by the hosting engine).
* It applies the provided `pattern` to the value.
* If `insensitive` is set to `true`, case is ignored during validation.
* If the value does not match the pattern, a validation error is raised.

## 🧷 Notes

* The plugin uses Java regular expressions (`java.util.regex`).
* If the `pattern` is invalid, a configuration error will be raised at startup.
* The `insensitive` field is optional and defaults to `false`.
