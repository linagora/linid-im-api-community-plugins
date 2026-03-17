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

package io.github.linagora.linid.im.hpp.service;

import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import io.github.linagora.linid.im.corelib.plugin.config.JinjaService;
import io.github.linagora.linid.im.corelib.plugin.config.dto.ProviderConfiguration;
import io.github.linagora.linid.im.corelib.plugin.entity.DynamicEntity;
import io.github.linagora.linid.im.corelib.plugin.task.TaskExecutionContext;
import io.github.linagora.linid.im.hpp.model.EndpointConfiguration;
import java.util.Map;
import java.util.Optional;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import tools.jackson.core.type.TypeReference;

/**
 * HTTP service implementation responsible for executing dynamic HTTP requests based on provider and endpoint configurations.
 *
 * <p>Supports URI, method, headers, and body templating via Jinja (Jinjava), and handles HTTP status errors with custom exceptions.
 */
@Service
public class HttpServiceImpl implements HttpService {

  /**
   * Service responsible for rendering templates using the Jinja templating engine (e.g., Jinjava).
   */
  private final JinjaService jinjaService;
  /**
   * Spring's {@link RestTemplate} used to perform synchronous HTTP requests.
   */
  private final RestTemplate restTemplate;

  private static final String MISSING_OPTION = "error.plugin.default.missing.option";
  private static final String OPTION = "option";

  /**
   * Default constructor.
   *
   * @param jinjaService Service used to render Jinja templates within URIs, request bodies, headers, and response mappings.
   */
  @Autowired
  public HttpServiceImpl(final JinjaService jinjaService) {
    this.jinjaService = jinjaService;
    this.restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
  }

  @Override
  public String request(TaskExecutionContext context,
                        ProviderConfiguration configuration,
                        EndpointConfiguration endpointConfiguration,
                        String action,
                        DynamicEntity entity) {
    String baseUrl = configuration.getOption("baseUrl")
        .orElseThrow(() -> new ApiException(
            500,
            I18nMessage.of(MISSING_OPTION, Map.of(OPTION, "baseUrl"))
        ));
    var headersMap = configuration.getOption("headers", new TypeReference<Map<String, String>>() {
    }).orElse(Map.of());
    String endpoint = Optional.ofNullable(endpointConfiguration.getUri())
        .orElseThrow(() -> new ApiException(
            500,
            I18nMessage.of(MISSING_OPTION, Map.of(OPTION, String.format("access.%s.uri", action)))
        ));
    String method = Optional.ofNullable(endpointConfiguration.getMethod())
        .orElseThrow(() -> new ApiException(
            500,
            I18nMessage.of(MISSING_OPTION, Map.of(OPTION, String.format("access.%s.method", action)))
        ));

    HttpMethod httpMethod = switch (Strings.toRootUpperCase(method)) {
      case "GET" -> HttpMethod.GET;
      case "POST" -> HttpMethod.POST;
      case "PUT" -> HttpMethod.PUT;
      case "PATCH" -> HttpMethod.PATCH;
      case "DELETE" -> HttpMethod.DELETE;
      default -> throw new ApiException(
          500,
          I18nMessage.of("error.plugin.default.invalid.option", Map.of(
              OPTION, String.format("access.%s.method", action),
              "value", method
          ))
      );
    };

    String templatedBody = Optional.ofNullable(endpointConfiguration.getBody()).orElse("");
    String body = jinjaService.render(context, entity, templatedBody);
    String uri = jinjaService.render(context, entity, String.format("%s%s", baseUrl, endpoint));

    HttpHeaders headers = new HttpHeaders();
    headersMap.forEach(headers::add);

    HttpEntity<String> requestEntity;

    if (httpMethod == HttpMethod.GET || httpMethod == HttpMethod.DELETE) {
      requestEntity = new HttpEntity<>(headers);
    } else {
      requestEntity = new HttpEntity<>(body, headers);
    }

    try {
      ResponseEntity<String> response = restTemplate.exchange(
          uri,
          httpMethod,
          requestEntity,
          String.class
      );
      return response.getBody();
    } catch (HttpStatusCodeException ex) {
      int status = ex.getStatusCode().value();
      String errorKey = status >= 500 ? "hpp.error500" : String.format("hpp.error%d", status);
      throw new ApiException(
          status,
          I18nMessage.of(errorKey),
          Map.of("body", ex.getResponseBodyAsString())
      );
    } catch (Exception ex) {
      throw new ApiException(500, I18nMessage.of("hpp.error500"), Map.of("exception", ex.getMessage()));
    }
  }
}
