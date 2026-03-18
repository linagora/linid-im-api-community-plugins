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

package io.github.linagora.linid.im.cmtp;

import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import io.github.linagora.linid.im.corelib.plugin.config.JinjaService;
import io.github.linagora.linid.im.corelib.plugin.config.dto.ValidationConfiguration;
import io.github.linagora.linid.im.corelib.plugin.task.TaskExecutionContext;
import io.github.linagora.linid.im.corelib.plugin.validation.ValidationPlugin;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * A {@link ValidationPlugin} implementation that compares two string values resolved from the
 * execution context using Jinja templates, with optional normalization (trim and case-insensitive
 * comparison).
 */
@Component
public class ContextCompareValidationPlugin implements ValidationPlugin {

  private static final String VALUE_1 = "value1";
  private static final String VALUE_2 = "value2";

  private final JinjaService jinjaService;

  /**
   * Default constructor.
   *
   * @param jinjaService Service responsible for rendering Jinja templates.
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
  public Optional<I18nMessage> validate(ValidationConfiguration configuration, Object value,
      TaskExecutionContext context) {
    var template1 = configuration.getOption(VALUE_1)
        .orElseThrow(() -> new ApiException(
            500,
            I18nMessage.of("error.plugin.default.missing.option", Map.of("option", VALUE_1))
        ));

    var template2 = configuration.getOption(VALUE_2)
        .orElseThrow(() -> new ApiException(
            500,
            I18nMessage.of("error.plugin.default.missing.option", Map.of("option", VALUE_2))
        ));

    String val1 = jinjaService.render(context, template1);
    String val2 = jinjaService.render(context, template2);

    if (val1 == null || val2 == null) {
      return Optional.of(I18nMessage.of(
          "error.plugin.contextCompareValidation.null.value",
          Map.of(VALUE_1, String.valueOf(val1), VALUE_2, String.valueOf(val2))));
    }

    boolean trim = configuration.getOption("trim", Boolean.class).orElse(false);
    boolean ignoreCase = configuration.getOption("ignoreCase", Boolean.class).orElse(false);

    if (trim) {
      val1 = val1.strip();
      val2 = val2.strip();
    }

    boolean match = ignoreCase
        ? val1.equalsIgnoreCase(val2)
        : val1.equals(val2);

    if (!match) {
      return Optional.of(I18nMessage.of(
          "error.plugin.contextCompareValidation.mismatch",
          Map.of(VALUE_1, val1, VALUE_2, val2)));
    }

    return Optional.empty();
  }
}
