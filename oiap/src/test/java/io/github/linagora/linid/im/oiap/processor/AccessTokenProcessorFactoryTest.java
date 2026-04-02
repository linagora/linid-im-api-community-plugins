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

package io.github.linagora.linid.im.oiap.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.oiap.model.OIDCPluginConfiguration;
import io.github.linagora.linid.im.oiap.model.TokenType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

@DisplayName("Test class: AccessTokenProcessorFactory")
class AccessTokenProcessorFactoryTest {

  private static final String INVALID_TOKEN = "oiap.error.invalid.token";

  @Test
  @DisplayName("test getProcessor: should return the matching processor")
  void testGetMatchingProcessor() {
    AccessTokenProcessor jwsProcessor = Mockito.mock(AccessTokenProcessor.class);
    Mockito.when(jwsProcessor.supports(TokenType.JWS)).thenReturn(true);

    AccessTokenProcessorFactory factory =
        new AccessTokenProcessorFactory(List.of(jwsProcessor));
    OIDCPluginConfiguration config =
        new OIDCPluginConfiguration(
            "https://issuer.example.com", "audience", TokenType.JWS, List.of(), List.of());

    assertSame(jwsProcessor, factory.getProcessor(config));
  }

  @Test
  @DisplayName("test getProcessor: should return first matching processor when multiple match")
  void testGetFirstMatchingProcessor() {
    AccessTokenProcessor first = Mockito.mock(AccessTokenProcessor.class);
    AccessTokenProcessor second = Mockito.mock(AccessTokenProcessor.class);
    Mockito.when(first.supports(TokenType.JWS)).thenReturn(true);
    Mockito.when(second.supports(TokenType.JWS)).thenReturn(true);

    AccessTokenProcessorFactory factory =
        new AccessTokenProcessorFactory(List.of(first, second));
    OIDCPluginConfiguration config =
        new OIDCPluginConfiguration(
            "https://issuer.example.com", "audience", TokenType.JWS, List.of(), List.of());

    assertSame(first, factory.getProcessor(config));
  }

  @Test
  @DisplayName("test getProcessor: should throw 401 when no processor supports the token type")
  void testGetProcessorNoMatch() {
    AccessTokenProcessor processor = Mockito.mock(AccessTokenProcessor.class);
    Mockito.when(processor.supports(Mockito.any())).thenReturn(false);

    AccessTokenProcessorFactory factory =
        new AccessTokenProcessorFactory(List.of(processor));
    OIDCPluginConfiguration config =
        new OIDCPluginConfiguration(
            "https://issuer.example.com", "audience", TokenType.OPAQUE, List.of(), List.of());

    ApiException exception =
        assertThrows(ApiException.class, () -> factory.getProcessor(config));

    assertEquals(HttpStatus.UNAUTHORIZED.value(), exception.getStatusCode());
    assertEquals(INVALID_TOKEN, exception.getError().key());
  }

  @Test
  @DisplayName("test getProcessor: should throw 401 when processor list is empty")
  void testGetProcessorEmptyList() {
    AccessTokenProcessorFactory factory = new AccessTokenProcessorFactory(List.of());
    OIDCPluginConfiguration config =
        new OIDCPluginConfiguration(
            "https://issuer.example.com", "audience", TokenType.JWS, List.of(), List.of());

    ApiException exception =
        assertThrows(ApiException.class, () -> factory.getProcessor(config));

    assertEquals(HttpStatus.UNAUTHORIZED.value(), exception.getStatusCode());
    assertEquals(INVALID_TOKEN, exception.getError().key());
  }
}
