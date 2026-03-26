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

package io.github.linagora.linid.im.dpp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.plugin.config.dto.ProviderConfiguration;
import io.github.linagora.linid.im.corelib.plugin.entity.DynamicEntity;
import io.github.linagora.linid.im.corelib.plugin.task.TaskExecutionContext;
import io.github.linagora.linid.im.dpp.registry.DslRegistry;
import io.github.linagora.linid.im.dpp.service.CrudServiceImpl;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DisplayName("Test class: DatabaseProviderPlugin with E2E")
class DatabaseProviderPluginE2ETest {
  private static DatabaseProviderPlugin provider;
  private static ProviderConfiguration providerConfiguration;
  private static DslRegistry dslRegistry;
  private Statement stmt;
  private Connection conn;
  private static String jdbcUrl = "jdbc:postgresql://localhost:5432/testdb";
  private static String user = "testuser";
  private static String password = "testpassword";

  @BeforeAll
  static void setupClass() throws SQLException {
    providerConfiguration = new ProviderConfiguration();
    providerConfiguration.setName("test-db-provider");
    providerConfiguration.addOption("url", jdbcUrl);
    providerConfiguration.addOption("username", user);
    providerConfiguration.addOption("password", password);
    dslRegistry = new DslRegistry();
    var crudService = new CrudServiceImpl(dslRegistry);
    provider = new DatabaseProviderPlugin(crudService);
  }

  @AfterAll
  static void teardownClass() throws SQLException {
    dslRegistry.shutdown();
  }

  @BeforeEach
  void setup() throws SQLException {
    conn = DriverManager.getConnection(jdbcUrl, user, password);
    stmt = conn.createStatement();
  }

  @AfterEach
  void teardown() throws SQLException {
    stmt.close();
    conn.close();
  }

  @Test
  @DisplayName("Test create: should insert user in table test_table_1")
  void testCreateTestTable1() throws IOException, SQLException {
    var context = new TaskExecutionContext();
    DynamicEntity entity = DynamicEntityHelper.getEntity("TestTable1.yml");
    Map<String, Object> attrs = Map.of(
            "userName", "testCreate",
            "userEmail", "test@example.com",
            "age", 30,
            "isValid", true);
    entity.setAttributes(attrs);
    var result = provider.create(context, providerConfiguration, entity);
    assertEquals(5, result.getAttributes().size());
    assertTrue(result.getAttributes().containsKey("id"));
    assertEquals("testCreate", result.getAttributes().get("userName"));
    assertEquals("test@example.com", result.getAttributes().get("userEmail"));
    assertEquals(30, result.getAttributes().get("age"));
    assertEquals(true, result.getAttributes().get("isValid"));
    DatabaseTestUtils.deleteOne(stmt, "test_table_1", "email", "test@example.com");
  }

  @Test
  @DisplayName("Test create: should insert user in table test_table_2")
  void testCreateTestTable2() throws IOException, SQLException {
    var context = new TaskExecutionContext();
    DynamicEntity entity = DynamicEntityHelper.getEntity("TestTable2.yml");
    Map<String, Object> attrs = Map.of(
            "userName", "testCreate",
            "userEmail", "test@example.com");
    entity.setAttributes(attrs);
    var result = provider.create(context, providerConfiguration, entity);
    assertEquals(3, result.getAttributes().size());
    assertTrue(result.getAttributes().containsKey("id"));
    assertEquals("testCreate", result.getAttributes().get("userName"));
    assertEquals("test@example.com", result.getAttributes().get("userEmail"));
    DatabaseTestUtils.deleteOne(stmt, "test_table_2", "email", "test@example.com");
  }

  @Test
  @DisplayName("Test create: should insert user in table test_table_3")
  void testCreateTestTable3() throws IOException, SQLException {
    var context = new TaskExecutionContext();
    DynamicEntity entity = DynamicEntityHelper.getEntity("TestTable3.yml");
    Map<String, Object> attrs = Map.of(
            "id", "id_4",
            "userName", "testCreate",
            "userEmail", "test@example.com");
    entity.setAttributes(attrs);
    var result = provider.create(context, providerConfiguration, entity);
    assertEquals(3, result.getAttributes().size());
    assertEquals("id_4", result.getAttributes().get("id"));
    assertEquals("testCreate", result.getAttributes().get("userName"));
    assertEquals("test@example.com", result.getAttributes().get("userEmail"));
    DatabaseTestUtils.deleteOne(stmt, "test_table_3", "email", "test@example.com");
  }

