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
import io.github.linagora.linid.im.kap.model.KafkaConnection;
import io.github.linagora.linid.im.kap.model.KafkaOptions;
import io.github.linagora.linid.im.kap.model.KafkaRetryConfig;
import io.github.linagora.linid.im.kap.model.KafkaSaslConfig;
import io.github.linagora.linid.im.kap.model.KafkaSslConfig;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.stereotype.Component;

/**
 * Default implementation of {@link KafkaProducerFactory}.
 *
 * <p>Producers are cached by a deterministic key derived from the connection configuration,
 * so that repeated calls with the same config reuse the same producer instance.
 */
@Slf4j
@Component
public class KafkaProducerFactoryImpl implements KafkaProducerFactory {

  private final ConcurrentHashMap<String, KafkaProducer<String, String>> producers =
      new ConcurrentHashMap<>();

  @Override
  public KafkaProducer<String, String> getOrCreate(KafkaConnection connection,
                                                    KafkaOptions options) {
    String key = buildKey(connection, options);
    return producers.computeIfAbsent(key, k -> createProducer(connection, options));
  }

  /**
   * Creates a new {@link KafkaProducer} with the given configuration.
   *
   * <p>Sets the thread context classloader to the Kafka classloader before creating the producer
   * to avoid {@code ClassNotFoundException} in plugin classloader environments.
   *
   * @param connection Kafka connection configuration.
   * @param options    Optional advanced options. May be {@code null}.
   * @return A new {@link KafkaProducer} instance.
   */
  KafkaProducer<String, String> createProducer(KafkaConnection connection,
                                                KafkaOptions options) {
    Properties props = buildProperties(connection, options);
    Thread currentThread = Thread.currentThread();
    ClassLoader originalClassLoader = currentThread.getContextClassLoader();
    try {
      currentThread.setContextClassLoader(KafkaProducer.class.getClassLoader());
      return new KafkaProducer<>(props);
    } catch (Exception e) {
      log.error("Failed to create Kafka producer for brokers '{}': {}",
          String.join(",", connection.brokers()), e.getMessage(), e);
      throw new ApiException(500,
          I18nMessage.of("kap.error.producer.creation",
              Map.of("brokers", String.join(",", connection.brokers()))));
    } finally {
      currentThread.setContextClassLoader(originalClassLoader);
    }
  }

  /**
   * Builds Kafka producer {@link Properties} from the plugin configuration.
   *
   * <p>Maps retry options to native Kafka producer properties:
   * {@code retry.attempts} → {@code retries},
   * {@code retry.backoffMs} → {@code retry.backoff.ms},
   * {@code timeoutMs} → {@code delivery.timeout.ms}.
   *
   * @param connection Kafka connection configuration.
   * @param options    Optional advanced options. May be {@code null}.
   * @return Kafka producer properties.
   */
  Properties buildProperties(KafkaConnection connection, KafkaOptions options) {
    if (connection.brokers() == null || connection.brokers().isEmpty()) {
      throw new ApiException(500,
          I18nMessage.of("error.plugin.default.missing.option",
              Map.of("option", "connection.brokers")));
    }

    Properties props = new Properties();

    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
        String.join(",", connection.brokers()));
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

    if (connection.clientId() != null) {
      props.put(ProducerConfig.CLIENT_ID_CONFIG, connection.clientId());
    }

    configureSecurityProtocol(props, connection);
    configureSsl(props, connection.ssl());
    configureSasl(props, connection.sasl());

    if (options != null) {
      configureOptions(props, options);
      configureRetry(props, options);
    }

