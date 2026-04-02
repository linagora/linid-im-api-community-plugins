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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import io.github.linagora.linid.im.corelib.plugin.config.dto.AuthenticationConfiguration;
import io.github.linagora.linid.im.corelib.plugin.task.TaskExecutionContext;
import io.github.linagora.linid.im.oiap.model.OIDCPluginConfiguration;
import io.github.linagora.linid.im.oiap.model.OIDCPluginConfigurationFactory;
import io.github.linagora.linid.im.oiap.model.TokenType;
import io.github.linagora.linid.im.oiap.processor.AccessTokenProcessor;
import io.github.linagora.linid.im.oiap.processor.AccessTokenProcessorFactory;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

@DisplayName("Test class: OIDCAuthenticationPlugin")
class OIDCAuthenticationPluginTest {

  private static final String INVALID_TOKEN = "oiap.error.invalid.token";

  private static final OIDCPluginConfiguration DEFAULT_CONFIG =
      new OIDCPluginConfiguration(
          "https://auth.example.com",
          "my-audience",
          TokenType.JWS,
          List.of("sub", "email"),
          List.of("preferred_username", "scope", "roles"));

  private OIDCPluginConfigurationFactory configurationFactory;
  private AccessTokenProcessorFactory processorFactory;
  private AccessTokenProcessor processor;
  private OIDCAuthenticationPlugin plugin;

  @BeforeEach
  void setUp() {
    configurationFactory = Mockito.mock(OIDCPluginConfigurationFactory.class);
    processorFactory = Mockito.mock(AccessTokenProcessorFactory.class);
    processor = Mockito.mock(AccessTokenProcessor.class);
    Mockito.when(configurationFactory.create(Mockito.any())).thenReturn(DEFAULT_CONFIG);
    Mockito.when(processorFactory.getProcessor(Mockito.any())).thenReturn(processor);
    plugin = new OIDCAuthenticationPlugin(configurationFactory, processorFactory);
  }

  @Test
  @DisplayName("test supports: should return true for oidc type")
  void testSupportsOidcType() {
    assertTrue(plugin.supports("oidc"));
  }

  @Test
  @DisplayName("test supports: should return false for other types")
  void testSupportsOtherTypes() {
    assertFalse(plugin.supports("ldap"));
    assertFalse(plugin.supports("other"));
    assertFalse(plugin.supports(""));
  }

  @Test
  @DisplayName("test supports: should return false for null type")
  void testSupportsNull() {
    assertFalse(plugin.supports(null));
  }

  @Test
  @DisplayName("test validateToken: should throw 401 when Authorization header is missing")
  void testValidateTokenThrow401WhenAuthorizationHeaderIsMissing() {
    var request = Mockito.mock(HttpServletRequest.class);
    Mockito.when(request.getHeader("Authorization")).thenReturn(null);

    ApiException exception =
        assertThrows(
            ApiException.class,
            () ->
                plugin.validateToken(
                    new AuthenticationConfiguration(), request, new TaskExecutionContext()));

    assertEquals(HttpStatus.UNAUTHORIZED.value(), exception.getStatusCode());
    assertEquals(INVALID_TOKEN, exception.getError().key());
  }

  @Test
  @DisplayName(
      "test validateToken: should throw 401 when Authorization header does not start with Bearer")
  void testValidateTokenThrow401WhenAuthorizationHeaderHasInvalidFormat() {
    var request = Mockito.mock(HttpServletRequest.class);
    Mockito.when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

    ApiException exception =
        assertThrows(
            ApiException.class,
            () ->
                plugin.validateToken(
                    new AuthenticationConfiguration(), request, new TaskExecutionContext()));

    assertEquals(HttpStatus.UNAUTHORIZED.value(), exception.getStatusCode());
    assertEquals(INVALID_TOKEN, exception.getError().key());
  }

  @Test
  @DisplayName("test validateToken: should throw 401 when Bearer token is blank")
  void testValidateTokenThrow401WhenBearerTokenIsBlank() {
    var request = Mockito.mock(HttpServletRequest.class);
    Mockito.when(request.getHeader("Authorization")).thenReturn("Bearer ");

    ApiException exception =
        assertThrows(
            ApiException.class,
            () ->
                plugin.validateToken(
                    new AuthenticationConfiguration(), request, new TaskExecutionContext()));

    assertEquals(HttpStatus.UNAUTHORIZED.value(), exception.getStatusCode());
    assertEquals(INVALID_TOKEN, exception.getError().key());
  }

  @Test
  @DisplayName("test validateToken: should accept Bearer prefix regardless of case")
  void testValidateTokenAcceptsCaseInsensitiveBearerPrefix() {
    Mockito.when(processor.process(Mockito.eq("some.token"), Mockito.any()))
        .thenReturn(Map.of("sub", "user123", "email", "user@example.com"));

    var request = Mockito.mock(HttpServletRequest.class);
    Mockito.when(request.getHeader("Authorization")).thenReturn("bearer some.token");
    var context = new TaskExecutionContext();

    assertDoesNotThrow(
        () -> plugin.validateToken(new AuthenticationConfiguration(), request, context));
  }

  @Test
  @DisplayName("test validateToken: should throw 401 when a required claim is missing")
  void testValidateTokenThrow401WhenRequiredClaimIsMissing() {
    Mockito.when(processor.process(Mockito.anyString(), Mockito.any()))
        .thenReturn(Map.of("email", "user@example.com"));

    var request = requestWithToken("some.token");

    ApiException exception =
        assertThrows(
            ApiException.class,
            () ->
                plugin.validateToken(
                    new AuthenticationConfiguration(), request, new TaskExecutionContext()));

    assertEquals(HttpStatus.UNAUTHORIZED.value(), exception.getStatusCode());
    assertEquals(INVALID_TOKEN, exception.getError().key());
  }

