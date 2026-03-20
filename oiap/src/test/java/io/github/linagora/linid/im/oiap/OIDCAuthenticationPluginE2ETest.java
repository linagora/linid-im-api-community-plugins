/*
 * Copyright (C) 2020-2026 Linagora
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version, provided you comply with the Additional Terms applicable for LinID Identity Manager software by
 * LINAGORA pursuant to Section 7 of the GNU Affero General Public License, subsections (b), (c), and (e), pursuant to
 * which these Appropriate Legal Notices must notably (i) retain the display of the "LinID™" trademark/logo at the top
 * of the interface window, the display of the “You are using the Open Source and free version of LinID™, powered by
 * Linagora © 2009–2013. Contribute to LinID R&D by subscribing to an Enterprise offer!” infobox and in the e-mails
 * sent with the Program, notice appended to any type of outbound messages (e.g. e-mail and meeting requests) as well
 * as in the LinID Identity Manager user interface, (ii) retain all hypertext links between LinID Identity Manager
 * and https://linid.org/, as well as between LINAGORA and LINAGORA.com, and (iii) refrain from infringing LINAGORA
 * intellectual property rights over its trademarks and commercial brands. Other Additional Terms apply, see
 * <http://www.linagora.com/licenses/> for more details.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License and its applicable Additional Terms for
 * LinID Identity Manager along with this program. If not, see <http://www.gnu.org/licenses/> for the GNU Affero
 * General Public License version 3 and <http://www.linagora.com/licenses/> for the Additional Terms applicable to the
 * LinID Identity Manager software.
 */

package io.github.linagora.linid.im.oiap;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.plugin.config.dto.AuthenticationConfiguration;
import io.github.linagora.linid.im.corelib.plugin.task.TaskExecutionContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

@WireMockTest
@DisplayName("Test class: OIDCAuthenticationPlugin with e2e")
class OIDCAuthenticationPluginE2ETest {

  private static final OIDCAuthenticationPlugin PLUGIN = new OIDCAuthenticationPlugin();

  private static final String AUDIENCE = "my-backend-service";

  private static RSAKey rsaKey;

  private static final String MISSING_CLAIM = "oiap.error.claim.missing";

  @BeforeAll
  static void setup() throws Exception {
    rsaKey = new RSAKeyGenerator(2048).keyID("test-key").generate();
  }

  private void stubOIDCProvider(WireMockRuntimeInfo wmRuntimeInfo) {
    String issuerUrl = issuerUrl(wmRuntimeInfo);
    stubFor(get("/.well-known/openid-configuration").willReturn(okJson("""
        {
          "issuer": "%s",
          "authorization_endpoint": "%s/auth",
          "jwks_uri": "%s/jwks",
          "response_types_supported": ["code"],
          "subject_types_supported": ["public"],
          "id_token_signing_alg_values_supported": ["RS256"]
        }
        """.formatted(issuerUrl, issuerUrl, issuerUrl))));

    JWKSet jwkSet = new JWKSet(rsaKey.toPublicJWK());
    stubFor(get("/jwks").willReturn(okJson(jwkSet.toString())));
  }

  private String issuerUrl(WireMockRuntimeInfo wmRuntimeInfo) {
    return "http://localhost:" + wmRuntimeInfo.getHttpPort();
  }

  private String signToken(JWTClaimsSet claimsSet, String typ) throws Exception {
    JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
        .type(new JOSEObjectType(typ))
        .keyID("test-key")
        .build();
    SignedJWT signedJWT = new SignedJWT(header, claimsSet);
    signedJWT.sign(new RSASSASigner(rsaKey));
    return signedJWT.serialize();
  }

  private JWTClaimsSet validClaimsSet(WireMockRuntimeInfo wmRuntimeInfo) {
    return new JWTClaimsSet.Builder()
        .issuer(issuerUrl(wmRuntimeInfo))
        .audience(AUDIENCE)
        .subject("user123")
        .claim("email", "user@example.com")
        .expirationTime(new Date(System.currentTimeMillis() + 60000))
        .issueTime(new Date())
        .build();
  }

