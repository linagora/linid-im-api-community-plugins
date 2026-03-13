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

package io.github.linagora.linid.im.dpp;

import io.github.linagora.linid.im.corelib.plugin.config.dto.ProviderConfiguration;
import io.github.linagora.linid.im.corelib.plugin.entity.DynamicEntity;
import io.github.linagora.linid.im.corelib.plugin.provider.ProviderPlugin;
import io.github.linagora.linid.im.corelib.plugin.task.TaskEngine;
import io.github.linagora.linid.im.corelib.plugin.task.TaskExecutionContext;
import io.github.linagora.linid.im.dpp.service.CrudService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

/**
 * Database Provider Plugin implementation.
 */
@Component
public class DatabaseProviderPlugin implements ProviderPlugin {

  private final CrudService crudService;
  private final TaskEngine taskEngine;

  @Autowired
  public DatabaseProviderPlugin(CrudService crudService, TaskEngine taskEngine) {
    this.crudService = crudService;
    this.taskEngine = taskEngine;
  }

  @Override
  public DynamicEntity create(
      TaskExecutionContext context,
      ProviderConfiguration config,
      DynamicEntity dynamicEntity) {

    taskEngine.execute(dynamicEntity, context, "beforeCreate");
    crudService.insert(config, dynamicEntity);
    taskEngine.execute(dynamicEntity, context, "afterCreate");

    return dynamicEntity;
  }

  /**
   * Deletes an entity from the database.
   *
   * @param context task execution context
   * @param config provider configuration
   * @param id entity identifier
   * @param dynamicEntity entity metadata
   * @return true if deletion succeeded
   */
  public boolean delete(
      TaskExecutionContext context,
      ProviderConfiguration config,
      String id,
      DynamicEntity dynamicEntity) {

    taskEngine.execute(dynamicEntity, context, "beforeDelete");
    crudService.delete(config, id, dynamicEntity);
    taskEngine.execute(dynamicEntity, context, "afterDelete");

    return true;
  }

  @Override
  public boolean supports(String type) {
    return "database".equals(type);
  }

  @Override
  public DynamicEntity patch(
      TaskExecutionContext context,
      ProviderConfiguration config,
      String id,
      DynamicEntity dynamicEntity) {

    taskEngine.execute(dynamicEntity, context, "beforePatch");
    crudService.update(config, id, dynamicEntity);
    taskEngine.execute(dynamicEntity, context, "afterPatch");

    return dynamicEntity;
  }

  @Override
  public DynamicEntity findById(
      TaskExecutionContext context,
      ProviderConfiguration config,
      String id,
      DynamicEntity dynamicEntity) {

    taskEngine.execute(dynamicEntity, context, "beforeFindById");
    DynamicEntity result = crudService.selectOne(config, id, dynamicEntity);
    taskEngine.execute(dynamicEntity, context, "afterFindById");

    return result;
  }

  @Override
  public Page<DynamicEntity> findAll(
      TaskExecutionContext context,
      ProviderConfiguration config,
      MultiValueMap<String, String> filters,
      Pageable pageable,
      DynamicEntity dynamicEntity) {

    taskEngine.execute(dynamicEntity, context, "beforeFindAll");
    Page<DynamicEntity> result = crudService.select(config, dynamicEntity);
    taskEngine.execute(dynamicEntity, context, "afterFindAll");

    return result;
  }

  @Override
  public DynamicEntity update(
      TaskExecutionContext context,
      ProviderConfiguration config,
      String id,
      DynamicEntity dynamicEntity) {

    taskEngine.execute(dynamicEntity, context, "beforeUpdate");
    crudService.update(config, id, dynamicEntity);
    taskEngine.execute(dynamicEntity, context, "afterUpdate");

    return dynamicEntity;
  }
}