  @Test
  @DisplayName("test validateToken: should throw 401 when a required claim is null")
  void testValidateTokenThrow401WhenRequiredClaimIsNull() {
    Map<String, Object> claims = new HashMap<>();
    claims.put("sub", null);
    claims.put("email", "user@example.com");
    Mockito.when(processor.process(Mockito.anyString(), Mockito.any())).thenReturn(claims);

    var request = requestWithToken("some.token");

    ApiException exception =
        assertThrows(
            ApiException.class,
            () ->
                plugin.validateToken(
                    new AuthenticationConfiguration(), request, new TaskExecutionContext()));

    assertEquals(HttpStatus.UNAUTHORIZED.value(), exception.getStatusCode());
    assertEquals(INVALID_TOKEN, exception.getError().key());
  }

  @Test
  @DisplayName("test validateToken: should throw 401 when a required claim is blank")
  void testValidateTokenThrow401WhenRequiredClaimIsBlank() {
    Mockito.when(processor.process(Mockito.anyString(), Mockito.any()))
        .thenReturn(Map.of("sub", "", "email", "user@example.com"));

    var request = requestWithToken("some.token");

    ApiException exception =
        assertThrows(
            ApiException.class,
            () ->
                plugin.validateToken(
                    new AuthenticationConfiguration(), request, new TaskExecutionContext()));

    assertEquals(HttpStatus.UNAUTHORIZED.value(), exception.getStatusCode());
    assertEquals(INVALID_TOKEN, exception.getError().key());
  }

  @Test
  @DisplayName(
      "test validateToken: should propagate required claims to context when token is valid")
  void testValidateTokenPropagateRequiredClaimsToContext() {
    Mockito.when(processor.process(Mockito.anyString(), Mockito.any()))
        .thenReturn(Map.of("sub", "user123", "email", "user@example.com"));

    var context = new TaskExecutionContext();
    var request = requestWithToken("some.token");

    assertDoesNotThrow(
        () -> plugin.validateToken(new AuthenticationConfiguration(), request, context));

    @SuppressWarnings("unchecked")
    Map<String, Object> claims = (Map<String, Object>) context.get("claims");
    assertEquals("user123", claims.get("sub"));
    assertEquals("user@example.com", claims.get("email"));
  }

  @Test
  @DisplayName("test validateToken: should propagate optional claims when present in token")
  void testValidateTokenPropagateOptionalClaimsWhenPresent() {
    Mockito.when(processor.process(Mockito.anyString(), Mockito.any()))
        .thenReturn(
            Map.of(
                "sub", "user123",
                "email", "user@example.com",
                "preferred_username", "jdoe",
                "scope", "openid profile",
                "roles", "admin"));

    var context = new TaskExecutionContext();
    var request = requestWithToken("some.token");

    assertDoesNotThrow(
        () -> plugin.validateToken(new AuthenticationConfiguration(), request, context));

    @SuppressWarnings("unchecked")
    Map<String, Object> claims = (Map<String, Object>) context.get("claims");
    assertEquals("jdoe", claims.get("preferred_username"));
    assertEquals("openid profile", claims.get("scope"));
    assertEquals("admin", claims.get("roles"));
  }

  @Test
  @DisplayName("test validateToken: should not propagate claims outside the allowed list")
  void testValidateTokenNotPropagateClaimsOutsideAllowedList() {
    Mockito.when(processor.process(Mockito.anyString(), Mockito.any()))
        .thenReturn(
            Map.of(
                "sub", "user123",
                "email", "user@example.com",
                "custom_claim", "sensitive-value"));

    var context = new TaskExecutionContext();
    var request = requestWithToken("some.token");

    assertDoesNotThrow(
        () -> plugin.validateToken(new AuthenticationConfiguration(), request, context));

    @SuppressWarnings("unchecked")
    Map<String, Object> claims = (Map<String, Object>) context.get("claims");
    assertFalse(claims.containsKey("custom_claim"));
  }

  @Test
  @DisplayName("test validateToken: should propagate ApiException 401 when processor rejects token")
  void testValidateTokenPropagate401WhenProcessorRejectsToken() {
    Mockito.when(processor.process(Mockito.anyString(), Mockito.any()))
        .thenThrow(
            new ApiException(HttpStatus.UNAUTHORIZED.value(), I18nMessage.of(INVALID_TOKEN)));

    var request = requestWithToken("some.token");

    ApiException exception =
        assertThrows(
            ApiException.class,
            () ->
                plugin.validateToken(
                    new AuthenticationConfiguration(), request, new TaskExecutionContext()));

    assertEquals(HttpStatus.UNAUTHORIZED.value(), exception.getStatusCode());
    assertEquals(INVALID_TOKEN, exception.getError().key());
  }

  @Test
  @DisplayName(
      "test validateToken: should propagate ApiException 500 when processor fails to configure")
  void testValidateTokenPropagate500WhenProcessorFailsToConfigure() {
    Mockito.when(processor.process(Mockito.anyString(), Mockito.any()))
        .thenThrow(
            new ApiException(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                I18nMessage.of("oiap.error.jwt.processor.creation")));

    var request = requestWithToken("some.token");

    ApiException exception =
        assertThrows(
            ApiException.class,
            () ->
                plugin.validateToken(
                    new AuthenticationConfiguration(), request, new TaskExecutionContext()));

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getStatusCode());
  }

  private HttpServletRequest requestWithToken(String token) {
    var request = Mockito.mock(HttpServletRequest.class);
    Mockito.when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
    return request;
  }
}
