# 🌐 Http Plugin (hpp)

The `hpp` module provides HTTP-related plugins for LinID. It currently includes:

- **HttpProviderPlugin**: A provider plugin for CRUD operations over HTTP REST APIs.
- **HttpTaskPlugin**: A task plugin for executing HTTP requests within task lifecycles.

## HttpProviderPlugin

The `HttpProviderPlugin` is a provider plugin designed to interact with configurable HTTP REST APIs, supporting full CRUD operations. Entity mapping from HTTP responses is handled by dedicated task plugins such as [`EntityMapperTaskPlugin`](../emtp/README.md).

## ✅ Use Case

Use this plugin when you need to:

- Integrate a data source accessible via HTTP REST API.
- Dynamically configure endpoints, HTTP methods, and request bodies.
- Insert pre- and post-response phases for custom processing.

## 🔄 Transforming HTTP Responses to JSON

The `HttpProviderPlugin` does not automatically transform raw HTTP responses into JSON objects.
If your API returns a raw response that must be parsed before processing, you should use the [`JsonParsingTaskPlugin`](../jptp/README.md).

For configuration details, see the [JsonParsingTaskPlugin documentation](../jptp/README.md).

## 🔧 Configuration

### Minimal Example

```yaml
providers:
  - type: http
    name: http
    baseUrl: https://myapi.com # mandatory

entities:
  - name: user
    provider: http
    route: users
    disabledRoutes: ['patch', 'findAll']
    tasks:
      - type: json-parsing
        source: response
        destination: response
        phases: [
            'afterResponseCreate',
            'afterResponseUpdate',
            'afterResponseFindById',
            # "afterResponseFindAll" -> this phase is unused
          ]
    access:
      create:
        uri: /api/users
        method: POST
        body: >
          {
            "name": "{{ entity.name }}"
          }
      delete:
        uri: /api/users/{{ entity.id }}
        method: DELETE
      findById:
        uri: /api/users/{{ entity.id }}
        method: GET
      update:
        uri: /api/users/{{ entity.id }}
        method: PUT
        body: >
          {
            "name": "{{ entity.name }}"
          }
```

### Full Example with Pagination

```yaml
entities:
  - name: user
    provider: http
    route: users
    disabledRoutes: ['create', 'update', 'patch', 'delete', 'findById']
    tasks:
      - type: json-parsing
        source: response
        destination: response
        phases: ['afterResponseFindAll']
    access:
      findAll:
        uri: /api/users
        method: GET
        page: { { response.page } }
        size: { { response.size } }
        total: { { response.totalElements } }
        itemsCount: { { response.elements.size() } }
```

### Full Example with Entity Mapping

```yaml
entities:
  - name: user
    provider: http
    route: users
    disabledRoutes: ['patch']
    tasks:
      - type: json-parsing
        source: response
        destination: response
        phases:
          - afterResponseCreate
          - afterResponseUpdate
          - afterResponseFindById
          - afterResponseFindAll
      - name: mapUserDetail
        type: entity-mapper
        mapping:
          id: '{{ context.response.id }}'
          name: '{{ context.response.name }}'
          email: '{{ context.response.email }}'
        phases:
          - afterCreate
          - afterUpdate
          - afterFindById
      - name: mapUserFindAll
        type: entity-mapper
        mapping:
          id: '{{ context.response.elements[context.index].id }}'
          name: '{{ context.response.elements[context.index].name }}'
          email: '{{ context.response.elements[context.index].email }}'
        phases:
          - afterFindAll
    access:
      create:
        uri: /api/users
        method: POST
        body: >
          {
            "name": "{{ entity.name }}",
            "email": "{{ entity.email }}"
          }
      findById:
        uri: /api/users/{{ entity.id }}
        method: GET
      update:
        uri: /api/users/{{ entity.id }}
        method: PUT
        body: >
          {
            "name": "{{ entity.name }}",
            "email": "{{ entity.email }}"
          }
      delete:
        uri: /api/users/{{ entity.id }}
        method: DELETE
      findAll:
        uri: /api/users
        method: GET
        page: '{{ response.page }}'
        size: '{{ response.size }}'
        total: '{{ response.totalElements }}'
        itemsCount: '{{ response.elements.size() }}'
```

### Configuration Fields

| Key                                   | Required | Description                                                                                  |
| ------------------------------------- | -------- | -------------------------------------------------------------------------------------------- |
| `baseUrl`                             | ✅       | Base URL of the HTTP API                                                                     |
| `headers`                             | ❌       | Optional HTTP headers (e.g., `Content-Type`, `Authorization`)                                |
| `disabledRoutes`                      | ❌       | List of disabled actions for the entity (e.g., `patch`, `findAll`)                           |
| `access`                              | ❌       | Specific configuration for each CRUD action (`create`, `update`, `delete`, `findById`, etc.) |
| `uri`                                 | ✅       | Endpoint URI (supports Jinja templating)                                                     |
| `method`                              | ✅       | HTTP method (`GET`, `POST`, `PUT`, `DELETE`)                                                 |
| `body`                                | ❌       | HTTP request body (supports Jinja templating)                                                |
| `page`, `size`, `total`, `itemsCount` | ❌       | Pagination info for `findAll`                                                                |

