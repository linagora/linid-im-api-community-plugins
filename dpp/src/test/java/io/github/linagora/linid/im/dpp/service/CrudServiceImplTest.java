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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.plugin.config.dto.EntityConfiguration;
import io.github.linagora.linid.im.corelib.plugin.config.dto.ProviderConfiguration;
import io.github.linagora.linid.im.corelib.plugin.entity.DynamicEntity;
import io.github.linagora.linid.im.corelib.plugin.task.TaskEngine;
import io.github.linagora.linid.im.dpp.model.DatabasePluginConfiguration;
import io.github.linagora.linid.im.dpp.registry.DslRegistry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jooq.DSLContext;
import org.jooq.SortField;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

/**
 * Unit tests for {@link CrudServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Test class: CrudServiceImpl")
class CrudServiceImplTest {

  @Mock
  private DslRegistry dslRegistry;

  @Mock
  private TaskEngine taskEngine;

  @Mock
  private DSLContext dsl;

  @Test
  @DisplayName("test resolveIdColumn: should throw exception when no primary key configured for update")
  void testUpdateWithoutPrimaryKey() {
    CrudServiceImpl service = new CrudServiceImpl(taskEngine, dslRegistry);
    ProviderConfiguration providerConfig = new ProviderConfiguration();

    EntityConfiguration entityConfig = new EntityConfiguration();
    entityConfig.setAccess(new HashMap<>());
    entityConfig.getAccess().put("update", Map.of("table", "users"));
    entityConfig.setName("users");
    entityConfig.setAttributes(List.of()); // No primary key

    DynamicEntity dynamicEntity = new DynamicEntity();
    dynamicEntity.setConfiguration(entityConfig);
    dynamicEntity.setAttributes(new HashMap<>());
    dynamicEntity.getAttributes().put("name", "Test");
    DatabasePluginConfiguration databasePluginConfiguration = new DatabasePluginConfiguration();

    databasePluginConfiguration.setTable("users");

    ApiException exception = assertThrows(ApiException.class, () -> {
      service.update(providerConfig, databasePluginConfiguration, "1", dynamicEntity);
    });

    assertEquals("dpp.error.no-primary", exception.getMessage());
  }

  @Test
  @DisplayName("test resolveIdColumn: should throw exception when no primary key configured for delete")
  void testDeleteWithoutPrimaryKey() {
    CrudServiceImpl service = new CrudServiceImpl(taskEngine, dslRegistry);
    ProviderConfiguration providerConfig = new ProviderConfiguration();

    EntityConfiguration entityConfig = new EntityConfiguration();
    entityConfig.setAccess(new HashMap<>());
    entityConfig.getAccess().put("delete", Map.of("table", "users"));
    entityConfig.setName("users");
    entityConfig.setAttributes(List.of()); // No primary key

    DynamicEntity dynamicEntity = new DynamicEntity();
    dynamicEntity.setConfiguration(entityConfig);
    dynamicEntity.setAttributes(new HashMap<>());
    DatabasePluginConfiguration databasePluginConfiguration = new DatabasePluginConfiguration();
    databasePluginConfiguration.setTable("users");

    ApiException exception = assertThrows(ApiException.class, () -> {
      service.delete(providerConfig, databasePluginConfiguration, "1", dynamicEntity);
    });

    assertEquals("dpp.error.no-primary", exception.getMessage());
  }

  @Test
  @DisplayName("test resolveIdColumn: should throw exception when no primary key configured for selectOne")
  void testSelectOneWithoutPrimaryKey() {
    CrudServiceImpl service = new CrudServiceImpl(taskEngine, dslRegistry);
    ProviderConfiguration providerConfig = new ProviderConfiguration();

    EntityConfiguration entityConfig = new EntityConfiguration();
    entityConfig.setAccess(new HashMap<>());
    entityConfig.getAccess().put("select", Map.of("table", "users"));
    entityConfig.setName("users");
    entityConfig.setAttributes(List.of()); // No primary key

    DynamicEntity dynamicEntity = new DynamicEntity();
    dynamicEntity.setConfiguration(entityConfig);
    dynamicEntity.setAttributes(new HashMap<>());
    DatabasePluginConfiguration databasePluginConfiguration = new DatabasePluginConfiguration();

    databasePluginConfiguration.setTable("users");

    ApiException exception = assertThrows(ApiException.class, () -> {
      service.selectOne(null, providerConfig, databasePluginConfiguration, "1", dynamicEntity);
    });

    assertEquals("dpp.error.no-primary", exception.getMessage());
  }

  @Test
  @DisplayName("test insert: should throw exception when create access configuration is missing")
  void testInsertWithMissingCreateAccess() {
    CrudServiceImpl service = new CrudServiceImpl(taskEngine, dslRegistry);
    ProviderConfiguration providerConfig = new ProviderConfiguration();

    EntityConfiguration entityConfig = new EntityConfiguration();
    entityConfig.setAccess(new HashMap<>());
    entityConfig.setName("users");
    entityConfig.setAttributes(List.of());

    DynamicEntity dynamicEntity = new DynamicEntity();
    dynamicEntity.setConfiguration(entityConfig);
    dynamicEntity.setAttributes(new HashMap<>());
    DatabasePluginConfiguration databasePluginConfiguration = new DatabasePluginConfiguration();
    databasePluginConfiguration.setTable("users");

    assertThrows(ApiException.class, () -> {
      service.insert(providerConfig, databasePluginConfiguration, dynamicEntity);
    });
  }

  @Test
  @DisplayName("test update: should throw exception when update access configuration is missing")
  void testUpdateWithMissingUpdateAccess() {
    CrudServiceImpl service = new CrudServiceImpl(taskEngine, dslRegistry);
    ProviderConfiguration providerConfig = new ProviderConfiguration();

    EntityConfiguration entityConfig = new EntityConfiguration();
    entityConfig.setAccess(new HashMap<>());
    entityConfig.setName("users");
    entityConfig.setAttributes(List.of());

    DynamicEntity dynamicEntity = new DynamicEntity();
    dynamicEntity.setConfiguration(entityConfig);
    dynamicEntity.setAttributes(new HashMap<>());
    DatabasePluginConfiguration databasePluginConfiguration = new DatabasePluginConfiguration();

    databasePluginConfiguration.setTable("users");

    assertThrows(ApiException.class, () -> {
      service.update(providerConfig, databasePluginConfiguration, "1", dynamicEntity);
    });
  }

  @Test
  @DisplayName("test patch: should throw exception when update access configuration is missing")
  void testPatchWithMissingUpdateAccess() {
    CrudServiceImpl service = new CrudServiceImpl(taskEngine, dslRegistry);
    ProviderConfiguration providerConfig = new ProviderConfiguration();

    EntityConfiguration entityConfig = new EntityConfiguration();
    entityConfig.setAccess(new HashMap<>());
    entityConfig.setName("users");
    entityConfig.setAttributes(List.of());

    DynamicEntity dynamicEntity = new DynamicEntity();
    dynamicEntity.setConfiguration(entityConfig);
    dynamicEntity.setAttributes(new HashMap<>());
    DatabasePluginConfiguration databasePluginConfiguration = new DatabasePluginConfiguration();

    databasePluginConfiguration.setTable("users");

    assertThrows(ApiException.class, () -> {
      service.patch(providerConfig, databasePluginConfiguration, "1", dynamicEntity);
    });
  }

  @Test
  @DisplayName("test resolveIdColumn: should throw exception when no primary key configured for patch")
  void testPatchWithoutPrimaryKey() {
    CrudServiceImpl service = new CrudServiceImpl(taskEngine, dslRegistry);
    ProviderConfiguration providerConfig = new ProviderConfiguration();

    EntityConfiguration entityConfig = new EntityConfiguration();
    entityConfig.setAccess(new HashMap<>());
    entityConfig.getAccess().put("patch", Map.of("table", "users"));
    entityConfig.setName("users");
    entityConfig.setAttributes(List.of()); // No primary key

    DynamicEntity dynamicEntity = new DynamicEntity();
    dynamicEntity.setConfiguration(entityConfig);
    dynamicEntity.setAttributes(new HashMap<>());
    DatabasePluginConfiguration databasePluginConfiguration = new DatabasePluginConfiguration();

    databasePluginConfiguration.setTable("users");

    ApiException exception = assertThrows(ApiException.class, () -> {
      service.patch(providerConfig, databasePluginConfiguration, "1", dynamicEntity);
    });

    assertEquals("dpp.error.no-primary", exception.getMessage());
  }

  @Test
  @DisplayName("test toSortField: should build sort field from Sort.Order")
  void testToSortField() {
    CrudServiceImpl service = new CrudServiceImpl(taskEngine, dslRegistry);
    Sort.Order ascOrder = Sort.Order.asc("name");
    SortField<Object> ascField = service.toSortField(ascOrder);
    assertTrue(ascField.toString().toLowerCase().contains("name"));
  }
}
