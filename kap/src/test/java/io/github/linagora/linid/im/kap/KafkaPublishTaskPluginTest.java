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

package io.github.linagora.linid.im.kap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.plugin.config.JinjaService;
import io.github.linagora.linid.im.corelib.plugin.config.dto.TaskConfiguration;
import io.github.linagora.linid.im.corelib.plugin.entity.DynamicEntity;
import io.github.linagora.linid.im.corelib.plugin.task.TaskExecutionContext;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

@DisplayName("Test class: KafkaPublishTaskPlugin")
class KafkaPublishTaskPluginTest {

  private JinjaService jinjaService;
  private KafkaProducerFactory producerFactory;
  private KafkaProducer<String, String> mockProducer;
  private KafkaPublishTaskPlugin plugin;

  @SuppressWarnings("unchecked")
  @BeforeEach
  void setUp() {
    jinjaService = Mockito.mock(JinjaService.class);
    producerFactory = Mockito.mock(KafkaProducerFactory.class);
    mockProducer = Mockito.mock(KafkaProducer.class);
    plugin = new KafkaPublishTaskPlugin(jinjaService, producerFactory);

    Mockito.when(producerFactory.getOrCreate(Mockito.any(), Mockito.any()))
        .thenReturn(mockProducer);

    Mockito.when(jinjaService.render(
            Mockito.any(TaskExecutionContext.class), Mockito.any(DynamicEntity.class),
            Mockito.anyString()))
        .thenAnswer(invocation -> invocation.getArgument(2));
  }

  @SuppressWarnings("unchecked")
  private ProducerRecord<String, String> captureRecord() {
    ArgumentCaptor<ProducerRecord<String, String>> captor =
        ArgumentCaptor.forClass(ProducerRecord.class);
    Mockito.verify(mockProducer).send(captor.capture(), Mockito.any(Callback.class));
    return captor.getValue();
  }

  // --- supports ---

  @Test
  @DisplayName("test supports: should return true on valid type")
  void testSupports() {
    assertTrue(plugin.supports("kafka-publish"));
  }

  @Test
  @DisplayName("test supports: should return false on other type")
  void testSupportsOtherType() {
    assertFalse(plugin.supports("context-mapping"));
    assertFalse(plugin.supports("other"));
  }

  // --- missing options ---

  @Test
  @DisplayName("test execute: should throw when connection option is missing")
  void testExecuteMissingConnection() {
    var config = new TaskConfiguration();
    config.addOption("topic", "my-topic");
    config.addOption("payload", "data");

    var exception = assertThrows(ApiException.class,
        () -> plugin.execute(config, new DynamicEntity(), new TaskExecutionContext()));

    assertEquals(500, exception.getStatusCode());
    assertEquals("error.plugin.default.missing.option", exception.getError().key());
    assertEquals(Map.of("option", "connection"), exception.getError().context());
  }

  @Test
  @DisplayName("test execute: should throw when topic option is missing")
  void testExecuteMissingTopic() {
    var config = buildBasicConfig();
    config.getOptions().remove("topic");

    var exception = assertThrows(ApiException.class,
        () -> plugin.execute(config, new DynamicEntity(), new TaskExecutionContext()));

    assertEquals(500, exception.getStatusCode());
    assertEquals("error.plugin.default.missing.option", exception.getError().key());
    assertEquals(Map.of("option", "topic"), exception.getError().context());
  }

  @Test
  @DisplayName("test execute: should throw when payload option is missing")
  void testExecuteMissingPayload() {
    var config = buildBasicConfig();
    config.getOptions().remove("payload");

    var exception = assertThrows(ApiException.class,
        () -> plugin.execute(config, new DynamicEntity(), new TaskExecutionContext()));

    assertEquals(500, exception.getStatusCode());
    assertEquals("error.plugin.default.missing.option", exception.getError().key());
    assertEquals(Map.of("option", "payload"), exception.getError().context());
  }

  // --- successful send ---

