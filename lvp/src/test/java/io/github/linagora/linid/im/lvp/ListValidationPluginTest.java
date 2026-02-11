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

package io.github.linagora.linid.im.lvp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.plugin.config.dto.ValidationConfiguration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Test class: ListValidationPlugin")
public class ListValidationPluginTest {
  @Test
  @DisplayName("test supports: should return true on valid type")
  void testSupports() {
    var plugin = new ListValidationPlugin();

    assertTrue(plugin.supports("list"));
    assertFalse(plugin.supports("other"));
  }

  @Test
  @DisplayName("test validate: should thrown an exception on missing allowedValues option")
  void testValidateMissingAllowedValuesOption() {
    var plugin = new ListValidationPlugin();
    var configuration = new ValidationConfiguration();

    ApiException exception =
        assertThrows(ApiException.class, () -> plugin.validate(configuration, ""));

    assertEquals(500, exception.getStatusCode());
    assertEquals("error.plugin.default.missing.option", exception.getError().key());
    assertEquals(Map.of("option", "allowedValues"), exception.getError().context());
  }

  @Test
  @DisplayName("test validate: should thrown an exception on invalid allowedValues option")
  void testValidateInvalidAllowedValuesOption() {
    var plugin = new ListValidationPlugin();
    var configuration = new ValidationConfiguration();
    configuration.addOption("allowedValues", "invalid");
    ApiException exception =
        assertThrows(ApiException.class, () -> plugin.validate(configuration, ""));

    assertEquals(500, exception.getStatusCode());
    assertEquals("error.plugin.default.invalid.option", exception.getError().key());
    assertEquals(Map.of("option", "allowedValues"), exception.getError().context());
  }

  @Test
  @DisplayName("test validate: should return message on null value")
  void testValidateNullValue() {
    var plugin = new ListValidationPlugin();
    var configuration = new ValidationConfiguration();
    configuration.addOption("allowedValues", new String[] {"TEST"});

    var error = plugin.validate(configuration, null);

    assertNotNull(error);
    assertTrue(error.isPresent());
    assertEquals("error.plugin.listValidation.invalid.value", error.get().key());
    assertEquals(Map.of("allowedValues", List.of("TEST"), "value", "null"), error.get().context());
  }

  @Test
  @DisplayName("test validate: should return message on empty string value")
  void testValidateEmptyStringValue() {
    var plugin = new ListValidationPlugin();
    var configuration = new ValidationConfiguration();
    configuration.addOption("allowedValues", new String[] {"TEST"});

    var error = plugin.validate(configuration, "");

    assertNotNull(error);
    assertTrue(error.isPresent());
    assertEquals("error.plugin.listValidation.invalid.value", error.get().key());
    assertEquals(Map.of("allowedValues", List.of("TEST"), "value", ""), error.get().context());
  }

  @Test
  @DisplayName("test validate: should validate case-sensitive values")
  void testValidateCaseSensitive() {
    var plugin = new ListValidationPlugin();
    var configuration = new ValidationConfiguration();
    configuration.addOption("allowedValues", new String[] {"TEST"});

    var error = plugin.validate(configuration, "test");

    assertNotNull(error);
    assertTrue(error.isPresent());
    assertEquals("error.plugin.listValidation.invalid.value", error.get().key());
    assertEquals(Map.of("allowedValues", List.of("TEST"), "value", "test"), error.get().context());
  }

  @Test
  @DisplayName("test validate: should return message on invalid value")
  void testValidateInvalidValue() {
    var plugin = new ListValidationPlugin();
    var configuration = new ValidationConfiguration();
    configuration.addOption("allowedValues", new String[] {"TEST"});

    var error = plugin.validate(configuration, "A");

    assertNotNull(error);
    assertTrue(error.isPresent());
    assertEquals("error.plugin.listValidation.invalid.value", error.get().key());
    assertEquals(Map.of("allowedValues", List.of("TEST"), "value", "A"), error.get().context());
  }

  @Test
  @DisplayName("test validate: should not return message on valid value")
  void testValidateValidValue() {
    var plugin = new ListValidationPlugin();
    var configuration = new ValidationConfiguration();
    configuration.addOption("allowedValues", new String[] {"TEST"});

    var error = plugin.validate(configuration, "TEST");

    assertNotNull(error);
    assertTrue(error.isEmpty());
  }

  @Test
  @DisplayName("test validate: should validate with multiple allowed values")
  void testValidateMultipleAllowedValues() {
    var plugin = new ListValidationPlugin();
    var configuration = new ValidationConfiguration();
    configuration.addOption("allowedValues", new String[] {"VALUE1", "VALUE2", "VALUE3"});

    var error1 = plugin.validate(configuration, "VALUE1");
    var error2 = plugin.validate(configuration, "VALUE2");
    var error3 = plugin.validate(configuration, "VALUE3");
    var errorInvalid = plugin.validate(configuration, "VALUE4");

    assertTrue(error1.isEmpty());
    assertTrue(error2.isEmpty());
    assertTrue(error3.isEmpty());
    assertTrue(errorInvalid.isPresent());
  }

  @Test
  @DisplayName("test validate: should validate with allowed values of different types")
  void testValidateAllowedValuesOfDifferentTypes() {
    var plugin = new ListValidationPlugin();
    var configuration = new ValidationConfiguration();
    configuration.addOption("allowedValues", new String[] {"STRING", "123", "true"});
    
    var errorString = plugin.validate(configuration, "STRING");
    var errorNumber = plugin.validate(configuration, 123);
    var errorBoolean = plugin.validate(configuration, true);
    var errorInvalid = plugin.validate(configuration, "invalid");
    
    assertTrue(errorString.isEmpty());
    assertTrue(errorNumber.isEmpty());
    assertTrue(errorBoolean.isEmpty());
    assertTrue(errorInvalid.isPresent());
  }
}