  @Test
  @DisplayName("Test create exceptions: test that creating a user with email"
      + "that already exists in table test_table_1 should throw ApiException")
  void testCreateExceptions() throws IOException {
    var context = new TaskExecutionContext();
    DynamicEntity entity = DynamicEntityHelper.getEntity("TestTable1.yml");
    Map<String, Object> attrs = Map.of(
            "userName", "testCreate",
            "userEmail", "alice.dupont@example.com",
            "age", 30,
            "isValid", true);
    entity.setAttributes(attrs);
    assertThrows(ApiException.class, () -> provider.create(context,
            providerConfiguration, entity));
  }

  @Test
  @DisplayName("Test delete: should delete user from database in table test_table_1")
  void testDeleteTestTable1() throws IOException, SQLException {
    var context = new TaskExecutionContext();
    var id = DatabaseTestUtils.insertOne(
            stmt,
            "test_table_1",
            new String[] { "name", "email", "age", "is_valid" },
            new String[] { "'testDelete1'", "'test@example.com'", "21", "true" });
    DynamicEntity entity = DynamicEntityHelper.getEntity("TestTable1.yml");
    Map<String, Object> attrs = Map.of(
            "id", id);
    entity.setAttributes(attrs);
    boolean deleted = provider.delete(context, providerConfiguration,
            String.valueOf(id), entity);
    assertEquals(true, deleted);
    var result = DatabaseTestUtils.fetchOne(stmt, "test_table_1", "email",
            "test@example.com");
    assertEquals(true, result.isEmpty());
  }

  @Test
  @DisplayName("Test delete: should delete user from database in table test_table_2")
  void testDeleteTestTable2() throws IOException, SQLException {
    var context = new TaskExecutionContext();
    var id = DatabaseTestUtils.insertOne(
            stmt,
            "test_table_2",
            new String[] { "name", "email" },
            new String[] { "'testDelete1'", "'test@example.com'" });
    DynamicEntity entity = DynamicEntityHelper.getEntity("TestTable2.yml");
    Map<String, Object> attrs = Map.of(
            "id", id);
    entity.setAttributes(attrs);
    boolean deleted = provider.delete(context, providerConfiguration,
            String.valueOf(id), entity);
    assertEquals(true, deleted);
    var result = DatabaseTestUtils.fetchOne(stmt, "test_table_2", "email",
            "test@example.com");
    assertEquals(true, result.isEmpty());
  }

  @Test
  @DisplayName("Test delete: should delete user from database in table test_table_3")
  void testDeleteTestTable3() throws IOException, SQLException {
    var context = new TaskExecutionContext();
    var id = DatabaseTestUtils.insertOne(
            stmt,
            "test_table_3",
            new String[] { "id", "name", "email" },
            new String[] { "'id_4'", "'testDelete1'", "'test@example.com'" });
    DynamicEntity entity = DynamicEntityHelper.getEntity("TestTable3.yml");
    Map<String, Object> attrs = Map.of(
            "id", id);
    entity.setAttributes(attrs);
    boolean deleted = provider.delete(context, providerConfiguration,
            String.valueOf(id), entity);
    assertEquals(true, deleted);
    var result = DatabaseTestUtils.fetchOne(stmt, "test_table_3", "email",
            "test@example.com");
    assertEquals(true, result.isEmpty());
  }

  @Test
  @DisplayName("Test delete throw ApiException when id not exist")
  void testDeleteNonExisting() throws IOException {
    var context = new TaskExecutionContext();
    DynamicEntity entity = DynamicEntityHelper.getEntity("TestTable1.yml");
    assertThrows(ApiException.class, () -> provider.delete(context,
            providerConfiguration, "999999", entity));
  }

  @Test
  @DisplayName("Test update: should update user in table test_table_1")
  void testUpdateTestTable1() throws IOException, SQLException {
    var context = new TaskExecutionContext();
    var id = DatabaseTestUtils.insertOne(
            stmt,
            "test_table_1",
            new String[] { "name", "email", "age", "is_valid" },
            new String[] { "'test'", "'test@example.com'", "21", "true" });
    DynamicEntity entity = DynamicEntityHelper.getEntity("TestTable1.yml");
    Map<String, Object> attrs = Map.of(
            "userName", "testUpdated",
            "userEmail", "test-updated@example.com",
            "age", 42,
            "isValid", false);
    entity.setAttributes(attrs);
    provider.update(context, providerConfiguration, String.valueOf(id), entity);
    var result = DatabaseTestUtils.fetchOne(stmt, "test_table_1", "email",
            "test-updated@example.com");
    assertEquals("testUpdated", result.get("name"));
    assertEquals("test-updated@example.com", result.get("email"));
    assertEquals(42, result.get("age"));
    assertEquals(false, result.get("is_valid"));
    DatabaseTestUtils.deleteOne(stmt, "test_table_1", "email",
            "test-updated@example.com");
  }

