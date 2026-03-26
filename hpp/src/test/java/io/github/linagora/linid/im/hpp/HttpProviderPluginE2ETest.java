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

package io.github.linagora.linid.im.hpp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hubspot.jinjava.Jinjava;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.plugin.config.JinjaService;
import io.github.linagora.linid.im.corelib.plugin.config.dto.EntityConfiguration;
import io.github.linagora.linid.im.corelib.plugin.config.dto.ProviderConfiguration;
import io.github.linagora.linid.im.corelib.plugin.config.dto.TaskConfiguration;
import io.github.linagora.linid.im.corelib.plugin.entity.DynamicEntity;
import io.github.linagora.linid.im.corelib.plugin.task.TaskEngine;
import io.github.linagora.linid.im.corelib.plugin.task.TaskExecutionContext;
import io.github.linagora.linid.im.hpp.service.HttpServiceImpl;
import io.github.linagora.linid.im.jptp.JsonParsingTaskPlugin;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.util.LinkedMultiValueMap;

@DisplayName("Test class: HttpProviderPlugin with e2e")
class HttpProviderPluginE2ETest {

  private HttpProviderPlugin provider;

  @BeforeEach
  void setup() {
    var jinjaService = new JinjaServiceTest();
    var httpService = new HttpServiceImpl(jinjaService);
    var taskEngine = new TaskEngineTest();
    provider = new HttpProviderPlugin(taskEngine, httpService, jinjaService);
  }

  private TaskConfiguration buildJsonParsingTask() {
    var task = new TaskConfiguration();
    task.setType("json-parsing");
    task.addOption("source", "response");
    task.addOption("destination", "response");
    task.setPhases(List.of(
        "afterResponseCreate",
        "afterResponseUpdate",
        "afterResponsePatch",
        "afterResponseFindById",
        "afterResponseFindAll"
    ));
    return task;
  }

  @Test
  @DisplayName("Test create: should retrieve valid json")
  void testCreate() {
    var entity = new DynamicEntity();
    var context = new TaskExecutionContext();
    var providerConfiguration = new ProviderConfiguration();
    var entityConfiguration = new EntityConfiguration();
    var attributes = new HashMap<String, Object>();
    var access = new HashMap<String, Object>();
    var createAccess = new HashMap<String, Object>();
    providerConfiguration.addOption("baseUrl", "http://localhost:3000");
    providerConfiguration.addOption("headers", Map.of("Content-Type", "application/json"));

    attributes.put("test0", "value");

    createAccess.put("uri", "/v1/test_api/user");
    createAccess.put("method", "POST");
    createAccess.put("body", "{ \"test\": \"{{ entity.test0 }}\"}");

    access.put("create", createAccess);

    entityConfiguration.setAccess(access);
    entityConfiguration.setTasks(List.of(buildJsonParsingTask()));
    entity.setConfiguration(entityConfiguration);
    entity.setAttributes(attributes);

    var result = provider.create(context, providerConfiguration, entity);

    assertNotNull(result);
    assertNotNull(result.getAttributes());
    assertEquals(1, result.getAttributes().size());
    assertEquals("value", result.getAttributes().get("test0"));
  }

  @Test
  @DisplayName("Test findAll: should retrieve valid json")
  void testFindAll() {
    var entity = new DynamicEntity();
    var context = new TaskExecutionContext();
    var providerConfiguration = new ProviderConfiguration();
    var entityConfiguration = new EntityConfiguration();
    var attributes = new HashMap<String, Object>();
    var access = new HashMap<String, Object>();
    var findAllAccess = new HashMap<String, Object>();
    providerConfiguration.addOption("baseUrl", "http://localhost:3000");
    providerConfiguration.addOption("headers", Map.of("Content-Type", "application/json"));

    attributes.put("test0", "value");

    findAllAccess.put("uri", "/v1/test_api/user");
    findAllAccess.put("method", "GET");
    findAllAccess.put("page", "{{ context.response.pagination.page }}");
    findAllAccess.put("size", "{{ context.response.pagination.size }}");
    findAllAccess.put("total", "{{ context.response.pagination.total }}");
    findAllAccess.put("itemsCount", "{{ context.response.content | length }}");

    access.put("findAll", findAllAccess);

    entityConfiguration.setAccess(access);
    entityConfiguration.setTasks(List.of(buildJsonParsingTask()));
    entity.setConfiguration(entityConfiguration);
    entity.setAttributes(attributes);

    var result = provider.findAll(context, providerConfiguration, new LinkedMultiValueMap<>(), Pageable.unpaged(), entity);

    assertNotNull(result);
    assertEquals(1, result.getTotalElements());
    assertEquals(1, result.getTotalPages());
    assertEquals(0, result.getNumber());
    assertEquals(1, result.getSize());
    assertEquals(1, result.getContent().size());

    var entityResult = result.getContent().getFirst();

    assertNotNull(entityResult.getAttributes());
    assertEquals(0, entityResult.getAttributes().size());
  }

