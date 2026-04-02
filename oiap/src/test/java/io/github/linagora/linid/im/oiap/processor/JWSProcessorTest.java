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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nimbusds.oauth2.sdk.GeneralException;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.oiap.model.OIDCPluginConfiguration;
import io.github.linagora.linid.im.oiap.model.TokenType;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

@DisplayName("Test class: JWSProcessor")
class JWSProcessorTest {

  private static final String JWT_PROCESSOR_CREATION_ERROR = "oiap.error.jwt.processor.creation";

  private final JWSProcessor processor = new JWSProcessor();

  @Test
  @DisplayName("test supports: should return true for JWS token type")
  void testSupportsJws() {
    assertTrue(processor.supports(TokenType.JWS));
  }

  @Test
  @DisplayName("test supports: should return false for OPAQUE token type")
  void testDoesNotSupportOpaque() {
    assertFalse(processor.supports(TokenType.OPAQUE));
  }

  @Test
  @DisplayName("test supports: should return false for JWE token type")
  void testDoesNotSupportJwe() {
    assertFalse(processor.supports(TokenType.JWE));
  }

  @Test
  @DisplayName("test supports: should return false for NESTED token type")
  void testDoesNotSupportNested() {
    assertFalse(processor.supports(TokenType.NESTED));
  }

  @Test
  @DisplayName("test process: should throw 500 when provider metadata resolution throws GeneralException")
  void testProcessThrows500OnGeneralException() {
    try (MockedStatic<OIDCProviderMetadata> opMetadata =
             Mockito.mockStatic(OIDCProviderMetadata.class)) {
      opMetadata
          .when(() -> OIDCProviderMetadata.resolve(Mockito.any(Issuer.class)))
          .thenThrow(new GeneralException("unreachable"));

      OIDCPluginConfiguration config =
          new OIDCPluginConfiguration(
              "https://unreachable.invalid", "audience", TokenType.JWS, List.of(), List.of());

      ApiException exception =
          assertThrows(ApiException.class, () -> processor.process("some.token", config));

      assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getStatusCode());
      assertEquals(JWT_PROCESSOR_CREATION_ERROR, exception.getError().key());
    }
  }

  @Test
  @DisplayName("test process: should throw 500 when provider metadata resolution throws IOException")
  void testProcessThrows500OnIoException() {
    try (MockedStatic<OIDCProviderMetadata> opMetadata =
             Mockito.mockStatic(OIDCProviderMetadata.class)) {
      opMetadata
          .when(() -> OIDCProviderMetadata.resolve(Mockito.any(Issuer.class)))
          .thenThrow(new IOException("network error"));

      OIDCPluginConfiguration config =
          new OIDCPluginConfiguration(
              "https://unreachable.invalid", "audience", TokenType.JWS, List.of(), List.of());

      ApiException exception =
          assertThrows(ApiException.class, () -> processor.process("some.token", config));

      assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getStatusCode());
      assertEquals(JWT_PROCESSOR_CREATION_ERROR, exception.getError().key());
    }
  }
}
