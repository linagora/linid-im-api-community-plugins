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

package io.github.linagora.linid.im.myplugin;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import io.github.linagora.linid.im.corelib.plugin.authorization.AllowAllAuthorizationPlugin;
import io.github.linagora.linid.im.corelib.plugin.config.dto.RootConfiguration;
import io.github.linagora.linid.im.corelib.plugin.entity.DynamicEntity;
import io.github.linagora.linid.im.corelib.plugin.task.TaskExecutionContext;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.LinkedMultiValueMap;

@ExtendWith(MockitoExtension.class)
@DisplayName("Test class:  MyPluginAuthorizationPlugin")
class MyPluginAuthorizationPluginTest {

  @InjectMocks
  private MyPluginAuthorizationPlugin plugin;

  @Test
  @DisplayName("Test support: should return true only on MyPlugin")
  void testSupports() {
    assertTrue(plugin.supports("MyPlugin"));
    assertFalse(plugin.supports("Other"));
  }

  @Test
  @DisplayName("Test updateConfiguration: should not throw exception")
  void testUpdateConfiguration() {
    assertDoesNotThrow(() -> new AllowAllAuthorizationPlugin().updateConfiguration(Mockito.mock(RootConfiguration.class)));
  }

  @Test
  @DisplayName("Test validateToken: should not throw exception")
  void testValidateToken() {
    var request = Mockito.mock(HttpServletRequest.class);
    var context = new TaskExecutionContext();
    var entity = new DynamicEntity();
    var filters = new LinkedMultiValueMap<String, String>();

    assertDoesNotThrow(() -> new AllowAllAuthorizationPlugin().validateToken(request, context));
    assertDoesNotThrow(() -> new AllowAllAuthorizationPlugin().isAuthorized(request, entity, "read", context));
    assertDoesNotThrow(() -> new AllowAllAuthorizationPlugin().isAuthorized(request, entity, "123", "read", context));
    assertDoesNotThrow(() -> new AllowAllAuthorizationPlugin().isAuthorized(request, entity, filters, "read", context));
  }
}
