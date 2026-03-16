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

package io.github.linagora.linid.im.hpp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hubspot.jinjava.Jinjava;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.plugin.config.JinjaService;
import io.github.linagora.linid.im.corelib.plugin.config.dto.TaskConfiguration;
import io.github.linagora.linid.im.corelib.plugin.entity.DynamicEntity;
import io.github.linagora.linid.im.corelib.plugin.task.TaskExecutionContext;
import io.github.linagora.linid.im.hpp.service.HttpServiceImpl;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Test class: HttpTaskPlugin with e2e")
class HttpTaskPluginE2ETest {

  private HttpTaskPlugin taskPlugin;

  @BeforeEach
  void setup() {
    var jinjaService = new JinjaServiceTest();
    var httpService = new HttpServiceImpl(jinjaService);
    taskPlugin = new HttpTaskPlugin(httpService);
  }

  @Test
  @DisplayName("Test GET request: should store response in context")
  void testGetRequest() {
    var config = new TaskConfiguration();
    config.setType("http");
    config.addOption("url", "http://localhost:3000/v1/test_api/user/1");
    config.addOption("method", "GET");

    var entity = new DynamicEntity();
    entity.setAttributes(new HashMap<>());
    var context = new TaskExecutionContext();

    taskPlugin.execute(config, entity, context);

    assertNotNull(context.get("response"));
    assertTrue(context.get("response").toString().contains("\"id\""));
    assertTrue(context.get("response").toString().contains("John"));
  }

  @Test
  @DisplayName("Test DELETE request: should store response in context")
  void testDeleteRequest() {
    var config = new TaskConfiguration();
    config.setType("http");
    config.addOption("url", "http://localhost:3000/v1/test_api/user/1");
    config.addOption("method", "DELETE");

    var entity = new DynamicEntity();
    entity.setAttributes(new HashMap<>());
    var context = new TaskExecutionContext();

    taskPlugin.execute(config, entity, context);

    assertNotNull(context.get("response"));
    assertTrue(context.get("response").toString().contains("\"status\":\"deleted\""));
  }

  @Test
  @DisplayName("Test GET request with Jinja URL: should render template and store response")
  void testGetRequestWithJinjaUrl() {
    var config = new TaskConfiguration();
    config.setType("http");
    config.addOption("url", "http://localhost:3000/v1/test_api/user/{{ context.id }}");
    config.addOption("method", "GET");

    var entity = new DynamicEntity();
    entity.setAttributes(new HashMap<>());
    var context = new TaskExecutionContext();
    context.put("id", "1");

    taskPlugin.execute(config, entity, context);

    assertNotNull(context.get("response"));
    assertTrue(context.get("response").toString().contains("John"));
  }

  @Test
  @DisplayName("Test request: should throw exception on 4xx")
  void testExceptionOn4xx() {
    var config = new TaskConfiguration();
    config.setType("http");
    config.addOption("url", "http://localhost:3000/v1/test_api/400");
    config.addOption("method", "GET");

    var entity = new DynamicEntity();
    entity.setAttributes(new HashMap<>());
    var context = new TaskExecutionContext();

    var exception = assertThrows(ApiException.class, () -> taskPlugin.execute(config, entity, context));

    assertEquals("hpp.error400", exception.getError().key());
    assertEquals(400, exception.getStatusCode());
  }

  @Test
  @DisplayName("Test request: should throw exception on 5xx")
  void testExceptionOn5xx() {
    var config = new TaskConfiguration();
    config.setType("http");
    config.addOption("url", "http://localhost:3000/v1/test_api/500");
    config.addOption("method", "GET");

    var entity = new DynamicEntity();
    entity.setAttributes(new HashMap<>());
    var context = new TaskExecutionContext();

    var exception = assertThrows(ApiException.class, () -> taskPlugin.execute(config, entity, context));

    assertEquals("hpp.error500", exception.getError().key());
  }

  static class JinjaServiceTest implements JinjaService {

    @Override
    public String render(TaskExecutionContext taskContext, String template) {
      return render(taskContext, null, Map.of(), template);
    }

    @Override
    public String render(TaskExecutionContext taskContext, DynamicEntity entity, String template) {
      return render(taskContext, entity, Map.of(), template);
    }

    @Override
    public String render(TaskExecutionContext taskContext, DynamicEntity entity, Map<String, Object> map,
                         String template) {
      var context = new HashMap<String, Object>();

      if (entity != null && entity.getAttributes() != null) {
        context.put("entity", entity.getAttributes());
      }
      context.put("context", taskContext);
      context.putAll(map);

      return new Jinjava().render(template, context);
    }
  }
}
