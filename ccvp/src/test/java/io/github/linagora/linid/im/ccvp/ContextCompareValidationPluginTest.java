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

package io.github.linagora.linid.im.ccvp;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.linagora.linid.im.ccvp.model.ContextCompareOptions;
import io.github.linagora.linid.im.ccvp.model.ContextCompareValidation;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.plugin.config.JinjaService;
import io.github.linagora.linid.im.corelib.plugin.config.dto.TaskConfiguration;
import io.github.linagora.linid.im.corelib.plugin.task.TaskExecutionContext;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("Test class: ContextCompareValidationPlugin")
class ContextCompareValidationPluginTest {

  @Test
  @DisplayName("test supports: should return true on valid type")
  void testSupports() {
    var plugin = new ContextCompareValidationPlugin(null);

    assertTrue(plugin.supports("context-compare"));
    assertFalse(plugin.supports("other"));
  }

  @Test
  @DisplayName("test execute: should do nothing without validation config")
  void testExecuteWithoutValidation() {
    var jinjaService = Mockito.mock(JinjaService.class);
    var plugin = new ContextCompareValidationPlugin(jinjaService);
    TaskConfiguration configuration = new TaskConfiguration();
    TaskExecutionContext context = new TaskExecutionContext();

    assertDoesNotThrow(() -> plugin.execute(configuration, null, context));
  }

  @Test
  @DisplayName("test validation: should pass when values match")
  void testValidationPass() {
    var jinjaService = Mockito.mock(JinjaService.class);
    Mockito.when(jinjaService.render(Mockito.any(), Mockito.eq("{{ firstName }}"))).thenReturn("John");
    Mockito.when(jinjaService.render(Mockito.any(), Mockito.eq("{{ requestFirstName }}"))).thenReturn("John");
    var plugin = new ContextCompareValidationPlugin(jinjaService);

    TaskConfiguration configuration = new TaskConfiguration();
    configuration.addOption("validation", List.of(
        new ContextCompareValidation("{{ firstName }}", "{{ requestFirstName }}", null)
    ));

    TaskExecutionContext context = new TaskExecutionContext();

    assertDoesNotThrow(() -> plugin.execute(configuration, null, context));
  }

  @Test
  @DisplayName("test validation: should throw on mismatch")
  void testValidationMismatch() {
    var jinjaService = Mockito.mock(JinjaService.class);
    Mockito.when(jinjaService.render(Mockito.any(), Mockito.eq("{{ firstName }}"))).thenReturn("John");
    Mockito.when(jinjaService.render(Mockito.any(), Mockito.eq("{{ requestFirstName }}"))).thenReturn("Jane");
    var plugin = new ContextCompareValidationPlugin(jinjaService);

    TaskConfiguration configuration = new TaskConfiguration();
    configuration.addOption("validation", List.of(
        new ContextCompareValidation("{{ firstName }}", "{{ requestFirstName }}", null)
    ));

    TaskExecutionContext context = new TaskExecutionContext();

    var exception = assertThrows(ApiException.class,
        () -> plugin.execute(configuration, null, context));

    assertEquals(400, exception.getStatusCode());
  }

  @Test
  @DisplayName("test validation: should pass with trim option when values differ only by whitespace")
  void testValidationPassWithTrim() {
    var jinjaService = Mockito.mock(JinjaService.class);
    Mockito.when(jinjaService.render(Mockito.any(), Mockito.eq("{{ value1 }}"))).thenReturn("  hello  ");
    Mockito.when(jinjaService.render(Mockito.any(), Mockito.eq("{{ value2 }}"))).thenReturn("hello");
    var plugin = new ContextCompareValidationPlugin(jinjaService);

    TaskConfiguration configuration = new TaskConfiguration();
    configuration.addOption("validation", List.of(
        new ContextCompareValidation("{{ value1 }}", "{{ value2 }}",
            new ContextCompareOptions(true, false))
    ));

    TaskExecutionContext context = new TaskExecutionContext();

    assertDoesNotThrow(() -> plugin.execute(configuration, null, context));
  }

  @Test
  @DisplayName("test validation: should fail without trim when values differ by whitespace")
  void testValidationFailWithoutTrim() {
    var jinjaService = Mockito.mock(JinjaService.class);
    Mockito.when(jinjaService.render(Mockito.any(), Mockito.eq("{{ value1 }}"))).thenReturn("  hello  ");
    Mockito.when(jinjaService.render(Mockito.any(), Mockito.eq("{{ value2 }}"))).thenReturn("hello");
    var plugin = new ContextCompareValidationPlugin(jinjaService);

    TaskConfiguration configuration = new TaskConfiguration();
    configuration.addOption("validation", List.of(
        new ContextCompareValidation("{{ value1 }}", "{{ value2 }}",
            new ContextCompareOptions(false, false))
    ));

    TaskExecutionContext context = new TaskExecutionContext();

    var exception = assertThrows(ApiException.class,
        () -> plugin.execute(configuration, null, context));

    assertEquals(400, exception.getStatusCode());
  }

