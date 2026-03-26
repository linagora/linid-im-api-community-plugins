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

import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import io.github.linagora.linid.im.corelib.plugin.config.dto.AttributeConfiguration;
import io.github.linagora.linid.im.corelib.plugin.config.dto.ProviderConfiguration;
import io.github.linagora.linid.im.corelib.plugin.entity.DynamicEntity;
import io.github.linagora.linid.im.corelib.plugin.provider.ProviderPlugin;
import io.github.linagora.linid.im.corelib.plugin.task.TaskExecutionContext;
import io.github.linagora.linid.im.dpp.model.DatabasePluginConfiguration;
import io.github.linagora.linid.im.dpp.service.CrudService;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Database Provider Plugin implementation.
 */
@Component
public class DatabaseProviderPlugin implements ProviderPlugin {

  /**
   * Service to perform CRUD operations.
   */
  private final CrudService crudService;

  /**
   * Jackson ObjectMapper used to convert configuration maps into
   * {@link DatabasePluginConfiguration} instances.
   */
  private final ObjectMapper mapper = new ObjectMapper();

  /**
   * Constructor for DatabaseProviderPlugin.
   *
   * @param crudService the service to perform CRUD operations based on provider
   *                    configuration and dynamic entity metadata
   */
  @Autowired
  public DatabaseProviderPlugin(final CrudService crudService) {
    this.crudService = crudService;
  }

  @Override
  public boolean supports(String type) {
    return "database".equals(type);
  }

  @Override
  public DynamicEntity create(final TaskExecutionContext context,
                              final ProviderConfiguration config,
                              final DynamicEntity dynamicEntity) {
    DatabasePluginConfiguration databasePluginConfiguration = getDatabaseConfiguration("create", dynamicEntity);

    DynamicEntity result = crudService.insert(config, databasePluginConfiguration, dynamicEntity);

    return result;
  }

  @Override
  public boolean delete(final TaskExecutionContext context,
                        final ProviderConfiguration config,
                        final String id,
                        final DynamicEntity dynamicEntity) {
    DatabasePluginConfiguration databasePluginConfiguration = getDatabaseConfiguration("delete", dynamicEntity);
    String type = resolveIdType(dynamicEntity);

    crudService.delete(config, databasePluginConfiguration, mapId(type, id), dynamicEntity);

    return true;
  }

  @Override
  public DynamicEntity patch(final TaskExecutionContext context,
                             final ProviderConfiguration config,
                             final String id,
                             final DynamicEntity dynamicEntity) {
    DatabasePluginConfiguration databasePluginConfiguration = getDatabaseConfiguration("patch", dynamicEntity);
    String type = resolveIdType(dynamicEntity);
    Object validId = mapId(type, id);

    // Perform a partial update using the provided dynamicEntity (patch payload).
    DynamicEntity result = crudService.patch(config, databasePluginConfiguration, validId, dynamicEntity);

    return result;
  }

  @Override
  public DynamicEntity findById(final TaskExecutionContext context,
                                final ProviderConfiguration config,
                                final String id,
                                final DynamicEntity dynamicEntity) {
    DatabasePluginConfiguration databasePluginConfiguration = getDatabaseConfiguration("findById", dynamicEntity);
    String type = resolveIdType(dynamicEntity);

    DynamicEntity result = crudService.selectOne(
        context,
        config,
        databasePluginConfiguration,
        mapId(type, id),
        dynamicEntity);

    return result;
  }

  @Override
  public Page<DynamicEntity> findAll(final TaskExecutionContext context,
                                     final ProviderConfiguration config,
                                     final MultiValueMap<String, String> filters,
                                     final Pageable pageable,
                                     final DynamicEntity dynamicEntity) {

    DatabasePluginConfiguration databasePluginConfiguration = getDatabaseConfiguration("findAll", dynamicEntity);

    Page<DynamicEntity> result = crudService.select(config, databasePluginConfiguration, dynamicEntity,
        pageable);

    return result;
  }

  @Override
  public DynamicEntity update(final TaskExecutionContext context,
                              final ProviderConfiguration config,
                              final String id,
                              final DynamicEntity dynamicEntity) {
    DatabasePluginConfiguration databasePluginConfiguration = getDatabaseConfiguration("update", dynamicEntity);
    String type = resolveIdType(dynamicEntity);

    DynamicEntity result = crudService.update(
        config,
        databasePluginConfiguration,
        mapId(type, id),
        dynamicEntity);

    return result;
  }

  /**
   * Extracts and converts the database configuration for the specified action
   * from the dynamic entity's configuration.
   *
   * @param action        the action name (e.g., "create", "update", "delete").
   * @param dynamicEntity the entity containing configuration maps.
   * @return the database configuration or an empty configuration if none found.
   */
  public DatabasePluginConfiguration getDatabaseConfiguration(final String action, final DynamicEntity dynamicEntity) {
    var access = dynamicEntity.getConfiguration().getAccess();

    if (!access.containsKey(action)) {
      return new DatabasePluginConfiguration();
    }

    return mapper.convertValue(
        access.get(action),
        new TypeReference<>() {
        }
    );
  }

  /**
   * Converts the given id string to the expected type.
   *
   * @param type the expected type (Long, Integer, UUID, etc.)
   * @param id   the id value as string
   * @return the converted id
   */
  public Object mapId(final String type, final String id) {
    return switch (type) {
      case "Long" -> Long.parseLong(id);
      case "Integer" -> Integer.parseInt(id);
      case "Double" -> Double.parseDouble(id);
      case "UUID" -> UUID.fromString(id);
      default -> id;
    };
  }

  /**
   * Resolves the type of the primary key attribute.
   *
   * @param dynamicEntity the entity configuration
   * @return the type of the primary key
   * @throws ApiException if no primary key is defined
   */
  public String resolveIdType(final DynamicEntity dynamicEntity) {
    return dynamicEntity.getConfiguration().getAttributes().stream()
        .filter(attr -> Boolean.TRUE.equals(attr.getAccess().get("primaryKey")))
        .findFirst()
        .map(AttributeConfiguration::getType)
        .orElseThrow(
            () -> new ApiException(
                500,
                I18nMessage.of(
                    "dpp.error.noPrimary",
                    Map.of("entity", dynamicEntity.getConfiguration().getName()))));
  }
}
