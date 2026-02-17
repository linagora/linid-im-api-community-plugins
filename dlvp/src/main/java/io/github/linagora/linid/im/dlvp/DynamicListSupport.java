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
import io.github.linagora.linid.im.corelib.plugin.config.JinjaService;
import io.github.linagora.linid.im.corelib.plugin.config.dto.EntityConfiguration;
import io.github.linagora.linid.im.corelib.plugin.config.dto.PluginConfiguration;
import io.github.linagora.linid.im.corelib.plugin.config.dto.TaskConfiguration;
import io.github.linagora.linid.im.corelib.plugin.entity.DynamicEntity;
import io.github.linagora.linid.im.corelib.plugin.task.TaskExecutionContext;
import io.github.linagora.linid.im.dlvp.model.DynamicListConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Shared support interface for Dynamic List plugins.
 *
 * <p>Provides default methods for extracting values from parsed API responses
 * and building dynamic entities from plugin configurations.
 *
 * <p>Implemented by both {@link DynamicListRoutePlugin} and {@link DynamicListValidationPlugin}.
 */
public interface DynamicListSupport {

  /**
   * Extracts string values from the parsed response using Jinja templates.
   *
   * <p>Uses the {@code itemsCount} template to determine how many items to extract,
   * then iterates using the {@code elementValue} template with an {@code index} variable.
   *
   * @param jinjaService the Jinja template rendering service
   * @param context the task execution context containing the parsed response
   * @param dlConfig the dynamic list configuration with itemsCount and elementValue templates
   * @return the list of extracted string values
   */
  default List<String> extractValues(JinjaService jinjaService,
                                     TaskExecutionContext context,
                                     DynamicListConfiguration dlConfig) {
    DynamicEntity emptyEntity = new DynamicEntity();

    int itemsCount = Integer.parseInt(
        jinjaService.render(context, emptyEntity, dlConfig.getItemsCount()).trim()
    );

    List<String> values = new ArrayList<>();

    for (int i = 0; i < itemsCount; i++) {
      String value = jinjaService.render(
          context, emptyEntity, Map.of("index", i), dlConfig.getElementValue());
      values.add(value);
    }

    return values;
  }

  /**
   * Builds a {@link DynamicEntity} with task configurations extracted from the plugin configuration.
   *
   * <p>TODO: Tasks should be part of PluginConfiguration directly.
   * See <a href="https://github.com/linagora/linid-im-api-corelib/issues/15">linid-im-api-corelib#15</a>.
   *
   * @param configuration the plugin configuration containing optional task definitions
   * @return a dynamic entity with task configurations set
   */
  default DynamicEntity buildDynamicEntity(PluginConfiguration configuration) {
    DynamicEntity entity = new DynamicEntity();
    EntityConfiguration config = new EntityConfiguration();

    configuration
        .getOption("tasks", new TypeReference<List<TaskConfiguration>>() {
        })
        .ifPresent(config::setTasks);

    entity.setConfiguration(config);
    return entity;
  }
}
