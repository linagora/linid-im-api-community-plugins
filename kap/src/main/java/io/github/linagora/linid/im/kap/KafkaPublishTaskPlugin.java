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

import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import io.github.linagora.linid.im.corelib.plugin.config.JinjaService;
import io.github.linagora.linid.im.corelib.plugin.config.dto.TaskConfiguration;
import io.github.linagora.linid.im.corelib.plugin.entity.DynamicEntity;
import io.github.linagora.linid.im.corelib.plugin.task.TaskExecutionContext;
import io.github.linagora.linid.im.corelib.plugin.task.TaskPlugin;
import io.github.linagora.linid.im.kap.model.KafkaConnection;
import io.github.linagora.linid.im.kap.model.KafkaHeader;
import io.github.linagora.linid.im.kap.model.KafkaOptions;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;

/**
 * Task plugin that publishes messages to Apache Kafka.
 *
 * <p>This plugin reads connection, topic, payload, key, headers, and options from the task
 * configuration, resolves Jinja templates, and sends a message asynchronously to the configured
 * Kafka topic. It supports SSL/SASL authentication, compression, and partitioning. Retry logic
 * is delegated to the native Kafka producer configuration.
 */
@Slf4j
@Component
public class KafkaPublishTaskPlugin implements TaskPlugin {

  private static final String MISSING_OPTION = "error.plugin.default.missing.option";
  private static final String OPTION = "option";

  private static final KafkaOptions DEFAULT_OPTIONS = new KafkaOptions(
      null, null, null, null, null, null, null);

  private final JinjaService jinjaService;
  private final KafkaProducerFactory producerFactory;

  /**
   * Constructor with dependency injection.
   *
   * @param jinjaService    Service for rendering Jinja templates.
   * @param producerFactory Factory for creating and caching Kafka producers.
   */
  public KafkaPublishTaskPlugin(final JinjaService jinjaService,
                                final KafkaProducerFactory producerFactory) {
    this.jinjaService = jinjaService;
    this.producerFactory = producerFactory;
  }

  /**
   * Returns whether this plugin handles the given task type.
   *
   * @param type the task type identifier
   * @return {@code true} if the type is {@code "kafka-publish"}, {@code false} otherwise
   */
  @Override
  public boolean supports(final String type) {
    return "kafka-publish".equals(type);
  }

  /**
   * Publishes a message to a Kafka topic asynchronously.
   *
   * <p>Reads connection, topic, payload, key, headers, and options from the task configuration,
   * resolves Jinja templates against the execution context and entity attributes, then sends
   * the message using a cached Kafka producer. Retry logic is handled natively by the Kafka
   * producer via {@code retries}, {@code delivery.timeout.ms}, and {@code retry.backoff.ms}
   * properties.
   *
   * @param configuration the task configuration containing Kafka options
   * @param entity        the dynamic entity whose attributes are available in templates
   * @param context       the execution context containing contextual data
   */
  @Override
  public void execute(final TaskConfiguration configuration,
                      final DynamicEntity entity,
                      final TaskExecutionContext context) {
    KafkaConnection connection = configuration
        .getOption("connection", new TypeReference<KafkaConnection>() {})
        .orElseThrow(() -> new ApiException(500,
            I18nMessage.of(MISSING_OPTION, Map.of(OPTION, "connection"))));

    String topicTemplate = configuration.getOption("topic")
        .orElseThrow(() -> new ApiException(500,
            I18nMessage.of(MISSING_OPTION, Map.of(OPTION, "topic"))));

    String payloadTemplate = configuration.getOption("payload")
        .orElseThrow(() -> new ApiException(500,
            I18nMessage.of(MISSING_OPTION, Map.of(OPTION, "payload"))));

    String keyTemplate = configuration.getOption("key").orElse(null);

    List<KafkaHeader> headerTemplates = configuration
        .getOption("headers", new TypeReference<List<KafkaHeader>>() {})
        .orElse(List.of());

    KafkaOptions options = configuration
        .getOption("options", new TypeReference<KafkaOptions>() {})
        .orElse(DEFAULT_OPTIONS);

    String topic = jinjaService.render(context, entity, topicTemplate);
    String key = keyTemplate != null ? jinjaService.render(context, entity, keyTemplate) : null;
    String payload = jinjaService.render(context, entity, payloadTemplate);

    if (options.isNormalizeWhitespace()) {
      payload = payload.strip().replaceAll("\\s+", " ");
    }

    List<Header> headers = headerTemplates.stream()
        .<Header>map(h -> new RecordHeader(
            jinjaService.render(context, entity, h.name()),
            jinjaService.render(context, entity, h.value()).getBytes(StandardCharsets.UTF_8)))
        .toList();

    ProducerRecord<String, String> record = new ProducerRecord<>(
        topic, options.partition(), options.timestamp(), key, payload, headers);

    KafkaProducer<String, String> producer = producerFactory.getOrCreate(connection, options);

    producer.send(record, (metadata, exception) -> {
      if (exception != null) {
        log.error("Failed to send message to topic '{}': {}",
            record.topic(), exception.getMessage(), exception);
        return;
      }
      log.info("Message sent to topic '{}' partition {} offset {}",
          metadata.topic(), metadata.partition(), metadata.offset());
    });
  }
}
