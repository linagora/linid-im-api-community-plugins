# DynamicListValidationPlugin

The `DynamicListValidationPlugin` is a dual plugin (validation + route) that validates attribute values and exposes structured elements from an external API.

## Use Case

Use this plugin when:

- You need to validate that a value belongs to a dynamic list fetched from an external API
- You want to expose a paginated endpoint returning structured `{ label, value }` elements (e.g., for frontend dropdowns)
- You want to enforce constraints that depend on external data (e.g., user types, categories from another system)

## Architecture

The plugin follows the HPP (HTTP Provider Plugin) pattern with clear separation of responsibilities:

- **`HttpService`**: Thin HTTP service that sends an HTTP request to the external API and returns the raw response body.
- **`DynamicListRoutePlugin`**: Route orchestrator that coordinates the HTTP call, task execution, element extraction, and pagination.
- **`DynamicListValidationPlugin`**: Validation orchestrator that coordinates the HTTP call, task execution, element extraction, and value checking.

### Phases

Task execution uses multiple phases, organized into three groups:

#### Token Validation (route plugin only)

- **`beforeTokenValidationDynamicList`**: Executed before token validation.
- **`afterTokenValidationDynamicList`**: Executed after token validation.

#### HTTP Call

- **`beforeDynamicList`**: Executed before the HTTP request to the external API.
- **`afterDynamicList`**: Executed after the HTTP response is received (e.g., JSON parsing).

#### Value Mapping

- **`beforeDynamicListMapping`**: Executed before value extraction from the parsed response.
- **`afterDynamicListMapping`**: Executed after value extraction (e.g., post-processing).

## Configuration

### Route Configuration

The route plugin exposes a dynamic endpoint that returns structured elements from the external API as a paginated response.

```yaml
routes:
  - name: dynamic-list
    route: /api/something
    url: https://external-api.com/api/personnes
    method: GET
    headers:
      Authorization: 'Bearer my-token'
    tasks:
      - type: json-parsing
        source: response
        destination: response
        phases:
          - beforeDynamicListMapping
    page: '{{ context.response.page }}'
    size: '{{ context.response.size }}'
    total: '{{ context.response.totalElements }}'
    itemsCount: '{{ context.response.content.size() }}'
    elementMapping:
      label: '{{ context.response.content[index].name }}'
      value: '{{ context.response.content[index].id }}'
```

### Validation Configuration (inline)

```yaml
entities:
  - name: user
    attributes:
      - name: type
        type: STRING
        validations:
          - name: typeValidation
            type: dynamic-list
            url: https://external-api.com/api/users/types
            method: POST
            headers:
              Authorization: 'Bearer my-token'
            body: '{"filter": ""}'
            tasks:
              - type: json-parsing
                source: response
                destination: response
                phases:
                  - beforeDynamicListMapping
            itemsCount: '{{ context.response.content.size() }}'
            elementMapping:
              label: '{{ context.response.content[index].name }}'
              value: '{{ context.response.content[index].id }}'
```

### Validation Configuration (global)

```yaml
validations:
  - name: typeValidation
    type: dynamic-list
    url: https://external-api.com/api/users/types
    method: GET
    tasks:
      - type: json-parsing
        source: response
        destination: response
        phases:
          - beforeDynamicListMapping
    itemsCount: '{{ context.response.content.size() }}'
    elementMapping:
      label: '{{ context.response.content[index].name }}'
      value: '{{ context.response.content[index].id }}'

entities:
  - name: user
    attributes:
      - name: type
        type: STRING
        validations:
          - name: typeValidation
```

### Configuration Fields

#### Route

| Key              | Required | Description                                                                                                                 |
| ---------------- | -------- | --------------------------------------------------------------------------------------------------------------------------- |
| `route`          | Yes      | The API route to expose (e.g., `/api/personnes`).                                                                           |
| `url`            | Yes      | Full URL of the external API to call. Supports Jinja templating.                                                            |
| `method`         | Yes      | HTTP method: `GET` or `POST` only.                                                                                          |
| `headers`        | No       | Optional HTTP headers to send with the request (e.g., `Authorization`).                                                     |
| `body`           | No       | Optional request body for POST requests. Supports Jinja templating.                                                         |
| `tasks`          | No       | List of tasks to execute at specific phases (e.g., `json-parsing`).                                                         |
| `page`           | Yes      | Current page number from response. Supports Jinja templating.                                                               |
| `size`           | Yes      | Page size from response. Supports Jinja templating.                                                                         |
| `total`          | Yes      | Total number of elements from response. Supports Jinja templating.                                                          |
| `itemsCount`     | Yes      | Number of items in the current page. Supports Jinja templating.                                                             |
| `elementMapping` | Yes      | Map of keys to Jinja templates for extracting structured elements per item. Must contain at least `label` and `value` keys. |

