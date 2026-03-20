# OIDCAuthenticationPlugin

The `OIDCAuthenticationPlugin` is an authentication plugin that validates OAuth2 JWT access tokens issued by an OpenID
Connect provider.

Conforms to [RFC 9068 — JWT Profile for OAuth 2.0 Access Tokens](https://datatracker.ietf.org/doc/html/rfc9068).

## Use Case

Use this plugin when:

- You need to protect routes by validating Bearer tokens issued by an OIDC provider (e.g., LemonLdap, Keycloak, Okta,
  Auth0, etc.)
- You want to enforce audience and issuer constraints on incoming JWT access tokens
- You want to propagate user identity claims (subject, email, roles, etc.) into the task execution context for
  downstream use

## Architecture

The plugin is composed of the following classes:

- **`OIDCAuthenticationPlugin`**: Main entry point. Extracts the Bearer token from the request, builds the JWT
  processor, validates the token, checks required claims, and propagates extracted claims to the task execution context.
- **`OIDCPluginConfiguration`**: Immutable configuration record. Reads and validates `issuer-uri` and `audience` from
  the plugin options.
- **`JWTProcessorConfigurer`**: Strategy interface for configuring a `ConfigurableJWTProcessor`. Provides a shared
  `configureClaimsVerifier` method that enforces issuer, audience, expiration, and required JWT claims.
- **`JWSProcessorConfigurer`**: Implementation of `JWTProcessorConfigurer` for signed tokens (JWS). Configures the type
  verifier (accepting `at+jwt` and `application/at+jwt`) and resolves signing keys from the OIDC provider's JWKS
  endpoint.

## Configuration

The plugin is identified by the type `oiap`.

```yaml
authentication:
  type: oiap
  options:
    issuer-uri: https://auth.example.com/realms/my-realm
    audience: my-api
```

### Configuration Fields

| Key          | Required | Description                                                                |
|--------------|----------|----------------------------------------------------------------------------|
| `issuer-uri` | Yes      | URI of the OIDC provider. Used to resolve provider metadata via discovery. |
| `audience`   | Yes      | Expected `aud` claim value to enforce during JWT validation.               |

## Behavior

On each incoming request, the plugin performs the following steps:

1. Extract the Bearer token from the `Authorization` header (`Bearer <token>`).
2. Read `issuer-uri` and `audience` from the plugin configuration.
3. Resolve the OIDC provider metadata from the discovery endpoint (`<issuer-uri>/.well-known/openid-configuration`).
4. Fetch the signing keys from the provider's JWKS endpoint.
5. Verify the token type — only `at+jwt` and `application/at+jwt` are accepted (as per RFC 9068).
6. Validate the token signature, `iss`, `aud`, and `exp` claims.
7. Validate that `sub` and `email` are present, non-null and non-blank in the token payload.
8. Propagate the extracted claims to the task execution context under the key `claims`.

### Propagated Claims

The following claims are extracted from the token and stored in `context["claims"]`:

| Claim                | Required | Description                         |
|----------------------|----------|-------------------------------------|
| `sub`                | Yes      | Subject identifier.                 |
| `email`              | Yes      | User email address.                 |
| `preferred_username` | No       | Preferred username of the user.     |
| `scope`              | No       | OAuth2 scopes granted to the token. |
| `roles`              | No       | Roles assigned to the user.         |

Only claims with a non-null value are included in the propagated map.

### Error Handling

| Situation                                                       | HTTP Status | i18n Key                              | Message                                                     |
|-----------------------------------------------------------------|-------------|---------------------------------------|-------------------------------------------------------------|
| Missing or malformed `Authorization` header                     | `401`       | `oiap.error.token.invalid`            | The provided access token is invalid.                       |
| Token signature/type/expiration/issuer/audience invalid         | `401`       | `oiap.error.token.invalid`            | The provided access token is invalid.                       |
| OIDC provider metadata cannot be resolved                       | `401`       | `oiap.error.token.invalid`            | The provided access token is invalid.                       |
| Required claim missing or blank (`sub`, `email`)                | `401`       | `oiap.error.claim.missing`            | The required claim '{{ claim }}' is missing from the token. |
| Required configuration option missing (`issuer-uri`,`audience`) | `500`       | `error.plugin.default.missing.option` | —                                                           |

## Notes

- The plugin makes a live HTTP call to the OIDC provider on every request to resolve metadata and fetch signing keys.
  Caching strategies will be implemented in the future to improve performance and reduce latency.
- Only signed tokens (JWS) are currently supported. Encrypted tokens (JWE) are not.
- Token validation is strict: any error during metadata resolution, signature verification, or claims checking results
  in a `401` response. This choice is intentional to avoid leaking information about the validity of the token or the
  existence of certain claims.
- Missing `issuer-uri` or `audience` configuration options result in a `500` response.
- Claims outside the allowed list (`sub`, `email`, `preferred_username`, `scope`, `roles`) are never propagated to the
  context.
- Validation is performed using the [Nimbus JOSE + JWT](https://connect2id.com/products/nimbus-jose-jwt) library.

## How to Run Tests

Unit tests and end-to-end tests use **JUnit 5**, **Mockito**, and **WireMock** (embedded — no external server required).

Run all tests with:

```bash
mvn test
```
