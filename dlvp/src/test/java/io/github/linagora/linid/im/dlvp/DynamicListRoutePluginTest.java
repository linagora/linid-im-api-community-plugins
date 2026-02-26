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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.plugin.authorization.AuthorizationFactory;
import io.github.linagora.linid.im.corelib.plugin.authorization.AuthorizationPlugin;
import io.github.linagora.linid.im.corelib.plugin.config.JinjaService;
import io.github.linagora.linid.im.corelib.plugin.config.dto.PluginConfiguration;
import io.github.linagora.linid.im.corelib.plugin.config.dto.RouteConfiguration;
import io.github.linagora.linid.im.corelib.plugin.entity.DynamicEntity;
import io.github.linagora.linid.im.corelib.plugin.route.RouteDescription;
import io.github.linagora.linid.im.corelib.plugin.task.TaskEngine;
import io.github.linagora.linid.im.corelib.plugin.task.TaskExecutionContext;
import io.github.linagora.linid.im.dlvp.model.DynamicListEntry;
import io.github.linagora.linid.im.dlvp.service.HttpService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("Test class: DynamicListRoutePlugin")
class DynamicListRoutePluginTest {

  @Mock
  private HttpService httpService;

  @Mock
  private JinjaService jinjaService;

  @Mock
  private TaskEngine taskEngine;

  @Mock
  private AuthorizationFactory authorizationFactory;

  @Mock
  private AuthorizationPlugin authorizationPlugin;

  @InjectMocks
  private DynamicListRoutePlugin plugin;

  @Test
  @DisplayName("test supports: should return true on valid type")
  void testSupports() {
    assertTrue(plugin.supports("dynamic-list"));
    assertFalse(plugin.supports("other"));
  }

  @Test
  @DisplayName("test getRoutes: should return dynamic route from configuration")
  void testGetRoutes() {
    var configuration = new RouteConfiguration();
    configuration.addOption("route", "/api/etablissement/types");
    plugin.setConfiguration(configuration);

    var expected = List.of(new RouteDescription("GET", "/api/etablissement/types", null, List.of()));
    assertEquals(expected, plugin.getRoutes(List.of()));
  }

  @Test
  @DisplayName("test match: should match dynamic route from configuration")
  void testMatch() {
    var configuration = new RouteConfiguration();
    configuration.addOption("route", "/api/etablissement/types");
    plugin.setConfiguration(configuration);

    assertTrue(plugin.match("/api/etablissement/types", "GET"));
    assertFalse(plugin.match("/api/other", "GET"));
    assertFalse(plugin.match("/api/etablissement/types", "POST"));
  }

  @Test
  @DisplayName("test match: should be case-insensitive on method")
  void testMatchCaseInsensitiveMethod() {
    var configuration = new RouteConfiguration();
    configuration.addOption("route", "/api/etablissement/types");
    plugin.setConfiguration(configuration);

    assertTrue(plugin.match("/api/etablissement/types", "get"));
    assertTrue(plugin.match("/api/etablissement/types", "Get"));
  }

  @Test
  @DisplayName("test match: should return false when no configuration")
  void testMatchNoConfiguration() {
    assertFalse(plugin.match("/api/anything", "GET"));
  }

  @Test
  @DisplayName("test getRoutes: should throw exception when route is null")
  void testGetRoutesNullRoute() {
    var configuration = new RouteConfiguration();
    plugin.setConfiguration(configuration);

    ApiException exception =
        assertThrows(ApiException.class, () -> plugin.getRoutes(List.of()));

    assertEquals(500, exception.getStatusCode());
    assertEquals("error.plugin.default.missing.option", exception.getError().key());
  }

  @Test
  @DisplayName("test match: should throw exception when route is null")
  void testMatchNullRoute() {
    var configuration = new RouteConfiguration();
    plugin.setConfiguration(configuration);

    ApiException exception =
        assertThrows(ApiException.class, () -> plugin.match("/api/anything", "GET"));

    assertEquals(500, exception.getStatusCode());
    assertEquals("error.plugin.default.missing.option", exception.getError().key());
  }

