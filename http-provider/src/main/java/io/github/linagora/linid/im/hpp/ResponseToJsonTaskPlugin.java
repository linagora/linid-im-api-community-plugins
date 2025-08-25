/*
 * Copyright (C) 2020-2025 Linagora
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

package io.github.linagora.linid.im.hpp;

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
 * Task plugin that converts a JSON string response stored in the task execution context into a parsed JSON object (Map&lt;String,
 * Object&gt;).
 *
 * <p>This plugin supports the task type "response-to-json".
 *
 * <p>During execution, it retrieves the JSON string under the "response" key in the context,
 * parses it into a Map using Jackson's ObjectMapper, and replaces the context's "response" value with the parsed Map.
 *
 * <p>If the JSON parsing fails, it throws an {@link ApiException} with an appropriate error message.
 */
@Component
public class ResponseToJsonTaskPlugin implements TaskPlugin {

  @Override
  public boolean supports(final @NonNull String type) {
    return "response-to-json".equals(type);
  }

  @Override
  public void execute(final TaskConfiguration configuration,
                      final DynamicEntity entity,
                      final TaskExecutionContext context) {
    String json = (String) context.get("response");

    try {
      var response = new ObjectMapper().readValue(json, new TypeReference<Map<String, Object>>() {
      });
      context.put("response", response);
    } catch (JsonProcessingException e) {
      throw new ApiException(500, I18nMessage.of("hpp.error.json"));
    }
  }

}
