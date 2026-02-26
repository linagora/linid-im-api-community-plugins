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

package io.github.linagora.linid.im.dlvp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import io.github.linagora.linid.im.corelib.plugin.authorization.AuthorizationFactory;
import io.github.linagora.linid.im.corelib.plugin.config.JinjaService;
import io.github.linagora.linid.im.corelib.plugin.config.dto.EntityConfiguration;
import io.github.linagora.linid.im.corelib.plugin.entity.DynamicEntity;
import io.github.linagora.linid.im.corelib.plugin.route.AbstractRoutePlugin;
import io.github.linagora.linid.im.corelib.plugin.route.RouteDescription;
import io.github.linagora.linid.im.corelib.plugin.task.TaskEngine;
import io.github.linagora.linid.im.corelib.plugin.task.TaskExecutionContext;
import io.github.linagora.linid.im.dlvp.model.DynamicListConfiguration;
import io.github.linagora.linid.im.dlvp.model.DynamicListEntry;
import io.github.linagora.linid.im.dlvp.service.HttpService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * Dynamic List Route Plugin implementation for LinID Directory Manager.
 *
 * <p>This plugin exposes a configurable GET endpoint that retrieves structured elements from an external
 * API and returns them as a paginated JSON response with {@code { label, value }} objects.
 *
 * <p>The route path is dynamic and defined via the {@code route} configuration option.
 * The plugin orchestrates HTTP calls, task execution (e.g., json-parsing),
 * and value extraction using Jinja templates.
 */
@Slf4j
@Component
public class DynamicListRoutePlugin extends AbstractRoutePlugin implements DynamicListSupport {

  /** I18n key for missing configuration option errors. */
  private static final String MISSING_OPTION = "error.plugin.default.missing.option";

  /** I18n context key used to identify the option name in error messages. */
  private static final String OPTION = "option";

  /** Service used to execute HTTP requests to external APIs. */
  private final HttpService httpService;

  /** Service used to render Jinja templates for value extraction. */
  private final JinjaService jinjaService;

  /** Engine to execute lifecycle tasks before and after response mapping. */
  private final TaskEngine taskEngine;

  /** Factory to retrieve the authorization plugin for token validation. */
  private final AuthorizationFactory authorizationFactory;

  /** Object mapper for converting configuration options to typed models. */
  private final ObjectMapper mapper = new ObjectMapper();

  /**
   * Default constructor.
   *
   * @param httpService service used to execute HTTP requests to external APIs
   * @param jinjaService service used to render Jinja templates for value extraction
   * @param taskEngine engine to execute lifecycle tasks before and after response mapping
   * @param authorizationFactory factory to retrieve the authorization plugin
   */
  @Autowired
  public DynamicListRoutePlugin(final HttpService httpService,
                                final JinjaService jinjaService,
                                final TaskEngine taskEngine,
                                final AuthorizationFactory authorizationFactory) {
    this.httpService = httpService;
    this.jinjaService = jinjaService;
    this.taskEngine = taskEngine;
    this.authorizationFactory = authorizationFactory;
  }

  @Override
  public boolean supports(final @NonNull String type) {
    return "dynamic-list".equals(type);
  }

  @Override
  public List<RouteDescription> getRoutes(final List<EntityConfiguration> entities) {
    String route = getConfiguration().getOption("route")
        .orElseThrow(() -> new ApiException(
            500,
            I18nMessage.of(MISSING_OPTION, Map.of(OPTION, "route"))
        ));
    return List.of(new RouteDescription("GET", route, null, List.of()));
  }

  @Override
  public boolean match(final String url, final String method) {
    if (getConfiguration() == null) {
      return false;
    }
    String route = getConfiguration().getOption("route")
        .orElseThrow(() -> new ApiException(
            500,
            I18nMessage.of(MISSING_OPTION, Map.of(OPTION, "route"))
        ));
    return "GET".equalsIgnoreCase(method) && url.endsWith(route);
  }

  @Override
  public ResponseEntity<Page<DynamicListEntry>> execute(final HttpServletRequest request) {
    String route = getConfiguration().getOption("route")
        .orElseThrow(() -> new ApiException(
            500,
            I18nMessage.of(MISSING_OPTION, Map.of(OPTION, "route"))
        ));
    log.info("Receiving request on dynamic-list route '{}'", route);

    DynamicListConfiguration dlConfig = mapper.convertValue(
        getConfiguration().getOptions(), new TypeReference<>() {
        });

    TaskExecutionContext context = new TaskExecutionContext();
    DynamicEntity entity = buildDynamicEntity(getConfiguration());

    var authorizationPlugin = authorizationFactory.getAuthorizationPlugin();

    taskEngine.execute(entity, context, "beforeTokenValidationDynamicList");
    authorizationPlugin.validateToken(request, context);
    taskEngine.execute(entity, context, "afterTokenValidationDynamicList");

    taskEngine.execute(entity, context, "beforeDynamicList");

    String response = httpService.request(context, getConfiguration());
    context.put("response", response);

    taskEngine.execute(entity, context, "afterDynamicList");

    taskEngine.execute(entity, context, "beforeDynamicListMapping");

    List<DynamicListEntry> elements = extractElements(jinjaService, context, dlConfig);
    Page<DynamicListEntry> page = buildPage(context, dlConfig, elements);

    taskEngine.execute(entity, context, "afterDynamicListMapping");

    return ResponseEntity.ok(page);
  }

  Page<DynamicListEntry> buildPage(final TaskExecutionContext context,
                                    final DynamicListConfiguration dlConfig,
                                    final List<DynamicListEntry> elements) {
    DynamicEntity emptyEntity = new DynamicEntity();

    int page = Integer.parseInt(jinjaService.render(context, emptyEntity,
        Optional.ofNullable(dlConfig.getPage()).orElse("0")).trim());
    int size = Integer.parseInt(jinjaService.render(context, emptyEntity,
        Optional.ofNullable(dlConfig.getSize()).orElse("0")).trim());
    long total = Long.parseLong(jinjaService.render(context, emptyEntity,
        Optional.ofNullable(dlConfig.getTotal()).orElse("0")).trim());

    return new PageImpl<>(elements, PageRequest.of(page, Math.max(size, 1)), total);
  }

}