  @Test
  @DisplayName("test execute: should send message with correct topic, key, and payload")
  void testExecuteSuccess() {
    var config = buildBasicConfig();
    config.addOption("key", "{{context.userId}}");

    Mockito.when(jinjaService.render(
            Mockito.any(TaskExecutionContext.class), Mockito.any(DynamicEntity.class),
            Mockito.eq("{{context.userId}}")))
        .thenReturn("user-123");

    plugin.execute(config, new DynamicEntity(), new TaskExecutionContext());

    var record = captureRecord();
    assertEquals("test-topic", record.topic());
    assertEquals("user-123", record.key());
    assertEquals("{\"event\":\"created\"}", record.value());
  }

  @Test
  @DisplayName("test execute: should send message without key when key is not configured")
  void testExecuteWithoutKey() {
    plugin.execute(buildBasicConfig(), new DynamicEntity(), new TaskExecutionContext());

    assertNull(captureRecord().key());
  }

  // --- headers ---

  @Test
  @DisplayName("test execute: should resolve and attach all headers")
  void testExecuteWithHeaders() {
    var config = buildBasicConfig();
    config.addOption("headers", List.of(
        Map.of("name", "correlationId", "value", "{{context.requestId}}"),
        Map.of("name", "source", "value", "linid")));

    Mockito.when(jinjaService.render(
            Mockito.any(TaskExecutionContext.class), Mockito.any(DynamicEntity.class),
            Mockito.eq("{{context.requestId}}")))
        .thenReturn("req-456");

    plugin.execute(config, new DynamicEntity(), new TaskExecutionContext());

    List<Header> headers = new ArrayList<>();
    captureRecord().headers().forEach(headers::add);

    assertEquals(2, headers.size());
    assertEquals("correlationId", headers.get(0).key());
    assertEquals("req-456", new String(headers.get(0).value(), StandardCharsets.UTF_8));
    assertEquals("source", headers.get(1).key());
    assertEquals("linid", new String(headers.get(1).value(), StandardCharsets.UTF_8));
  }

  // --- options ---

  @Test
  @DisplayName("test execute: should apply partition and timestamp from options")
  void testExecuteWithPartitionAndTimestamp() {
    var config = buildBasicConfig();
    config.addOption("options", Map.of(
        "partition", 2,
        "timestamp", 1234567890L));

    plugin.execute(config, new DynamicEntity(), new TaskExecutionContext());

    var record = captureRecord();
    assertEquals(2, record.partition());
    assertEquals(1234567890L, record.timestamp());
  }

  // --- normalizeWhitespace ---

  @Test
  @DisplayName("test execute: should normalize whitespace in payload when enabled")
  void testExecuteNormalizeWhitespace() {
    var config = buildBasicConfig();
    config.addOption("options", Map.of("normalizeWhitespace", true));

    Mockito.when(jinjaService.render(
            Mockito.any(TaskExecutionContext.class), Mockito.any(DynamicEntity.class),
            Mockito.eq("{\"event\":\"created\"}")))
        .thenReturn("  { \"event\" :  \n  \"created\" }  ");

    plugin.execute(config, new DynamicEntity(), new TaskExecutionContext());

    assertEquals("{ \"event\" : \"created\" }", captureRecord().value());
  }

  @Test
  @DisplayName("test execute: should not normalize whitespace when option is not set")
  void testExecuteNoNormalizeWhitespace() {
    String rawPayload = "  { \"event\" :  \n  \"created\" }  ";
    Mockito.when(jinjaService.render(
            Mockito.any(TaskExecutionContext.class), Mockito.any(DynamicEntity.class),
            Mockito.eq("{\"event\":\"created\"}")))
        .thenReturn(rawPayload);

    plugin.execute(buildBasicConfig(), new DynamicEntity(), new TaskExecutionContext());

    assertEquals(rawPayload, captureRecord().value());
  }

  // --- helpers ---

  private TaskConfiguration buildBasicConfig() {
    var config = new TaskConfiguration();
    config.addOption("connection", Map.of(
        "brokers", List.of("kafka:9092"),
        "clientId", "test-client"));
    config.addOption("topic", "test-topic");
    config.addOption("payload", "{\"event\":\"created\"}");
    return config;
  }
}
