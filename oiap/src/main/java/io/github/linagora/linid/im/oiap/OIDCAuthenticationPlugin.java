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

import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimNames;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import io.github.linagora.linid.im.corelib.plugin.authentication.AuthenticationPlugin;
import io.github.linagora.linid.im.corelib.plugin.config.dto.AuthenticationConfiguration;
import io.github.linagora.linid.im.corelib.plugin.task.TaskExecutionContext;
import io.github.linagora.linid.im.oiap.model.OIDCPluginConfiguration;
import io.github.linagora.linid.im.oiap.processor.JWSProcessorConfigurer;
import io.github.linagora.linid.im.oiap.processor.JWTProcessorConfigurer;
import io.github.linagora.linid.im.oiap.processor.OIDCValidationParameters;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Authentication plugin that validates OAuth2 JWT access tokens issued by an OpenID Connect provider.
 *
 * <p>Resolves the OIDC provider metadata from the configured issuer URI, validates the token signature,
 * type, expiration, issuer, audience, and required claims, then propagates the extracted claims to the task execution context.
 *
 * <p>Conforms to <a href="https://datatracker.ietf.org/doc/html/rfc9068">RFC 9068</a> for JWT access token validation.
 */
@Slf4j
@Component
public class OIDCAuthenticationPlugin implements AuthenticationPlugin {

  /**
   * I18n key used when the access token is invalid or cannot be validated.
   */
  private static final String INVALID_TOKEN = "oiap.error.token.invalid";

  /**
   * I18n key used when a required claim is missing or blank in the JWT payload.
   */
  private static final String MISSING_CLAIM = "oiap.error.claim.missing";

  /**
   * Expected prefix of the Authorization header value for Bearer tokens.
   */
  private static final String ACCESS_TOKEN_PREFIX = "Bearer ";

  /**
   * Strategy for configuring the JWT processor based on the token type.
   * Currently only supports signed JWS tokens as defined in RFC 9068.
   * It will be extended in the future to support encrypted JWE tokens as well.
   */
  private final JWTProcessorConfigurer jwtProcessorConfigurer = new JWSProcessorConfigurer();

  /**
   * Claims that must be present and non-blank in the JWT payload and will be propagated to the execution context.
   */
  private static final Set<String> REQUIRED_PROPAGATED_CLAIMS = Set.of(
      JWTClaimNames.SUBJECT,
      "email"
  );

  /**
   * Full set of claim names to extract from the JWT payload and propagate to the execution context.
   */
  private static final Set<String> ALL_PROPAGATED_CLAIMS = Stream.concat(
      Stream.of("preferred_username", "scope", "roles"),
      REQUIRED_PROPAGATED_CLAIMS.stream()
  ).collect(Collectors.toUnmodifiableSet());

  /**
   * Key used to store the extracted claims map in the task execution context.
   */
  private static final String CLAIMS_CONTEXT_KEY = "claims";

  @Override
  public boolean supports(final String type) {
    return "oiap".equals(type);
  }

  @Override
  public void validateToken(AuthenticationConfiguration configuration, HttpServletRequest request, TaskExecutionContext context) {
    String accessToken = extractAccessToken(request);
    ConfigurableJWTProcessor<SecurityContext> jwtProcessor = buildJWTProcessor(configuration);
    JWTClaimsSet claimsSet = processToken(jwtProcessor, accessToken);
    validateRequiredClaims(claimsSet);
    propagateClaims(context, claimsSet);
  }

  /**
   * Extracts the Bearer access token from the Authorization header of the incoming HTTP request.
   *
   * @param request the incoming HTTP request
   * @return the raw access token string
   * @throws ApiException with HTTP 401 if the Authorization header is missing, malformed, or empty
   */
  private String extractAccessToken(HttpServletRequest request) throws ApiException {
    String authorizationHeader = request.getHeader("Authorization");

    if (authorizationHeader == null || !authorizationHeader.startsWith(ACCESS_TOKEN_PREFIX)) {
      throw new ApiException(HttpStatus.UNAUTHORIZED.value(), I18nMessage.of(INVALID_TOKEN));
    }

    String token = authorizationHeader.substring(ACCESS_TOKEN_PREFIX.length()).trim();
    if (token.isEmpty()) {
      throw new ApiException(HttpStatus.UNAUTHORIZED.value(), I18nMessage.of(INVALID_TOKEN));
    }
    return token;
  }