  @Test
  @DisplayName("Test update: should update user in table test_table_2")
  void testUpdateTestTable2() throws IOException, SQLException {
    var context = new TaskExecutionContext();
    var id = DatabaseTestUtils.insertOne(
            stmt,
            "test_table_2",
            new String[] { "name", "email" },
            new String[] { "'test'", "'test@example.com'" });
    DynamicEntity entity = DynamicEntityHelper.getEntity("TestTable2.yml");
    Map<String, Object> attrs = Map.of(
            "userName", "testUpdated",
            "userEmail", "test-updated@example.com");
    entity.setAttributes(attrs);
    provider.update(context, providerConfiguration, String.valueOf(id), entity);
    var result = DatabaseTestUtils.fetchOne(stmt, "test_table_2", "email",
            "test-updated@example.com");
    assertEquals("testUpdated", result.get("name"));
    assertEquals("test-updated@example.com", result.get("email"));
    DatabaseTestUtils.deleteOne(stmt, "test_table_2", "email",
            "test-updated@example.com");
  }

  @Test
  @DisplayName("Test update: should update user in table test_table_3")
  void testUpdateTestTable3() throws IOException, SQLException {
    var context = new TaskExecutionContext();
    var id = DatabaseTestUtils.insertOne(
            stmt,
            "test_table_3",
            new String[] { "id", "name", "email" },
            new String[] { "'id_4'", "'test'", "'test@example.com'" });
    DynamicEntity entity = DynamicEntityHelper.getEntity("TestTable3.yml");
    Map<String, Object> attrs = Map.of(
            "id", "id_4",
            "userName", "testUpdated",
            "userEmail", "test-updated@example.com");
    entity.setAttributes(attrs);
    provider.update(context, providerConfiguration, String.valueOf(id), entity);
    var result = DatabaseTestUtils.fetchOne(stmt, "test_table_3", "email",
            "test-updated@example.com");
    assertEquals("testUpdated", result.get("name"));
    assertEquals("test-updated@example.com", result.get("email"));
    DatabaseTestUtils.deleteOne(stmt, "test_table_3", "email",
            "test-updated@example.com");
  }

  @Test
  @DisplayName("Test update throw ApiException when id not exist")
  void testUpdateNonExisting() throws IOException {
    var context = new TaskExecutionContext();
    DynamicEntity entity = DynamicEntityHelper.getEntity("TestTable1.yml");
    Map<String, Object> attrs = Map.of("id", "999999");
    entity.setAttributes(attrs);
    assertThrows(ApiException.class, () -> provider.update(context,
            providerConfiguration, "999999", entity));
  }

  @Test
  @DisplayName("Test findAll: should return all users from table test_table_1")
  void testFindAllTestTable1() throws IOException {
    var context = new TaskExecutionContext();
    DynamicEntity entity = DynamicEntityHelper.getEntity("TestTable1.yml");
    Page<DynamicEntity> result = provider.findAll(context, providerConfiguration,
            null, PageRequest.of(0, 10), entity);
    assertEquals(3, result.getTotalElements());
    List<DynamicEntity> users = result.getContent();
    assertEquals("Alice Dupont", users.get(0).getAttributes().get("userName"));
    assertEquals("alice.dupont@example.com",
            users.get(0).getAttributes().get("userEmail"));
    assertEquals(32, users.get(0).getAttributes().get("age"));
    assertEquals(true, users.get(0).getAttributes().get("isValid"));
    assertEquals("Bob Martin", users.get(1).getAttributes().get("userName"));
    assertEquals("bob.martin@example.com",
            users.get(1).getAttributes().get("userEmail"));
    assertEquals(18, users.get(1).getAttributes().get("age"));
    assertEquals(false, users.get(1).getAttributes().get("isValid"));
    assertEquals("Charlie Bernard",
            users.get(2).getAttributes().get("userName"));
    assertEquals("charlie.bernard@example.com",
            users.get(2).getAttributes().get("userEmail"));
    assertEquals(54, users.get(2).getAttributes().get("age"));
    assertEquals(true, users.get(2).getAttributes().get("isValid"));
  }

