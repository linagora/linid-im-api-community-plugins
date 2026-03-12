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

import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import io.github.linagora.linid.im.corelib.plugin.config.dto.ProviderConfiguration;
import io.github.linagora.linid.im.corelib.plugin.entity.DynamicEntity;
import io.github.linagora.linid.im.corelib.plugin.task.TaskEngine;
import io.github.linagora.linid.im.corelib.plugin.task.TaskExecutionContext;
import io.github.linagora.linid.im.dpp.model.DatabasePluginConfiguration;
import io.github.linagora.linid.im.dpp.registry.DslRegistry;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SortField;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default implementation of {@link CrudService} using jOOQ for dynamic SQL
 * operations.
 *
 * <p>
 * This service resolves table names and column mappings dynamically using
 * the configuration contained in {@link DynamicEntity}.
 * </p>
 */
@Slf4j
@Service
public class CrudServiceImpl implements CrudService {

  /**
   * Registry to obtain DSLContext instances based on provider configuration.
   */
  private final DslRegistry dslRegistry;

  /**
   * Constructor for CrudServiceImpl.
   *
   * @param dslRegistry the registry to obtain DSLContext instances based on
   *                    provider configuration
   */
  public CrudServiceImpl(final DslRegistry dslRegistry) {
    this.dslRegistry = dslRegistry;
  }

  @Override
  @Transactional(readOnly = true)
  public Page<DynamicEntity> select(final ProviderConfiguration config,
      final DatabasePluginConfiguration databasePluginConfiguration,
      final DynamicEntity dynamicEntity,
      final Pageable pageable) {
    DSLContext dsl = dslRegistry.getDsl(config);
    String tableName = databasePluginConfiguration.getTable();
    Table<?> table = DSL.table(DSL.name(tableName));
    int offset = (int) pageable.getOffset();
    int limit = pageable.getPageSize();
    Collection<SortField<Object>> sortFields = pageable.getSort().stream()
        .map(this::toSortField)
        .toList();

    try {
      Result<Record> records = dsl.select()
          .from(table)
          .orderBy(sortFields)
          .limit(limit)
          .offset(offset)
          .fetch();

      int total = dsl.fetchCount(table);

      List<DynamicEntity> results = records.stream()
          .map(record -> mappingEntity(record, dynamicEntity))
          .collect(Collectors.toList());

      return new PageImpl<>(results, pageable, total);
    } catch (Exception e) {
      log.error("Error SELECT on tableName `{}`: {}", tableName, e.getMessage());
      throw new ApiException(400,
          I18nMessage.of("dpp.error.select", Map.of("tableName", tableName, "message", e.getMessage())));
    }
  }

  @Override
  @Transactional(readOnly = true)
  public DynamicEntity selectOne(final TaskExecutionContext context,
      final ProviderConfiguration config,
      final DatabasePluginConfiguration databasePluginConfiguration,
      final Object id,
      final DynamicEntity dynamicEntity) {
    DSLContext dsl = dslRegistry.getDsl(config);
    String tableName = databasePluginConfiguration.getTable();
    Table<?> table = DSL.table(DSL.name(tableName));
    var idColumn = resolveIdColumn(dynamicEntity);

    try {
      Record record = dsl.select()
          .from(table)
          .where(DSL.field(idColumn).eq(id))
          .fetchOne();

      if (record == null) {
        throw new ApiException(404,
            I18nMessage.of("dpp.error.record.notFound", Map.of("tableName", tableName, "id", id)));
      }

      DynamicEntity result = mappingEntity(record, dynamicEntity);
      return result;
    } catch (Exception e) {
      log.error("Error SELECT ONE on tableName `{}`: {}", tableName, e.getMessage());
      throw new ApiException(404,
          I18nMessage.of("dpp.error.selectOne", Map.of("tableName", tableName, "message", e.getMessage())));
    }
  }

  @Override
  @Transactional
  public DynamicEntity insert(final ProviderConfiguration config,
      final DatabasePluginConfiguration databasePluginConfiguration,
      final DynamicEntity dynamicEntity) {
    String tableName = databasePluginConfiguration.getTable();
    DSLContext dsl = dslRegistry.getDsl(config);
    Table<?> table = DSL.table(DSL.name(tableName));
    Map<Field<?>, Object> fields = buildFields(tableName, dynamicEntity, false);

    try {

      Record record = dsl.insertInto(table)
          .set(fields)
          .returning(fields.keySet())
          .fetchOne();

      if (record == null) {
        throw new ApiException(400,
            I18nMessage.of("dpp.error.insert", Map.of("tableName", tableName, "message", "No record inserted")));
      }

      return mappingEntity(record, dynamicEntity);
    } catch (Exception e) {
      log.error("Error INSERT on tableName `{}`: {}", tableName, e.getMessage());
      throw new ApiException(400,
          I18nMessage.of("dpp.error.insert", Map.of("tableName", tableName, "message", e.getMessage())));
    }
  }

  @Override
  @Transactional
  public DynamicEntity update(final ProviderConfiguration config,
      final DatabasePluginConfiguration databasePluginConfiguration,
      final Object id,
      final DynamicEntity dynamicEntity) {
    DSLContext dsl = dslRegistry.getDsl(config);
    String tableName = databasePluginConfiguration.getTable();
    Table<?> table = DSL.table(DSL.name(tableName));
    var idColumn = resolveIdColumn(dynamicEntity);
    Map<Field<?>, Object> fields = buildFields(tableName, dynamicEntity, false);

    try {
      Record record = dsl.update(table)
          .set(fields)
          .where(DSL.field(idColumn).eq(id))
          .returning(fields.keySet())
          .fetchOne();

      if (record == null) {
        throw new ApiException(404,
            I18nMessage.of("dpp.error.record.notFound",
                Map.of("tableName", tableName, "id", id)));
      }

      return mappingEntity(record, dynamicEntity);
    } catch (Exception e) {
      log.error("Error UPDATE on tableName `{}`: {}", tableName, e.getMessage());
      throw new ApiException(400,
          I18nMessage.of("dpp.error.update", Map.of("tableName", tableName, "message", e.getMessage())));
    }
  }

