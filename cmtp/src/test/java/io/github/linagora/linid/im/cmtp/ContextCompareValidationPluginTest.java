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

package io.github.linagora.linid.im.cmtp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.plugin.config.JinjaService;
import io.github.linagora.linid.im.corelib.plugin.config.dto.ValidationConfiguration;
import io.github.linagora.linid.im.corelib.plugin.task.TaskExecutionContext;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("Test class: ContextCompareValidationPlugin")
class ContextCompareValidationPluginTest {

  @Test
  @DisplayName("test supports: should return true on valid type")
  void testSupports() {
    var jinjaService = Mockito.mock(JinjaService.class);
    var plugin = new ContextCompareValidationPlugin(jinjaService);

    assertTrue(plugin.supports("context-compare"));
    assertFalse(plugin.supports("other"));
  }

  @Test
  @DisplayName("test validate: should throw on missing value1 option")
  void testValidateMissingValue1() {
    var jinjaService = Mockito.mock(JinjaService.class);
    var plugin = new ContextCompareValidationPlugin(jinjaService);
    var configuration = new ValidationConfiguration();
    configuration.addOption("value2", "{{ lastName }}");
    var context = new TaskExecutionContext();

    ApiException exception = assertThrows(ApiException.class,
        () -> plugin.validate(configuration, "ignored", context));

    assertEquals(500, exception.getStatusCode());
    assertEquals("error.plugin.default.missing.option", exception.getError().key());
    assertEquals(Map.of("option", "value1"), exception.getError().context());
  }

  @Test
  @DisplayName("test validate: should throw on missing value2 option")
  void testValidateMissingValue2() {
    var jinjaService = Mockito.mock(JinjaService.class);
    var plugin = new ContextCompareValidationPlugin(jinjaService);
    var configuration = new ValidationConfiguration();
    configuration.addOption("value1", "{{ firstName }}");
    var context = new TaskExecutionContext();

    ApiException exception = assertThrows(ApiException.class,
        () -> plugin.validate(configuration, "ignored", context));

    assertEquals(500, exception.getStatusCode());
    assertEquals("error.plugin.default.missing.option", exception.getError().key());
    assertEquals(Map.of("option", "value2"), exception.getError().context());
  }

  @Test
  @DisplayName("test validate: should return empty when rendered values match")
  void testValidateMatch() {
    var jinjaService = Mockito.mock(JinjaService.class);
    Mockito.when(jinjaService.render(Mockito.any(), Mockito.eq("{{ firstName }}"))).thenReturn("John");
    Mockito.when(jinjaService.render(Mockito.any(), Mockito.eq("{{ requestFirstName }}"))).thenReturn("John");
    var plugin = new ContextCompareValidationPlugin(jinjaService);
    var configuration = new ValidationConfiguration();
    configuration.addOption("value1", "{{ firstName }}");
    configuration.addOption("value2", "{{ requestFirstName }}");
    var context = new TaskExecutionContext();

    var error = plugin.validate(configuration, "ignored", context);

    assertNotNull(error);
    assertTrue(error.isEmpty());
  }

  @Test
  @DisplayName("test validate: should return error when rendered values mismatch")
  void testValidateMismatch() {
    var jinjaService = Mockito.mock(JinjaService.class);
    Mockito.when(jinjaService.render(Mockito.any(), Mockito.eq("{{ firstName }}"))).thenReturn("John");
    Mockito.when(jinjaService.render(Mockito.any(), Mockito.eq("{{ requestFirstName }}"))).thenReturn("Jane");
    var plugin = new ContextCompareValidationPlugin(jinjaService);
    var configuration = new ValidationConfiguration();
    configuration.addOption("value1", "{{ firstName }}");
    configuration.addOption("value2", "{{ requestFirstName }}");
    var context = new TaskExecutionContext();

    var error = plugin.validate(configuration, "ignored", context);

    assertNotNull(error);
    assertTrue(error.isPresent());
    assertEquals("error.plugin.contextCompareValidation.mismatch", error.get().key());
    assertEquals(Map.of("value1", "John", "value2", "Jane"), error.get().context());
  }

  @Test
  @DisplayName("test validate: should pass with trim when rendered values differ only by whitespace")
  void testValidatePassWithTrim() {
    var jinjaService = Mockito.mock(JinjaService.class);
    Mockito.when(jinjaService.render(Mockito.any(), Mockito.eq("{{ value1 }}"))).thenReturn("  hello  ");
    Mockito.when(jinjaService.render(Mockito.any(), Mockito.eq("{{ value2 }}"))).thenReturn("hello");
    var plugin = new ContextCompareValidationPlugin(jinjaService);
    var configuration = new ValidationConfiguration();
    configuration.addOption("value1", "{{ value1 }}");
    configuration.addOption("value2", "{{ value2 }}");
    configuration.addOption("trim", true);
    var context = new TaskExecutionContext();

    var error = plugin.validate(configuration, "ignored", context);

    assertNotNull(error);
    assertTrue(error.isEmpty());
  }

  @Test
  @DisplayName("test validate: should fail without trim when rendered values differ by whitespace")
  void testValidateFailWithoutTrim() {
    var jinjaService = Mockito.mock(JinjaService.class);
    Mockito.when(jinjaService.render(Mockito.any(), Mockito.eq("{{ value1 }}"))).thenReturn("  hello  ");
    Mockito.when(jinjaService.render(Mockito.any(), Mockito.eq("{{ value2 }}"))).thenReturn("hello");
    var plugin = new ContextCompareValidationPlugin(jinjaService);
    var configuration = new ValidationConfiguration();
    configuration.addOption("value1", "{{ value1 }}");
    configuration.addOption("value2", "{{ value2 }}");
    var context = new TaskExecutionContext();

    var error = plugin.validate(configuration, "ignored", context);

    assertNotNull(error);
    assertTrue(error.isPresent());
  }