  private HttpServletRequest requestWithToken(String token) {
    var request = Mockito.mock(HttpServletRequest.class);
    Mockito.when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
    return request;
  }

  private AuthenticationConfiguration configurationFor(WireMockRuntimeInfo wmRuntimeInfo) {
    var configuration = new AuthenticationConfiguration();
    configuration.addOption("issuer-uri", issuerUrl(wmRuntimeInfo));
    configuration.addOption("audience", AUDIENCE);
    return configuration;
  }

  @Test
  @DisplayName("Test validateToken: should succeed and propagate claims when token is valid")
  void testValidateToken_shouldSucceedAndPropagateClaimsWhenTokenIsValid(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
    stubOIDCProvider(wmRuntimeInfo);
    String token = signToken(validClaimsSet(wmRuntimeInfo), "at+jwt");
    var context = new TaskExecutionContext();

    assertDoesNotThrow(() -> PLUGIN.validateToken(configurationFor(wmRuntimeInfo), requestWithToken(token), context));

    @SuppressWarnings("unchecked")
    Map<String, Object> claims = (Map<String, Object>) context.get("claims");
    assertNotNull(claims);
    assertEquals("user123", claims.get("sub"));
    assertEquals("user@example.com", claims.get("email"));
  }

  @Test
  @DisplayName("Test validateToken: should succeed when typ header is application/at+jwt")
  void testValidateToken_shouldSucceedWhenTypIsApplicationAtJwt(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
    stubOIDCProvider(wmRuntimeInfo);
    String token = signToken(validClaimsSet(wmRuntimeInfo), "application/at+jwt");

    assertDoesNotThrow(
        () -> PLUGIN.validateToken(configurationFor(wmRuntimeInfo), requestWithToken(token), new TaskExecutionContext()));
  }

  @Test
  @DisplayName("Test validateToken: should propagate all optional claims when all are present")
  void testValidateToken_shouldPropagateAllOptionalClaimsWhenAllArePresent(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
    stubOIDCProvider(wmRuntimeInfo);
    JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
        .issuer(issuerUrl(wmRuntimeInfo))
        .audience(AUDIENCE)
        .subject("user123")
        .claim("email", "user@example.com")
        .claim("preferred_username", "jdoe")
        .claim("scope", "openid profile")
        .claim("roles", "admin")
        .expirationTime(new Date(System.currentTimeMillis() + 60000))
        .issueTime(new Date())
        .build();
    String token = signToken(claimsSet, "at+jwt");
    var context = new TaskExecutionContext();

    assertDoesNotThrow(() -> PLUGIN.validateToken(configurationFor(wmRuntimeInfo), requestWithToken(token), context));

    @SuppressWarnings("unchecked")
    Map<String, Object> claims = (Map<String, Object>) context.get("claims");
    assertNotNull(claims);
    assertEquals("jdoe", claims.get("preferred_username"));
    assertEquals("openid profile", claims.get("scope"));
    assertEquals("admin", claims.get("roles"));
  }

  @Test
  @DisplayName("Test validateToken: should not propagate claims that are not in the allowed list")
  void testValidateToken_shouldNotPropagateClaimsOutsideAllowedList(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
    stubOIDCProvider(wmRuntimeInfo);
    JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
        .issuer(issuerUrl(wmRuntimeInfo))
        .audience(AUDIENCE)
        .subject("user123")
        .claim("email", "user@example.com")
        .claim("custom_claim", "sensitive-value")
        .claim("internal_id", "42")
        .expirationTime(new Date(System.currentTimeMillis() + 60000))
        .issueTime(new Date())
        .build();
    String token = signToken(claimsSet, "at+jwt");
    var context = new TaskExecutionContext();

    assertDoesNotThrow(() -> PLUGIN.validateToken(configurationFor(wmRuntimeInfo), requestWithToken(token), context));

    @SuppressWarnings("unchecked")
    Map<String, Object> claims = (Map<String, Object>) context.get("claims");
    assertNotNull(claims);
    assertFalse(claims.containsKey("custom_claim"));
    assertFalse(claims.containsKey("internal_id"));
  }

