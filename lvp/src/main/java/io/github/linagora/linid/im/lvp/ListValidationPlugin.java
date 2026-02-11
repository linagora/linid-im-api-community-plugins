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

package io.github.linagora.linid.im.lvp;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import io.github.linagora.linid.im.corelib.plugin.config.dto.ValidationConfiguration;
import io.github.linagora.linid.im.corelib.plugin.validation.ValidationPlugin;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * List Validation Plugin implementation for LinID Directory Manager.
 *
 * <p>This plugin validates that a value is included in a predefined list of allowed values. The
 * list of allowed values must be provided in the plugin configuration under the "allowedValues"
 * option.
 *
 * <p>If the value is not included in the allowed values, an error message is returned indicating
 * the valid options. The plugin also handles cases where the "allowedValues" option is missing or
 * invalid, throwing an appropriate exception.
 */
@Slf4j
@Component
public class ListValidationPlugin implements ValidationPlugin {

  private static final String ALLOWED_VALUES = "allowedValues";

  @Override
  public boolean supports(@NonNull String type) {
    return "list".equals(type);
  }

  @Override
  public Optional<I18nMessage> validate(
      final ValidationConfiguration configuration, final Object value) {

    Map<String, Object> options = configuration.getOptions();

    if (!options.containsKey(ALLOWED_VALUES)) {
      throw new ApiException(
          500,
          I18nMessage.of("error.plugin.default.missing.option", Map.of("option", ALLOWED_VALUES)));
    }

    List<String> allowedValues = configuration.getOption(
            ALLOWED_VALUES,
            new TypeReference<List<String>>() {}
        )
        .orElseThrow(() -> new ApiException(
            500,
            I18nMessage.of(
                "error.plugin.default.invalid.option",
                Map.of("option", ALLOWED_VALUES)
            )
        ));

    if (value == null || !allowedValues.contains(value.toString())) {
      return Optional.of(
          I18nMessage.of(
              "error.plugin.listValidation.invalid.value",
              Map.of(ALLOWED_VALUES, allowedValues, "value", String.valueOf(value))));
    }

    return Optional.empty();
  }
}
