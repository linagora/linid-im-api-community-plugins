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

package io.github.linagora.linid.im.jptp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.plugin.config.dto.TaskConfiguration;
import io.github.linagora.linid.im.corelib.plugin.entity.DynamicEntity;
import io.github.linagora.linid.im.corelib.plugin.task.TaskExecutionContext;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Test class: JsonParsingTaskPlugin")
class JsonParsingTaskPluginTest {

  private final JsonParsingTaskPlugin plugin = new JsonParsingTaskPlugin();

  @Test
  @DisplayName("test supports: should return true for json-parsing type")
  void testSupports() {
    assertTrue(plugin.supports("json-parsing"));
  }

  @Test
  @DisplayName("test supports: should return false for other types")
  void testSupportsOtherType() {
    assertFalse(plugin.supports("response-to-json"));
    assertFalse(plugin.supports("other"));
  }

  private TaskConfiguration buildConfiguration(String source, String destination) {
    var configuration = new TaskConfiguration();
    configuration.addOption("source", source);
    configuration.addOption("destination", destination);
    return configuration;
  }

  @Test
  @DisplayName("test execute: should parse JSON in-place")
  void testExecuteInPlace() {
    var context = new TaskExecutionContext();
    context.put("response", "{\"name\":\"test\"}");

    plugin.execute(buildConfiguration("response", "response"), new DynamicEntity(), context);

    assertNotNull(context.get("response"));
    assertEquals(LinkedHashMap.class, context.get("response").getClass());
    assertEquals(Map.of("name", "test"), context.get("response"));
  }

  @Test
  @DisplayName("test execute: should store parsed JSON in custom destination key")
  void testExecuteCustomDestination() {
    var context = new TaskExecutionContext();
    context.put("rawData", "{\"key\":\"value\"}");

    plugin.execute(buildConfiguration("rawData", "parsedData"), new DynamicEntity(), context);

    assertEquals("{\"key\":\"value\"}", context.get("rawData"));
    assertNotNull(context.get("parsedData"));
    assertEquals(Map.of("key", "value"), context.get("parsedData"));
  }

  @Test
  @DisplayName("test execute: should throw exception on missing source option")
  void testExecuteMissingSource() {
    var configuration = new TaskConfiguration();
    configuration.addOption("destination", "output");

    var exception = assertThrows(ApiException.class,
        () -> plugin.execute(configuration, new DynamicEntity(), new TaskExecutionContext()));

    assertEquals(500, exception.getStatusCode());
    assertEquals("error.plugin.default.missing.option", exception.getError().key());
    assertEquals(Map.of("option", "source"), exception.getError().context());
  }

  @Test
  @DisplayName("test execute: should throw exception on missing destination option")
  void testExecuteMissingDestination() {
    var configuration = new TaskConfiguration();
    configuration.addOption("source", "response");

    var exception = assertThrows(ApiException.class,
        () -> plugin.execute(configuration, new DynamicEntity(), new TaskExecutionContext()));

    assertEquals(500, exception.getStatusCode());
    assertEquals("error.plugin.default.missing.option", exception.getError().key());
    assertEquals(Map.of("option", "destination"), exception.getError().context());
  }

  @Test
  @DisplayName("test execute: should throw exception on invalid JSON")
  void testExecuteInvalidJson() {
    var context = new TaskExecutionContext();
    context.put("response", "{invalid json");

    var exception = assertThrows(ApiException.class,
        () -> plugin.execute(buildConfiguration("response", "response"),
            new DynamicEntity(), context));

    assertEquals(500, exception.getStatusCode());
    assertEquals("jptp.error.json.parsing", exception.getError().key());
    assertEquals(Map.of("source", "response"), exception.getError().context());
  }

  @Test
  @DisplayName("test execute: should throw exception when source value is null in context")
  void testExecuteNullSourceValue() {
    var context = new TaskExecutionContext();

    var exception = assertThrows(ApiException.class,
        () -> plugin.execute(buildConfiguration("response", "response"),
            new DynamicEntity(), context));

    assertEquals(500, exception.getStatusCode());
    assertEquals("jptp.error.source.unknown", exception.getError().key());
    assertEquals(Map.of("source", "response"), exception.getError().context());
  }

  @Test
  @DisplayName("test execute: should not modify other context keys")
  void testExecutePreservesOtherKeys() {
    var context = new TaskExecutionContext();
    context.put("response", "{\"key\":\"value\"}");
    context.put("other", "untouched");

    plugin.execute(buildConfiguration("response", "response"), new DynamicEntity(), context);

    assertEquals("untouched", context.get("other"));
    assertEquals(Map.of("key", "value"), context.get("response"));
  }
}
