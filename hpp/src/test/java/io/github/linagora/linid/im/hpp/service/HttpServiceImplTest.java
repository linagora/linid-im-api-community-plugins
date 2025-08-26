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

package io.github.linagora.linid.im.hpp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.plugin.config.JinjaService;
import io.github.linagora.linid.im.corelib.plugin.config.dto.ProviderConfiguration;
import io.github.linagora.linid.im.corelib.plugin.entity.DynamicEntity;
import io.github.linagora.linid.im.corelib.plugin.task.TaskExecutionContext;
import io.github.linagora.linid.im.hpp.model.EndpointConfiguration;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("Test class: HttpServiceImpl")
class HttpServiceImplTest {

  @Test
  @DisplayName("test request: should throw exception without options")
  void testRequestWithoutOptions() {
    var jinjaService = Mockito.mock(JinjaService.class);
    Mockito.when(jinjaService.render(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn("test");

    var context = new TaskExecutionContext();
    var providerConfiguration = new ProviderConfiguration();
    var endpointConfiguration = new EndpointConfiguration();
    var entity = new DynamicEntity();

    var service = new HttpServiceImpl(jinjaService);

    var exception = assertThrows(ApiException.class, () -> service
        .request(context, providerConfiguration, endpointConfiguration, "create", entity));
    assertNotNull(exception);
    assertEquals("error.plugin.default.missing.option", exception.getError().key());
    assertEquals(Map.of("option", "baseUrl"), exception.getError().context());

    providerConfiguration.addOption("baseUrl", "test");
    exception = assertThrows(ApiException.class, () -> service
        .request(context, providerConfiguration, endpointConfiguration, "create", entity));
    assertNotNull(exception);
    assertEquals("error.plugin.default.missing.option", exception.getError().key());
    assertEquals(Map.of("option", "access.create.uri"), exception.getError().context());

    endpointConfiguration.setUri("test");
    exception = assertThrows(ApiException.class, () -> service
        .request(context, providerConfiguration, endpointConfiguration, "create", entity));
    assertNotNull(exception);
    assertEquals("error.plugin.default.missing.option", exception.getError().key());
    assertEquals(Map.of("option", "access.create.method"), exception.getError().context());
  }

  @Test
  @DisplayName("test request: should throw exception with invalid method")
  void testRequestWithInvalidMethod() {
    var jinjaService = Mockito.mock(JinjaService.class);
    Mockito.when(jinjaService.render(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn("test");

    var context = new TaskExecutionContext();
    var providerConfiguration = new ProviderConfiguration();
    var endpointConfiguration = new EndpointConfiguration();
    var entity = new DynamicEntity();

    providerConfiguration.addOption("baseUrl", "test");
    endpointConfiguration.setUri("test");
    endpointConfiguration.setMethod("invalid");

    var service = new HttpServiceImpl(jinjaService);

    var exception = assertThrows(ApiException.class, () -> service
        .request(context, providerConfiguration, endpointConfiguration, "create", entity));
    assertNotNull(exception);
    assertEquals("error.plugin.default.invalid.option", exception.getError().key());
    assertEquals(Map.of("option", "access.create.method", "value", "invalid"), exception.getError().context());
  }
}