Voici un encadré clair que tu peux insérer dans ton README pour indiquer que **PATCH n’est plus supporté** :

---

## ⚠️ No PATCH Support

The `HttpProviderPlugin` **does not support the PATCH method**.

- Any configuration or task using `PATCH` will **throw an exception** if invoked.
- Use `POST` for creation or `PUT` for updates instead.
- Ensure that `patch` is included in `disabledRoutes` for your entities to prevent accidental usage:

```yaml
entities:
  - name: user
    provider: http
    route: users
    disabledRoutes: ['patch']
```

- If you previously relied on PATCH for partial updates, you should migrate your logic to use full `PUT` updates or a custom workflow.

This explicitly prevents runtime errors and ensures consistency across all entity operations.

## 🛠 Behavior

- For each CRUD action, the plugin executes a lifecycle phase **after** receiving the HTTP response:
  - `afterResponseCreate`
  - `afterResponseUpdate`
  - `afterResponseFindById`
  - `afterResponseFindAll`

- These phases allow inserting custom logic (e.g., JSON parsing via jptp) after the HTTP response is received and before the result is returned to the service layer.

- The provider returns entities with **empty attributes**. The [`EntityMapperTaskPlugin`](../emtp/README.md) (emtp) **must** be configured on the generic service-level phases (`afterCreate`, `afterUpdate`, `afterFindById`, `afterFindAll`) to populate entity attributes from the HTTP response stored in the execution context.

- For `findAll`, the service iterates over each entity in the page and executes `afterFindAll` with a `context.index` value, allowing emtp to map array elements (e.g., `context.response.elements[context.index].id`).

### Example of a Task plugin configuration

```yaml
entities:
  - name: user
    provider: http
    route: users
    disabledRoutes: ['patch', 'findAll']
    tasks:
      - type: json-parsing
        source: response
        destination: response
        phases:
          [
            'afterResponseCreate',
            'afterResponseUpdate',
            'afterResponseFindById',
            'afterResponseFindAll',
          ]
```

This `json-parsing` task (from the jptp plugin) converts raw HTTP responses into JSON before further processing. The `source` and `destination` options specify which context key to read from and write to.

## 🧷 Important Notes

- Templating uses Jinja (via `JinjaService`) to dynamically inject entity and response values.
- If a route is declared in `disabledRoutes`, the plugin ignores any corresponding `access` configuration.
- Entity mapping from HTTP responses should be handled by the [`EntityMapperTaskPlugin`](../emtp/README.md) configured on generic service-level phases (`afterCreate`, `afterUpdate`, `afterFindById`, `afterFindAll`).
- The plugin naturally integrates with the task engine to allow customized processing on responses.

---

## HttpTaskPlugin

The `HttpTaskPlugin` is a task plugin that executes HTTP requests defined in task configuration. It allows performing HTTP calls at any point in the task lifecycle.

### ✅ Use Case

Use this plugin when you need to:

- Execute an HTTP call as a side-effect during entity processing (e.g., notify an external system).
- Fetch data from an external API and store it in the execution context for subsequent tasks.
- Chain HTTP calls with other task plugins (e.g., `json-parsing`, `context-mapping`).

### 🔧 Configuration

```yaml
tasks:
  - type: http
    name: call-external-api
    url: 'http://example.com/api/resource'
    method: 'POST'
    headers:
      Content-Type: application/json
    body: '{"key": "{{ entity.value }}"}'
    phases: ['beforeCreate']
```

### Configuration Fields

| Key       | Required | Description                                       |
| --------- | -------- | ------------------------------------------------- |
| `url`     | ✅       | Full URL to call (supports Jinja templating)      |
| `method`  | ✅       | HTTP method (`GET`, `POST`, `PUT`, `DELETE`)      |
| `headers` | ❌       | Optional HTTP headers (e.g., `Content-Type`)      |
| `body`    | ❌       | Optional request body (supports Jinja templating) |

### 🛠 Behavior

- The plugin executes the configured HTTP request.
- The raw HTTP response is stored in the execution context under the key `response`.
- The `url` and `body` fields support Jinja templating, allowing dynamic values based on entity attributes and context.
- HTTP errors (4xx, 5xx) throw an `ApiException` with the corresponding `hpp.error*` i18n key.

### Example: Chaining with json-parsing

```yaml
tasks:
  - type: http
    name: fetch-data
    url: 'http://api.example.com/data/{{ context.id }}'
    method: GET
    phases: ['afterResponseFindById']
  - type: json-parsing
    name: parse-data
    source: response
    destination: response
    phases: ['afterResponseFindById']
```

---

## 🧪 How to Run Tests

To run the tests including the HTTP fake server:

1. Navigate to the fake HTTP server directory:

```bash
cd src/test/fake-http-server
```

2. Install the necessary Node.js dependencies:

```bash
npm install
```

3. Start the fake HTTP server in the background:

```bash
npm run start &
```

4. In another terminal (or after starting the server), run the Maven tests:

```bash
mvn test
```

This setup launches a local fake HTTP server that your tests use to simulate HTTP API calls. Running `mvn test` then executes the unit tests and integration tests relying on this server.
