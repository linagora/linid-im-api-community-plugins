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
import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * Immutable configuration record for the OIDC authentication plugin.
 *
 * <p>Holds the issuer URI and audience values required to validate JWT access tokens,
 * and provides a factory method to build an instance from an {@link AuthenticationConfiguration}.
 *
 * @param issuerURI the URI of the OpenID Connect provider used to resolve provider metadata
 * @param audience  the expected audience value to enforce during JWT validation
 */
public record OIDCPluginConfiguration(String issuerURI, String audience) {

  /**
   * Configuration key for the issuer URI option.
   */
  private static final String ISSUER_URI = "issuer-uri";

  /**
   * Configuration key for the audience option.
   */
  private static final String AUDIENCE = "audience";

  /**
   * I18n key used when a required configuration option is missing.
   */
  private static final String MISSING_OPTION = "error.plugin.default.missing.option";

  /**
   * Creates an {@link OIDCPluginConfiguration} from the given authentication configuration.
   *
   * @param configuration the authentication configuration containing the plugin options
   * @return a new {@link OIDCPluginConfiguration} instance
   * @throws ApiException with HTTP 500 if a required option is missing
   */
  public static OIDCPluginConfiguration from(AuthenticationConfiguration configuration) {
    String issuerURI = getRequiredOption(configuration, ISSUER_URI);
    String audience = getRequiredOption(configuration, AUDIENCE);
    return new OIDCPluginConfiguration(issuerURI, audience);
  }

  /**
   * Retrieves a required option from the given configuration by key.
   *
   * @param configuration the authentication configuration to query
   * @param key           the option key to look up
   * @return the option value as a string
   * @throws ApiException with HTTP 500 if the option is absent
   */
  private static String getRequiredOption(AuthenticationConfiguration configuration, String key) {
    return configuration.getOption(key).orElseThrow(
        () -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR.value(), I18nMessage.of(MISSING_OPTION, Map.of("option", key))));
  }
}
