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

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.DefaultJOSEObjectTypeVerifier;
import com.nimbusds.jose.proc.JWSAlgorithmFamilyJWSKeySelector;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * {@link JWTProcessorConfigurer} implementation for signed JWT access tokens (JWS).
 *
 * <p>Configures the processor with a JWS type verifier and a key selector resolved from the OIDC
 * provider JWKS endpoint. Claims verification is delegated to
 * {@link JWTProcessorConfigurer#configureClaimsVerifier}.
 *
 * <p>Conforms to <a href="https://datatracker.ietf.org/doc/html/rfc9068">RFC 9068</a>.
 */
public class JWSProcessorConfigurer implements JWTProcessorConfigurer {

  /**
   * JWT type header values accepted for JWS access tokens, as defined by RFC 9068.
   */
  private static final JOSEObjectType[] SUPPORTED_JWS_TYPES =
      new JOSEObjectType[] {new JOSEObjectType("at+jwt"), new JOSEObjectType("application/at+jwt")};

  @Override
  public void configure(ConfigurableJWTProcessor<SecurityContext> processor, OIDCValidationParameters parameters)
      throws IllegalArgumentException, KeySourceException, MalformedURLException {
    processor.setJWSTypeVerifier(new DefaultJOSEObjectTypeVerifier<>(SUPPORTED_JWS_TYPES));
    configureKeySelector(processor, parameters.metadata());
    configureClaimsVerifier(processor, parameters);
  }

  /**
   * Configures the JWS key selector on the given JWT processor using the JWKS URI from the OIDC provider metadata.
   *
   * <p>The key selector dynamically determines the accepted signing algorithms from the available JWK sources.
   *
   * @param processor the JWT processor to configure
   * @param metadata  the OIDC provider metadata containing the JWKS URI
   * @throws IllegalArgumentException if the JWKS URI is invalid
   * @throws KeySourceException       if the JWK source cannot be built
   * @throws MalformedURLException    if the JWKS URI is malformed
   */
  private void configureKeySelector(ConfigurableJWTProcessor<SecurityContext> processor, OIDCProviderMetadata metadata)
      throws IllegalArgumentException, KeySourceException, MalformedURLException {
    URL jwkSetURL = metadata.getJWKSetURI().toURL();
    JWKSource<SecurityContext> keySource = JWKSourceBuilder.create(jwkSetURL).build();
    JWSKeySelector<SecurityContext> keySelector = JWSAlgorithmFamilyJWSKeySelector.fromJWKSource(keySource);
    processor.setJWSKeySelector(keySelector);
  }
}
