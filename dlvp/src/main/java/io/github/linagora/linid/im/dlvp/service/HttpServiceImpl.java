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

package io.github.linagora.linid.im.dlvp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import io.github.linagora.linid.im.corelib.plugin.config.JinjaService;
import io.github.linagora.linid.im.corelib.plugin.config.dto.PluginConfiguration;
import io.github.linagora.linid.im.corelib.plugin.entity.DynamicEntity;
import io.github.linagora.linid.im.corelib.plugin.task.TaskExecutionContext;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Implementation of {@link HttpService} that executes HTTP requests to external APIs.
 *
 * <p>Supports GET and POST methods only. URL and body are rendered via Jinja templates before execution.
 * Returns the raw response body as a string.
 */
@Slf4j
@Service
public class HttpServiceImpl implements HttpService {

  /** I18n key for missing configuration option errors. */
  private static final String MISSING_OPTION = "error.plugin.default.missing.option";

  /** I18n key for invalid configuration option errors. */
  private static final String INVALID_OPTION = "error.plugin.default.invalid.option";

  /** I18n context key used to identify the option name in error messages. */
  private static final String OPTION = "option";

  /** Service responsible for rendering Jinja templates for URL and request body. */
  private final JinjaService jinjaService;

  /** Reusable HTTP client for executing requests. */
  private final WebClient webClient;

  /**
   * Default constructor.
   *
   * @param jinjaService service used to render Jinja templates for URL and body
   */
  @Autowired
  public HttpServiceImpl(final JinjaService jinjaService) {
    this.jinjaService = jinjaService;
    this.webClient = WebClient.create();
  }

  @Override
  public String request(final TaskExecutionContext context, final PluginConfiguration configuration) {
    String url = configuration.getOption("url")
        .orElseThrow(() -> new ApiException(
            500,
            I18nMessage.of(MISSING_OPTION, Map.of(OPTION, "url"))
        ));
    String method = configuration.getOption("method")
        .orElseThrow(() -> new ApiException(
            500,
            I18nMessage.of(MISSING_OPTION, Map.of(OPTION, "method"))
        ));
    Map<String, String> headers = configuration
        .getOption("headers", new TypeReference<Map<String, String>>() {
        }).orElse(Map.of());
    String templatedBody = configuration.getOption("body").orElse("");

    DynamicEntity emptyEntity = new DynamicEntity();
    String renderedUrl = jinjaService.render(context, emptyEntity, url);
    String renderedBody = jinjaService.render(context, emptyEntity, templatedBody);

    WebClient.RequestHeadersSpec<?> request;

    if ("GET".equalsIgnoreCase(method)) {
      request = webClient.get().uri(renderedUrl);
    } else if ("POST".equalsIgnoreCase(method)) {
      request = webClient.post().uri(renderedUrl)
          .bodyValue(renderedBody);
    } else {
      throw new ApiException(500, I18nMessage.of(
          INVALID_OPTION,
          Map.of(OPTION, "method", "value", method)
      ));
    }

    return request
        .headers(httpHeaders -> headers.forEach(httpHeaders::add))
        .retrieve()
        .onStatus(
            HttpStatusCode::is4xxClientError,
            clientResponse -> clientResponse.bodyToMono(String.class)
                .map(errorBody -> new ApiException(
                    clientResponse.statusCode().value(),
                    I18nMessage.of("dlvp.error.external.api"),
                    Map.of("body", errorBody)
                ))
        )
        .onStatus(
            HttpStatusCode::is5xxServerError,
            clientResponse -> clientResponse.bodyToMono(String.class)
                .map(errorBody -> new ApiException(
                    502,
                    I18nMessage.of("dlvp.error.external.api.unavailable"),
                    Map.of("body", errorBody)
                ))
        )
        .bodyToMono(String.class)
        .block();
  }
}