#### Validation

| Key              | Required | Description                                                                                                                 |
| ---------------- | -------- | --------------------------------------------------------------------------------------------------------------------------- |
| `url`            | Yes      | Full URL of the external API to call. Supports Jinja templating.                                                            |
| `method`         | Yes      | HTTP method: `GET` or `POST` only.                                                                                          |
| `headers`        | No       | Optional HTTP headers to send with the request (e.g., `Authorization`).                                                     |
| `body`           | No       | Optional request body for POST requests. Supports Jinja templating.                                                         |
| `tasks`          | No       | List of tasks to execute at specific phases (e.g., `json-parsing`).                                                         |
| `itemsCount`     | Yes      | Number of items in the response. Supports Jinja templating.                                                                 |
| `elementMapping` | Yes      | Map of keys to Jinja templates for extracting structured elements per item. Must contain at least `label` and `value` keys. |

## Behavior

- The plugin consists of two types:
  - **Validation plugin** (`type: dynamic-list`): validates that a value is included in the list fetched from the external API. Only the `value` field of each element is used for comparison.
  - **Route plugin** (`name: dynamic-list`): exposes a configurable endpoint (via `route`) returning structured elements as a paginated `Page<DynamicListEntry>` response, where `DynamicListEntry` is a record with `label` and `value` fields.

- Both plugins share a common HTTP service (`HttpService`) that sends an HTTP request to the configured `url` with optional `headers` and `body`, and returns the raw response body.

- The **route plugin** orchestration flow:
  1. Validate the authentication token via the authorization plugin (`beforeTokenValidationDynamicList` / `afterTokenValidationDynamicList`).
  2. Execute tasks at the `beforeDynamicList` phase.
  3. Send the HTTP request to the external API.
  4. Execute tasks at the `afterDynamicList` phase (e.g., `json-parsing` to parse JSON).
  5. Execute tasks at the `beforeDynamicListMapping` phase.
  6. Use Jinja templates (`itemsCount` + `elementMapping`) to extract structured elements from the parsed response.
  7. Execute tasks at the `afterDynamicListMapping` phase.

- The **validation plugin** orchestration flow:
  1. Execute tasks at the `beforeDynamicList` phase.
  2. Send the HTTP request to the external API.
  3. Execute tasks at the `afterDynamicList` phase (e.g., `json-parsing` to parse JSON).
  4. Execute tasks at the `beforeDynamicListMapping` phase.
  5. Use Jinja templates (`itemsCount` + `elementMapping`) to extract structured elements from the parsed response.
  6. Compare the input value against the `value` field of each extracted element.
  7. Execute tasks at the `afterDynamicListMapping` phase.

- Only `GET` and `POST` methods are supported. Any other method results in an error.
- JSON responses are parsed via the `json-parsing` task plugin (from the `jptp` module).

- Error handling:
  - External API 4xx errors are forwarded with the original status code.
  - External API 5xx errors return a `502` status.
  - Invalid JSON responses return a `500` status.

## Notes

- Templating uses Jinja (via `JinjaService`) to dynamically extract elements from the response.
- The `elementMapping` templates must use `index` to iterate over items (e.g., `{{ context.response.content[index].name }}`).
- The `elementMapping` must include `label` and `value` keys. These are the only keys used by the `DynamicListEntry` record.
- The route plugin returns `DynamicListEntry` records (`{ label, value }`); the validation plugin validates against `value` only.
- The validation is **case-sensitive**.
- `null` values are rejected by the validation.
- Each validation call makes a live HTTP request to the external API.
- The route endpoint is dynamic and configured via the `route` option (not hardcoded).

---

## How to Run Tests

To run the tests including the HTTP fake server:

1. Navigate to the fake HTTP server directory:

```bash
cd src/test/fake-http-server
```

2. Install the necessary Node.js dependencies:

```bash
npm install
```

3. Start the fake HTTP server:

```bash
npm run start
```

4. In another terminal, run the Maven tests:

```bash
mvn test
```

This setup launches a local fake HTTP server that your tests use to simulate HTTP API calls. Running `mvn test` then executes the unit tests and integration tests relying on this server.
