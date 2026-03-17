# 🧩 ContextCompareValidationPlugin

The `ContextCompareValidationPlugin` is a task plugin that validates context values by comparing pairs resolved from Jinja templates.

## ✅ Use Case

Use this plugin when:

- You need to ensure two context values match before proceeding (e.g. the `firstName` sent in the request matches the one stored for the user).
- You want optional normalization (trim whitespace, case-insensitive comparison) before comparing.

## 🔧 Configuration

```yaml
- type: context-compare
  name: validateFirstName
  validation:
    - value1: '{{ context.user.firstName }}'
      value2: '{{ context.request.firstName }}'
      options:
        trim: true
        ignoreCase: true
```

### Configuration Fields

| Key          | Description                                                                |
| ------------ | -------------------------------------------------------------------------- |
| `validation` | A list of comparison entries, each with `value1`, `value2`, and `options`. |
| `value1`     | Jinja template resolved from the execution context for the first value.    |
| `value2`     | Jinja template resolved from the execution context for the second value.   |
| `options`    | Optional normalization options applied before comparison (see below).      |

### Options

| Option       | Type    | Default | Description                                                |
| ------------ | ------- | ------- | ---------------------------------------------------------- |
| `trim`       | boolean | `false` | Removes leading and trailing whitespace before comparison. |
| `ignoreCase` | boolean | `false` | Performs a case-insensitive comparison.                    |

## 🛠 Behavior

1. Each entry in `validation` is evaluated.
2. `value1` and `value2` are resolved from the execution context using Jinja templates.
3. Optional normalization is applied before comparison:
   - `trim`: removes leading and trailing whitespace.
   - `ignoreCase`: performs a case-insensitive comparison.
4. If the resolved values do not match, execution stops with a `400` error.
5. If `value1` or `value2` is `null`, execution stops with a `400` error.

## 🧪 Jinja Templates

Templates are rendered using [Jinjava](https://github.com/HubSpot/jinjava), a Jinja2-compatible template engine for Java.

Templates can access the full context via the variable names directly:

```jinja
"{{ firstName }}"
"{{ context.user.email | lower }}"
```

Available filters include `upper`, `lower`, `md5`, `url_encode`, etc.
