# 🌐 HttpProviderPlugin

The `HttpProviderPlugin` is a provider plugin designed to interact with configurable HTTP REST APIs, supporting full CRUD operations and response mapping into dynamic entities.

## ✅ Use Case

Use this plugin when you need to:

- Integrate a data source accessible via HTTP REST API.
- Dynamically configure endpoints, HTTP methods, and request bodies.
- Insert pre- and post-response mapping phases for custom processing.

## 🔄 Transforming HTTP Responses to JSON

The `HttpProviderPlugin` does not automatically transform raw HTTP responses into JSON objects.
If your API returns a raw response that must be parsed before entity mapping, you should use the [`JsonParsingTaskPlugin`](../jptp/README.md).

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
            'beforeResponseMappingCreate',
            'beforeResponseMappingUpdate',
            # "beforeResponseMappingPatch", -> this phase is unused
            'beforeResponseMappingFindById',
            # "beforeResponseMappingFindAll" -> this phase is unused
          ]
    access:
      create:
        uri: /api/users
        method: POST
        body: >
          {
            "name": "{{ entity.name }}"
          }
        entityMapping:
          id: { { response.id } }
      delete:
        uri: /api/users/{{ entity.id }}
        method: DELETE
        result: true
      findById:
        uri: /api/users/{{ entity.id }}
        method: GET
        entityMapping:
          id: { { response.id } }
      update:
        uri: /api/users/{{ entity.id }}
        method: PUT
        body: >
          {
            "name": "{{ entity.name }}"
          }
        entityMapping:
          id: { { response.id } }
```

### Full Example with Pagination and Mapping

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
         phases: ["beforeResponseMappingFindAll"]
    access:
      findAll:
        uri: /api/users
        method: GET
        page: {{ response.page }}
        size: {{ response.size }}
        total: {{ response.totalElements }}
        itemsCount: {{ response.elements.size() }}
        entityMapping:
          id: {{ response.elements[index].id }}
```

### Configuration Fields

| Key                                   | Required | Description                                                                                  |
| ------------------------------------- | -------- | -------------------------------------------------------------------------------------------- |
| `baseUrl`                             | ✅       | Base URL of the HTTP API                                                                     |
| `headers`                             | ❌       | Optional HTTP headers (e.g., `Content-Type`, `Authorization`)                                |
| `disabledRoutes`                      | ❌       | List of disabled actions for the entity (e.g., `patch`, `findAll`)                           |
| `access`                              | ❌       | Specific configuration for each CRUD action (`create`, `update`, `delete`, `findById`, etc.) |
| `uri`                                 | ✅       | Endpoint URI (supports Jinja templating)                                                     |
| `method`                              | ✅       | HTTP method (`GET`, `POST`, `PUT`, `PATCH`, `DELETE`)                                        |
| `body`                                | ❌       | HTTP request body (supports Jinja templating)                                                |
| `entityMapping`                       | ❌       | Maps fields from the HTTP response to the dynamic entity                                     |
| `result`                              | ❌       | Expression evaluated to verify success (e.g., for `delete`)                                  |
| `page`, `size`, `total`, `itemsCount` | ❌       | Pagination info for `findAll`; mapping can use `index` for iterating items.                  |

## 🛠 Behavior

- For each CRUD action, the plugin executes two lifecycle phases **before** and **after** the response mapping:
  - `beforeResponseMappingCreate` / `afterResponseMappingCreate`
  - `beforeResponseMappingUpdate` / `afterResponseMappingUpdate`
  - `beforeResponseMappingPatch` / `afterResponseMappingPatch`
  - `beforeResponseMappingFindById` / `afterResponseMappingFindById`
  - `beforeResponseMappingFindAll` / `afterResponseMappingFindAll`

- These phases allow inserting custom logic at precise points during entity processing.

- The plugin relies on external task plugins (like jptp's `json-parsing`) that can run during lifecycle phases to perform operations such as converting HTTP responses to JSON.

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
            'beforeResponseMappingCreate',
            'beforeResponseMappingUpdate',
            'beforeResponseMappingPatch',
            'beforeResponseMappingFindById',
            'beforeResponseMappingFindAll',
          ]
```

This `json-parsing` task (from the jptp plugin) converts raw HTTP responses into JSON before entity mapping. The `source` and `destination` options specify which context key to read from and write to.

## 🧷 Important Notes

- Templating uses Jinja (via `JinjaService`) to dynamically inject entity and response values.
- If a route is declared in `disabledRoutes`, the plugin ignores any corresponding `access` configuration.
- The `findAll` entity mapping must always use an `index` parameter to iterate over the response items.
- The plugin naturally integrates with the task engine to allow customized processing on responses.

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
