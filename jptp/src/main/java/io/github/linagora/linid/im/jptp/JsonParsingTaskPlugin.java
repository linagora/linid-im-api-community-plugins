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

package io.github.linagora.linid.im.jptp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import io.github.linagora.linid.im.corelib.plugin.config.dto.TaskConfiguration;
import io.github.linagora.linid.im.corelib.plugin.entity.DynamicEntity;
import io.github.linagora.linid.im.corelib.plugin.task.TaskExecutionContext;
import io.github.linagora.linid.im.corelib.plugin.task.TaskPlugin;
import java.util.Map;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * Task plugin that parses a JSON string from the execution context.
 *
 * <p>This plugin reads a value from a configurable {@code source} key in the context, parses it as
 * JSON, and stores the result under a configurable {@code destination} key.
 */
@Component
public class JsonParsingTaskPlugin implements TaskPlugin {

  private static final String MISSING_OPTION = "error.plugin.default.missing.option";
  private static final String OPTION = "option";

  /** Shared ObjectMapper instance for JSON parsing. */
  private final ObjectMapper mapper = new ObjectMapper();

  @Override
  public boolean supports(final @NonNull String type) {
    return "json-parsing".equals(type);
  }

  @Override
  public void execute(final TaskConfiguration configuration,
                      final DynamicEntity entity,
                      final TaskExecutionContext context) {
    String source = configuration.getOption("source")
        .orElseThrow(() -> new ApiException(500,
            I18nMessage.of(MISSING_OPTION, Map.of(OPTION, "source"))));
    String destination = configuration.getOption("destination")
        .orElseThrow(() -> new ApiException(500,
            I18nMessage.of(MISSING_OPTION, Map.of(OPTION, "destination"))));

    String json = (String) context.get(source);

    if (json == null) {
      throw new ApiException(500, I18nMessage.of("jptp.error.source.unknown",
          Map.of("source", source)));
    }

    try {
      Object parsed = mapper.readValue(json, new TypeReference<Object>() {});
      context.put(destination, parsed);
    } catch (JsonProcessingException e) {
      throw new ApiException(500, I18nMessage.of("jptp.error.json.parsing",
          Map.of("source", source)));
    }
  }
}
