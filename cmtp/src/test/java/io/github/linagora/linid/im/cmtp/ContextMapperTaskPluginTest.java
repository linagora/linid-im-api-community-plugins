/*
 * Copyright (C) 2020-2025 Linagora
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

package io.github.linagora.linid.im.cmtp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.linagora.linid.im.corelib.plugin.config.JinjaService;
import io.github.linagora.linid.im.corelib.plugin.config.dto.TaskConfiguration;
import io.github.linagora.linid.im.corelib.plugin.task.TaskExecutionContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("Test class: ContextMapperTaskPlugin")
class ContextMapperTaskPluginTest {

  @Test
  @DisplayName("test supports: should return true on valid type")
  void testSupports() {
    ContextMapperTaskPlugin plugin = new ContextMapperTaskPlugin(null);

    assertTrue(plugin.supports("context-mapping"));
    assertFalse(plugin.supports("other"));
  }

  @Test
  @DisplayName("test execute: should do nothing without mapping")
  void testExecuteWithoutMappingDoNothing() {
    var jinjaService = Mockito.mock(JinjaService.class);
    Mockito.when(jinjaService.render(Mockito.any(), Mockito.any())).thenReturn("newValue");
    var plugin = new ContextMapperTaskPlugin(jinjaService);
    TaskConfiguration configuration = new TaskConfiguration();

    TaskExecutionContext context = new TaskExecutionContext();
    context.put("key", "value");

    var expected = Map.copyOf(context);

    plugin.execute(configuration, null, context);

    assertEquals(expected, context);
  }

  @Test
  @DisplayName("test execute: should adding variable")
  void testExecuteAdding() {
    var jinjaService = Mockito.mock(JinjaService.class);
    Mockito.when(jinjaService.render(Mockito.any(), Mockito.any())).thenReturn("newValue");
    var plugin = new ContextMapperTaskPlugin(jinjaService);
    TaskConfiguration configuration = new TaskConfiguration();
    configuration.addOption("adding", Map.of(
        "test1", "test",
        "test2", "{{key}}",
        "test3", "{{ context.key }}"
    ));

    TaskExecutionContext context = new TaskExecutionContext();
    context.put("key", "value");

    var expected = Map.of(
        "key", "value",
        "test1", "newValue",
        "test2", "newValue",
        "test3", "newValue"
    );

    plugin.execute(configuration, null, context);

    assertEquals(expected, context);
  }

  @Test
  @DisplayName("test execute: should removing variable")
  void testExecuteRemoving() {
    var jinjaService = Mockito.mock(JinjaService.class);
    Mockito.when(jinjaService.render(Mockito.any(), Mockito.any())).thenReturn("newValue");
    var plugin = new ContextMapperTaskPlugin(jinjaService);
    TaskConfiguration configuration = new TaskConfiguration();
    configuration.addOption("removing", List.of("key", "other"));

    TaskExecutionContext context = new TaskExecutionContext();
    context.put("key", "value");

    var expected = Map.of();

    plugin.execute(configuration, null, context);

    assertEquals(expected, context);
  }

  @Test
  @DisplayName("test execute: should map with default template and custom templates")
  void testExecute() {
    var jinjaService = Mockito.mock(JinjaService.class);
    Mockito.when(jinjaService.render(Mockito.any(), Mockito.any())).thenReturn("newValue");
    var plugin = new ContextMapperTaskPlugin(jinjaService);

    Map<String, String> mapping = new HashMap<>();
    mapping.put("key", "key1,key2");
    Map<String, String> templates = new HashMap<>();
    templates.put("key.key1", "custom: {{key}}");

    TaskConfiguration configuration = new TaskConfiguration();

    configuration.getOptions().put("mapping", mapping);
    configuration.getOptions().put("templates", templates);

    TaskExecutionContext context = new TaskExecutionContext();
    context.put("key", "test");

    var expected = Map.of(
        "key", "test",
        "key1", "newValue",
        "key2", "newValue"
    );

    plugin.execute(configuration, null, context);

    assertEquals(expected, context);
  }

  @Test
  @DisplayName("test execute: should add, map et remove")
  public void testExecuteAll() {
    var jinjaService = Mockito.mock(JinjaService.class);
    Mockito.when(jinjaService.render(Mockito.any(), Mockito.any())).thenReturn("newValue");
    var plugin = new ContextMapperTaskPlugin(jinjaService);
    TaskConfiguration configuration = new TaskConfiguration();
    configuration.addOption("adding", Map.of("test1", "value"));
    configuration.addOption("mapping", Map.of("test1", "test2"));
    configuration.addOption("templates", Map.of("test1.test2", "{{test1}}"));
    configuration.addOption("removing", List.of("key", "test1"));

    TaskExecutionContext context = new TaskExecutionContext();
    context.put("key", "value");

    var expected = Map.of("test2", "newValue");

    plugin.execute(configuration, null, context);

    assertEquals(expected, context);
  }

}
