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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hubspot.jinjava.Jinjava;
import io.github.linagora.linid.im.corelib.plugin.config.JinjaService;
import io.github.linagora.linid.im.corelib.plugin.config.dto.TaskConfiguration;
import io.github.linagora.linid.im.corelib.plugin.entity.DynamicEntity;
import io.github.linagora.linid.im.corelib.plugin.task.TaskExecutionContext;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * End-to-end tests for {@link KafkaPublishTaskPlugin} with a real Kafka broker.
 *
 * <p>Requires a running Kafka broker on {@code localhost:9092}.
 * Use {@code docker compose -f kap/src/test/resources/docker-compose.yml up -d} before running.
 */
@DisplayName("Test class: KafkaPublishTaskPlugin E2E")
class KafkaPublishTaskPluginE2ETest {

  private static final String BROKER = "localhost:9092";

  private KafkaPublishTaskPlugin plugin;
  private KafkaProducerFactoryImpl producerFactory;
  private KafkaConsumer<String, String> consumer;

  @BeforeEach
  void setUp() {
    producerFactory = new KafkaProducerFactoryImpl();
    plugin = new KafkaPublishTaskPlugin(new JinjaServiceImpl(), producerFactory);
  }

  @AfterEach
  void tearDown() {
    if (consumer != null) {
      consumer.close();
    }
    producerFactory.closeAll();
  }

  @Test
  @DisplayName("test e2e: should publish a message to Kafka and consume it")
  void testPublishAndConsume() {
    String topic = "e2e-test-publish-" + System.currentTimeMillis();

    var config = new TaskConfiguration();
    config.addOption("connection", Map.of("brokers", List.of(BROKER)));
    config.addOption("topic", topic);
    config.addOption("key", "test-key");
    config.addOption("payload", "{\"event\":\"created\",\"id\":\"42\"}");

    plugin.execute(config, new DynamicEntity(), new TaskExecutionContext());
    producerFactory.flushAll();

    consumer = createConsumer(topic);

    ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));

    assertEquals(1, records.count());
    ConsumerRecord<String, String> record = records.iterator().next();
    assertEquals("test-key", record.key());
    assertTrue(record.value().contains("\"event\":\"created\""));
    assertTrue(record.value().contains("\"id\":\"42\""));
  }

  @Test
  @DisplayName("test e2e: should resolve entity templates in payload")
  void testPublishWithEntityTemplates() {
    String topic = "e2e-test-entity-" + System.currentTimeMillis();

    var config = new TaskConfiguration();
    config.addOption("connection", Map.of("brokers", List.of(BROKER)));
    config.addOption("topic", topic);
    config.addOption("key", "{{ entity.id }}");
    config.addOption("payload", "{\"id\":\"{{ entity.id }}\",\"email\":\"{{ entity.email }}\"}");

    var entity = new DynamicEntity();
    entity.setAttributes(new HashMap<>(Map.of("id", "user-99", "email", "test@example.com")));

    plugin.execute(config, entity, new TaskExecutionContext());
    producerFactory.flushAll();

    consumer = createConsumer(topic);

    ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));

    assertEquals(1, records.count());
    ConsumerRecord<String, String> record = records.iterator().next();
    assertEquals("user-99", record.key());
    assertTrue(record.value().contains("\"id\":\"user-99\""));
    assertTrue(record.value().contains("\"email\":\"test@example.com\""));
  }

  @Test
  @DisplayName("test e2e: should include headers in the published message")
  void testPublishWithHeaders() {
    String topic = "e2e-test-headers-" + System.currentTimeMillis();

    var config = new TaskConfiguration();
    config.addOption("connection", Map.of("brokers", List.of(BROKER)));
    config.addOption("topic", topic);
    config.addOption("payload", "{}");
    config.addOption("headers", List.of(
        Map.of("name", "source", "value", "linid"),
        Map.of("name", "version", "value", "1.0")));

    plugin.execute(config, new DynamicEntity(), new TaskExecutionContext());
    producerFactory.flushAll();

    consumer = createConsumer(topic);

    ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));

    assertEquals(1, records.count());
    ConsumerRecord<String, String> record = records.iterator().next();

    Header sourceHeader = record.headers().lastHeader("source");
    assertNotNull(sourceHeader);
    assertEquals("linid", new String(sourceHeader.value(), StandardCharsets.UTF_8));

    Header versionHeader = record.headers().lastHeader("version");
    assertNotNull(versionHeader);
    assertEquals("1.0", new String(versionHeader.value(), StandardCharsets.UTF_8));
  }

  /**
   * Creates a consumer assigned directly to partition 0 of the given topic, seeking to the
   * beginning. This avoids consumer group coordination issues with Kafka KRaft single-node.
   */
  private KafkaConsumer<String, String> createConsumer(String topic) {
    Properties props = new Properties();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BROKER);
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

    var kafkaConsumer = new KafkaConsumer<String, String>(props);
    TopicPartition partition = new TopicPartition(topic, 0);
    kafkaConsumer.assign(List.of(partition));
    kafkaConsumer.seekToBeginning(List.of(partition));
    return kafkaConsumer;
  }

  /**
   * JinjaService implementation using Jinjava directly, matching production behavior.
   */
  static class JinjaServiceImpl implements JinjaService {

    private final Jinjava jinjava = new Jinjava();

    @Override
    public String render(TaskExecutionContext taskContext, String template) {
      return render(taskContext, null, Map.of(), template);
    }

    @Override
    public String render(TaskExecutionContext taskContext, DynamicEntity entity, String template) {
      return render(taskContext, entity, Map.of(), template);
    }

    @Override
    public String render(TaskExecutionContext taskContext, DynamicEntity entity,
                         Map<String, Object> map, String template) {
      var context = new HashMap<String, Object>();

      context.put("context", taskContext);
      if (entity != null && entity.getAttributes() != null) {
        context.put("entity", entity.getAttributes());
      } else {
        context.put("entity", Map.of());
      }
      context.putAll(map);

      return jinjava.render(template, context);
    }
  }
}