  @Test
  @DisplayName("Test findById: should retrieve valid json")
  void testFindById() {
    var entity = new DynamicEntity();
    var context = new TaskExecutionContext();
    var providerConfiguration = new ProviderConfiguration();
    var entityConfiguration = new EntityConfiguration();
    var attributes = new HashMap<String, Object>();
    var access = new HashMap<String, Object>();
    var findByIdAccess = new HashMap<String, Object>();
    providerConfiguration.addOption("baseUrl", "http://localhost:3000");
    providerConfiguration.addOption("headers", Map.of("Content-Type", "application/json"));

    findByIdAccess.put("uri", "/v1/test_api/user/{{ context.id }}");
    findByIdAccess.put("method", "GET");

    access.put("findById", findByIdAccess);

    entityConfiguration.setAccess(access);
    entityConfiguration.setTasks(List.of(buildJsonParsingTask()));
    entity.setConfiguration(entityConfiguration);
    entity.setAttributes(attributes);

    var result = provider.findById(context, providerConfiguration, "1", entity);

    assertNotNull(result);
    assertNotNull(result.getAttributes());
    assertEquals(0, result.getAttributes().size());
  }

  @Test
  @DisplayName("Test delete: should delete")
  void testDelete() {
    var entity = new DynamicEntity();
    var context = new TaskExecutionContext();
    var providerConfiguration = new ProviderConfiguration();
    var entityConfiguration = new EntityConfiguration();
    var attributes = new HashMap<String, Object>();
    var access = new HashMap<String, Object>();
    var deleteAccess = new HashMap<String, Object>();

    providerConfiguration.addOption("baseUrl", "http://localhost:3000");
    providerConfiguration.addOption("headers", Map.of("Content-Type", "application/json"));

    deleteAccess.put("uri", "/v1/test_api/user/{{ context.id }}");
    deleteAccess.put("method", "DELETE");
    deleteAccess.put("result", "{{ context.response.status == \"deleted\" }}");

    access.put("delete", deleteAccess);

    entityConfiguration.setAccess(access);
    entityConfiguration.setTasks(List.of(buildJsonParsingTask()));
    entity.setConfiguration(entityConfiguration);
    entity.setAttributes(attributes);

    var result = provider.delete(context, providerConfiguration, "1", entity);

    assertTrue(result);
  }

  @Test
  @DisplayName("Test patch: should retrieve valid json")
  void testPatch() {
    var entity = new DynamicEntity();
    var context = new TaskExecutionContext();
    var providerConfiguration = new ProviderConfiguration();
    var entityConfiguration = new EntityConfiguration();
    var attributes = new HashMap<String, Object>();
    var access = new HashMap<String, Object>();
    var patchAccess = new HashMap<String, Object>();
    providerConfiguration.addOption("baseUrl", "http://localhost:3000");
    providerConfiguration.addOption("headers", Map.of("Content-Type", "application/json"));

    attributes.put("id", "1");

    patchAccess.put("uri", "/v1/test_api/user/{{ context.id }}");
    patchAccess.put("method", "PATCH");

    access.put("patch", patchAccess);

    entityConfiguration.setAccess(access);
    entityConfiguration.setTasks(List.of(buildJsonParsingTask()));
    entity.setConfiguration(entityConfiguration);
    entity.setAttributes(attributes);

    ApiException exception = assertThrows(ApiException.class, () -> {
      provider.patch(context, providerConfiguration, "1", entity);
    });

    assertNotNull(exception);
    assertEquals("error.plugin.default.invalid.option", exception.getMessage());
  }

  @Test
  @DisplayName("Test update: should retrieve valid json")
  void testUpdate() {
    var entity = new DynamicEntity();
    var context = new TaskExecutionContext();
    var providerConfiguration = new ProviderConfiguration();
    var entityConfiguration = new EntityConfiguration();
    var attributes = new HashMap<String, Object>();
    var access = new HashMap<String, Object>();
    var updateAccess = new HashMap<String, Object>();
    providerConfiguration.addOption("baseUrl", "http://localhost:3000");
    providerConfiguration.addOption("headers", Map.of("Content-Type", "application/json"));

    attributes.put("id", "1");

    updateAccess.put("uri", "/v1/test_api/user/{{ context.id }}");
    updateAccess.put("method", "PUT");

    access.put("update", updateAccess);

    entityConfiguration.setAccess(access);
    entityConfiguration.setTasks(List.of(buildJsonParsingTask()));
    entity.setConfiguration(entityConfiguration);
    entity.setAttributes(attributes);

    var result = provider.update(context, providerConfiguration, "1", entity);

    assertNotNull(result);
    assertNotNull(result.getAttributes());
    assertEquals(1, result.getAttributes().size());
    assertEquals("1", result.getAttributes().get("id"));
  }

