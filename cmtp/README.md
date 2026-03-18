# 🧩 Context Plugin (`cmtp`)

This module contains context-related plugins for LinID:

- **ContextMapperTaskPlugin** — a `TaskPlugin` that copies and transforms context values using Jinja templates.
- **ContextCompareValidationPlugin** — a `ValidationPlugin` that compares two configuration values with optional normalization.

---

## ContextMapperTaskPlugin

The `ContextMapperTaskPlugin` is a task plugin that copies and transforms values from one context key to one or more target keys using [Jinjava](https://github.com/HubSpot/jinjava) templates.

## ✅ Use Case

Use this plugin when:

- One task generates a context value under a key (`key1`)
- Another task requires that value under a different key (`key2`, `key3`, etc.)
- You want to optionally transform the value during the copy (e.g. add prefixes, suffixes, encode...)
- You want to inject keys into the context before the mapping step
- You want to clean up the context afterward by removing unused keys

## 🔧 Configuration

```yaml
- type: context-mapping
  name: myContextMapper
  adding:
    project: LinID
    env: '{{ env | upper }}'
  mapping:
    key1: key2,key3
    email: emailHash
  templates:
    key1.key2: '{{ key1 }}-suffix'
    key1.key3: 'prefix-{{ key1 }}'
    email.emailHash: '{{ email | md5 }}'
  default-template: '{{ context[inputKey] }}'
  removing:
    - obsoleteKey
    - temporaryData
```

### Configuration Fields

| Key                | Description                                                             |
| ------------------ | ----------------------------------------------------------------------- |
| `adding`           | A map of key-template pairs to inject into the context before mapping.  |
| `mapping`          | A map of input keys to one or more output keys (comma-separated).       |
| `templates`        | A map of transformation templates keyed by `inputKey.outputKey`.        |
| `default-template` | Template used when no specific template is defined for a mapping entry. |
| `removing`         | A list of context keys to remove after mapping has been applied.        |

## 🛠 Behavior

The plugin processes context values in the following order:

1. **Adding**:
   - Each entry in `adding` is rendered using Jinjava.
   - The entire context is available in the template as variables (e.g. `{{ env }}`).
   - The result is stored under the specified key in the context.

2. **Mapping**:
   - For each `inputKey` in `mapping`, its value is retrieved from the context.
   - For each corresponding `outputKey`, a template is applied:
     - If `templates[inputKey.outputKey]` is defined, it is used.
     - Otherwise, `default-template` is used.

   - Templates have access to the entire context.
   - If `inputKey` does not exist, its value is considered as the empty string (`""`).
   - The rendered result is stored in the context under `outputKey`.

3. **Removing**:
   - All keys listed in `removing` are deleted from the context if they exist.

## 🧪 Jinjava Templates

Templates are rendered using [Jinjava](https://github.com/HubSpot/jinjava), a Jinja2-compatible template engine for Java.

Templates can access the full context via the variable names directly:

```jinja
"{{ email | lower }}"
"Hello, {{ key1 }}"
"Env: {{ env | upper }}"
```

Or explicitly:

```jinja
"{{ key1 }}"
"{{ email }}"
```

Available filters include `upper`, `lower`, `md5`, `url_encode`, etc.

## 🧷 Notes

- Input keys not present in the context are treated as empty strings.
- All templates (`adding`, `templates`, `default-template`) can reference any context variable.
- You can use `adding` to prepare derived or default values before mapping.
- Comma-separated mappings allow copying a single key to multiple destinations.

---

## ContextCompareValidationPlugin

The `ContextCompareValidationPlugin` is a validation plugin that compares two string values resolved from the execution context using Jinja templates, with optional normalization.

### ✅ Use Case

Use this plugin when:

- You need to ensure two context-derived values match (e.g. the `firstName` sent in the request matches the one stored for the user).
- You want optional normalization (trim whitespace, case-insensitive comparison) before comparing.

### 🔧 Configuration

```yaml
validations:
  - type: context-compare
    value1: '{{ context.user.firstName }}'
    value2: '{{ context.request.firstName }}'
    trim: true
    ignoreCase: true
```

### Configuration Fields

| Key          | Type    | Required | Default | Description                                                |
| ------------ | ------- | -------- | ------- | ---------------------------------------------------------- |
| `value1`     | string  | yes      |         | Jinja template resolved from the execution context.        |
| `value2`     | string  | yes      |         | Jinja template resolved from the execution context.        |
| `trim`       | boolean | no       | `false` | Removes leading and trailing whitespace before comparison. |
| `ignoreCase` | boolean | no       | `false` | Performs a case-insensitive comparison.                    |

### 🛠 Behavior

1. `value1` and `value2` templates are resolved from the execution context using Jinja.
2. Optional normalization is applied before comparison:
   - `trim`: removes leading and trailing whitespace.
   - `ignoreCase`: performs a case-insensitive comparison.
3. If the values do not match, the validation fails with an error message.
4. If `value1` or `value2` is missing from the configuration, a `500` error is thrown.