  @Test
  @DisplayName("test validate: should pass with ignoreCase when rendered values differ by case")
  void testValidatePassWithIgnoreCase() {
    var jinjaService = Mockito.mock(JinjaService.class);
    Mockito.when(jinjaService.render(Mockito.any(), Mockito.eq("{{ value1 }}"))).thenReturn("Hello");
    Mockito.when(jinjaService.render(Mockito.any(), Mockito.eq("{{ value2 }}"))).thenReturn("hello");
    var plugin = new ContextCompareValidationPlugin(jinjaService);
    var configuration = new ValidationConfiguration();
    configuration.addOption("value1", "{{ value1 }}");
    configuration.addOption("value2", "{{ value2 }}");
    configuration.addOption("ignoreCase", true);
    var context = new TaskExecutionContext();

    var error = plugin.validate(configuration, "ignored", context);

    assertNotNull(error);
    assertTrue(error.isEmpty());
  }

  @Test
  @DisplayName("test validate: should fail without ignoreCase when rendered values differ by case")
  void testValidateFailWithoutIgnoreCase() {
    var jinjaService = Mockito.mock(JinjaService.class);
    Mockito.when(jinjaService.render(Mockito.any(), Mockito.eq("{{ value1 }}"))).thenReturn("Hello");
    Mockito.when(jinjaService.render(Mockito.any(), Mockito.eq("{{ value2 }}"))).thenReturn("hello");
    var plugin = new ContextCompareValidationPlugin(jinjaService);
    var configuration = new ValidationConfiguration();
    configuration.addOption("value1", "{{ value1 }}");
    configuration.addOption("value2", "{{ value2 }}");
    var context = new TaskExecutionContext();

    var error = plugin.validate(configuration, "ignored", context);

    assertNotNull(error);
    assertTrue(error.isPresent());
  }

  @Test
  @DisplayName("test validate: should pass with both trim and ignoreCase")
  void testValidatePassWithTrimAndIgnoreCase() {
    var jinjaService = Mockito.mock(JinjaService.class);
    Mockito.when(jinjaService.render(Mockito.any(), Mockito.eq("{{ value1 }}"))).thenReturn("  Hello  ");
    Mockito.when(jinjaService.render(Mockito.any(), Mockito.eq("{{ value2 }}"))).thenReturn("hello");
    var plugin = new ContextCompareValidationPlugin(jinjaService);
    var configuration = new ValidationConfiguration();
    configuration.addOption("value1", "{{ value1 }}");
    configuration.addOption("value2", "{{ value2 }}");
    configuration.addOption("trim", true);
    configuration.addOption("ignoreCase", true);
    var context = new TaskExecutionContext();

    var error = plugin.validate(configuration, "ignored", context);

    assertNotNull(error);
    assertTrue(error.isEmpty());
  }

  @Test
  @DisplayName("test validate: should work with null value parameter")
  void testValidateNullValue() {
    var jinjaService = Mockito.mock(JinjaService.class);
    Mockito.when(jinjaService.render(Mockito.any(), Mockito.eq("{{ value1 }}"))).thenReturn("hello");
    Mockito.when(jinjaService.render(Mockito.any(), Mockito.eq("{{ value2 }}"))).thenReturn("hello");
    var plugin = new ContextCompareValidationPlugin(jinjaService);
    var configuration = new ValidationConfiguration();
    configuration.addOption("value1", "{{ value1 }}");
    configuration.addOption("value2", "{{ value2 }}");
    var context = new TaskExecutionContext();

    var error = plugin.validate(configuration, null, context);

    assertNotNull(error);
    assertTrue(error.isEmpty());
  }

  @Test
  @DisplayName("test validate: should return error when rendered value1 is null")
  void testValidateNullRenderedValue1() {
    var jinjaService = Mockito.mock(JinjaService.class);
    Mockito.when(jinjaService.render(Mockito.any(), Mockito.eq("{{ value1 }}"))).thenReturn(null);
    Mockito.when(jinjaService.render(Mockito.any(), Mockito.eq("{{ value2 }}"))).thenReturn("hello");
    var plugin = new ContextCompareValidationPlugin(jinjaService);
    var configuration = new ValidationConfiguration();
    configuration.addOption("value1", "{{ value1 }}");
    configuration.addOption("value2", "{{ value2 }}");
    var context = new TaskExecutionContext();

    var error = plugin.validate(configuration, "ignored", context);

    assertNotNull(error);
    assertTrue(error.isPresent());
    assertEquals("error.plugin.contextCompareValidation.null.value", error.get().key());
  }

  @Test
  @DisplayName("test validate: should return error when rendered value2 is null")
  void testValidateNullRenderedValue2() {
    var jinjaService = Mockito.mock(JinjaService.class);
    Mockito.when(jinjaService.render(Mockito.any(), Mockito.eq("{{ value1 }}"))).thenReturn("hello");
    Mockito.when(jinjaService.render(Mockito.any(), Mockito.eq("{{ value2 }}"))).thenReturn(null);
    var plugin = new ContextCompareValidationPlugin(jinjaService);
    var configuration = new ValidationConfiguration();
    configuration.addOption("value1", "{{ value1 }}");
    configuration.addOption("value2", "{{ value2 }}");
    var context = new TaskExecutionContext();

    var error = plugin.validate(configuration, "ignored", context);

    assertNotNull(error);
    assertTrue(error.isPresent());
    assertEquals("error.plugin.contextCompareValidation.null.value", error.get().key());
  }
}
