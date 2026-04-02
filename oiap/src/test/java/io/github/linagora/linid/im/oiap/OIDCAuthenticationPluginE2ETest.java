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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.plugin.config.dto.AuthenticationConfiguration;
import io.github.linagora.linid.im.corelib.plugin.task.TaskExecutionContext;
import io.github.linagora.linid.im.oiap.model.OIDCPluginConfigurationFactory;
import io.github.linagora.linid.im.oiap.processor.AccessTokenProcessorFactory;
import io.github.linagora.linid.im.oiap.processor.JWSProcessor;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * End-to-end tests for {@link OIDCAuthenticationPlugin}.
 *
 * <p>These tests require a running LemonLDAP::NG instance with PostgreSQL, started via:
 *
 * <pre>{@code
 * cd oiap/src/test/resources
 * docker compose up -d
 * }</pre>
 */
@DisplayName("Test class: OIDCAuthenticationPlugin E2E")
class OIDCAuthenticationPluginE2ETest {

  private static final String ISSUER_URI = "http://localhost:8080";
  private static final String CLIENT_ID = "linid-im-client";
  private static final String CLIENT_SECRET = "linid-im-secret";
  private static final String TOKEN_ENDPOINT = ISSUER_URI + "/oauth2/token";

  private static final List<String> REQUIRED_CLAIMS = List.of("sub", "email");
  private static final List<String> OPTIONAL_CLAIMS =
      List.of("preferred_username", "scope", "roles");

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private OIDCAuthenticationPlugin plugin;

  @BeforeAll
  static void checkLemonLdapAvailable() {
    try (var client = HttpClient.newHttpClient()) {
      var request =
          HttpRequest.newBuilder()
              .uri(URI.create(ISSUER_URI + "/.well-known/openid-configuration"))
              .GET()
              .build();
      var response = client.send(request, HttpResponse.BodyHandlers.ofString());
      Assumptions.assumeTrue(
          response.statusCode() == 200,
          "LemonLDAP not available (HTTP " + response.statusCode() + "). "
              + "Start it with: cd oiap/src/test/resources && docker compose up -d");
    } catch (Exception e) {
      Assumptions.assumeTrue(
          false,
          "LemonLDAP is not reachable at " + ISSUER_URI
              + ". Start it with: cd oiap/src/test/resources && docker compose up -d");
    }
  }

  @BeforeEach
  void setUp() {
    var configurationFactory = new OIDCPluginConfigurationFactory();
    var jwsProcessor = new JWSProcessor();
    var processorFactory = new AccessTokenProcessorFactory(List.of(jwsProcessor));
    plugin = new OIDCAuthenticationPlugin(configurationFactory, processorFactory);
  }

  @Test
  @DisplayName("test validateToken: should validate a real JWT and propagate claims")
  void testValidateTokenWithRealJwt() throws Exception {
    String accessToken = acquireAccessToken("dwho", "dwho");
    var context = new TaskExecutionContext();
    var request = requestWithToken(accessToken);

    assertDoesNotThrow(() -> plugin.validateToken(buildConfiguration(), request, context));

    @SuppressWarnings("unchecked")
    Map<String, Object> claims = (Map<String, Object>) context.get("claims");
    assertNotNull(claims, "Claims should be propagated to context");
    assertEquals("dwho", claims.get("sub"));
    assertEquals("dwho@badwolf.org", claims.get("email"));
    assertEquals("dwho", claims.get("preferred_username"));
    assertTrue(claims.containsKey("scope"));
    assertNotNull(claims.get("scope"));
    assertEquals("admin,user", claims.get("roles"));
  }

  @Test
  @DisplayName("test validateToken: should validate token for a different user")
  void testValidateTokenForDifferentUser() throws Exception {
    String accessToken = acquireAccessToken("testuser", "testuser");
    var context = new TaskExecutionContext();
    var request = requestWithToken(accessToken);

    assertDoesNotThrow(() -> plugin.validateToken(buildConfiguration(), request, context));

    @SuppressWarnings("unchecked")
    Map<String, Object> claims = (Map<String, Object>) context.get("claims");
    assertEquals("testuser", claims.get("sub"));
    assertEquals("testuser@example.com", claims.get("email"));
  }

  @Test
  @DisplayName("test validateToken: should throw 401 for a tampered JWT")
  void testValidateTokenThrow401ForTamperedJwt() throws Exception {
    String accessToken = acquireAccessToken("dwho", "dwho");
    String tamperedToken = accessToken.substring(0, accessToken.lastIndexOf('.')) + ".invalidsig";
    var request = requestWithToken(tamperedToken);

    ApiException exception =
        assertThrows(
            ApiException.class,
            () ->
                plugin.validateToken(
                    buildConfiguration(), request, new TaskExecutionContext()));

    assertEquals(HttpStatus.UNAUTHORIZED.value(), exception.getStatusCode());
    assertEquals("oiap.error.invalid.token", exception.getError().key());
  }

