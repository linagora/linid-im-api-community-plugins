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

package io.github.linagora.linid.im.hpp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

@DisplayName("Test class: ResponseToJsonTaskPlugin")
public class ResponseToJsonTaskPluginTest {

  @Test
  @DisplayName("test supports: should return true on valid type")
  void testSupports() {
    var plugin = new ResponseToJsonTaskPlugin();

    assertTrue(plugin.supports("response-to-json"));
    assertFalse(plugin.supports("other"));
  }

  @Test
  @DisplayName("test execute: should set json in response")
  public void testExecute() {
    var plugin = new ResponseToJsonTaskPlugin();
    var context = new TaskExecutionContext();
    context.put("response", "{\"test\":\"test\"}");
    plugin.execute(new TaskConfiguration(), new DynamicEntity(), context);

    assertNotNull(context.get("response"));
    assertEquals(LinkedHashMap.class, context.get("response").getClass());
    assertEquals(Map.of("test", "test"), context.get("response"));
  }

  @Test
  @DisplayName("test execute: should throw exception on invalid json")
  public void testExecuteThrowException() {
    var plugin = new ResponseToJsonTaskPlugin();
    var context = new TaskExecutionContext();
    context.put("response", "{\"test\":\"test\"");

    var exception = assertThrows(ApiException.class, () -> plugin
        .execute(new TaskConfiguration(), new DynamicEntity(), context));

    assertNotNull(exception);
    assertEquals("hpp.error.json", exception.getError().key());
  }
}
