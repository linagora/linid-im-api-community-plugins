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

package io.github.linagora.linid.im.dlvp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import io.github.linagora.linid.im.corelib.plugin.config.JinjaService;
import io.github.linagora.linid.im.corelib.plugin.config.dto.PluginConfiguration;
import io.github.linagora.linid.im.corelib.plugin.config.dto.ValidationConfiguration;
import io.github.linagora.linid.im.corelib.plugin.entity.DynamicEntity;
import io.github.linagora.linid.im.corelib.plugin.task.TaskEngine;
import io.github.linagora.linid.im.corelib.plugin.task.TaskExecutionContext;
import io.github.linagora.linid.im.dlvp.service.HttpService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Test class: DynamicListValidationPlugin")
class DynamicListValidationPluginTest {

  @Mock
  private HttpService httpService;

  @Mock
  private JinjaService jinjaService;

  @Mock
  private TaskEngine taskEngine;

  @InjectMocks
  private DynamicListValidationPlugin plugin;

  @Test
  @DisplayName("test supports: should return true on valid type")
  void testSupports() {
    assertTrue(plugin.supports("dynamic-list"));
    assertFalse(plugin.supports("other"));
  }

  private ValidationConfiguration buildConfiguration() {
    var configuration = new ValidationConfiguration();
    configuration.addOption("url", "https://api.example.com/types");
    configuration.addOption("method", "GET");
    configuration.addOption("itemsCount", "{{ context.response.content.size() }}");
    configuration.addOption("elementMapping", Map.of(
        "label", "{{ context.response.content[index].name }}",
        "value", "{{ context.response.content[index].id }}"
    ));
    configuration.addOption("tasks", List.of(
        Map.of("type", "json-parsing", "source", "response",
            "destination", "response",
            "phases", List.of("beforeDynamicListMapping"))
    ));
    return configuration;
  }

  private void mockServiceAndJinja(String[]... labelValuePairs) {
    when(httpService.request(any(TaskExecutionContext.class), any(PluginConfiguration.class)))
        .thenReturn("{\"content\":[]}");

    when(jinjaService.render(any(TaskExecutionContext.class), any(DynamicEntity.class),
        eq("{{ context.response.content.size() }}")))
        .thenReturn(String.valueOf(labelValuePairs.length));

    for (int i = 0; i < labelValuePairs.length; i++) {
      when(jinjaService.render(any(TaskExecutionContext.class), any(DynamicEntity.class),
          eq(Map.of("index", i)), eq("{{ context.response.content[index].name }}")))
          .thenReturn(labelValuePairs[i][0]);
      when(jinjaService.render(any(TaskExecutionContext.class), any(DynamicEntity.class),
          eq(Map.of("index", i)), eq("{{ context.response.content[index].id }}")))
          .thenReturn(labelValuePairs[i][1]);
    }
  }

  @Test
  @DisplayName("test validate: should return message on null value without making HTTP request")
  void testValidateNullValue() {
    var configuration = buildConfiguration();

    var error = plugin.validate(configuration, null);

    assertNotNull(error);
    assertTrue(error.isPresent());
    assertEquals("error.plugin.dynamicListValidation.invalid.value", error.get().key());
    assertEquals(
        Map.of("allowedValues", List.of(), "value", "null"),
        error.get().context()
    );
  }

  @Test
  @DisplayName("test validate: should return message on invalid value")
  void testValidateInvalidValue() {
    var configuration = buildConfiguration();
    mockServiceAndJinja(new String[]{"TypeA", "A"}, new String[]{"TypeB", "B"});

    var error = plugin.validate(configuration, "C");

    assertNotNull(error);
    assertTrue(error.isPresent());
    assertEquals("error.plugin.dynamicListValidation.invalid.value", error.get().key());
    assertEquals(
        Map.of("allowedValues", List.of("A", "B"), "value", "C"),
        error.get().context()
    );
  }

  @Test
  @DisplayName("test validate: should not return message on valid value")
  void testValidateValidValue() {
    var configuration = buildConfiguration();
    mockServiceAndJinja(new String[]{"TypeA", "A"}, new String[]{"TypeB", "B"});

    var error = plugin.validate(configuration, "A");

    assertNotNull(error);
    assertTrue(error.isEmpty());

    verify(taskEngine).execute(any(DynamicEntity.class), any(TaskExecutionContext.class),
        eq("beforeDynamicList"));
    verify(taskEngine).execute(any(DynamicEntity.class), any(TaskExecutionContext.class),
        eq("afterDynamicList"));
    verify(taskEngine).execute(any(DynamicEntity.class), any(TaskExecutionContext.class),
        eq("beforeDynamicListMapping"));
    verify(taskEngine).execute(any(DynamicEntity.class), any(TaskExecutionContext.class),
        eq("afterDynamicListMapping"));
  }

  @Test
  @DisplayName("test validate: should return message on empty list")
  void testValidateEmptyList() {
    var configuration = buildConfiguration();
    mockServiceAndJinja();

    var error = plugin.validate(configuration, "A");

    assertNotNull(error);
    assertTrue(error.isPresent());
    assertEquals("error.plugin.dynamicListValidation.invalid.value", error.get().key());
  }

  @Test
  @DisplayName("test validate: should validate case-sensitive values")
  void testValidateCaseSensitive() {
    var configuration = buildConfiguration();
    mockServiceAndJinja(new String[]{"Active", "ACTIVE"});

    var error = plugin.validate(configuration, "active");

    assertNotNull(error);
    assertTrue(error.isPresent());
    assertEquals("error.plugin.dynamicListValidation.invalid.value", error.get().key());
  }

  @Test
  @DisplayName("test validate: should propagate exception from service")
  void testValidateExternalApiFails() {
    var configuration = buildConfiguration();
    when(httpService.request(any(TaskExecutionContext.class), any(PluginConfiguration.class)))
        .thenThrow(new ApiException(502, I18nMessage.of("dlvp.error.external.api.unavailable")));

    assertThrows(ApiException.class, () -> plugin.validate(configuration, "A"));
  }

  @Test
  @DisplayName("test validate: should convert non-string value types using toString")
  void testValidateNonStringValueTypes() {
    var configuration = buildConfiguration();
    mockServiceAndJinja(new String[]{"FortyTwo", "42"}, new String[]{"True", "true"});

    var errorInt = plugin.validate(configuration, 42);

    assertNotNull(errorInt);
    assertTrue(errorInt.isEmpty());

    var errorBool = plugin.validate(configuration, true);

    assertNotNull(errorBool);
    assertTrue(errorBool.isEmpty());
  }
}
