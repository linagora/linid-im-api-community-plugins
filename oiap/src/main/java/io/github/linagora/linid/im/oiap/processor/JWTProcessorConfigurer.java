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

import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimNames;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import java.util.Set;

/**
 * Strategy interface for configuring a {@link ConfigurableJWTProcessor}.
 *
 * <p>Each implementation handles a specific type of JWT (e.g. signed JWS, encrypted JWE) and is
 * responsible for setting the appropriate key selectors and type verifiers on the processor.
 * Once configured, the processor calls the {@link com.nimbusds.jwt.proc.JWTProcessor#process(String, SecurityContext)}
 * method with the raw token string which internally parses the token and dispatches to the correct typed overload.
 *
 * <p>The claims verifier configuration is shared across all implementations via
 * {@link #configureClaimsVerifier} and enforces issuer, audience, and required claims validation
 * regardless of the token type.
 */
public interface JWTProcessorConfigurer {

  /**
   * Set of JWT claim names that must be present in the token payload.
   *
   * <p>Enforced by {@link #configureClaimsVerifier} on all JWT processor implementations,
   * regardless of the token type. Includes {@code exp}, {@code aud}, and {@code iss}.
   */
  Set<String> REQUIRED_CLAIMS = Set.of(
      JWTClaimNames.EXPIRATION_TIME,
      JWTClaimNames.AUDIENCE,
      JWTClaimNames.ISSUER
  );

  /**
   * Configures the given JWT processor for the token type handled by this strategy.
   *
   * @param processor  the JWT processor to configure
   * @param parameters the validation parameters containing OIDC metadata and audience
   * @throws Exception if the processor cannot be configured
   */
  void configure(ConfigurableJWTProcessor<SecurityContext> processor, OIDCValidationParameters parameters) throws Exception;

  /**
   * Configures the claims verifier on the given JWT processor, enforcing issuer, audience, and required claim validation.
   *
   * <p>This method is shared across all implementations since claim validation applies
   * to the JWT payload regardless of the token type. The full set of required claims is
   * defined in {@link #REQUIRED_CLAIMS}.
   *
   * @param processor  the JWT processor to configure
   * @param parameters the validation parameters containing OIDC metadata and audience
   */
  default void configureClaimsVerifier(ConfigurableJWTProcessor<SecurityContext> processor, OIDCValidationParameters parameters) {
    String issuer = parameters.metadata().getIssuer().getValue();
    JWTClaimsSet checkedClaims = new JWTClaimsSet.Builder().issuer(issuer).build();
    processor.setJWTClaimsSetVerifier(new DefaultJWTClaimsVerifier<>(parameters.audience(), checkedClaims, REQUIRED_CLAIMS));
  }
}
