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

package io.github.linagora.linid.im.myplugin;

import io.github.linagora.linid.im.corelib.plugin.config.dto.ProviderConfiguration;
import io.github.linagora.linid.im.corelib.plugin.entity.DynamicEntity;
import io.github.linagora.linid.im.corelib.plugin.provider.ProviderPlugin;
import io.github.linagora.linid.im.corelib.plugin.task.TaskExecutionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

/**
 * MyPlugin provider javadoc.
 */
@Slf4j
@Component
public class MyPluginProviderPlugin implements ProviderPlugin {

  @Override
  public boolean supports(final @NonNull String type) {
    return "MyPlugin".equals(type);
  }

  @Override
  public DynamicEntity create(final TaskExecutionContext context,
                              final ProviderConfiguration configuration,
                              final DynamicEntity dynamicEntity) {
    log.info("Creating new entity on provider 'MyPlugin'");
    return null;
  }

  @Override
  public DynamicEntity update(final TaskExecutionContext context,
                              final ProviderConfiguration configuration,
                              final String id,
                              final DynamicEntity dynamicEntity) {
    log.info("Updating entity on provider 'MyPlugin'");
    return null;
  }

  @Override
  public DynamicEntity patch(final TaskExecutionContext context,
                             final ProviderConfiguration configuration,
                             final String id,
                             final DynamicEntity dynamicEntity) {
    log.info("Patching entity on provider 'MyPlugin'");
    return null;
  }

  @Override
  public boolean delete(final TaskExecutionContext context,
                        final ProviderConfiguration configuration,
                        final String id,
                        final DynamicEntity dynamicEntity) {
    log.info("Delete entity on provider 'MyPlugin'");
    return false;
  }

  @Override
  public DynamicEntity findById(final TaskExecutionContext context,
                                final ProviderConfiguration configuration,
                                final String id,
                                final DynamicEntity dynamicEntity) {
    log.info("Finding entity by id on provider 'MyPlugin'");
    return null;
  }

  @Override
  public Page<DynamicEntity> findAll(final TaskExecutionContext context,
                                     final ProviderConfiguration configuration,
                                     final MultiValueMap<String, String> filters,
                                     final Pageable pageable,
                                     final DynamicEntity dynamicEntity) {
    log.info("Finding all entities on provider 'MyPlugin'");
    return null;
  }
}