  @Test
  @DisplayName("Test request: should throw exception on 4xx")
  void testExceptionOn4xx() {
    var entity = new DynamicEntity();
    var context = new TaskExecutionContext();
    var providerConfiguration = new ProviderConfiguration();
    var entityConfiguration = new EntityConfiguration();
    var attributes = new HashMap<String, Object>();
    var access = new HashMap<String, Object>();
    var createAccess = new HashMap<String, Object>();

    providerConfiguration.addOption("baseUrl", "http://localhost:3000");
    providerConfiguration.addOption("headers", Map.of("Content-Type", "application/json"));

    attributes.put("id", "2");

    createAccess.put("uri", "/v1/test_api/400");
    createAccess.put("method", "GET");

    access.put("create", createAccess);

    entityConfiguration.setAccess(access);
    entity.setConfiguration(entityConfiguration);
    entity.setAttributes(attributes);

    var exception = assertThrows(ApiException.class, () -> provider
        .create(context, providerConfiguration, entity));

    assertEquals("hpp.error400", exception.getError().key());
    assertEquals(400, exception.getStatusCode());
  }

  @Test
  @DisplayName("Test request: should throw exception on 404")
  void testExceptionOn404() {
    var entity = new DynamicEntity();
    var context = new TaskExecutionContext();
    var providerConfiguration = new ProviderConfiguration();
    var entityConfiguration = new EntityConfiguration();
    var attributes = new HashMap<String, Object>();
    var access = new HashMap<String, Object>();
    var createAccess = new HashMap<String, Object>();

    providerConfiguration.addOption("baseUrl", "http://localhost:3000");
    providerConfiguration.addOption("headers", Map.of("Content-Type", "application/json"));

    attributes.put("id", "2");

    createAccess.put("uri", "/v1/test_api/404");
    createAccess.put("method", "GET");

    access.put("create", createAccess);

    entityConfiguration.setAccess(access);
    entity.setConfiguration(entityConfiguration);
    entity.setAttributes(attributes);

    var exception = assertThrows(ApiException.class, () -> provider
        .create(context, providerConfiguration, entity));

    assertEquals("hpp.error404", exception.getError().key());
    assertEquals(404, exception.getStatusCode());
  }

  @Test
  @DisplayName("Test request: should throw exception on 5xx")
  void testExceptionOn5xx() {
    var entity = new DynamicEntity();
    var context = new TaskExecutionContext();
    var providerConfiguration = new ProviderConfiguration();
    var entityConfiguration = new EntityConfiguration();
    var attributes = new HashMap<String, Object>();
    var access = new HashMap<String, Object>();
    var createAccess = new HashMap<String, Object>();

    providerConfiguration.addOption("baseUrl", "http://localhost:3000");
    providerConfiguration.addOption("headers", Map.of("Content-Type", "application/json"));

    createAccess.put("uri", "/v1/test_api/500");
    createAccess.put("method", "GET");

    access.put("create", createAccess);

    entityConfiguration.setAccess(access);
    entity.setConfiguration(entityConfiguration);
    entity.setAttributes(attributes);

    var exception = assertThrows(ApiException.class, () -> provider
        .create(context, providerConfiguration, entity));

    assertEquals("hpp.error500", exception.getError().key());
  }

  static class JinjaServiceTest implements JinjaService {

    @Override
    public String render(TaskExecutionContext taskContext, String template) {
      return render(taskContext, null, Map.of(), template);
    }

    @Override
    public String render(TaskExecutionContext taskContext, DynamicEntity entity, String template) {
      return render(taskContext, entity, Map.of(), template);
    }

    @Override
    public String render(TaskExecutionContext taskContext, DynamicEntity entity, Map<String, Object> map, String template) {
      var context = new HashMap<String, Object>();

      context.put("entity", entity.getAttributes());
      context.put("context", taskContext);
      context.putAll(map);

      return new Jinjava().render(template, context);
    }
  }

  static class TaskEngineTest implements TaskEngine {

    private final JsonParsingTaskPlugin jsonParsingPlugin = new JsonParsingTaskPlugin();

    @Override
    public void execute(DynamicEntity dynamicEntity, TaskExecutionContext context, String phase) {
      if (dynamicEntity.getConfiguration() == null) {
        return;
      }
      dynamicEntity.getConfiguration().getTasks()
          .stream()
          .filter(task -> task.getPhases().contains(phase))
          .forEach(task -> {
            if (jsonParsingPlugin.supports(task.getType())) {
              jsonParsingPlugin.execute(task, dynamicEntity, context);
            }
          });
    }
  }
}
