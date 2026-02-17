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

package io.github.linagora.linid.im.dlvp.service;

import io.github.linagora.linid.im.corelib.plugin.config.dto.PluginConfiguration;
import io.github.linagora.linid.im.corelib.plugin.task.TaskExecutionContext;

/**
 * Service interface for executing HTTP requests to external APIs.
 *
 * <p>This service is responsible only for performing the HTTP call and returning the raw response body.
 * It supports GET and POST methods, custom headers, and optional request body.
 *
 * <p>Shared between {@link io.github.linagora.linid.im.dlvp.DynamicListRoutePlugin}
 * and {@link io.github.linagora.linid.im.dlvp.DynamicListValidationPlugin}.
 */
@FunctionalInterface
public interface HttpService {

  /**
   * Executes an HTTP request based on the provided plugin configuration.
   *
   * <p>Required configuration options:
   * <ul>
   *   <li>{@code url} - the external API URL to call</li>
   *   <li>{@code method} - the HTTP method (GET or POST)</li>
   * </ul>
   *
   * <p>Optional configuration options:
   * <ul>
   *   <li>{@code headers} - HTTP headers to send with the request</li>
   *   <li>{@code body} - request body (for POST requests)</li>
   * </ul>
   *
   * @param context the current task execution context, used for Jinja template rendering
   * @param configuration the plugin configuration containing request options
   * @return the raw response body as a string
   */
  String request(TaskExecutionContext context, PluginConfiguration configuration);
}
