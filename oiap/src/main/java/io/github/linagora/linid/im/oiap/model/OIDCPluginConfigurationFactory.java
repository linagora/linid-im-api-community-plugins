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

import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import io.github.linagora.linid.im.corelib.plugin.config.dto.AuthenticationConfiguration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;

/**
 * Factory that builds an {@link OIDCPluginConfiguration} from an {@link AuthenticationConfiguration},
 * validating that all required options are present.
 */
@Component
public class OIDCPluginConfigurationFactory {

  /**
   * Option key for the OIDC provider issuer URI.
   */
  private static final String ISSUER_URI = "issuerURI";

  /**
   * Option key for the expected audience claim value.
   */
  private static final String AUDIENCE = "audience";

  /**
   * Option key for the access token format ({@link TokenType}).
   */
  private static final String TOKEN_TYPE = "tokenType";

  /**
   * Option key for the list of claim names that must be present in the token.
   */
  private static final String REQUIRED_CLAIMS = "requiredClaims";

  /**
   * Option key for the list of claim names that are propagated when present.
   */
  private static final String OPTIONAL_CLAIMS = "optionalClaims";

  /**
   * I18n key used when a required option is missing from the configuration.
   */
  private static final String MISSING_OPTION = "error.plugin.default.missing.option";

  /**
   * Creates an {@link OIDCPluginConfiguration} from the given authentication configuration.
   *
   * @param configuration the authentication configuration containing plugin options
   * @return a fully validated {@link OIDCPluginConfiguration}
   * @throws ApiException with HTTP 500 if any required option is missing
   */
  public OIDCPluginConfiguration create(AuthenticationConfiguration configuration) {
    String issuerURI = configuration.getOption(ISSUER_URI)
        .orElseThrow(() -> new ApiException(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            I18nMessage.of(MISSING_OPTION, Map.of("option", ISSUER_URI))));

    String audience = configuration.getOption(AUDIENCE)
        .orElseThrow(() -> new ApiException(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            I18nMessage.of(MISSING_OPTION, Map.of("option", AUDIENCE))));

    String tokenTypeValue = configuration.getOption(TOKEN_TYPE)
        .orElseThrow(() -> new ApiException(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            I18nMessage.of(MISSING_OPTION, Map.of("option", TOKEN_TYPE))));

    TokenType tokenType;
    try {
      tokenType = TokenType.valueOf(tokenTypeValue.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new ApiException(
          HttpStatus.INTERNAL_SERVER_ERROR.value(),
          I18nMessage.of(ErrorKey.INVALID_TOKEN_TYPE.getKey(), Map.of("tokenType", tokenTypeValue)));
    }

    List<String> requiredClaims = configuration.getOption(REQUIRED_CLAIMS, new TypeReference<List<String>>() {
        })
        .orElseThrow(() -> new ApiException(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            I18nMessage.of(MISSING_OPTION, Map.of("option", REQUIRED_CLAIMS))));

    List<String> optionalClaims = configuration.getOption(OPTIONAL_CLAIMS, new TypeReference<List<String>>() {
        })
        .orElse(List.of());

    return new OIDCPluginConfiguration(issuerURI, audience, tokenType, requiredClaims, optionalClaims);
  }
}
