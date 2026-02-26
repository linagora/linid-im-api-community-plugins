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

package io.github.linagora.linid.im.dlvp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hubspot.jinjava.Jinjava;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.plugin.authorization.AbstractAuthorizationPlugin;
import io.github.linagora.linid.im.corelib.plugin.authorization.AuthorizationFactory;
import io.github.linagora.linid.im.corelib.plugin.config.JinjaService;
import io.github.linagora.linid.im.corelib.plugin.config.dto.RootConfiguration;
import io.github.linagora.linid.im.corelib.plugin.config.dto.RouteConfiguration;
import io.github.linagora.linid.im.corelib.plugin.entity.DynamicEntity;
import io.github.linagora.linid.im.corelib.plugin.task.TaskEngine;
import io.github.linagora.linid.im.corelib.plugin.task.TaskExecutionContext;
import io.github.linagora.linid.im.dlvp.model.DynamicListEntry;
import io.github.linagora.linid.im.dlvp.service.HttpServiceImpl;
import io.github.linagora.linid.im.jptp.JsonParsingTaskPlugin;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;

@DisplayName("Test class: DynamicListRoutePlugin E2E")
class DynamicListRoutePluginE2ETest {

  private DynamicListRoutePlugin plugin;

  @BeforeEach
  void setup() {
    JinjaService jinjaService = new JinjaServiceTest();
    var httpService = new HttpServiceImpl(jinjaService);
    TaskEngine taskEngine = new TaskEngineTest();
    AuthorizationFactory authorizationFactory = NoOpAuthorizationPluginTest::new;
    plugin = new DynamicListRoutePlugin(httpService, jinjaService, taskEngine,
        authorizationFactory);
  }

  private RouteConfiguration buildConfiguration(String url, String method) {
    var configuration = new RouteConfiguration();
    configuration.addOption("route", "/api/types");
    configuration.addOption("url", url);
    configuration.addOption("method", method);
    configuration.addOption("itemsCount", "{{ context.response.content.size() }}");
    configuration.addOption("elementMapping", Map.of(
        "label", "{{ context.response.content[index].name }}",
        "value", "{{ context.response.content[index].id }}"
    ));
    configuration.addOption("page", "{{ context.response.page }}");
    configuration.addOption("size", "{{ context.response.size }}");
    configuration.addOption("total", "{{ context.response.totalElements }}");
    configuration.addOption("tasks", List.of(
        Map.of("type", "json-parsing", "source", "response",
            "destination", "response",
            "phases", List.of("beforeDynamicListMapping"))
    ));
    return configuration;
  }

  @Test
  @DisplayName("test execute: should return paginated elements from external API via GET")
  void testExecuteGet() {
    plugin.setConfiguration(buildConfiguration("http://localhost:3001/v1/test_api/types", "GET"));

    var response = plugin.execute(null);

    assertNotNull(response);
    assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());

