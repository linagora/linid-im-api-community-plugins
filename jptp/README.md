# JsonParsingTaskPlugin

The `JsonParsingTaskPlugin` is a task plugin that parses a JSON string from the execution context.

## Use Case

Use this plugin when:

- You need to convert a raw JSON string (e.g., an HTTP response body) into a parsed object for downstream processing.
- You want to decouple JSON parsing from transport logic (e.g., HTTP).
- You need configurable source and destination keys instead of hardcoded ones.

## Configuration

```yaml
- type: json-parsing
  source: response
  destination: response
```

### Configuration Fields

| Key           | Required | Description                                        |
| ------------- | -------- | -------------------------------------------------- |
| `source`      | Yes      | The context key containing the raw JSON string.    |
| `destination` | Yes      | The context key where the parsed result is stored. |

## Behavior

1. The plugin reads the value at the `source` key from the task execution context.
2. It parses the JSON string using Jackson.
3. It stores the parsed result at the `destination` key in the context.

- If the value at `source` is `null`, the plugin throws an error with status `500`.
- If the value at `source` is not valid JSON, the plugin throws an error with status `500`.

## Example

### In-place parsing

Parse the `response` key and replace it with the parsed result:

```yaml
tasks:
  - type: json-parsing
    source: response
    destination: response
    phases:
      - beforeResponseMapping
```

### Custom source and destination

Parse a raw body stored under `rawBody` and store the result under `parsedBody`:

```yaml
tasks:
  - type: json-parsing
    source: rawBody
    destination: parsedBody
    phases:
      - beforeResponseMapping
```

## Notes

- The plugin type identifier is `json-parsing`.
