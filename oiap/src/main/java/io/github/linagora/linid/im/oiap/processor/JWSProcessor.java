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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.DefaultJOSEObjectTypeVerifier;
import com.nimbusds.jose.proc.JWSAlgorithmFamilyJWSKeySelector;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimNames;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.nimbusds.oauth2.sdk.GeneralException;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import io.github.linagora.linid.im.oiap.model.ErrorKey;
import io.github.linagora.linid.im.oiap.model.OIDCPluginConfiguration;
import io.github.linagora.linid.im.oiap.model.TokenType;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * {@link AccessTokenProcessor} implementation for signed (JWS) JWT access tokens.
 *
 * <p>Conforms to <a href="https://datatracker.ietf.org/doc/html/rfc9068">RFC 9068</a>.
 */
@Slf4j
@Component
public class JWSProcessor implements AccessTokenProcessor {

  /**
   * JWT type header values accepted for JWS access tokens, as defined by RFC 9068.
   */
  private static final JOSEObjectType[] SUPPORTED_JWT_TYPES =
      new JOSEObjectType[] {new JOSEObjectType("at+jwt"), new JOSEObjectType("application/at+jwt")};

  /**
   * JWT claim names that must be present in the token payload.
   */
  private static final Set<String> REQUIRED_CLAIMS =
      Set.of(JWTClaimNames.EXPIRATION_TIME, JWTClaimNames.AUDIENCE, JWTClaimNames.ISSUER);

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean supports(TokenType type) {
    return TokenType.JWS == type;
  }

  /**
   * Validates the JWT access token and returns its claims.
   *
   * @param accessToken the raw JWT string
   * @param config      the plugin configuration containing the issuer URI and expected audience
   * @return a map of claim names to their values
   * @throws ApiException with HTTP 401 if the token is invalid, expired, or fails signature
   *                      verification; with HTTP 500 if the JWT processor cannot be created
   */
  @Override
  public Map<String, Object> process(String accessToken, OIDCPluginConfiguration config)
      throws ApiException {
    ConfigurableJWTProcessor<SecurityContext> jwtProcessor = createJWTProcessor(config);
    try {
      JWTClaimsSet claimsSet = jwtProcessor.process(accessToken, null);
      return claimsSet.getClaims();
    } catch (ParseException | BadJOSEException | JOSEException e) {
      log.debug("Failed to validate JWT access token", e);
      throw new ApiException(
          HttpStatus.UNAUTHORIZED.value(), I18nMessage.of(ErrorKey.INVALID_TOKEN.getKey()));
    }
  }

  /**
   * Builds and configures a {@link ConfigurableJWTProcessor} for the given plugin configuration.
   *
   * @param config the plugin configuration containing the issuer URI and expected audience
   * @return a fully configured JWT processor ready to validate and parse tokens
   * @throws ApiException with HTTP 500 if any error occurs during provider metadata resolution or
   *                      processor configuration, such as network errors, invalid issuer URI, or failure to retrieve
   *                      the JWK Set
   */
  private ConfigurableJWTProcessor<SecurityContext> createJWTProcessor(
      OIDCPluginConfiguration config) throws ApiException {
    try {
      OIDCProviderMetadata opMetadata =
          OIDCProviderMetadata.resolve(new Issuer(config.issuerURI()));
      ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
      jwtProcessor.setJWSTypeVerifier(new DefaultJOSEObjectTypeVerifier<>(SUPPORTED_JWT_TYPES));
      configureJWSKeySelector(jwtProcessor, opMetadata);
      configureClaimsVerifier(jwtProcessor, opMetadata, config.audience());
      return jwtProcessor;
    } catch (GeneralException | IOException | KeySourceException e) {
      log.debug("Failed to create JWT processor for issuer URI {}", config.issuerURI(), e);
      throw new ApiException(
          HttpStatus.INTERNAL_SERVER_ERROR.value(),
          I18nMessage.of(ErrorKey.JWT_PROCESSOR_CREATION_ERROR.getKey()));
    }
  }

  /**
   * Configures the JWS key selector on the processor using the provider's JWK Set.
   *
   * <p>The key selector automatically negotiates the signing algorithm from the JWK Set.
   *
   * @param processor the JWT processor to configure
   * @param metadata  the OIDC provider metadata providing the JWK Set URI
   * @throws KeySourceException if the JWKs cannot be retrieved or no suitable public JWKs are found
   * @throws IOException        if the JWK Set URI cannot be converted to a URL
   */
  private void configureJWSKeySelector(
      ConfigurableJWTProcessor<SecurityContext> processor, OIDCProviderMetadata metadata)
      throws KeySourceException, IOException {
    URL jwkSetURL = metadata.getJWKSetURI().toURL();
    JWKSource<SecurityContext> keySource =
        JWKSourceBuilder.create(jwkSetURL).retrying(true).build();
    JWSKeySelector<SecurityContext> keySelector =
        JWSAlgorithmFamilyJWSKeySelector.fromJWKSource(keySource);
    processor.setJWSKeySelector(keySelector);
  }

  /**
   * Configures the claims verifier on the processor.
   *
   * <p>Enforces that the token contains the expected issuer and audience, and that the claims
   * defined in {@link #REQUIRED_CLAIMS} are present.
   *
   * @param processor the JWT processor to configure
   * @param metadata  the OIDC provider metadata providing the issuer identifier
   * @param audience  the expected {@code aud} claim value that the JWT must contain
   */
  private void configureClaimsVerifier(
      ConfigurableJWTProcessor<SecurityContext> processor,
      OIDCProviderMetadata metadata,
      String audience) {
    String issuer = metadata.getIssuer().getValue();
    JWTClaimsSet checkedClaims = new JWTClaimsSet.Builder().issuer(issuer).build();
    processor.setJWTClaimsSetVerifier(
        new DefaultJWTClaimsVerifier<>(audience, checkedClaims, REQUIRED_CLAIMS));
  }
}