  @Test
  @DisplayName("Test validateToken: should propagate non-null optional claims and exclude null optional claims from context")
  void testValidateToken_shouldPropagateNonNullOptionalClaimsAndExcludeNullOnes(WireMockRuntimeInfo wmRuntimeInfo)
      throws Exception {
    stubOIDCProvider(wmRuntimeInfo);
    JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
        .issuer(issuerUrl(wmRuntimeInfo))
        .audience(AUDIENCE)
        .subject("user123")
        .claim("email", "user@example.com")
        .claim("preferred_username", "jdoe")
        .claim("scope", null)
        .claim("roles", null)
        .expirationTime(new Date(System.currentTimeMillis() + 60000))
        .issueTime(new Date())
        .build();
    String token = signToken(claimsSet, "at+jwt");
    var context = new TaskExecutionContext();

    assertDoesNotThrow(() -> PLUGIN.validateToken(configurationFor(wmRuntimeInfo), requestWithToken(token), context));

    @SuppressWarnings("unchecked")
    Map<String, Object> claims = (Map<String, Object>) context.get("claims");
    assertNotNull(claims);
    assertEquals("jdoe", claims.get("preferred_username"));
    assertFalse(claims.containsKey("scope"));
    assertFalse(claims.containsKey("roles"));
  }

  @Test
  @DisplayName("Test validateToken: should throw 401 when sub claim is missing")
  void testValidateToken_shouldThrow401WhenSubIsMissing(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
    stubOIDCProvider(wmRuntimeInfo);
    JWTClaimsSet claimsWithoutSub = new JWTClaimsSet.Builder()
        .issuer(issuerUrl(wmRuntimeInfo))
        .audience(AUDIENCE)
        .claim("email", "user@example.com")
        .expirationTime(new Date(System.currentTimeMillis() + 60000))
        .issueTime(new Date())
        .build();
    String token = signToken(claimsWithoutSub, "at+jwt");

    ApiException exception = assertThrows(ApiException.class,
        () -> PLUGIN.validateToken(configurationFor(wmRuntimeInfo), requestWithToken(token), new TaskExecutionContext()));

    assertEquals(HttpStatus.UNAUTHORIZED.value(), exception.getStatusCode());
    assertEquals(MISSING_CLAIM, exception.getError().key());
    assertEquals(Map.of("claim", "sub"), exception.getError().context());
  }

  @Test
  @DisplayName("Test validateToken: should throw 401 when email claim is missing")
  void testValidateToken_shouldThrow401WhenEmailIsMissing(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
    stubOIDCProvider(wmRuntimeInfo);
    JWTClaimsSet claimsWithoutEmail = new JWTClaimsSet.Builder()
        .issuer(issuerUrl(wmRuntimeInfo))
        .audience(AUDIENCE)
        .subject("user123")
        .expirationTime(new Date(System.currentTimeMillis() + 60000))
        .issueTime(new Date())
        .build();
    String token = signToken(claimsWithoutEmail, "at+jwt");

    ApiException exception = assertThrows(ApiException.class,
        () -> PLUGIN.validateToken(configurationFor(wmRuntimeInfo), requestWithToken(token), new TaskExecutionContext()));

    assertEquals(HttpStatus.UNAUTHORIZED.value(), exception.getStatusCode());
    assertEquals(MISSING_CLAIM, exception.getError().key());
    assertEquals(Map.of("claim", "email"), exception.getError().context());
  }