    Page<DynamicListEntry> page = response.getBody();
    assertNotNull(page);
    assertEquals(List.of(
        new DynamicListEntry("TYPE1", "1"),
        new DynamicListEntry("TYPE2", "2"),
        new DynamicListEntry("TYPE3", "3")
    ), page.getContent());
    assertEquals(0, page.getNumber());
    assertEquals(10, page.getSize());
    assertEquals(3, page.getTotalElements());
  }

  @Test
  @DisplayName("test execute: should return elements with custom headers")
  void testExecuteWithHeaders() {
    var configuration = buildConfiguration(
        "http://localhost:3001/v1/test_api/types/with-headers", "GET");
    configuration.addOption("headers", Map.of("Authorization", "Bearer my-token"));
    plugin.setConfiguration(configuration);

    var response = plugin.execute(null);

    assertNotNull(response);
    assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());

    Page<DynamicListEntry> page = response.getBody();
    assertNotNull(page);
    assertEquals(List.of(
        new DynamicListEntry("SECURE_TYPE1", "S1"),
        new DynamicListEntry("SECURE_TYPE2", "S2")
    ), page.getContent());
    assertEquals(2, page.getTotalElements());
  }

  @Test
  @DisplayName("test execute: should throw exception on 401 without auth header")
  void testExecuteUnauthorized() {
    plugin.setConfiguration(buildConfiguration(
        "http://localhost:3001/v1/test_api/types/with-headers", "GET"));

    ApiException exception = assertThrows(ApiException.class, () -> plugin.execute(null));

    assertEquals(401, exception.getStatusCode());
    assertEquals("dlvp.error.external.api", exception.getError().key());
  }

  @Test
  @DisplayName("test execute: should throw exception on 4xx error")
  void testExecute4xx() {
    plugin.setConfiguration(buildConfiguration("http://localhost:3001/v1/test_api/400", "GET"));

    ApiException exception = assertThrows(ApiException.class, () -> plugin.execute(null));

    assertEquals(400, exception.getStatusCode());
    assertEquals("dlvp.error.external.api", exception.getError().key());
  }

  @Test
  @DisplayName("test execute: should throw exception on 5xx error")
  void testExecute5xx() {
    plugin.setConfiguration(buildConfiguration("http://localhost:3001/v1/test_api/500", "GET"));

    ApiException exception = assertThrows(ApiException.class, () -> plugin.execute(null));

    assertEquals(502, exception.getStatusCode());
    assertEquals("dlvp.error.external.api.unavailable", exception.getError().key());
  }

  @Test
  @DisplayName("test execute: should throw exception on invalid JSON response")
  void testExecuteInvalidJson() {
    plugin.setConfiguration(buildConfiguration(
        "http://localhost:3001/v1/test_api/invalid-json", "GET"));

    ApiException exception = assertThrows(ApiException.class, () -> plugin.execute(null));

    assertEquals(500, exception.getStatusCode());
    assertEquals("jptp.error.json.parsing", exception.getError().key());
  }

  @Test
  @DisplayName("test execute: should return empty page when API returns empty content")
  void testExecuteEmptyContent() {
    plugin.setConfiguration(buildConfiguration(
        "http://localhost:3001/v1/test_api/types/empty", "GET"));

    var response = plugin.execute(null);

    assertNotNull(response);
    Page<DynamicListEntry> page = response.getBody();
    assertNotNull(page);
    assertTrue(page.getContent().isEmpty());
    assertEquals(0, page.getTotalElements());
  }

  @Test
  @DisplayName("test execute: should return elements via POST method")
  void testExecutePost() {
    var configuration = buildConfiguration(
        "http://localhost:3001/v1/test_api/types/search", "POST");
    configuration.addOption("headers",
        Map.of("Content-Type", "application/json"));
    configuration.addOption("body", "{\"filter\":\"\"}");
    plugin.setConfiguration(configuration);

    var response = plugin.execute(null);

    assertNotNull(response);
    assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());

    Page<DynamicListEntry> page = response.getBody();
    assertNotNull(page);
    assertEquals(List.of(
        new DynamicListEntry("TYPE1", "1"),
        new DynamicListEntry("TYPE2", "2"),
        new DynamicListEntry("TYPE3", "3")
    ), page.getContent());
  }

  @Test
  @DisplayName("test execute: should throw exception on invalid method")
  void testExecuteInvalidMethod() {
    plugin.setConfiguration(buildConfiguration("http://localhost:3001/v1/test_api/types", "DELETE"));

    ApiException exception = assertThrows(ApiException.class, () -> plugin.execute(null));

    assertEquals(500, exception.getStatusCode());
    assertEquals("error.plugin.default.invalid.option", exception.getError().key());
  }

  /**
   * No-op AuthorizationPlugin for testing that allows all requests.
   */
  static class NoOpAuthorizationPluginTest extends AbstractAuthorizationPlugin {

    @Override
    public void updateConfiguration(RootConfiguration configuration) {
      // No-op
    }

    @Override
    public void validateToken(HttpServletRequest request, TaskExecutionContext context) {
      // No-op: allow all requests in tests
    }

    @Override
    public void isAuthorized(HttpServletRequest request, DynamicEntity entity,
                             String action, TaskExecutionContext context) {
      // No-op
    }

    @Override
    public void isAuthorized(HttpServletRequest request, DynamicEntity entity,
                             String id, String action, TaskExecutionContext context) {
      // No-op
    }

    @Override
    public void isAuthorized(HttpServletRequest request, DynamicEntity entity,
                             org.springframework.util.MultiValueMap<String, String> filters,
                             String action, TaskExecutionContext context) {
      // No-op
    }

    @Override
    public boolean supports(String type) {
      return true;
    }
  }

  /**
   * JinjaService test implementation using Jinjava directly.
   */
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
    public String render(
        TaskExecutionContext taskContext, DynamicEntity entity,
        Map<String, Object> map, String template) {
      var context = new HashMap<String, Object>();

      context.put("context", taskContext);
      if (entity != null && entity.getAttributes() != null) {
        context.put("entity", entity.getAttributes());
      }
      context.putAll(map);

      return new Jinjava().render(template, context);
    }
  }

  /**
   * TaskEngine test implementation that executes json-parsing inline.
   */
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
