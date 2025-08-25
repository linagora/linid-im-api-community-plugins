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

package io.github.linagora.linid.im.hpp.service;

import io.github.linagora.linid.im.corelib.plugin.config.dto.ProviderConfiguration;
import io.github.linagora.linid.im.corelib.plugin.entity.DynamicEntity;
import io.github.linagora.linid.im.corelib.plugin.task.TaskExecutionContext;
import io.github.linagora.linid.im.hpp.model.EndpointConfiguration;

/**
 * Functional interface for performing HTTP requests in a dynamic and configurable way.
 *
 * <p>This service abstracts the execution of HTTP calls based on dynamic runtime context,
 * provider configuration, and endpoint configuration. It supports templated input using engines like Jinjava.
 */
@FunctionalInterface
public interface HttpService {

  /**
   * Executes an HTTP request based on the provided context and configurations.
   *
   * @param context the current execution context of the task; may contain runtime variables
   * @param configuration the global provider configuration (e.g., headers, base URL, credentials)
   * @param endpointConfiguration the specific endpoint configuration (URI, method, body, mappings, etc.)
   * @param action a symbolic name or type of action being performed (e.g., "get", "delete")
   * @param entity the entity involved in the request; used for templating and payload generation
   * @return the raw response body as a {@link String}
   */
  String request(TaskExecutionContext context,
                 ProviderConfiguration configuration,
                 EndpointConfiguration endpointConfiguration,
                 String action,
                 DynamicEntity entity);
}
