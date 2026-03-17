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

package io.github.linagora.linid.im.ccvp;

import io.github.linagora.linid.im.ccvp.model.ContextCompareOptions;
import io.github.linagora.linid.im.ccvp.model.ContextCompareValidation;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import io.github.linagora.linid.im.corelib.plugin.config.JinjaService;
import io.github.linagora.linid.im.corelib.plugin.config.dto.TaskConfiguration;
import io.github.linagora.linid.im.corelib.plugin.entity.DynamicEntity;
import io.github.linagora.linid.im.corelib.plugin.task.TaskExecutionContext;
import io.github.linagora.linid.im.corelib.plugin.task.TaskPlugin;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;

/**
 * Task plugin that validates context values by comparing pairs resolved from Jinja templates.
 *
 * <p>
 * This plugin reads a list of {@code validation} entries from the task configuration.
 * Each entry specifies two Jinja templates ({@code value1} and {@code value2}) that are
 * resolved from the execution context and compared. Optional normalization options
 * ({@code trim}, {@code ignoreCase}) can be applied before comparison.
 */
@Component
public class ContextCompareValidationPlugin implements TaskPlugin {

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
  public ContextCompareValidationPlugin(final JinjaService jinjaService) {
    this.jinjaService = jinjaService;
  }

  @Override
  public boolean supports(@NonNull String type) {
    return "context-compare".equals(type);
  }

  @Override
  public void execute(TaskConfiguration configuration, DynamicEntity entity, TaskExecutionContext context) {
    var validations = configuration.getOption("validation",
            new TypeReference<List<ContextCompareValidation>>() {})
        .orElse(List.of());

    for (ContextCompareValidation entry : validations) {
      if (entry.value1() == null || entry.value2() == null) {
        throw new ApiException(400, I18nMessage.of(
            "ccvp.error.context.compare.null.value"));
      }

      String rendered1 = jinjaService.render(context, entry.value1());
      String rendered2 = jinjaService.render(context, entry.value2());

      ContextCompareOptions opts = entry.options();

      if (opts != null && Boolean.TRUE.equals(opts.trim())) {
        rendered1 = rendered1.strip();
        rendered2 = rendered2.strip();
      }

      boolean match = opts != null && Boolean.TRUE.equals(opts.ignoreCase())
          ? rendered1.equalsIgnoreCase(rendered2)
          : rendered1.equals(rendered2);

      if (!match) {
        throw new ApiException(400, I18nMessage.of(
            "ccvp.error.context.compare.mismatch",
            Map.of("value1", rendered1, "value2", rendered2)));
      }
    }
  }
}