  @Test
  @DisplayName("test execute: should orchestrate service, tasks and return paginated response")
  void testExecute() {
    var configuration = new RouteConfiguration();
    configuration.addOption("route", "/api/etablissement/types");
    configuration.addOption("url", "https://external-api.com/types");
    configuration.addOption("method", "GET");
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
    plugin.setConfiguration(configuration);

    when(authorizationFactory.getAuthorizationPlugin()).thenReturn(authorizationPlugin);
    when(httpService.request(any(TaskExecutionContext.class), any(PluginConfiguration.class)))
        .thenReturn("{\"content\":[{\"name\":\"Type 1\",\"id\":\"1\"},{\"name\":\"Type 2\",\"id\":\"2\"}],"
            + "\"page\":0,\"size\":10,\"totalElements\":2}");

    when(jinjaService.render(any(TaskExecutionContext.class), any(DynamicEntity.class),
        eq("{{ context.response.content.size() }}"))).thenReturn("2");
    when(jinjaService.render(any(TaskExecutionContext.class), any(DynamicEntity.class),
        eq(Map.of("index", 0)), eq("{{ context.response.content[index].name }}")))
        .thenReturn("Type 1");
    when(jinjaService.render(any(TaskExecutionContext.class), any(DynamicEntity.class),
        eq(Map.of("index", 0)), eq("{{ context.response.content[index].id }}")))
        .thenReturn("1");
    when(jinjaService.render(any(TaskExecutionContext.class), any(DynamicEntity.class),
        eq(Map.of("index", 1)), eq("{{ context.response.content[index].name }}")))
        .thenReturn("Type 2");
    when(jinjaService.render(any(TaskExecutionContext.class), any(DynamicEntity.class),
        eq(Map.of("index", 1)), eq("{{ context.response.content[index].id }}")))
        .thenReturn("2");
    when(jinjaService.render(any(TaskExecutionContext.class), any(DynamicEntity.class),
        eq("{{ context.response.page }}"))).thenReturn("0");
    when(jinjaService.render(any(TaskExecutionContext.class), any(DynamicEntity.class),
        eq("{{ context.response.size }}"))).thenReturn("10");
    when(jinjaService.render(any(TaskExecutionContext.class), any(DynamicEntity.class),
        eq("{{ context.response.totalElements }}"))).thenReturn("2");

    var response = plugin.execute(null);

    assertNotNull(response);
    assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());

    Page<DynamicListEntry> page = response.getBody();
    assertNotNull(page);
    assertEquals(List.of(
        new DynamicListEntry("Type 1", "1"),
        new DynamicListEntry("Type 2", "2")
    ), page.getContent());
    assertEquals(0, page.getNumber());
    assertEquals(10, page.getSize());
    assertEquals(2, page.getTotalElements());

    verify(authorizationPlugin).validateToken(eq(null), any(TaskExecutionContext.class));
    verify(taskEngine).execute(any(DynamicEntity.class), any(TaskExecutionContext.class),
        eq("beforeTokenValidationDynamicList"));
    verify(taskEngine).execute(any(DynamicEntity.class), any(TaskExecutionContext.class),
        eq("afterTokenValidationDynamicList"));
    verify(taskEngine).execute(any(DynamicEntity.class), any(TaskExecutionContext.class),
        eq("beforeDynamicList"));
    verify(taskEngine).execute(any(DynamicEntity.class), any(TaskExecutionContext.class),
        eq("afterDynamicList"));
    verify(taskEngine).execute(any(DynamicEntity.class), any(TaskExecutionContext.class),
        eq("beforeDynamicListMapping"));
    verify(taskEngine).execute(any(DynamicEntity.class), any(TaskExecutionContext.class),
        eq("afterDynamicListMapping"));
  }
}