  @Override
  @Transactional
  public DynamicEntity patch(final ProviderConfiguration config,
      final DatabasePluginConfiguration databasePluginConfiguration,
      final Object id,
      final DynamicEntity dynamicEntity) {
    DSLContext dsl = dslRegistry.getDsl(config);
    String tableName = databasePluginConfiguration.getTable();
    Table<?> table = DSL.table(DSL.name(tableName));
    var idColumn = resolveIdColumn(dynamicEntity);
    Map<Field<?>, Object> fields = buildFields(tableName, dynamicEntity, true);

    try {
      Record record = dsl.update(table)
          .set(fields)
          .where(DSL.field(idColumn).eq(id))
          // Rebuild fields to have all fields
          .returning(buildFields(tableName, dynamicEntity, false).keySet())
          .fetchOne();

      if (record == null) {
        throw new ApiException(
            404,
            I18nMessage.of(
                "dpp.error.record.notFound",
                Map.of("tableName", tableName, "id", id)));
      }

      return mappingEntity(record, dynamicEntity);
    } catch (Exception e) {
      log.error("Error PATCH on tableName `{}`: {}", tableName, e.getMessage());
      throw new ApiException(400,
          I18nMessage.of("dpp.error.patch", Map.of("tableName", tableName, "message", e.getMessage())));
    }
  }

  @Override
  @Transactional
  public void delete(final ProviderConfiguration config,
      final DatabasePluginConfiguration databasePluginConfiguration,
      final Object id,
      final DynamicEntity dynamicEntity) {
    DSLContext dsl = dslRegistry.getDsl(config);
    String tableName = databasePluginConfiguration.getTable();
    Table<?> table = DSL.table(DSL.name(tableName));
    var idColumn = resolveIdColumn(dynamicEntity);

    try {
      int deleted = dsl.deleteFrom(table)
          .where(DSL.field(idColumn).eq(id))
          .execute();

      if (deleted == 0) {
        throw new ApiException(404,
            I18nMessage.of("dpp.error.record.notFound",
                Map.of("tableName", tableName, "id", id)));
      }
    } catch (Exception e) {
      log.error("Error DELETE on tableName `{}`: {}", tableName, e.getMessage());
      throw new ApiException(400,
          I18nMessage.of("dpp.error.delete", Map.of("tableName", tableName, "message", e.getMessage())));
    }
  }

  /**
   * Resolves the primary key column name from the dynamic entity configuration.
   *
   * @param dynamicEntity the dynamic entity containing the configuration with
   *                      attributes
   * @return the primary key column name as a jOOQ Name object
   */
  private Name resolveIdColumn(final DynamicEntity dynamicEntity) {
    String name = dynamicEntity.getConfiguration().getAttributes().stream()
        .filter(attr -> Boolean.TRUE.equals(attr.getAccess().get("primaryKey")))
        .findFirst()
        .map(attr -> (String) attr.getAccess().get("column"))
        .orElseThrow(
            () -> new ApiException(
                500,
                I18nMessage.of(
                    "dpp.error.noPrimary",
                    Map.of("entity", dynamicEntity.getConfiguration().getName()))));

    return DSL.name(name);
  }

  /**
   * Builds a map of fields and values for a full update/insert/patch operation.
   *
   * @param dynamicEntity the source entity
   * @param partial boolean for patch operation
   * @return the map of database fields and values
   */
  private Map<Field<?>, Object> buildFields(final String tableName, final DynamicEntity dynamicEntity, boolean partial) {
    Map<String, Object> attributes = dynamicEntity.getAttributes();

    return dynamicEntity.getConfiguration().getAttributes().stream()
        .filter(attr -> attr.getAccess().get("column") != null)
        .filter(attr -> !partial || attributes.containsKey(attr.getName()))
        .collect(Collectors.toMap(
            attr -> DSL.field(DSL.name(tableName, (String) attr.getAccess().get("column"))),
            attr -> attributes.getOrDefault(attr.getName(), DSL.defaultValue())));
  }

  /**
   * Maps a jOOQ record to a DynamicEntity.
   *
   * @param record        the database record
   * @param dynamicEntity the entity configuration
   * @return the mapped dynamic entity
   */
  private DynamicEntity mappingEntity(final Record record, final DynamicEntity dynamicEntity) {
    Map<String, Object> recordMap = record.intoMap();
    DynamicEntity entity = new DynamicEntity();
    entity.setConfiguration(dynamicEntity.getConfiguration());
    entity.setAttributes(new HashMap<>());

    Map<String, Object> attributes = new HashMap<>();

    dynamicEntity.getConfiguration()
        .getAttributes()
        .forEach(attr -> {
          String key = attr.getName();
          Object value = recordMap.get((String) attr.getAccess().get("column"));
          attributes.put(key, value);
        });

    entity.setAttributes(attributes);

    return entity;
  }

  /**
   * Converts a Spring Sort.Order into a jOOQ SortField.
   *
   * @param order the sort order
   * @return the corresponding SortField
   */
  private SortField<Object> toSortField(final Sort.Order order) {
    String property = order.getProperty();
    Field<Object> field = DSL.field(DSL.name(property));

    if (order.isAscending()) {
      return field.asc();
    }

    return field.desc();
  }
}
