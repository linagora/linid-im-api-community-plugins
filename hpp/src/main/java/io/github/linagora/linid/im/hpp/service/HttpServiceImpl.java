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

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import io.github.linagora.linid.im.corelib.plugin.config.JinjaService;
import io.github.linagora.linid.im.corelib.plugin.config.dto.ProviderConfiguration;
import io.github.linagora.linid.im.corelib.plugin.entity.DynamicEntity;
import io.github.linagora.linid.im.corelib.plugin.task.TaskExecutionContext;
import io.github.linagora.linid.im.hpp.model.EndpointConfiguration;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

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

    String templatedBody = Optional.ofNullable(endpointConfiguration.getBody()).orElse("");
    String body = jinjaService.render(context, entity, templatedBody);
    String uri = jinjaService.render(context, entity, String.format("%s%s", baseUrl, endpoint));

    WebClient webClient = WebClient.create();

    WebClient.RequestHeadersSpec<?> request;

    if ("POST".equalsIgnoreCase(method)) {
      request = webClient.post().uri(uri).bodyValue(body);
    } else if ("PUT".equalsIgnoreCase(method)) {
      request = webClient.put().uri(uri).bodyValue(body);
    } else if ("PATCH".equalsIgnoreCase(method)) {
      request = webClient.patch().uri(uri).bodyValue(body);
    } else if ("DELETE".equalsIgnoreCase(method)) {
      request = webClient.delete().uri(uri);
    } else if ("GET".equalsIgnoreCase(method)) {
      request = webClient.get().uri(uri);
    } else {
      throw new ApiException(500, I18nMessage.of(
          "error.plugin.default.invalid.option",
          Map.of(OPTION, String.format("access.%s.method", action), "value", method)
      ));
    }

    return request
        .headers(httpHeaders -> headersMap.forEach(httpHeaders::add))
        .retrieve()
        .onStatus(
            HttpStatusCode::is4xxClientError,
            clientResponse -> clientResponse.bodyToMono(String.class)
                .map(errorBody -> new ApiException(
                    clientResponse.statusCode().value(),
                    I18nMessage.of(String.format("hpp.error%d", clientResponse.statusCode().value())),
                    Map.of("body", errorBody)
                ))
        )
        .onStatus(
            HttpStatusCode::is5xxServerError,
            clientResponse -> clientResponse.bodyToMono(String.class)
                .map(errorBody -> new ApiException(
                    500,
                    I18nMessage.of("hpp.error500"),
                    Map.of("body", errorBody)
                ))
        )
        .bodyToMono(String.class)
        .block();
  }
}
