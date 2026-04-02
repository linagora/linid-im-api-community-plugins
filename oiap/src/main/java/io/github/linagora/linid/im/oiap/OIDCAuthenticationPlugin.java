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

import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import io.github.linagora.linid.im.corelib.plugin.authentication.AuthenticationPlugin;
import io.github.linagora.linid.im.corelib.plugin.config.dto.AuthenticationConfiguration;
import io.github.linagora.linid.im.corelib.plugin.task.TaskExecutionContext;
import io.github.linagora.linid.im.oiap.model.ErrorKey;
import io.github.linagora.linid.im.oiap.model.OIDCPluginConfiguration;
import io.github.linagora.linid.im.oiap.model.OIDCPluginConfigurationFactory;
import io.github.linagora.linid.im.oiap.processor.AccessTokenProcessorFactory;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * {@link AuthenticationPlugin} implementation for OIDC-based authentication.
 *
 * <p>Extracts the Bearer token from the incoming HTTP request, delegates validation to the
 * appropriate {@link io.github.linagora.linid.im.oiap.processor.AccessTokenProcessor}, and
 * propagates a selected subset of JWT claims into the task execution context.
 */
@Slf4j
@Component
public class OIDCAuthenticationPlugin implements AuthenticationPlugin {

  /**
   * Expected prefix of the {@code Authorization} HTTP header value.
   */
  private static final String ACCESS_TOKEN_PREFIX = "Bearer ";

  /**
   * Key used to store the extracted claims map in the {@link TaskExecutionContext}.
   */
  private static final String CLAIMS_CONTEXT_KEY = "claims";

  /**
   * Factory used to build an {@link OIDCPluginConfiguration} from the authentication configuration.
   */
  private final OIDCPluginConfigurationFactory configurationFactory;

  /**
   * Factory used to select the {@link io.github.linagora.linid.im.oiap.processor.AccessTokenProcessor} matching the configured token type.
   */
  private final AccessTokenProcessorFactory processorFactory;

  public OIDCAuthenticationPlugin(
      OIDCPluginConfigurationFactory configurationFactory,
      AccessTokenProcessorFactory processorFactory) {
    this.configurationFactory = configurationFactory;
    this.processorFactory = processorFactory;
  }

  /**
   * Returns {@code true} when the plugin type is {@code "oidc"}.
   *
   * @param type the plugin type identifier from the authentication configuration
   * @return {@code true} if this plugin handles the given type
   */
  @Override
  public boolean supports(final String type) {
    return "oidc".equals(type);
  }

  /**
   * Validates the Bearer token carried in the HTTP request and populates the execution context with
   * the token's claims.
   *
   * <p>The method:
   *
   * <ol>
   *   <li>Extracts the Bearer token from the {@code Authorization} header.
   *   <li>Selects the first {@link io.github.linagora.linid.im.oiap.processor.AccessTokenProcessor}
   *       that supports the token format.
   *   <li>Validates the token and retrieves its claims.
   *   <li>Asserts that all required claims are present and non-blank.
   *   <li>Propagates the claims subset into the context.
   * </ol>
   *
   * @param configuration the authentication configuration for this plugin instance
   * @param request       the incoming HTTP request containing the {@code Authorization} header
   * @param context       the task execution context in which claims will be stored
   * @throws ApiException with HTTP 401 if the token is absent, malformed, invalid, or if required
   *                      claims are missing or blank; with HTTP 500 if any error occurs during processor
   *                      configuration.
   */
  @Override
  public void validateToken(
      AuthenticationConfiguration configuration,
      HttpServletRequest request,
      TaskExecutionContext context) {
    String accessToken = extractAccessToken(request);

    OIDCPluginConfiguration config = this.configurationFactory.create(configuration);

    Map<String, Object> claims =
        this.processorFactory.getProcessor(config).process(accessToken, config);

    validateRequiredClaims(claims, config);
    propagateClaims(context, config, claims);
  }

  /**
   * Extracts the Bearer token value from the {@code Authorization} header.
   *
   * @param request the incoming HTTP request
   * @return the raw token string, without the {@code "Bearer "} prefix
   * @throws ApiException with HTTP 401 if the header is absent, does not start with {@code "Bearer
   *                      "}, or contains an empty token value
   */
  private String extractAccessToken(HttpServletRequest request) throws ApiException {
    String authorizationHeader = request.getHeader("Authorization");

    if (authorizationHeader == null
        || !authorizationHeader
        .toLowerCase(Locale.ROOT)
        .startsWith(ACCESS_TOKEN_PREFIX.toLowerCase(Locale.ROOT))) {
      throw new ApiException(
          HttpStatus.UNAUTHORIZED.value(), I18nMessage.of(ErrorKey.INVALID_TOKEN.getKey()));
    }

    String token = authorizationHeader.substring(ACCESS_TOKEN_PREFIX.length()).trim();
    if (token.isEmpty()) {
      throw new ApiException(
          HttpStatus.UNAUTHORIZED.value(), I18nMessage.of(ErrorKey.INVALID_TOKEN.getKey()));
    }
    return token;
  }

  /**
   * Asserts that all required claims are present and non-blank.
   *
   * @param claims the claims map returned by the token processor
   * @param config the plugin configuration declaring the required claim names
   * @throws ApiException with HTTP 401 if any required claim is missing or blank
   */
  private void validateRequiredClaims(Map<String, Object> claims, OIDCPluginConfiguration config) {
    for (String claimName : config.requiredClaims()) {
      Object value = claims.get(claimName);
      if (value == null || value.toString().isBlank()) {
        log.debug("Missing or blank required claim: {}", claimName);
        throw new ApiException(
            HttpStatus.UNAUTHORIZED.value(), I18nMessage.of(ErrorKey.INVALID_TOKEN.getKey()));
      }
    }
  }

  /**
   * Copies the subset of claims (required + optional) into the task execution context under the
   * key {@value #CLAIMS_CONTEXT_KEY}.
   *
   * <p>Claims absent from the token are silently omitted from the propagated map.
   *
   * @param context the task execution context to populate
   * @param config  the plugin configuration declaring the required and optional claim names
   * @param claims  the full claims map returned by the token processor
   */
  private void propagateClaims(
      TaskExecutionContext context, OIDCPluginConfiguration config, Map<String, Object> claims) {
    List<String> allClaimsToPropagate =
        Stream.concat(config.requiredClaims().stream(), config.optionalClaims().stream())
            .distinct()
            .toList();

    Map<String, Object> extractedClaims =
        allClaimsToPropagate.stream()
            .filter(claim -> claims.get(claim) != null)
            .collect(Collectors.toMap(Function.identity(), claims::get));

    context.put(CLAIMS_CONTEXT_KEY, extractedClaims);
  }
}
