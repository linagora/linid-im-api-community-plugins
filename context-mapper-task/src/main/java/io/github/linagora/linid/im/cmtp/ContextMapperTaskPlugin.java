/*
 * Copyright (C) 2020-2025 Linagora
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version, provided you comply with the Additional Terms applicable for LinID Directory Manager software by
 * LINAGORA pursuant to Section 7 of the GNU Affero General Public License, subsections (b), (c), and (e), pursuant to
 * which these Appropriate Legal Notices must notably (i) retain the display of the "LinID™" trademark/logo at the top
 * of the interface window, the display of the “You are using the Open Source and free version of LinID™, powered by
 * Linagora © 2009–2013. Contribute to LinID R&D by subscribing to an Enterprise offer!” infobox and in the e-mails
 * sent with the Program, notice appended to any type of outbound messages (e.g. e-mail and meeting requests) as well
 * as in the LinID Directory Manager user interface, (ii) retain all hypertext links between LinID Directory Manager
 * and https://linid.org/, as well as between LINAGORA and LINAGORA.com, and (iii) refrain from infringing LINAGORA
 * intellectual property rights over its trademarks and commercial brands. Other Additional Terms apply, see
 * <http://www.linagora.com/licenses/> for more details.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License and its applicable Additional Terms for
 * LinID Directory Manager along with this program. If not, see <http://www.gnu.org/licenses/> for the GNU Affero
 * General Public License version 3 and <http://www.linagora.com/licenses/> for the Additional Terms applicable to the
 * LinID Directory Manager software.
 */

package io.github.linagora.linid.im.cmtp;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.linagora.linid.im.corelib.plugin.config.JinjaService;
import io.github.linagora.linid.im.corelib.plugin.config.dto.TaskConfiguration;
import io.github.linagora.linid.im.corelib.plugin.entity.DynamicEntity;
import io.github.linagora.linid.im.corelib.plugin.task.TaskExecutionContext;
import io.github.linagora.linid.im.corelib.plugin.task.TaskPlugin;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * Task plugin that maps context values from one or more input keys to one or more output keys.
 *
 * <p>
 * This plugin reads a {@code mapping} configuration that specifies which keys from the context
 * should be used as sources, and to which output keys their values should be copied (or transformed).
 */
@Component
public class ContextMapperTaskPlugin implements TaskPlugin {

  /**
   * Service responsible for rendering templates using the Jinja templating engine (e.g., Jinjava).
   */
  private final JinjaService jinjaService;

  /**
   * Default constructor.
   *
   * @param jinjaService Service responsible for rendering templates using the Jinja templating engine (e.g., Jinjava).
   */
  @Autowired
  public ContextMapperTaskPlugin(final JinjaService jinjaService) {
    this.jinjaService = jinjaService;
  }

  @Override
  public boolean supports(@NonNull String type) {
    return "context-mapping".equals(type);
  }

  @Override
  public void execute(TaskConfiguration configuration, DynamicEntity entity, TaskExecutionContext context) {
    String defaultTemplate = configuration.getOption("default-template")
        .orElse("");
    var adding = configuration.getOption("adding", new TypeReference<Map<String, String>>() {})
        .orElse(Map.of());
    var mapping = configuration.getOption("mapping", new TypeReference<Map<String, String>>() {})
        .orElse(Map.of());
    var templates = configuration.getOption("templates", new TypeReference<Map<String, String>>() {})
        .orElse(Map.of());
    var removing = configuration.getOption("removing", new TypeReference<List<String>>() {})
        .orElse(List.of());

    adding.forEach((key, value) -> {
      context.put(
          key,
          jinjaService.render(context, value)
      );
    });

    mapping.forEach((inputKey, keys) -> {
      Arrays.stream(keys.split(","))
          .forEach(outputKey -> {
            String template = templates.getOrDefault(String.format("%s.%s", inputKey, outputKey),  defaultTemplate);
            String rendered = jinjaService.render(context, template);

            context.put(outputKey, rendered);
          });
    });

    removing.forEach(context::remove);
  }
}