  @Test
  @DisplayName("Test findAll: should return all users from table test_table_2")
  void testFindAllTestTable2() throws IOException {
    var context = new TaskExecutionContext();
    DynamicEntity entity = DynamicEntityHelper.getEntity("TestTable2.yml");
    Page<DynamicEntity> result = provider.findAll(context, providerConfiguration,
            null, PageRequest.of(0, 10), entity);
    assertEquals(3, result.getTotalElements());
    List<DynamicEntity> users = result.getContent();
    assertEquals("Alice Dupont", users.get(0).getAttributes().get("userName"));
    assertEquals("alice.dupont@example.com",
            users.get(0).getAttributes().get("userEmail"));
    assertEquals("Bob Martin", users.get(1).getAttributes().get("userName"));
    assertEquals("bob.martin@example.com",
            users.get(1).getAttributes().get("userEmail"));
    assertEquals("Charlie Bernard",
            users.get(2).getAttributes().get("userName"));
    assertEquals("charlie.bernard@example.com",
            users.get(2).getAttributes().get("userEmail"));
  }

  @Test
  @DisplayName("Test findAll: should return all users from table test_table_3")
  void testFindAllTestTable3() throws IOException {
    var context = new TaskExecutionContext();
    DynamicEntity entity = DynamicEntityHelper.getEntity("TestTable3.yml");
    Page<DynamicEntity> result = provider.findAll(context, providerConfiguration,
            null, PageRequest.of(0, 10), entity);
    assertEquals(3, result.getTotalElements());
    List<DynamicEntity> users = result.getContent();
    assertEquals("Alice Dupont", users.get(0).getAttributes().get("userName"));
    assertEquals("alice.dupont@example.com",
            users.get(0).getAttributes().get("userEmail"));
    assertEquals("Bob Martin", users.get(1).getAttributes().get("userName"));
    assertEquals("bob.martin@example.com",
            users.get(1).getAttributes().get("userEmail"));
    assertEquals("Charlie Bernard",
            users.get(2).getAttributes().get("userName"));
    assertEquals("charlie.bernard@example.com",
            users.get(2).getAttributes().get("userEmail"));
  }

  @Test
  @DisplayName("Test patch: should patch user in table test_table_1")
  void testPatchTestTable1() throws IOException, SQLException {
    var context = new TaskExecutionContext();
    var id = DatabaseTestUtils.insertOne(
            stmt,
            "test_table_1",
            new String[] { "name", "email", "age", "is_valid" },
            new String[] { "'test'", "'test@example.com'", "21", "true" });
    DynamicEntity entity = DynamicEntityHelper.getEntity("TestTable1.yml");
    Map<String, Object> attrs = Map.of("userName", "testUpdated");
    entity.setAttributes(attrs);
    provider.patch(context, providerConfiguration, String.valueOf(id), entity);
    var result = DatabaseTestUtils.fetchOne(stmt, "test_table_1", "name",
            "testUpdated");
    assertEquals("testUpdated", result.get("name"));
    assertEquals("test@example.com", result.get("email"));
    assertEquals(21, result.get("age"));
    assertEquals(true, result.get("is_valid"));
    attrs = Map.of("age", 32);
    entity.setAttributes(attrs);
    provider.patch(context, providerConfiguration, String.valueOf(id), entity);
    result = DatabaseTestUtils.fetchOne(stmt, "test_table_1", "name",
            "testUpdated");
    assertEquals("testUpdated", result.get("name"));
    assertEquals("test@example.com", result.get("email"));
    assertEquals(32, result.get("age"));
    assertEquals(true, result.get("is_valid"));
    attrs = Map.of("isValid", false);
    entity.setAttributes(attrs);
    provider.patch(context, providerConfiguration, String.valueOf(id), entity);
    result = DatabaseTestUtils.fetchOne(stmt, "test_table_1", "name",
            "testUpdated");
    assertEquals("testUpdated", result.get("name"));
    assertEquals("test@example.com", result.get("email"));
    assertEquals(32, result.get("age"));
    assertEquals(false, result.get("is_valid"));
    DatabaseTestUtils.deleteOne(stmt, "test_table_1", "email",
            "test@example.com");
  }