  @Test
  @DisplayName("test validation: should pass with ignoreCase option")
  void testValidationPassWithIgnoreCase() {
    var jinjaService = Mockito.mock(JinjaService.class);
    Mockito.when(jinjaService.render(Mockito.any(), Mockito.eq("{{ value1 }}"))).thenReturn("Hello");
    Mockito.when(jinjaService.render(Mockito.any(), Mockito.eq("{{ value2 }}"))).thenReturn("hello");
    var plugin = new ContextCompareValidationPlugin(jinjaService);

    TaskConfiguration configuration = new TaskConfiguration();
    configuration.addOption("validation", List.of(
        new ContextCompareValidation("{{ value1 }}", "{{ value2 }}",
            new ContextCompareOptions(false, true))
    ));

    TaskExecutionContext context = new TaskExecutionContext();

    assertDoesNotThrow(() -> plugin.execute(configuration, null, context));
  }

  @Test
  @DisplayName("test validation: should fail without ignoreCase when values differ by case")
  void testValidationFailWithoutIgnoreCase() {
    var jinjaService = Mockito.mock(JinjaService.class);
    Mockito.when(jinjaService.render(Mockito.any(), Mockito.eq("{{ value1 }}"))).thenReturn("Hello");
    Mockito.when(jinjaService.render(Mockito.any(), Mockito.eq("{{ value2 }}"))).thenReturn("hello");
    var plugin = new ContextCompareValidationPlugin(jinjaService);

    TaskConfiguration configuration = new TaskConfiguration();
    configuration.addOption("validation", List.of(
        new ContextCompareValidation("{{ value1 }}", "{{ value2 }}",
            new ContextCompareOptions(false, false))
    ));

    TaskExecutionContext context = new TaskExecutionContext();

    var exception = assertThrows(ApiException.class,
        () -> plugin.execute(configuration, null, context));

    assertEquals(400, exception.getStatusCode());
  }

  @Test
  @DisplayName("test validation: should pass with both trim and ignoreCase")
  void testValidationPassWithTrimAndIgnoreCase() {
    var jinjaService = Mockito.mock(JinjaService.class);
    Mockito.when(jinjaService.render(Mockito.any(), Mockito.eq("{{ value1 }}"))).thenReturn("  Hello  ");
    Mockito.when(jinjaService.render(Mockito.any(), Mockito.eq("{{ value2 }}"))).thenReturn("hello");
    var plugin = new ContextCompareValidationPlugin(jinjaService);

    TaskConfiguration configuration = new TaskConfiguration();
    configuration.addOption("validation", List.of(
        new ContextCompareValidation("{{ value1 }}", "{{ value2 }}",
            new ContextCompareOptions(true, true))
    ));

    TaskExecutionContext context = new TaskExecutionContext();

    assertDoesNotThrow(() -> plugin.execute(configuration, null, context));
  }

  @Test
  @DisplayName("test validation: should throw when value1 is null")
  void testValidationNullValue1() {
    var jinjaService = Mockito.mock(JinjaService.class);
    var plugin = new ContextCompareValidationPlugin(jinjaService);

    TaskConfiguration configuration = new TaskConfiguration();
    configuration.addOption("validation", List.of(
        new ContextCompareValidation(null, "{{ value2 }}", null)
    ));

    TaskExecutionContext context = new TaskExecutionContext();

    var exception = assertThrows(ApiException.class,
        () -> plugin.execute(configuration, null, context));

    assertEquals(400, exception.getStatusCode());
  }

  @Test
  @DisplayName("test validation: should throw when value2 is null")
  void testValidationNullValue2() {
    var jinjaService = Mockito.mock(JinjaService.class);
    var plugin = new ContextCompareValidationPlugin(jinjaService);

    TaskConfiguration configuration = new TaskConfiguration();
    configuration.addOption("validation", List.of(
        new ContextCompareValidation("{{ value1 }}", null, null)
    ));

    TaskExecutionContext context = new TaskExecutionContext();

    var exception = assertThrows(ApiException.class,
        () -> plugin.execute(configuration, null, context));

    assertEquals(400, exception.getStatusCode());
  }

  @Test
  @DisplayName("test validation: should pass with null options (defaults)")
  void testValidationNullOptions() {
    var jinjaService = Mockito.mock(JinjaService.class);
    Mockito.when(jinjaService.render(Mockito.any(), Mockito.eq("{{ value1 }}"))).thenReturn("same");
    Mockito.when(jinjaService.render(Mockito.any(), Mockito.eq("{{ value2 }}"))).thenReturn("same");
    var plugin = new ContextCompareValidationPlugin(jinjaService);

    TaskConfiguration configuration = new TaskConfiguration();
    configuration.addOption("validation", List.of(
        new ContextCompareValidation("{{ value1 }}", "{{ value2 }}", null)
    ));

    TaskExecutionContext context = new TaskExecutionContext();

    assertDoesNotThrow(() -> plugin.execute(configuration, null, context));
  }

  @Test
  @DisplayName("test validation: should pass with null trim and ignoreCase in options")
  void testValidationNullTrimAndIgnoreCase() {
    var jinjaService = Mockito.mock(JinjaService.class);
    Mockito.when(jinjaService.render(Mockito.any(), Mockito.eq("{{ value1 }}"))).thenReturn("hello");
    Mockito.when(jinjaService.render(Mockito.any(), Mockito.eq("{{ value2 }}"))).thenReturn("hello");
    var plugin = new ContextCompareValidationPlugin(jinjaService);

    TaskConfiguration configuration = new TaskConfiguration();
    configuration.addOption("validation", List.of(
        new ContextCompareValidation("{{ value1 }}", "{{ value2 }}",
            new ContextCompareOptions(null, null))
    ));

    TaskExecutionContext context = new TaskExecutionContext();

    assertDoesNotThrow(() -> plugin.execute(configuration, null, context));
  }
}
