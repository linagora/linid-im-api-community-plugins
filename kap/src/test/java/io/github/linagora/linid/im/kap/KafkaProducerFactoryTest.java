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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.kap.model.KafkaConnection;
import io.github.linagora.linid.im.kap.model.KafkaOptions;
import io.github.linagora.linid.im.kap.model.KafkaRetryConfig;
import io.github.linagora.linid.im.kap.model.KafkaSaslConfig;
import io.github.linagora.linid.im.kap.model.KafkaSslConfig;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("Test class: KafkaProducerFactoryImpl")
class KafkaProducerFactoryTest {

  private final KafkaProducerFactoryImpl factory = new KafkaProducerFactoryImpl();

  @Test
  @DisplayName("test buildProperties: should set basic connection properties")
  void testBuildPropertiesBasic() {
    var connection = new KafkaConnection(
        List.of("kafka1:9092", "kafka2:9092"),
        "test-client",
        null,
        null);

    Properties props = factory.buildProperties(connection, null);

    assertEquals("kafka1:9092,kafka2:9092",
        props.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));
    assertEquals("test-client",
        props.get(ProducerConfig.CLIENT_ID_CONFIG));
    assertEquals(StringSerializer.class.getName(),
        props.get(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG));
    assertEquals(StringSerializer.class.getName(),
        props.get(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG));
    assertEquals("PLAINTEXT", props.get("security.protocol"));
  }

  @Test
  @DisplayName("test buildProperties: should set SSL properties when SSL is enabled")
  void testBuildPropertiesWithSsl() {
    var ssl = new KafkaSslConfig(true,
        "/path/truststore.jks", "trustpass",
        "/path/keystore.jks", "keypass");
    var connection = new KafkaConnection(
        List.of("kafka:9093"), "ssl-client", ssl, null);

    Properties props = factory.buildProperties(connection, null);

    assertEquals("SSL", props.get("security.protocol"));
    assertEquals("/path/truststore.jks", props.get("ssl.truststore.location"));
    assertEquals("trustpass", props.get("ssl.truststore.password"));
    assertEquals("/path/keystore.jks", props.get("ssl.keystore.location"));
    assertEquals("keypass", props.get("ssl.keystore.password"));
  }

  @Test
  @DisplayName("test buildProperties: should set SASL PLAIN properties")
  void testBuildPropertiesWithSaslPlain() {
    var sasl = new KafkaSaslConfig("plain", "user", "pass");
    var connection = new KafkaConnection(
        List.of("kafka:9092"), "sasl-client", null, sasl);

    Properties props = factory.buildProperties(connection, null);

    assertEquals("SASL_PLAINTEXT", props.get("security.protocol"));
    assertEquals("PLAIN", props.get("sasl.mechanism"));
    assertTrue(props.get("sasl.jaas.config").toString()
        .contains("PlainLoginModule"));
    assertTrue(props.get("sasl.jaas.config").toString()
        .contains("username=\"user\""));
    assertTrue(props.get("sasl.jaas.config").toString()
        .contains("password=\"pass\""));
  }

  @Test
  @DisplayName("test buildProperties: should set SASL_SSL when both SSL and SASL are enabled")
  void testBuildPropertiesWithSslAndSasl() {
    var ssl = new KafkaSslConfig(true, null, null, null, null);
    var sasl = new KafkaSaslConfig("scram-sha-256", "user", "pass");
    var connection = new KafkaConnection(
        List.of("kafka:9093"), "sasl-ssl-client", ssl, sasl);

    Properties props = factory.buildProperties(connection, null);

    assertEquals("SASL_SSL", props.get("security.protocol"));
    assertEquals("SCRAM-SHA-256", props.get("sasl.mechanism"));
    assertTrue(props.get("sasl.jaas.config").toString()
        .contains("ScramLoginModule"));
  }

  @Test
  @DisplayName("test buildProperties: should set compression and acks from options")
  void testBuildPropertiesWithOptions() {
    var connection = new KafkaConnection(
        List.of("kafka:9092"), null, null, null);
    var options = new KafkaOptions(null, "gzip", "all", null, null, null, null);

    Properties props = factory.buildProperties(connection, options);

    assertEquals("gzip", props.get(ProducerConfig.COMPRESSION_TYPE_CONFIG));
    assertEquals("all", props.get(ProducerConfig.ACKS_CONFIG));
  }

  @Test
  @DisplayName("test buildProperties: should not set clientId when null")
  void testBuildPropertiesNullClientId() {
    var connection = new KafkaConnection(
        List.of("kafka:9092"), null, null, null);

    Properties props = factory.buildProperties(connection, null);

    assertNull(props.get(ProducerConfig.CLIENT_ID_CONFIG));
  }

  @Test
  @DisplayName("test buildProperties: should not set SSL properties when disabled")
  void testBuildPropertiesSslDisabled() {
    var ssl = new KafkaSslConfig(false,
        "/path/truststore.jks", "trustpass", null, null);
    var connection = new KafkaConnection(
        List.of("kafka:9092"), null, ssl, null);

    Properties props = factory.buildProperties(connection, null);

    assertEquals("PLAINTEXT", props.get("security.protocol"));
    assertNull(props.get("ssl.truststore.location"));
  }

  @Test
  @DisplayName("test buildProperties: should throw on unsupported SASL mechanism")
  void testBuildPropertiesUnsupportedSaslMechanism() {
    var sasl = new KafkaSaslConfig("GSSAPI", "user", "pass");
    var connection = new KafkaConnection(
        List.of("kafka:9092"), null, null, sasl);

    assertThrows(IllegalArgumentException.class,
        () -> factory.buildProperties(connection, null));
  }

  @SuppressWarnings("unchecked")
  @Test
  @DisplayName("test getOrCreate: should return same producer for same connection config")
  void testGetOrCreateCachesSameConfig() {
    var connection = new KafkaConnection(
        List.of("localhost:9092"), "client-1", null, null);
    var options = new KafkaOptions(null, "gzip", "all", null, null, null, null);

    var factory1 = Mockito.spy(new KafkaProducerFactoryImpl());
    KafkaProducer<String, String> mockProducer = Mockito.mock(KafkaProducer.class);
    Mockito.doReturn(mockProducer).when(factory1).createProducer(Mockito.any(), Mockito.any());

    var producer1 = factory1.getOrCreate(connection, options);
    var producer2 = factory1.getOrCreate(connection, options);

    assertSame(producer1, producer2);
    Mockito.verify(factory1, Mockito.times(1)).createProducer(Mockito.any(), Mockito.any());
  }

  @SuppressWarnings("unchecked")
  @Test
  @DisplayName("test getOrCreate: should return different producers for different configs")
  void testGetOrCreateDifferentConfigs() {
    var connection1 = new KafkaConnection(
        List.of("kafka1:9092"), "client-1", null, null);
    var connection2 = new KafkaConnection(
        List.of("kafka2:9092"), "client-2", null, null);

    var factory1 = Mockito.spy(new KafkaProducerFactoryImpl());
    KafkaProducer<String, String> mock1 = Mockito.mock(KafkaProducer.class);
    KafkaProducer<String, String> mock2 = Mockito.mock(KafkaProducer.class);
    Mockito.doReturn(mock1).doReturn(mock2)
        .when(factory1).createProducer(Mockito.any(), Mockito.any());

    var producer1 = factory1.getOrCreate(connection1, null);
    var producer2 = factory1.getOrCreate(connection2, null);

    assertNotSame(producer1, producer2);
    Mockito.verify(factory1, Mockito.times(2)).createProducer(Mockito.any(), Mockito.any());
  }

  // --- null-safety ---

  @Test
  @DisplayName("test buildProperties: should throw when brokers is null")
  void testBuildPropertiesNullBrokers() {
    var connection = new KafkaConnection(null, null, null, null);

    assertThrows(ApiException.class,
        () -> factory.buildProperties(connection, null));
  }

  @Test
  @DisplayName("test buildProperties: should throw when brokers is empty")
  void testBuildPropertiesEmptyBrokers() {
    var connection = new KafkaConnection(Collections.emptyList(), null, null, null);

    assertThrows(ApiException.class,
        () -> factory.buildProperties(connection, null));
  }

  // --- JAAS quote escaping ---

  @Test
  @DisplayName("test buildProperties: should escape double quotes in SASL username and password")
  void testBuildPropertiesSaslEscapeQuotes() {
    var sasl = new KafkaSaslConfig("plain", "user\"name", "pass\"word");
    var connection = new KafkaConnection(
        List.of("kafka:9092"), null, null, sasl);

    Properties props = factory.buildProperties(connection, null);

    String jaasConfig = props.get("sasl.jaas.config").toString();
    assertTrue(jaasConfig.contains("username=\"user\\\"name\""));
    assertTrue(jaasConfig.contains("password=\"pass\\\"word\""));
  }

  @Test
  @DisplayName("test buildProperties: should escape backslashes before quotes in SASL credentials")
  void testBuildPropertiesSaslEscapeBackslashesAndQuotes() {
    var sasl = new KafkaSaslConfig("plain", "pass\\\"", "back\\slash");
    var connection = new KafkaConnection(
        List.of("kafka:9092"), null, null, sasl);

    Properties props = factory.buildProperties(connection, null);

    String jaasConfig = props.get("sasl.jaas.config").toString();
    assertTrue(jaasConfig.contains("username=\"pass\\\\\\\"\""));
    assertTrue(jaasConfig.contains("password=\"back\\\\slash\""));
  }

  // --- retry props mapping ---

  @Test
  @DisplayName("test buildProperties: should map retry options to Kafka producer properties")
  void testBuildPropertiesRetryMapping() {
    var connection = new KafkaConnection(
        List.of("kafka:9092"), null, null, null);
    var options = new KafkaOptions(null, null, null, null, 5000,
        new KafkaRetryConfig(10, 500), null);

    Properties props = factory.buildProperties(connection, options);

    assertEquals(5000, props.get(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG));
    assertEquals(10, props.get(ProducerConfig.RETRIES_CONFIG));
    assertEquals(500, props.get(ProducerConfig.RETRY_BACKOFF_MS_CONFIG));
  }

  @Test
  @DisplayName("test buildProperties: should not set retry props when options are null")
  void testBuildPropertiesNoRetryProps() {
    var connection = new KafkaConnection(
        List.of("kafka:9092"), null, null, null);

    Properties props = factory.buildProperties(connection, null);

    assertNull(props.get(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG));
    assertNull(props.get(ProducerConfig.RETRIES_CONFIG));
    assertNull(props.get(ProducerConfig.RETRY_BACKOFF_MS_CONFIG));
  }

  // --- closeAll ---

  @SuppressWarnings("unchecked")
  @Test
  @DisplayName("test closeAll: should close all cached producers")
  void testCloseAll() {
    var connection = new KafkaConnection(
        List.of("localhost:9092"), "client-1", null, null);

    var spyFactory = Mockito.spy(new KafkaProducerFactoryImpl());
    KafkaProducer<String, String> mockProducer = Mockito.mock(KafkaProducer.class);
    Mockito.doReturn(mockProducer).when(spyFactory).createProducer(Mockito.any(), Mockito.any());

    spyFactory.getOrCreate(connection, null);
    spyFactory.closeAll();

    Mockito.verify(mockProducer).close();
  }
}