  /**
   * Builds and configures a {@link ConfigurableJWTProcessor} for the given plugin configuration.
   *
   * <p>Resolves the OIDC provider metadata from the issuer URI, then delegates processor
   * configuration to {@link #jwtProcessorConfigurer}.
   *
   * @param configuration the plugin configuration containing the issuer URI and audience
   * @return a fully configured JWT processor
   * @throws ApiException with HTTP 401 if the OIDC provider metadata cannot be resolved or the processor cannot be built
   */
  private ConfigurableJWTProcessor<SecurityContext> buildJWTProcessor(AuthenticationConfiguration configuration)
      throws ApiException {
    OIDCPluginConfiguration config = OIDCPluginConfiguration.from(configuration);
    try {
      OIDCProviderMetadata opMetadata = OIDCProviderMetadata.resolve(new Issuer(config.issuerURI()));
      ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
      OIDCValidationParameters parameters = new OIDCValidationParameters(opMetadata, config.audience());
      jwtProcessorConfigurer.configure(jwtProcessor, parameters);
      return jwtProcessor;
    } catch (Exception e) {
      log.debug("Failed to build JWT processor for issuer URI: {}", config.issuerURI(), e);
      throw new ApiException(HttpStatus.UNAUTHORIZED.value(), I18nMessage.of(INVALID_TOKEN));
    }
  }

  /**
   * Decodes and validates the given access token using the provided JWT processor.
   *
   * @param jwtProcessor the configured JWT processor to use for validation
   * @param accessToken  the raw access token string to process
   * @return the validated {@link JWTClaimsSet} extracted from the token
   * @throws ApiException with HTTP 401 if the token is invalid, expired, or cannot be processed
   */
  private JWTClaimsSet processToken(ConfigurableJWTProcessor<SecurityContext> jwtProcessor, String accessToken) {
    try {
      return jwtProcessor.process(accessToken, null);
    } catch (Exception e) {
      log.debug("Failed to decode and validate JWT", e);
      throw new ApiException(HttpStatus.UNAUTHORIZED.value(), I18nMessage.of(INVALID_TOKEN));
    }
  }

  /**
   * Validates that each claim in {@link #REQUIRED_PROPAGATED_CLAIMS} is present and non-blank in the given claims set.
   *
   * @param claimsSet the JWT claims set to validate
   * @throws ApiException with HTTP 401 if any required claim is missing or blank
   */
  private void validateRequiredClaims(JWTClaimsSet claimsSet) {
    for (String claimName : REQUIRED_PROPAGATED_CLAIMS) {
      Object value = claimsSet.getClaim(claimName);
      if (value == null || value.toString().isBlank()) {
        log.debug("Missing or blank required claim: {}", claimName);
        throw new ApiException(HttpStatus.UNAUTHORIZED.value(), I18nMessage.of(MISSING_CLAIM, Map.of("claim", claimName)));
      }
    }
  }

  /**
   * Extracts the claims defined in {@link #ALL_PROPAGATED_CLAIMS} from the JWT claims set and stores them in the task execution context.
   *
   * <p>Only claims with a non-null value are included in the extracted map.
   *
   * @param context   the task execution context in which to store the extracted claims
   * @param claimsSet the validated JWT claims set from which to extract claims
   */
  private void propagateClaims(TaskExecutionContext context, JWTClaimsSet claimsSet) {
    Map<String, Object> extractedClaims =
        ALL_PROPAGATED_CLAIMS.stream()
            .filter(claim -> claimsSet.getClaim(claim) != null)
            .collect(Collectors.toMap(Function.identity(), claimsSet::getClaim));

    context.put(CLAIMS_CONTEXT_KEY, extractedClaims);
  }
}
