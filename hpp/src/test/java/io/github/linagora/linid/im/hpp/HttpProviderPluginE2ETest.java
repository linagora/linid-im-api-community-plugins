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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubspot.jinjava.Jinjava;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.plugin.config.JinjaService;
import io.github.linagora.linid.im.corelib.plugin.config.dto.EntityConfiguration;
import io.github.linagora.linid.im.corelib.plugin.config.dto.ProviderConfiguration;
import io.github.linagora.linid.im.corelib.plugin.entity.DynamicEntity;
import io.github.linagora.linid.im.corelib.plugin.task.TaskEngine;
import io.github.linagora.linid.im.corelib.plugin.task.TaskExecutionContext;
import io.github.linagora.linid.im.hpp.service.HttpServiceImpl;
import java.util.HashMap;
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
    var entityMapping = new HashMap<String, String>();

    providerConfiguration.addOption("baseUrl", "http://localhost:3000");
    providerConfiguration.addOption("headers", Map.of("Content-Type", "application/json"));

    attributes.put("test0", "value");

    entityMapping.put("id1", "{{ context.response.id }}");
    entityMapping.put("firstname1", "{{ context.response.firstname }}");
    entityMapping.put("lastname1", "{{ context.response.lastname }}");
    entityMapping.put("email1", "{{ context.response.email }}");
    entityMapping.put("test1", "{{ context.response.test }}");
    entityMapping.put("status1", "{{ context.response.status }}");

    createAccess.put("uri", "/v1/test_api/user");
    createAccess.put("method", "POST");
    createAccess.put("body", "{ \"test\": \"{{ entity.test0 }}\"}");
    createAccess.put("entityMapping", entityMapping);

    access.put("create", createAccess);

    entityConfiguration.setAccess(access);
    entity.setConfiguration(entityConfiguration);
    entity.setAttributes(attributes);

    var result = provider.create(context, providerConfiguration, entity);

    assertNotNull(result);
    assertNotNull(result.getAttributes());
    assertEquals(6, result.getAttributes().size());
    assertEquals("1", result.getAttributes().get("id1"));
    assertEquals("John", result.getAttributes().get("firstname1"));
    assertEquals("Doe", result.getAttributes().get("lastname1"));
    assertEquals("john.doe@gmail.com", result.getAttributes().get("email1"));
    assertEquals("value", result.getAttributes().get("test1"));
    assertEquals("created", result.getAttributes().get("status1"));
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
    var entityMapping = new HashMap<String, String>();

    providerConfiguration.addOption("baseUrl", "http://localhost:3000");
    providerConfiguration.addOption("headers", Map.of("Content-Type", "application/json"));

    attributes.put("test0", "value");

    entityMapping.put("id1", "{{ context.response.content[index].id }}");
    entityMapping.put("firstname1", "{{ context.response.content[index].firstname }}");
    entityMapping.put("lastname1", "{{ context.response.content[index].lastname }}");
    entityMapping.put("email1", "{{ context.response.content[index].email }}");
    entityMapping.put("test1", "{{ context.response.content[index].test }}");
    entityMapping.put("status1", "{{ context.response.content[index].status }}");

    findAllAccess.put("uri", "/v1/test_api/user");
    findAllAccess.put("method", "GET");
    findAllAccess.put("page", "{{ context.response.pagination.page }}");
    findAllAccess.put("size", "{{ context.response.pagination.size }}");
    findAllAccess.put("total", "{{ context.response.pagination.total }}");
    findAllAccess.put("itemsCount", "{{ context.response.content | length }}");
    findAllAccess.put("entityMapping", entityMapping);

    access.put("findAll", findAllAccess);

    entityConfiguration.setAccess(access);
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
    assertEquals(6, entityResult.getAttributes().size());
    assertEquals("1", entityResult.getAttributes().get("id1"));
    assertEquals("John", entityResult.getAttributes().get("firstname1"));
    assertEquals("Doe", entityResult.getAttributes().get("lastname1"));
    assertEquals("john.doe@gmail.com", entityResult.getAttributes().get("email1"));
    assertEquals("", entityResult.getAttributes().get("test1"));
    assertEquals("retrieved", entityResult.getAttributes().get("status1"));
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
    var entityMapping = new HashMap<String, String>();

    providerConfiguration.addOption("baseUrl", "http://localhost:3000");
    providerConfiguration.addOption("headers", Map.of("Content-Type", "application/json"));

    entityMapping.put("id1", "{{ context.response.id }}");
    entityMapping.put("firstname1", "{{ context.response.firstname }}");
    entityMapping.put("lastname1", "{{ context.response.lastname }}");
    entityMapping.put("email1", "{{ context.response.email }}");
    entityMapping.put("status1", "{{ context.response.status }}");

    findByIdAccess.put("uri", "/v1/test_api/user/{{ context.id }}");
    findByIdAccess.put("method", "GET");
    findByIdAccess.put("entityMapping", entityMapping);

    access.put("findById", findByIdAccess);

    entityConfiguration.setAccess(access);
    entity.setConfiguration(entityConfiguration);
    entity.setAttributes(attributes);

    var result = provider.findById(context, providerConfiguration, "1", entity);

    assertNotNull(result);
    assertNotNull(result.getAttributes());
    assertEquals(5, result.getAttributes().size());
    assertEquals("1", result.getAttributes().get("id1"));
    assertEquals("John", result.getAttributes().get("firstname1"));
    assertEquals("Doe", result.getAttributes().get("lastname1"));
    assertEquals("john.doe@gmail.com", result.getAttributes().get("email1"));
    assertEquals("retrieved", result.getAttributes().get("status1"));
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
    entity.setConfiguration(entityConfiguration);
    entity.setAttributes(attributes);

    var result = provider.delete(context, providerConfiguration, "1", entity);

    assertTrue(result);
  }

  @Test
  @DisplayName("Test delete: should not delete")
  void testDeleteFalse() {
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
    entity.setConfiguration(entityConfiguration);
    entity.setAttributes(attributes);

    var exception = assertThrows(ApiException.class, () -> provider
        .delete(context, providerConfiguration, "2", entity));

    assertEquals("hpp.error.delete", exception.getError().key());
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
    var entityMapping = new HashMap<String, String>();

    providerConfiguration.addOption("baseUrl", "http://localhost:3000");
    providerConfiguration.addOption("headers", Map.of("Content-Type", "application/json"));

    attributes.put("id", "1");

    entityMapping.put("id1", "{{ context.response.id }}");
    entityMapping.put("firstname1", "{{ context.response.firstname }}");
    entityMapping.put("lastname1", "{{ context.response.lastname }}");
    entityMapping.put("email1", "{{ context.response.email }}");
    entityMapping.put("status1", "{{ context.response.status }}");

    patchAccess.put("uri", "/v1/test_api/user/{{ context.id }}");
    patchAccess.put("method", "PATCH");
    patchAccess.put("entityMapping", entityMapping);

    access.put("patch", patchAccess);

    entityConfiguration.setAccess(access);
    entity.setConfiguration(entityConfiguration);
    entity.setAttributes(attributes);

    var result = provider.patch(context, providerConfiguration, "1", entity);

    assertNotNull(result);
    assertNotNull(result.getAttributes());
    assertEquals(5, result.getAttributes().size());
    assertEquals("1", result.getAttributes().get("id1"));
    assertEquals("John", result.getAttributes().get("firstname1"));
    assertEquals("Doe", result.getAttributes().get("lastname1"));
    assertEquals("john.patch@gmail.com", result.getAttributes().get("email1"));
    assertEquals("updated", result.getAttributes().get("status1"));
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
    var entityMapping = new HashMap<String, String>();

    providerConfiguration.addOption("baseUrl", "http://localhost:3000");
    providerConfiguration.addOption("headers", Map.of("Content-Type", "application/json"));

    attributes.put("id", "1");

    entityMapping.put("id1", "{{ context.response.id }}");
    entityMapping.put("firstname1", "{{ context.response.firstname }}");
    entityMapping.put("lastname1", "{{ context.response.lastname }}");
    entityMapping.put("email1", "{{ context.response.email }}");
    entityMapping.put("status1", "{{ context.response.status }}");

    updateAccess.put("uri", "/v1/test_api/user/{{ context.id }}");
    updateAccess.put("method", "PUT");
    updateAccess.put("entityMapping", entityMapping);

    access.put("update", updateAccess);

    entityConfiguration.setAccess(access);
    entity.setConfiguration(entityConfiguration);
    entity.setAttributes(attributes);

    var result = provider.update(context, providerConfiguration, "1", entity);

    assertNotNull(result);
    assertNotNull(result.getAttributes());
    assertEquals(5, result.getAttributes().size());
    assertEquals("1", result.getAttributes().get("id1"));
    assertEquals("John", result.getAttributes().get("firstname1"));
    assertEquals("Doe", result.getAttributes().get("lastname1"));
    assertEquals("john.put@gmail.com", result.getAttributes().get("email1"));
    assertEquals("updated", result.getAttributes().get("status1"));
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

  class JinjaServiceTest implements JinjaService {

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

  class TaskEngineTest implements TaskEngine {
    @Override
    public void execute(DynamicEntity dynamicEntity, TaskExecutionContext context, String phase) {
      if (phase.startsWith("beforeResponseMapping")) {
        String json = (String) context.get("response");
        Map<String, Object> response = null;
        try {
          response = new ObjectMapper().readValue(json, new TypeReference<Map<String, Object>>() {
          });
        } catch (JsonProcessingException e) {
          throw new RuntimeException(e);
        }
        context.put("response", response);
      }
    }
  }
}
