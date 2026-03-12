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
import io.github.linagora.linid.im.corelib.plugin.config.dto.ProviderConfiguration;
import io.github.linagora.linid.im.corelib.plugin.entity.DynamicEntity;
import io.github.linagora.linid.im.dpp.registry.DslRegistry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * Default implementation of {@link CrudService} using jOOQ for dynamic SQL operations.
 *
 * <p>This service resolves table names and column mappings dynamically using
 * the configuration contained in {@link DynamicEntity}.</p>
 */

@Slf4j
@Service
public class CrudServiceImpl implements CrudService {

  private final DslRegistry dslRegistry;

  public CrudServiceImpl(DslRegistry dslRegistry) {
    this.dslRegistry = dslRegistry;
  }

  @Override
  public Page<DynamicEntity> select(ProviderConfiguration config, DynamicEntity dynamicEntity) {
    DSLContext dsl = dslRegistry.getDsl(config);

    String tableName =
        (String)
            ((Map<String, Object>) dynamicEntity.getConfiguration().getAccess().get("select"))
                .get("table");

    Table<?> table = DSL.table(DSL.name(tableName));

    int total = dsl.fetchCount(table);

    try {
      Result<Record> records = dsl.select().from(table).fetch();

      List<DynamicEntity> results =
          records.stream()
              .map(
                  record -> {
                    Map<String, Object> recordMap = record.intoMap();
                    Map<String, Object> attributes = new HashMap<>();

                    for (var attr : dynamicEntity.getConfiguration().getAttributes()) {
                      String column = (String) attr.getAccess().get("column");

                      if (column != null && recordMap.containsKey(column)) {
                        attributes.put(attr.getName(), recordMap.get(column));
                      }
                    }

                    DynamicEntity entity = new DynamicEntity();
                    entity.setConfiguration(dynamicEntity.getConfiguration());
                    entity.setAttributes(attributes);

                    return entity;
                  })
              .collect(Collectors.toList());

      Pageable pageable = PageRequest.of(0, results.size());

      return new PageImpl<>(results, pageable, total);

    } catch (ApiException e) {
      log.error("Error SELECT on tableName `{}`: {}", tableName, e.getMessage());
      throw e;
    }
  }

  @Override
  public DynamicEntity selectOne(
      ProviderConfiguration config, String id, DynamicEntity dynamicEntity) {

    DSLContext dsl = dslRegistry.getDsl(config);

    String tableName =
        (String)
            ((Map<String, Object>) dynamicEntity.getConfiguration().getAccess().get("select"))
                .get("table");

    Table<?> table = DSL.table(DSL.name(tableName));

    String idColumn = resolveIdColumn(dynamicEntity);

    try {
      Record record =
          dsl.select()
              .from(table)
              .where(DSL.field(DSL.name(idColumn)).eq(Integer.parseInt(id)))
              .fetchOne();

      if (record == null) {
        return null;
      }

      Map<String, Object> recordMap = record.intoMap();
      Map<String, Object> attributes = new HashMap<>();

      for (var attr : dynamicEntity.getConfiguration().getAttributes()) {
        String column = (String) attr.getAccess().get("column");

        if (column != null && recordMap.containsKey(column)) {
          attributes.put(attr.getName(), recordMap.get(column));
        }
      }

      DynamicEntity entity = new DynamicEntity();
      entity.setConfiguration(dynamicEntity.getConfiguration());
      entity.setAttributes(attributes);

      return entity;

    } catch (ApiException e) {
      log.error("Error SELECT ONE on tableName `{}`: {}", tableName, e.getMessage());
      throw e;
    }
  }

  @Override
  @Transactional
  public int insert(ProviderConfiguration config, DynamicEntity dynamicEntity) {

    DSLContext dsl = dslRegistry.getDsl(config);

    String tableName =
        (String)
            ((Map<String, Object>) dynamicEntity.getConfiguration().getAccess().get("create"))
                .get("table");

    Table<?> table = DSL.table(DSL.name(tableName));

    Map<Field<?>, Object> fields = buildFields(dynamicEntity);

    try {
      return dsl.insertInto(table).set(fields).execute();
    } catch (ApiException e) {
      log.error("Error INSERT on tableName `{}`: {}", tableName, e.getMessage());
      throw e;
    }
  }

  @Override
  @Transactional
  public int update(
      ProviderConfiguration config, String id, DynamicEntity dynamicEntity) {

    DSLContext dsl = dslRegistry.getDsl(config);

    String tableName =
        (String)
            ((Map<String, Object>) dynamicEntity.getConfiguration().getAccess().get("update"))
                .get("table");

    Table<?> table = DSL.table(DSL.name(tableName));

    String idColumn = resolveIdColumn(dynamicEntity);

    Map<Field<?>, Object> fields = buildFields(dynamicEntity);

    try {
      return dsl.update(table)
          .set(fields)
          .where(DSL.field(DSL.name(idColumn)).eq(Integer.parseInt(id)))
          .execute();

    } catch (ApiException e) {
      log.error("Error UPDATE on tableName `{}`: {}", tableName, e.getMessage());
      throw e;
    }
  }

  @Override
  @Transactional
  public int delete(
      ProviderConfiguration config, String id, DynamicEntity dynamicEntity) {

    DSLContext dsl = dslRegistry.getDsl(config);

    String tableName =
        (String)
            ((Map<String, Object>) dynamicEntity.getConfiguration().getAccess().get("delete"))
                .get("table");

    Table<?> table = DSL.table(DSL.name(tableName));

    String idColumn = resolveIdColumn(dynamicEntity);

    try {
      return dsl.deleteFrom(table)
          .where(DSL.field(DSL.name(idColumn)).eq(Integer.parseInt(id)))
          .execute();

    } catch (ApiException e) {
      log.error("Error DELETE on tableName `{}`: {}", tableName, e.getMessage());
      throw e;
    }
  }

  private String resolveIdColumn(DynamicEntity dynamicEntity) {

    return dynamicEntity.getConfiguration().getAttributes().stream()
        .filter(attr -> Boolean.TRUE.equals(attr.getAccess().get("primaryKey")))
        .findFirst()
        .map(attr -> (String) attr.getAccess().get("column"))
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "No primary key attribute configured for entity"));
  }

  private Map<Field<?>, Object> buildFields(DynamicEntity dynamicEntity) {

    Map<String, Object> attributes = dynamicEntity.getAttributes();

    return dynamicEntity.getConfiguration().getAttributes().stream()
        .filter(
            attr ->
                attr.getAccess().get("column") != null
                    && attributes.containsKey(attr.getName()))
        .collect(
            Collectors.toMap(
                attr ->
                    DSL.field(
                        DSL.name((String) attr.getAccess().get("column"))),
                attr -> attributes.get(attr.getName())));
  }
}