    return props;
  }

  @Override
  public void flushAll() {
    producers.values().forEach(KafkaProducer::flush);
  }

  @Override
  @PreDestroy
  public void closeAll() {
    var snapshot = new ArrayList<>(producers.values());
    producers.clear();
    snapshot.forEach(producer -> {
      try {
        producer.close();
      } catch (Exception e) {
        log.warn("Error closing Kafka producer: {}", e.getMessage());
      }
    });
  }

  /**
   * Sets the {@code security.protocol} property based on SSL and SASL flags.
   *
   * @param props      the properties to populate.
   * @param connection the connection containing SSL and SASL configuration.
   */
  private void configureSecurityProtocol(Properties props, KafkaConnection connection) {
    boolean sslEnabled = connection.ssl() != null
        && Boolean.TRUE.equals(connection.ssl().enabled());
    boolean saslEnabled = connection.sasl() != null
        && connection.sasl().mechanism() != null;

    if (sslEnabled && saslEnabled) {
      props.put("security.protocol", "SASL_SSL");
    } else if (sslEnabled) {
      props.put("security.protocol", "SSL");
    } else if (saslEnabled) {
      props.put("security.protocol", "SASL_PLAINTEXT");
    } else {
      props.put("security.protocol", "PLAINTEXT");
    }
  }

  /**
   * Adds SSL truststore and keystore properties when SSL is enabled.
   *
   * @param props the properties to populate.
   * @param ssl   the SSL configuration, or {@code null} if SSL is not used.
   */
  private void configureSsl(Properties props, KafkaSslConfig ssl) {
    if (ssl == null || !Boolean.TRUE.equals(ssl.enabled())) {
      return;
    }
    if (ssl.truststoreLocation() != null) {
      props.put("ssl.truststore.location", ssl.truststoreLocation());
    }
    if (ssl.truststorePassword() != null) {
      props.put("ssl.truststore.password", ssl.truststorePassword());
    }
    if (ssl.keystoreLocation() != null) {
      props.put("ssl.keystore.location", ssl.keystoreLocation());
    }
    if (ssl.keystorePassword() != null) {
      props.put("ssl.keystore.password", ssl.keystorePassword());
    }
  }

  /**
   * Configures SASL mechanism and JAAS login module.
   *
   * <p>Supports {@code PLAIN}, {@code SCRAM-SHA-256}, and {@code SCRAM-SHA-512}.
   * Escapes double quotes in username and password to prevent JAAS config injection.
   *
   * @param props the properties to populate.
   * @param sasl  the SASL configuration, or {@code null} if SASL is not used.
   * @throws IllegalArgumentException if the mechanism is unsupported.
   */
  private void configureSasl(Properties props, KafkaSaslConfig sasl) {
    if (sasl == null || sasl.mechanism() == null) {
      return;
    }

    String mechanism = sasl.mechanism().toUpperCase();
    props.put("sasl.mechanism", mechanism);

    String loginModule;
    if ("PLAIN".equals(mechanism)) {
      loginModule = "org.apache.kafka.common.security.plain.PlainLoginModule";
    } else if (mechanism.startsWith("SCRAM")) {
      loginModule = "org.apache.kafka.common.security.scram.ScramLoginModule";
    } else {
      throw new IllegalArgumentException(
          "Unsupported SASL mechanism: " + mechanism
              + ". Supported: PLAIN, SCRAM-SHA-256, SCRAM-SHA-512.");
    }

    String username = (sasl.username() != null ? sasl.username() : "")
        .replace("\\", "\\\\").replace("\"", "\\\"");
    String password = (sasl.password() != null ? sasl.password() : "")
        .replace("\\", "\\\\").replace("\"", "\\\"");

    String jaasConfig = String.format(
        "%s required username=\"%s\" password=\"%s\";",
        loginModule, username, password);
    props.put("sasl.jaas.config", jaasConfig);
  }

  /**
   * Applies compression and acknowledgment settings from the options.
   *
   * @param props   the properties to populate.
   * @param options the advanced Kafka options.
   */
  private void configureOptions(Properties props, KafkaOptions options) {
    if (options.compression() != null) {
      props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, options.compression());
    }
    if (options.acks() != null) {
      props.put(ProducerConfig.ACKS_CONFIG, options.acks());
    }
  }

  /**
   * Maps retry and timeout options to native Kafka producer properties.
   *
   * <p>When not configured, Kafka uses its own defaults:
   * {@code retries = Integer.MAX_VALUE}, {@code delivery.timeout.ms = 120000},
   * {@code retry.backoff.ms = 100}.
   *
   * @param props   the properties to populate.
   * @param options the advanced Kafka options containing retry configuration.
   */
  private void configureRetry(Properties props, KafkaOptions options) {
    if (options.timeoutMs() != null) {
      props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, options.timeoutMs());
    }

    KafkaRetryConfig retry = options.retry();
    if (retry != null) {
      if (retry.attempts() != null) {
        props.put(ProducerConfig.RETRIES_CONFIG, retry.attempts());
      }
      if (retry.backoffMs() != null) {
        props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, retry.backoffMs());
      }
    }
  }

  /**
   * Builds a deterministic cache key from the connection and producer-level options.
   *
   * <p>Two configurations that produce the same key will share a {@link KafkaProducer} instance.
   *
   * @param connection the Kafka connection configuration.
   * @param options    the advanced options, or {@code null}.
   * @return a pipe-separated string used as cache key.
   */
  private String buildKey(KafkaConnection connection, KafkaOptions options) {
    String compression = options != null ? options.compression() : null;
    String acks = options != null ? options.acks() : null;

    return String.join("|",
        String.valueOf(connection.brokers()),
        String.valueOf(connection.clientId()),
        String.valueOf(connection.ssl()),
        String.valueOf(connection.sasl()),
        String.valueOf(compression),
        String.valueOf(acks));
  }
}
