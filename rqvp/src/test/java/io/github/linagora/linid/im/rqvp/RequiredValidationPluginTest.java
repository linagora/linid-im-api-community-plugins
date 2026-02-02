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

package io.github.linagora.linid.im.rqvp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.linagora.linid.im.corelib.plugin.config.dto.ValidationConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Test class: RequiredValidationPlugin")
class RequiredValidationPluginTest {

  private final RequiredValidationPlugin plugin = new RequiredValidationPlugin();
  private final ValidationConfiguration configuration = new ValidationConfiguration();

  @Test
  @DisplayName("test supports: should return true on valid type")
  void testSupports() {
    assertFalse(plugin.supports("other"));
    assertTrue(plugin.supports("required"));
  }

  @Test
  @DisplayName("test validate: should return message on null value")
  void testValidateNullValue() {
    var error = plugin.validate(configuration, null);

    assertEquals("error.plugin.required.empty.value", error.get().key());
  }

  @Test
  @DisplayName("test validate: should return message on empty string value")
  void testValidateEmptyStringValue() {
    var error = plugin.validate(configuration, "");

    assertNotNull(error);
    assertTrue(error.isPresent());
    assertEquals("error.plugin.required.empty.value", error.get().key());
  }

  @Test
  @DisplayName("test validate: should not return message on valid string value")
  void testValidateValidStringValue() {
    var error = plugin.validate(configuration, "valid value");

    assertNotNull(error);
    assertTrue(error.isEmpty());
  }

  @Test
  @DisplayName("test validate: should not return message on valid number value")
  void testValidateValidNumberValue() {
    var error = plugin.validate(configuration, 123);

    assertNotNull(error);
    assertTrue(error.isEmpty());
  }
}
