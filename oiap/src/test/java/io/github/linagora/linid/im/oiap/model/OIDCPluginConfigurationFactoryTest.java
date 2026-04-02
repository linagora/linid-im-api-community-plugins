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

package io.github.linagora.linid.im.oiap.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.plugin.config.dto.AuthenticationConfiguration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import tools.jackson.core.type.TypeReference;

@DisplayName("Test class: OIDCPluginConfigurationFactory")
class OIDCPluginConfigurationFactoryTest {

  private final OIDCPluginConfigurationFactory factory = new OIDCPluginConfigurationFactory();

  @Test
  @DisplayName("test create: should return valid configuration when all options are present")
  void testCreateWithAllOptions() {
    AuthenticationConfiguration authConfig = mockConfiguration(
        "https://issuer.example.com", "my-audience", "jws",
        Optional.of(List.of("sub", "email")), Optional.of(List.of("scope")));

    OIDCPluginConfiguration config = factory.create(authConfig);

    assertEquals("https://issuer.example.com", config.issuerURI());
    assertEquals("my-audience", config.audience());
    assertEquals(TokenType.JWS, config.tokenType());
    assertEquals(List.of("sub", "email"), config.requiredClaims());
    assertEquals(List.of("scope"), config.optionalClaims());
  }

  @Test
  @DisplayName("test create: should default optionalClaims to empty list when absent")
  void testCreateWithoutOptionalClaims() {
    AuthenticationConfiguration authConfig = mockConfiguration(
        "https://issuer.example.com", "my-audience", "JWS",
        Optional.of(List.of("sub")), Optional.empty());

    OIDCPluginConfiguration config = factory.create(authConfig);

    assertTrue(config.optionalClaims().isEmpty());
  }

  @Test
  @DisplayName("test create: should handle case-insensitive tokenType")
  void testCreateCaseInsensitiveTokenType() {
    AuthenticationConfiguration authConfig = mockConfiguration(
        "https://issuer.example.com", "my-audience", "jws",
        Optional.of(List.of("sub")), Optional.empty());

    OIDCPluginConfiguration config = factory.create(authConfig);

    assertEquals(TokenType.JWS, config.tokenType());
  }

  @Test
  @DisplayName("test create: should throw 500 when issuerURI is missing")
  void testCreateMissingIssuerUri() {
    AuthenticationConfiguration authConfig = Mockito.mock(AuthenticationConfiguration.class);
    Mockito.when(authConfig.getOption("issuerURI")).thenReturn(Optional.empty());

    ApiException exception =
        assertThrows(
            ApiException.class, () -> factory.create(authConfig));

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getStatusCode());
  }

  @Test
  @DisplayName("test create: should throw 500 when audience is missing")
  void testCreateMissingAudience() {
    AuthenticationConfiguration authConfig = Mockito.mock(AuthenticationConfiguration.class);
    Mockito.when(authConfig.getOption("issuerURI"))
        .thenReturn(Optional.of("https://issuer.example.com"));
    Mockito.when(authConfig.getOption("audience")).thenReturn(Optional.empty());

    ApiException exception =
        assertThrows(
            ApiException.class, () -> factory.create(authConfig));

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getStatusCode());
  }

  @Test
  @DisplayName("test create: should throw 500 when tokenType is missing")
  void testCreateMissingTokenType() {
    AuthenticationConfiguration authConfig = Mockito.mock(AuthenticationConfiguration.class);
    Mockito.when(authConfig.getOption("issuerURI"))
        .thenReturn(Optional.of("https://issuer.example.com"));
    Mockito.when(authConfig.getOption("audience")).thenReturn(Optional.of("my-audience"));
    Mockito.when(authConfig.getOption("tokenType")).thenReturn(Optional.empty());

    ApiException exception =
        assertThrows(
            ApiException.class, () -> factory.create(authConfig));

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getStatusCode());
  }

  @Test
  @DisplayName("test create: should throw 500 when requiredClaims is missing")
  void testCreateMissingRequiredClaims() {
    AuthenticationConfiguration authConfig = Mockito.mock(AuthenticationConfiguration.class);
    Mockito.when(authConfig.getOption("issuerURI"))
        .thenReturn(Optional.of("https://issuer.example.com"));
    Mockito.when(authConfig.getOption("audience")).thenReturn(Optional.of("my-audience"));
    Mockito.when(authConfig.getOption("tokenType")).thenReturn(Optional.of("JWS"));
    Mockito.doReturn(Optional.empty())
        .when(authConfig)
        .getOption(Mockito.eq("requiredClaims"), Mockito.any(TypeReference.class));

    ApiException exception =
        assertThrows(
            ApiException.class, () -> factory.create(authConfig));

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getStatusCode());
  }

  @Test
  @DisplayName("test create: should throw 500 for invalid tokenType")
  void testCreateInvalidTokenType() {
    AuthenticationConfiguration authConfig = Mockito.mock(AuthenticationConfiguration.class);
    Mockito.when(authConfig.getOption("issuerURI"))
        .thenReturn(Optional.of("https://issuer.example.com"));
    Mockito.when(authConfig.getOption("audience")).thenReturn(Optional.of("my-audience"));
    Mockito.when(authConfig.getOption("tokenType")).thenReturn(Optional.of("INVALID"));

    ApiException exception =
        assertThrows(
            ApiException.class,
            () -> factory.create(authConfig));

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getStatusCode());
    assertEquals("oiap.error.invalid.token.type", exception.getError().key());
  }

  private AuthenticationConfiguration mockConfiguration(
      String issuerUri,
      String audience,
      String tokenType,
      Optional<List<String>> requiredClaims,
      Optional<List<String>> optionalClaims) {
    AuthenticationConfiguration authConfig = Mockito.mock(AuthenticationConfiguration.class);
    Mockito.when(authConfig.getOption("issuerURI")).thenReturn(Optional.of(issuerUri));
    Mockito.when(authConfig.getOption("audience")).thenReturn(Optional.of(audience));
    Mockito.when(authConfig.getOption("tokenType")).thenReturn(Optional.of(tokenType));
    Mockito.doReturn(requiredClaims)
        .when(authConfig)
        .getOption(Mockito.eq("requiredClaims"), Mockito.any(TypeReference.class));
    Mockito.doReturn(optionalClaims)
        .when(authConfig)
        .getOption(Mockito.eq("optionalClaims"), Mockito.any(TypeReference.class));
    return authConfig;
  }
}
