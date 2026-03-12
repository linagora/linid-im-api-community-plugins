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

package io.github.linagora.linid.im.dpp.service;

import io.github.linagora.linid.im.corelib.plugin.config.dto.ProviderConfiguration;
import io.github.linagora.linid.im.corelib.plugin.entity.DynamicEntity;
import io.github.linagora.linid.im.corelib.plugin.task.TaskExecutionContext;
import io.github.linagora.linid.im.dpp.model.DatabasePluginConfiguration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interface for performing basic database operations using jOOQ.
 *
 * <p>All operations are dynamic and configuration-driven: table names and column names
 * are resolved at runtime from the entity configuration, without static entity definitions.</p>
 */
public interface CrudService {

  /**
   * Selects all rows from the given table.
   *
   * @param config the provider configuration (datasource credentials, pool settings)
   * @param databaseConfiguration the database configuration containing the table name
   * @param dynamicEntity the dynamic entity containing the table name in its configuration
   * @param pageable the pagination information (page number, page size, sorting)
   * @return the list
   */
  Page<DynamicEntity> select(ProviderConfiguration config,
                             DatabasePluginConfiguration databaseConfiguration,
                             DynamicEntity dynamicEntity,
                             Pageable pageable);

  /**
   * Selects a single row from the given table matching the given id.
   *
   * @param context the task execution context
   * @param config the provider configuration (datasource credentials, pool settings)
   * @param databaseConfiguration the database configuration containing the table name
   * @param id the value the identifier column must match
   * @param dynamicEntity the dynamic entity containing the table name in its configuration
   * @return the matching entity, or null if not found
   */
  DynamicEntity selectOne(TaskExecutionContext context,
                          ProviderConfiguration config,
                          DatabasePluginConfiguration databaseConfiguration,
                          Object id,
                          DynamicEntity dynamicEntity);

  /**
   * Inserts a new row into the given table.
   *
   * @param config the provider configuration (datasource credentials, pool settings)
   * @param databaseConfiguration the database configuration containing the table name
   * @param dynamicEntity the dynamic entity containing the table name and values in its configuration
   * @return the inserted entity
   */
  DynamicEntity insert(ProviderConfiguration config,
                       DatabasePluginConfiguration databaseConfiguration,
                       DynamicEntity dynamicEntity);

  /**
   * Updates rows in the given table where the id column matches the given value.
   *
   * @param config the provider configuration (datasource credentials, pool settings)
   * @param databaseConfiguration the database configuration containing the table name
   * @param id the value the identifier column must match
   * @param dynamicEntity the dynamic entity containing the table name and values in its configuration
   * @return the updated entity
   */
  DynamicEntity update(ProviderConfiguration config,
                       DatabasePluginConfiguration databaseConfiguration,
                       Object id,
                       DynamicEntity dynamicEntity);

  /**
   * Updates rows in the given table where the id column matches the given value.
   *
   * @param config the provider configuration (datasource credentials, pool settings)
   * @param databaseConfiguration the database configuration containing the table name
   * @param id the value the identifier column must match
   * @param dynamicEntity the dynamic entity containing the table name and values in its configuration
   * @return the updated entity
   */
  DynamicEntity patch(ProviderConfiguration config,
                      DatabasePluginConfiguration databaseConfiguration,
                      Object id,
                      DynamicEntity dynamicEntity);

  /**
   * Deletes rows from the given table where the id column matches the given value.
   *
   * @param config the provider configuration (datasource credentials, pool settings)
   * @param databaseConfiguration the database configuration containing the table name
   * @param id the value the identifier column must match
   * @param dynamicEntity the dynamic entity containing the table name in its configuration
   */
  void delete(ProviderConfiguration config,
              DatabasePluginConfiguration databaseConfiguration,
              Object id,
              DynamicEntity dynamicEntity);
}