  @Test
  @DisplayName("test validateToken: should throw 401 for a completely invalid token")
  void testValidateTokenThrow401ForInvalidToken() {
    var request = requestWithToken("not.a.jwt");

    ApiException exception =
        assertThrows(
            ApiException.class,
            () ->
                plugin.validateToken(
                    buildConfiguration(), request, new TaskExecutionContext()));

    assertEquals(HttpStatus.UNAUTHORIZED.value(), exception.getStatusCode());
    assertEquals("oiap.error.invalid.token", exception.getError().key());
  }

  @Test
  @DisplayName("test validateToken: should throw 401 for an expired token")
  void testValidateTokenThrow401ForExpiredToken() {
    String expiredToken =
        "eyJhbGciOiJSUzI1NiIsInR5cCI6ImF0K2p3dCJ9"
            + ".eyJzdWIiOiJkd2hvIiwiZW1haWwiOiJkd2hvQGJhZHdvbGYub3JnIiwiZXhwIjoxMH0"
            + ".invalidsignature";
    var request = requestWithToken(expiredToken);

    ApiException exception =
        assertThrows(
            ApiException.class,
            () ->
                plugin.validateToken(
                    buildConfiguration(), request, new TaskExecutionContext()));

    assertEquals(HttpStatus.UNAUTHORIZED.value(), exception.getStatusCode());
    assertEquals("oiap.error.invalid.token", exception.getError().key());
  }

  @Test
  @DisplayName("test validateToken: should throw 500 for unreachable issuer")
  void testValidateTokenThrow500ForUnreachableIssuer() throws Exception {
    String accessToken = acquireAccessToken("dwho", "dwho");
    var request = requestWithToken(accessToken);
    var config = new AuthenticationConfiguration();
    config.addOption("issuerURI", "http://unreachable.invalid");
    config.addOption("audience", CLIENT_ID);
    config.addOption("tokenType", "JWS");
    config.addOption("requiredClaims", REQUIRED_CLAIMS);

    ApiException exception =
        assertThrows(
            ApiException.class,
            () -> plugin.validateToken(config, request, new TaskExecutionContext()));

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getStatusCode());
    assertEquals("oiap.error.jwt.processor.creation", exception.getError().key());
  }

  @Test
  @DisplayName("test validateToken: should throw 401 for wrong audience")
  void testValidateTokenThrow401ForWrongAudience() throws Exception {
    String accessToken = acquireAccessToken("dwho", "dwho");
    var request = requestWithToken(accessToken);
    var config = new AuthenticationConfiguration();
    config.addOption("issuerURI", ISSUER_URI);
    config.addOption("audience", "wrong-audience");
    config.addOption("tokenType", "JWS");
    config.addOption("requiredClaims", REQUIRED_CLAIMS);

    ApiException exception =
        assertThrows(
            ApiException.class,
            () -> plugin.validateToken(config, request, new TaskExecutionContext()));

    assertEquals(HttpStatus.UNAUTHORIZED.value(), exception.getStatusCode());
    assertEquals("oiap.error.invalid.token", exception.getError().key());
  }

  private static String acquireAccessToken(String username, String password)
      throws IOException, InterruptedException {
    var params = new StringJoiner("&");
    params.add("grant_type=password");
    params.add("username=" + URLEncoder.encode(username, StandardCharsets.UTF_8));
    params.add("password=" + URLEncoder.encode(password, StandardCharsets.UTF_8));
    params.add("client_id=" + URLEncoder.encode(CLIENT_ID, StandardCharsets.UTF_8));
    params.add("client_secret=" + URLEncoder.encode(CLIENT_SECRET, StandardCharsets.UTF_8));
    params.add("scope=" + URLEncoder.encode("openid email profile", StandardCharsets.UTF_8));

    try (var client = HttpClient.newHttpClient()) {
      var request =
          HttpRequest.newBuilder()
              .uri(URI.create(TOKEN_ENDPOINT))
              .header("Content-Type", "application/x-www-form-urlencoded")
              .POST(HttpRequest.BodyPublishers.ofString(params.toString()))
              .build();

      var response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        throw new RuntimeException(
            "Token request failed (HTTP " + response.statusCode() + "): " + response.body());
      }

      JsonNode json = OBJECT_MAPPER.readTree(response.body());
      return json.get("access_token").stringValue();
    }
  }

  private static HttpServletRequest requestWithToken(String token) {
    var request = Mockito.mock(HttpServletRequest.class);
    Mockito.when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
    return request;
  }

  private static AuthenticationConfiguration buildConfiguration() {
    var config = new AuthenticationConfiguration();
    config.addOption("issuerURI", ISSUER_URI);
    config.addOption("audience", CLIENT_ID);
    config.addOption("tokenType", "JWS");
    config.addOption("requiredClaims", REQUIRED_CLAIMS);
    config.addOption("optionalClaims", OPTIONAL_CLAIMS);
    return config;
  }
}
