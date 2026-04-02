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

- **`OIDCAuthenticationPlugin`**: Main entry point. Extracts the Bearer token from the request, delegates validation to
  the appropriate `AccessTokenProcessor`, checks required claims, and propagates extracted claims to the task execution
  context.
- **`OIDCPluginConfiguration`**: Immutable configuration record holding `issuerURI`, `audience`, `tokenType`,
  `requiredClaims` and `optionalClaims`.
- **`OIDCPluginConfigurationFactory`**: Factory that builds an `OIDCPluginConfiguration` from an
  `AuthenticationConfiguration`, validating that all required options are present.
- **`TokenType`**: Enum of supported access token formats: `OPAQUE`, `JWS`, `JWE`, `NESTED`.
- **`ErrorKey`**: Enum of i18n error keys used by the plugin.
- **`AccessTokenProcessor`**: Strategy interface for validating access tokens. Implementations determine whether they
  support a given `TokenType` and perform the actual validation, returning the token's claims on success.
- **`AccessTokenProcessorFactory`**: Factory that selects the appropriate `AccessTokenProcessor` for a given plugin
  configuration based on its `tokenType`.
- **`JWSProcessor`**: Implementation of `AccessTokenProcessor` for signed tokens (JWS). Resolves the OIDC provider
  metadata, configures the type verifier (accepting `at+jwt` and `application/at+jwt`), resolves signing keys from the
  provider's JWKS endpoint, sets up claims verification, and validates the token.

## Configuration

The plugin is identified by the type `oidc`.

```yaml
authentication:
  type: oidc
  options:
    issuerURI: https://auth.example.com/realms/my-realm
    audience: my-api
    tokenType: JWS
    requiredClaims:
      - sub
      - email
    optionalClaims:
      - preferred_username
      - scope
      - roles
```

### Configuration Fields

| Key              | Required | Description                                                                                                                                             |
|------------------|----------|---------------------------------------------------------------------------------------------------------------------------------------------------------|
| `issuerURI`      | Yes      | URI of the OIDC provider. Used to resolve provider metadata via discovery.                                                                              |
| `audience`       | Yes      | Expected `aud` claim value to enforce during JWT validation.                                                                                            |
| `tokenType`      | Yes      | Access token format. Determines which `AccessTokenProcessor` handles validation. Supported values: `JWS`, `JWE`, `OPAQUE`, `NESTED` (case-insensitive). |
| `requiredClaims` | Yes      | List of claim names that must be present and non-blank in the validated token. These claims are propagated into the task execution context.             |
| `optionalClaims` | No       | List of claim names that are propagated into the task execution context when present. Defaults to an empty list.                                        |

## Behavior

On each incoming request, the plugin performs the following steps:

1. Extract the Bearer token from the `Authorization` header (`Bearer <token>`).
2. Build an `OIDCPluginConfiguration` from the plugin options via `OIDCPluginConfigurationFactory`.
3. Select the `AccessTokenProcessor` matching the configured `tokenType` via `AccessTokenProcessorFactory`.
4. Validate the token using the selected processor:
    - For `JWS`: resolve the OIDC provider metadata from the discovery endpoint
      (`<issuerURI>/.well-known/openid-configuration`), fetch the signing keys from the provider's JWKS endpoint, verify
      the token type (`at+jwt` / `application/at+jwt` as per RFC 9068), validate the token signature, `iss`, `aud`, and
      `exp` claims.
5. Validate that all `requiredClaims` are present, non-null and non-blank in the token payload.
6. Propagate the subset of claims (`requiredClaims` + `optionalClaims`) to the task execution context under the key
   `claims`. Only claims with a non-null value are included in the propagated map.

### Error Handling

| Situation                                                                                      | HTTP Status | i18n Key                              |
|------------------------------------------------------------------------------------------------|-------------|---------------------------------------|
| Missing or malformed `Authorization` header                                                    | `401`       | `oiap.error.invalid.token`            |
| Empty Bearer token                                                                             | `401`       | `oiap.error.invalid.token`            |
| No processor supports the configured token type                                                | `401`       | `oiap.error.invalid.token`            |
| Token signature/type/expiration/issuer/audience invalid                                        | `401`       | `oiap.error.invalid.token`            |
| Required claim missing or blank                                                                | `401`       | `oiap.error.invalid.token`            |
| OIDC provider metadata or JWK Set cannot be resolved                                           | `500`       | `oiap.error.jwt.processor.creation`   |
| Required configuration option missing (`issuerURI`, `audience`, `tokenType`, `requiredClaims`) | `500`       | `error.plugin.default.missing.option` |
| Invalid `tokenType` value                                                                      | `500`       | `oiap.error.invalid.token.type`       |

## Notes

- The plugin makes a live HTTP call to the OIDC provider on every request to resolve metadata and fetch signing keys.
  Caching strategies will be implemented in the future to improve performance and reduce latency.
- Only signed tokens (JWS) are currently supported via `JWSProcessor`. Encrypted tokens (JWE), opaque tokens, and
  nested tokens are declared in `TokenType` but do not yet have processor implementations.
- Token validation errors (invalid signature, expired token, bad claims) result in a `401` response. Server-side
  configuration errors (unreachable OIDC provider, invalid JWK Set) result in a `500` response.
- Missing required configuration options result in a `500` response. An invalid `tokenType` value also results in a
  `500` response with the `oiap.error.invalid.token.type` error key.
- Claims outside the combined `requiredClaims` and `optionalClaims` lists are never propagated to the context.
- Validation is performed using the [Nimbus JOSE + JWT](https://connect2id.com/products/nimbus-jose-jwt) library.

## How to Run Tests

Unit tests use **JUnit 5** and **Mockito**.

Run unit tests with:

```bash
mvn test -pl oiap
```

End-to-end tests require a running LemonLDAP::NG instance:

```bash
docker compose -f oiap/src/test/resources/docker-compose.yml up -d
mvn test -pl oiap
```