  @Test
  @DisplayName("Test validateToken: should throw 401 when sub claim is null")
  void testValidateToken_shouldThrow401WhenSubIsNull(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
    stubOIDCProvider(wmRuntimeInfo);
    JWTClaimsSet claimsWithNullSub = new JWTClaimsSet.Builder()
        .issuer(issuerUrl(wmRuntimeInfo))
        .audience(AUDIENCE)
        .subject(null)
        .claim("email", "user@example.com")
        .expirationTime(new Date(System.currentTimeMillis() + 60000))
        .issueTime(new Date())
        .build();
    String token = signToken(claimsWithNullSub, "at+jwt");

    ApiException exception = assertThrows(ApiException.class,
        () -> PLUGIN.validateToken(configurationFor(wmRuntimeInfo), requestWithToken(token), new TaskExecutionContext()));

    assertEquals(HttpStatus.UNAUTHORIZED.value(), exception.getStatusCode());
    assertEquals(MISSING_CLAIM, exception.getError().key());
    assertEquals(Map.of("claim", "sub"), exception.getError().context());
  }

  @Test
  @DisplayName("Test validateToken: should throw 401 when email claim is null")
  void testValidateToken_shouldThrow401WhenEmailIsNull(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
    stubOIDCProvider(wmRuntimeInfo);
    JWTClaimsSet claimsWithNullEmail = new JWTClaimsSet.Builder()
        .issuer(issuerUrl(wmRuntimeInfo))
        .audience(AUDIENCE)
        .subject("user123")
        .claim("email", null)
        .expirationTime(new Date(System.currentTimeMillis() + 60000))
        .issueTime(new Date())
        .build();
    String token = signToken(claimsWithNullEmail, "at+jwt");

    ApiException exception = assertThrows(ApiException.class,
        () -> PLUGIN.validateToken(configurationFor(wmRuntimeInfo), requestWithToken(token), new TaskExecutionContext()));

    assertEquals(HttpStatus.UNAUTHORIZED.value(), exception.getStatusCode());
    assertEquals(MISSING_CLAIM, exception.getError().key());
    assertEquals(Map.of("claim", "email"), exception.getError().context());
  }

  @Test
  @DisplayName("Test validateToken: should throw 401 when sub claim is blank")
  void testValidateToken_shouldThrow401WhenSubIsBlank(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
    stubOIDCProvider(wmRuntimeInfo);
    JWTClaimsSet claimsWithBlankSub = new JWTClaimsSet.Builder()
        .issuer(issuerUrl(wmRuntimeInfo))
        .audience(AUDIENCE)
        .subject("  ")
        .claim("email", "user@example.com")
        .expirationTime(new Date(System.currentTimeMillis() + 60000))
        .issueTime(new Date())
        .build();
    String token = signToken(claimsWithBlankSub, "at+jwt");

    ApiException exception = assertThrows(ApiException.class,
        () -> PLUGIN.validateToken(configurationFor(wmRuntimeInfo), requestWithToken(token), new TaskExecutionContext()));

    assertEquals(HttpStatus.UNAUTHORIZED.value(), exception.getStatusCode());
    assertEquals(MISSING_CLAIM, exception.getError().key());
    assertEquals(Map.of("claim", "sub"), exception.getError().context());
  }

  @Test
  @DisplayName("Test validateToken: should throw 401 when email claim is blank")
  void testValidateToken_shouldThrow401WhenEmailIsBlank(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
    stubOIDCProvider(wmRuntimeInfo);
    JWTClaimsSet claimsWithBlankEmail = new JWTClaimsSet.Builder()
        .issuer(issuerUrl(wmRuntimeInfo))
        .audience(AUDIENCE)
        .subject("user123")
        .claim("email", "  ")
        .expirationTime(new Date(System.currentTimeMillis() + 60000))
        .issueTime(new Date())
        .build();
    String token = signToken(claimsWithBlankEmail, "at+jwt");

    ApiException exception = assertThrows(ApiException.class,
        () -> PLUGIN.validateToken(configurationFor(wmRuntimeInfo), requestWithToken(token), new TaskExecutionContext()));

    assertEquals(HttpStatus.UNAUTHORIZED.value(), exception.getStatusCode());
    assertEquals(MISSING_CLAIM, exception.getError().key());
    assertEquals(Map.of("claim", "email"), exception.getError().context());
  }
}
