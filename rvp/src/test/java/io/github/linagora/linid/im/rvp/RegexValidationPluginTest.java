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

package io.github.linagora.linid.im.rvp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.plugin.config.dto.ValidationConfiguration;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Test class: RegexValidationPlugin")
public class RegexValidationPluginTest {

  @Test
  @DisplayName("test supports: should return true on valid type")
  void testSupports() {
    var plugin = new RegexValidationPlugin();

    assertTrue(plugin.supports("regex"));
    assertFalse(plugin.supports("other"));
  }

  @Test
  @DisplayName("test validate: should thrown an exception on missing pattern option")
  void testValidateMissingPatternOption() {
    var plugin = new RegexValidationPlugin();
    var configuration = new ValidationConfiguration();

    ApiException exception = assertThrows(ApiException.class, () -> plugin.validate(configuration, ""));

    assertEquals(500, exception.getStatusCode());
    assertEquals("error.plugin.default.missing.option", exception.getError().key());
    assertEquals(Map.of("option", "pattern"), exception.getError().context());
  }

  @Test
  @DisplayName("test validate: should thrown an exception on invalid pattern option")
  void testValidateInvalidPatternOption() {
    var plugin = new RegexValidationPlugin();
    var configuration = new ValidationConfiguration();
    configuration.addOption("pattern", "[a-z");

    ApiException exception = assertThrows(ApiException.class, () -> plugin.validate(configuration, ""));

    assertEquals(500, exception.getStatusCode());
    assertEquals("error.plugin.default.invalid.option", exception.getError().key());
    assertEquals(Map.of("option", "pattern", "value", "[a-z"), exception.getError().context());
  }

  @Test
  @DisplayName("test validate: should thrown an exception on invalid insensitive pattern option")
  void testValidateInvalidInsensitivePatternOption() {
    var plugin = new RegexValidationPlugin();
    var configuration = new ValidationConfiguration();
    configuration.addOption("pattern", "[a-z");
    configuration.addOption("insensitive", true);

    ApiException exception = assertThrows(ApiException.class, () -> plugin.validate(configuration, ""));

    assertEquals(500, exception.getStatusCode());
    assertEquals("error.plugin.default.invalid.option", exception.getError().key());
    assertEquals(Map.of("option", "pattern", "value", "[a-z"), exception.getError().context());
  }

  @Test
  @DisplayName("test validate: should return message on invalid value")
  void testValidateInvalidValue() {
    var plugin = new RegexValidationPlugin();
    var configuration = new ValidationConfiguration();
    configuration.addOption("pattern", "[a-z]{1}");

    var error = plugin.validate(configuration, "A");

    assertNotNull(error);
    assertTrue(error.isPresent());
    assertEquals("error.plugin.regexValidation.invalid.value", error.get().key());
    assertEquals(Map.of("pattern", "[a-z]{1}", "value", "A"), error.get().context());
  }

  @Test
  @DisplayName("test validate: should not return message on valid value")
  void testValidateValidValue() {
    var plugin = new RegexValidationPlugin();
    var configuration = new ValidationConfiguration();
    configuration.addOption("pattern", "[a-z]{1}");

    var error = plugin.validate(configuration, "a");

    assertNotNull(error);
    assertTrue(error.isEmpty());
  }

  @Test
  @DisplayName("test validate: should not return message on insensitive valid value")
  void testValidateInsensitiveValidValue() {
    var plugin = new RegexValidationPlugin();
    var configuration = new ValidationConfiguration();
    configuration.addOption("pattern", "[a-z]{1}");
    configuration.addOption("insensitive", true);

    var error = plugin.validate(configuration, "A");

    assertNotNull(error);
    assertTrue(error.isEmpty());
  }
}
