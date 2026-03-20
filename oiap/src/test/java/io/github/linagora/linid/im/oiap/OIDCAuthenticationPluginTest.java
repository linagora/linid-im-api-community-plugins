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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.plugin.config.dto.AuthenticationConfiguration;
import io.github.linagora.linid.im.corelib.plugin.task.TaskExecutionContext;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

@DisplayName("Test class: OIDCAuthenticationPlugin")
class OIDCAuthenticationPluginTest {

  private static final OIDCAuthenticationPlugin PLUGIN = new OIDCAuthenticationPlugin();

  private static final String MISSING_OPTION = "error.plugin.default.missing.option";
  private static final String INVALID_TOKEN = "oiap.error.token.invalid";

  @Test
  @DisplayName("Test supports: should return true for oiap type")
  void testSupports_shouldReturnTrueForOiapType() {
    assertTrue(PLUGIN.supports("oiap"));
  }

  @Test
  @DisplayName("Test supports: should return false for other types")
  void testSupports_shouldReturnFalseForOtherTypes() {
    assertFalse(PLUGIN.supports("ldap"));
    assertFalse(PLUGIN.supports("other"));
    assertFalse(PLUGIN.supports(""));
  }

  @Test
  @DisplayName("Test validateToken: should throw 401 when Authorization header is missing")
  void testValidateToken_shouldThrow401WhenAuthorizationHeaderIsMissing() {
    var request = Mockito.mock(HttpServletRequest.class);
    Mockito.when(request.getHeader("Authorization")).thenReturn(null);

    ApiException exception = assertThrows(ApiException.class,
        () -> PLUGIN.validateToken(new AuthenticationConfiguration(), request, new TaskExecutionContext()));

    assertEquals(HttpStatus.UNAUTHORIZED.value(), exception.getStatusCode());
    assertEquals(INVALID_TOKEN, exception.getError().key());
  }

  @Test
  @DisplayName("Test validateToken: should throw 401 when Authorization header does not start with Bearer")
  void testValidateToken_shouldThrow401WhenAuthorizationHeaderHasInvalidFormat() {
    var request = Mockito.mock(HttpServletRequest.class);
    Mockito.when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

    ApiException exception = assertThrows(ApiException.class,
        () -> PLUGIN.validateToken(new AuthenticationConfiguration(), request, new TaskExecutionContext()));

    assertEquals(HttpStatus.UNAUTHORIZED.value(), exception.getStatusCode());
    assertEquals(INVALID_TOKEN, exception.getError().key());
  }

  @Test
  @DisplayName("Test validateToken: should throw 401 when Bearer token is blank")
  void testValidateToken_shouldThrow401WhenBearerTokenIsBlank() {
    var request = Mockito.mock(HttpServletRequest.class);
    Mockito.when(request.getHeader("Authorization")).thenReturn("Bearer ");

    ApiException exception = assertThrows(ApiException.class,
        () -> PLUGIN.validateToken(new AuthenticationConfiguration(), request, new TaskExecutionContext()));

    assertEquals(HttpStatus.UNAUTHORIZED.value(), exception.getStatusCode());
    assertEquals(INVALID_TOKEN, exception.getError().key());
  }

  @Test
  @DisplayName("Test validateToken: should throw 500 when issuer-uri is missing from configuration")
  void testValidateToken_shouldThrow500WhenIssuerURIIsMissing() {
    var request = Mockito.mock(HttpServletRequest.class);
    Mockito.when(request.getHeader("Authorization")).thenReturn("Bearer some.fake.token");
    var configuration = new AuthenticationConfiguration();
    configuration.addOption("audience", "my-backend-service");

    ApiException exception = assertThrows(ApiException.class,
        () -> PLUGIN.validateToken(configuration, request, new TaskExecutionContext()));

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getStatusCode());
    assertEquals(MISSING_OPTION, exception.getError().key());
  }

  @Test
  @DisplayName("Test validateToken: should throw 500 when audience is missing from configuration")
  void testValidateToken_shouldThrow500WhenAudienceIsMissing() {
    var request = Mockito.mock(HttpServletRequest.class);
    Mockito.when(request.getHeader("Authorization")).thenReturn("Bearer some.fake.token");
    var configuration = new AuthenticationConfiguration();
    configuration.addOption("issuer-uri", "https://auth.example.com");

    ApiException exception = assertThrows(ApiException.class,
        () -> PLUGIN.validateToken(configuration, request, new TaskExecutionContext()));

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getStatusCode());
    assertEquals(MISSING_OPTION, exception.getError().key());
  }
}
