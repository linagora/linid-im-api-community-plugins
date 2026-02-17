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
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import io.github.linagora.linid.im.corelib.plugin.config.JinjaService;
import io.github.linagora.linid.im.corelib.plugin.config.dto.ValidationConfiguration;
import io.github.linagora.linid.im.corelib.plugin.entity.DynamicEntity;
import io.github.linagora.linid.im.corelib.plugin.task.TaskEngine;
import io.github.linagora.linid.im.corelib.plugin.task.TaskExecutionContext;
import io.github.linagora.linid.im.corelib.plugin.validation.ValidationPlugin;
import io.github.linagora.linid.im.dlvp.model.DynamicListConfiguration;
import io.github.linagora.linid.im.dlvp.service.HttpService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * Dynamic List Validation Plugin implementation for LinID Directory Manager.
 *
 * <p>This plugin validates that a value is included in a dynamic list of allowed values
 * fetched from an external API. The external API URL and response mapping are provided
 * in the plugin configuration.
 *
 * <p>The plugin orchestrates HTTP calls, task execution (e.g., json-parsing),
 * and value extraction using Jinja templates before performing the validation check.
 */
@Slf4j
@Component
public class DynamicListValidationPlugin implements ValidationPlugin, DynamicListSupport {

  /** Service used to execute HTTP requests to external APIs. */
  private final HttpService httpService;

  /** Service used to render Jinja templates for value extraction. */
  private final JinjaService jinjaService;

  /** Engine to execute lifecycle tasks before and after response mapping. */
  private final TaskEngine taskEngine;

  /** Object mapper for converting configuration options to typed models. */
  private final ObjectMapper mapper = new ObjectMapper();

  /**
   * Default constructor.
   *
   * @param httpService service used to execute HTTP requests to external APIs
   * @param jinjaService service used to render Jinja templates for value extraction
   * @param taskEngine engine to execute lifecycle tasks before and after response mapping
   */
  @Autowired
  public DynamicListValidationPlugin(final HttpService httpService,
                                     final JinjaService jinjaService,
                                     final TaskEngine taskEngine) {
    this.httpService = httpService;
    this.jinjaService = jinjaService;
    this.taskEngine = taskEngine;
  }

  @Override
  public boolean supports(@NonNull final String type) {
    return "dynamic-list".equals(type);
  }

  @Override
  public Optional<I18nMessage> validate(
      final ValidationConfiguration configuration, final Object value) {

    if (value == null) {
      return Optional.of(
          I18nMessage.of(
              "error.plugin.dynamicListValidation.invalid.value",
              Map.of(
                  "allowedValues", List.of(),
                  "value", "null"
              )
          )
      );
    }

    DynamicListConfiguration dlConfig = mapper.convertValue(
        configuration.getOptions(), new TypeReference<>() {
        });

    TaskExecutionContext context = new TaskExecutionContext();
    context.put("value", value);
    DynamicEntity entity = buildDynamicEntity(configuration);

    taskEngine.execute(entity, context, "beforeDynamicList");

    String response = httpService.request(context, configuration);
    context.put("response", response);

    taskEngine.execute(entity, context, "afterDynamicList");

    taskEngine.execute(entity, context, "beforeDynamicListMapping");

    List<String> allowedValues = extractValues(jinjaService, context, dlConfig);

    taskEngine.execute(entity, context, "afterDynamicListMapping");

    if (!allowedValues.contains(value.toString())) {
      return Optional.of(
          I18nMessage.of(
              "error.plugin.dynamicListValidation.invalid.value",
              Map.of(
                  "allowedValues", allowedValues,
                  "value", String.valueOf(value)
              )
          )
      );
    }

    return Optional.empty();
  }

}