  @Test
  @DisplayName("Test patch: should patch user in table test_table_2")
  void testPatchTestTable2() throws IOException, SQLException {
    var context = new TaskExecutionContext();
    var id = DatabaseTestUtils.insertOne(
            stmt,
            "test_table_2",
            new String[] { "name", "email" },
            new String[] { "'test'", "'test@example.com'" });
    DynamicEntity entity = DynamicEntityHelper.getEntity("TestTable2.yml");
    Map<String, Object> attrs = Map.of("userName", "testUpdated");
    entity.setAttributes(attrs);
    provider.patch(context, providerConfiguration, String.valueOf(id), entity);
    var result = DatabaseTestUtils.fetchOne(stmt, "test_table_2", "name",
            "testUpdated");
    assertEquals("testUpdated", result.get("name"));
    assertEquals("test@example.com", result.get("email"));
    DatabaseTestUtils.deleteOne(stmt, "test_table_2", "email",
            "test@example.com");
  }

  @Test
  @DisplayName("Test patch: should patch user in table test_table_3")
  void testPatchTestTable3() throws IOException, SQLException {
    var context = new TaskExecutionContext();
    var id = DatabaseTestUtils.insertOne(
            stmt,
            "test_table_3",
            new String[] { "id", "name", "email" },
            new String[] { "'id_4'", "'test'", "'test@example.com'" });
    DynamicEntity entity = DynamicEntityHelper.getEntity("TestTable3.yml");
    Map<String, Object> attrs = Map.of("userName", "testUpdated");
    entity.setAttributes(attrs);
    provider.patch(context, providerConfiguration, String.valueOf(id), entity);
    var result = DatabaseTestUtils.fetchOne(stmt, "test_table_3", "name",
            "testUpdated");
    assertEquals("testUpdated", result.get("name"));
    assertEquals("test@example.com", result.get("email"));
    DatabaseTestUtils.deleteOne(stmt, "test_table_3", "email",
            "test@example.com");
  }

  @Test
  @DisplayName("Test patch throw ApiException when id not exist")
  void testPatchNonExisting() throws IOException {
    var context = new TaskExecutionContext();
    DynamicEntity entity = DynamicEntityHelper.getEntity("TestTable1.yml");
    Map<String, Object> attrs = Map.of("id", "999999");
    entity.setAttributes(attrs);
    assertThrows(ApiException.class, () -> provider.patch(context,
            providerConfiguration, "999999", entity));
  }

  @Test
  @DisplayName("Test findById: should return user for table test_table_1")
  void testFindByIdTestTable1() throws IOException {
    var context = new TaskExecutionContext();
    DynamicEntity entity = DynamicEntityHelper.getEntity("TestTable1.yml");
    DynamicEntity user = provider.findById(context, providerConfiguration, "1",
            entity);
    assertEquals("Alice Dupont", user.getAttributes().get("userName"));
    assertEquals("alice.dupont@example.com",
            user.getAttributes().get("userEmail"));
    assertEquals(32, user.getAttributes().get("age"));
    assertEquals(true, user.getAttributes().get("isValid"));
  }

  @Test
  @DisplayName("Test findById: should return user for table test_table_2")
  void testFindByIdTestTable2() throws IOException, SQLException {
    var context = new TaskExecutionContext();
    DynamicEntity entity = DynamicEntityHelper.getEntity("TestTable2.yml");
    var result = DatabaseTestUtils.fetchOne(stmt, "test_table_2", "name",
            "Alice Dupont");
    DynamicEntity user = provider.findById(context, providerConfiguration,
            result.get("id").toString(),
            entity);
    assertEquals("Alice Dupont", user.getAttributes().get("userName"));
    assertEquals("alice.dupont@example.com",
            user.getAttributes().get("userEmail"));
  }

  @Test
  @DisplayName("Test findById: should return user for table test_table_3")
  void testFindByIdTestTable3() throws IOException {
    var context = new TaskExecutionContext();
    DynamicEntity entity = DynamicEntityHelper.getEntity("TestTable3.yml");
    DynamicEntity user = provider.findById(context, providerConfiguration,
            "id_1", entity);
    assertEquals("Alice Dupont", user.getAttributes().get("userName"));
    assertEquals("alice.dupont@example.com",
            user.getAttributes().get("userEmail"));
  }

  @Test
  @DisplayName("Test findById throw ApiException when id not exist")
  void testFindByIdNonExisting() throws IOException {
    var context = new TaskExecutionContext();
    DynamicEntity entity = DynamicEntityHelper.getEntity("TestTable1.yml");
    Map<String, Object> attrs = Map.of("id", "999999");
    entity.setAttributes(attrs);
    assertThrows(ApiException.class, () -> provider.findById(context,
            providerConfiguration, "999999", entity));
  }

}
