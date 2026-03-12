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

import io.github.linagora.linid.im.corelib.plugin.config.dto.AttributeConfiguration;
import io.github.linagora.linid.im.corelib.plugin.config.dto.EntityConfiguration;
import io.github.linagora.linid.im.corelib.plugin.config.dto.ProviderConfiguration;
import io.github.linagora.linid.im.corelib.plugin.entity.DynamicEntity;
import io.github.linagora.linid.im.corelib.plugin.task.TaskEngine;
import io.github.linagora.linid.im.corelib.plugin.task.TaskExecutionContext;
import io.github.linagora.linid.im.dpp.registry.DslRegistry;
import io.github.linagora.linid.im.dpp.service.CrudServiceImpl;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Test class: DatabaseProviderPlugin with E2E")
class DatabaseProviderPluginE2ETest {

  private DatabaseProviderPlugin provider;
  private ProviderConfiguration providerConfiguration;

  private String jdbcUrl = "jdbc:postgresql://localhost:5433/testdb";
  private String user = "testuser";
  private String password = "testpassword";

  @BeforeEach
  void setup() {
    providerConfiguration = new ProviderConfiguration();
    providerConfiguration.setName("test-db-provider");
    providerConfiguration.addOption("url", jdbcUrl);
    providerConfiguration.addOption("username", user);
    providerConfiguration.addOption("password", password);

    // Initialize provider with dependencies
    var dslRegistry = new DslRegistry();
    var crudService = new CrudServiceImpl(dslRegistry);
    var taskEngine = new TaskEngineTest();
    provider = new DatabaseProviderPlugin(crudService, taskEngine);
  }

  @Test
  @DisplayName("Test create: should insert user in database")
  void testCreate() {
    try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password)) {
      Statement stmt = conn.createStatement();

      // Verify that testCreate does not exist
      String sqlQuery = "SELECT 1 FROM users WHERE name = 'testCreate' AND email = 'test@example.com' LIMIT 1";
      ResultSet rs = stmt.executeQuery(sqlQuery);

      boolean testCreateExistBefore = rs.next();
      assertEquals(false, testCreateExistBefore);

      var entity = new DynamicEntity();
      var context = new TaskExecutionContext();
      var entityConfiguration = new EntityConfiguration();
      var attributes = new HashMap<String, Object>();
      var access = new HashMap<String, Object>();

      // Set access rights to the users table
      var createAccess = new HashMap<String, Object>();
      createAccess.put("table", "users");
      access.put("create", createAccess);
      entityConfiguration.setAccess(access);

      // Create attribute configuration (id, name and email)
      List<AttributeConfiguration> attributeConfigs = createUserAttributeConfigurations();

      entityConfiguration.setAttributes(attributeConfigs);

      attributes.put("userName", "testCreate");
      attributes.put("userEmail", "test@example.com");

      entity.setConfiguration(entityConfiguration);
      entity.setAttributes(attributes);

      // Call create function
      provider.create(context, providerConfiguration, entity);

      // Verify if row was created
      ResultSet rsAfter = stmt.executeQuery(sqlQuery);
      boolean testCreateExistAfter = rsAfter.next();
      assertEquals(true, testCreateExistAfter);

      // Delete created user
      String sqlQueryToDelete = "DELETE FROM users where email = 'test@example.com'";
      stmt.executeUpdate(sqlQueryToDelete);

    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  @Test
  @DisplayName("Test delete: should delete user from database")
  void testDelete() {
    try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password)) {
      Statement stmt = conn.createStatement();

      // Clean up existing data before test
      String cleanupSql = "DELETE FROM users WHERE email = 'testdelete@example.com'";
      stmt.executeUpdate(cleanupSql);

      // Add a row to the database and retrieve the ID
      String insertSql = "INSERT INTO users (name, email) VALUES ('testDelete', 'testdelete@example.com') RETURNING id";
      ResultSet rsInsert = stmt.executeQuery(insertSql);
      rsInsert.next();
      int insertedId = rsInsert.getInt("id");

      // Verify that the row exists
      String sqlQuery = "SELECT 1 FROM users WHERE id = " + insertedId + " LIMIT 1";
      ResultSet rsBefore = stmt.executeQuery(sqlQuery);
      boolean existsBefore = rsBefore.next();
      assertEquals(true, existsBefore);

      // Prepare entity for delete
      var entity = new DynamicEntity();
      var context = new TaskExecutionContext();
      var entityConfiguration = new EntityConfiguration();
      var access = new HashMap<String, Object>();

      // Configure access to the users table
      var deleteAccess = new HashMap<String, Object>();
      deleteAccess.put("table", "users");
      access.put("delete", deleteAccess);
      entityConfiguration.setAccess(access);

      // Configure attributes (id, name, email)
      List<AttributeConfiguration> attributeConfigs = createUserAttributeConfigurations();

      entityConfiguration.setAttributes(attributeConfigs);
      entity.setConfiguration(entityConfiguration);

      // Call delete function
      boolean deleteResult = provider.delete(context, providerConfiguration, String.valueOf(insertedId), entity);
      assertEquals(true, deleteResult);

      // Verify that the row no longer exists
      ResultSet rsAfter = stmt.executeQuery(sqlQuery);
      boolean existsAfter = rsAfter.next();
      assertEquals(false, existsAfter);

    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  @Test
  @DisplayName("Test update: should update user in database")
  void testUpdate() {
    try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password)) {
      Statement stmt = conn.createStatement();

      // Add a row to the database and retrieve the ID
      String insertSql = "INSERT INTO users (name, email) VALUES ('test', 'test@example.com') RETURNING id";
      ResultSet rsInsert = stmt.executeQuery(insertSql);
      rsInsert.next();
      int insertedId = rsInsert.getInt("id");

      // Verify that the row exists with initial values
      String sqlQueryBefore = "SELECT name, email FROM users WHERE id = " + insertedId;
      ResultSet rsBefore = stmt.executeQuery(sqlQueryBefore);
      rsBefore.next();
      assertEquals("test", rsBefore.getString("name"));
      assertEquals("test@example.com", rsBefore.getString("email"));

      // Init entity
      var entity = new DynamicEntity();
      var context = new TaskExecutionContext();
      var entityConfiguration = new EntityConfiguration();
      var attributes = new HashMap<String, Object>();
      var access = new HashMap<String, Object>();

      // Configure access to the users table
      var updateAccess = new HashMap<String, Object>();
      updateAccess.put("table", "users");
      access.put("update", updateAccess);
      entityConfiguration.setAccess(access);

      // Configure attributes (id, name, email)
      List<AttributeConfiguration> attributeConfigs = createUserAttributeConfigurations();

      entityConfiguration.setAttributes(attributeConfigs);

      // Set new values
      attributes.put("userName", "testUpdated");
      attributes.put("userEmail", "testupdated@example.com");

      entity.setConfiguration(entityConfiguration);
      entity.setAttributes(attributes);

      // Call update function
      DynamicEntity updateResult = provider.update(context, providerConfiguration, String.valueOf(insertedId), entity);
      assertEquals(entity, updateResult);

      // Verify that the row was successfully updated
      String sqlQueryAfter = "SELECT name, email FROM users WHERE id = " + insertedId;
      ResultSet rsAfter = stmt.executeQuery(sqlQueryAfter);
      rsAfter.next();
      assertEquals("testUpdated", rsAfter.getString("name"));
      assertEquals("testupdated@example.com", rsAfter.getString("email"));

      // Delete the row created for the test
      String sqlQueryToDelete = "DELETE FROM users WHERE id = " + insertedId;
      stmt.executeUpdate(sqlQueryToDelete);

    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  @Test
  @DisplayName("Test findAll: should return all users from database")
  void testFindAll() {
    try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password)) {
      // Init entity
      var entity = new DynamicEntity();
      var context = new TaskExecutionContext();
      var entityConfiguration = new EntityConfiguration();
      var access = new HashMap<String, Object>();

      // Configure acces to table users
      var selectAccess = new HashMap<String, Object>();
      selectAccess.put("table", "users");
      access.put("select", selectAccess);
      entityConfiguration.setAccess(access);

      // Configure attributs (id, name, email)
      List<AttributeConfiguration> attributeConfigs = createUserAttributeConfigurations();

      entityConfiguration.setAttributes(attributeConfigs);
      entity.setConfiguration(entityConfiguration);

      // Call function findAll
      var result = provider.findAll(context, providerConfiguration, null, null, entity);

      // Check only 3 users is return
      assertEquals(3, result.getTotalElements());

      // Check all the users in BDD
      List<DynamicEntity> users = result.getContent();

      assertEquals("Alice Dupont", users.get(0).getAttributes().get("userName"));
      assertEquals("alice.dupont@example.com", users.get(0).getAttributes().get("userEmail"));

      assertEquals("Bob Martin", users.get(1).getAttributes().get("userName"));
      assertEquals("bob.martin@example.com", users.get(1).getAttributes().get("userEmail"));

      assertEquals("Charlie Bernard", users.get(2).getAttributes().get("userName"));
      assertEquals("charlie.bernard@example.com", users.get(2).getAttributes().get("userEmail"));

    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  @Test
  @DisplayName("Test patch: should partially update user in database")
  void testPatch() {
    try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password)) {
      Statement stmt = conn.createStatement();

      // Add a row to the database and retrieve the ID
      String insertSql = "INSERT INTO users (name, email) VALUES ('testPatch', 'testpatch@example.com') RETURNING id";
      ResultSet rsInsert = stmt.executeQuery(insertSql);
      rsInsert.next();
      int insertedId = rsInsert.getInt("id");

      // Init entity with only the name to patch (partial update)
      var entity = new DynamicEntity();
      var context = new TaskExecutionContext();
      var entityConfiguration = new EntityConfiguration();
      var attributes = new HashMap<String, Object>();
      var access = new HashMap<String, Object>();

      // Configure access to the users table
      var updateAccess = new HashMap<String, Object>();
      updateAccess.put("table", "users");
      access.put("update", updateAccess);
      entityConfiguration.setAccess(access);

      // Configure attributes (id, name, email)
      List<AttributeConfiguration> attributeConfigs = createUserAttributeConfigurations();
      entityConfiguration.setAttributes(attributeConfigs);

      // Only patch the name
      attributes.put("userName", "testPatchUpdated");

      entity.setConfiguration(entityConfiguration);
      entity.setAttributes(attributes);

      // Call patch function
      DynamicEntity patchResult = provider.patch(context, providerConfiguration, String.valueOf(insertedId), entity);
      assertEquals(entity, patchResult);

      // Verify that only the name was updated
      ResultSet rsAfter = stmt.executeQuery("SELECT name, email FROM users WHERE id = " + insertedId);
      rsAfter.next();
      assertEquals("testPatchUpdated", rsAfter.getString("name"));
      assertEquals("testpatch@example.com", rsAfter.getString("email"));

      // Delete the row created for the test
      stmt.executeUpdate("DELETE FROM users WHERE id = " + insertedId);

    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  @Test
  @DisplayName("Test findById: should return user by id from database")
  void testFindById() {
    try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password)) {
      Statement stmt = conn.createStatement();

      // Add a row to the database and retrieve the ID
      String insertSql = "INSERT INTO users (name, email) VALUES ('testFindById', 'testfindbyid@example.com') RETURNING id";
      ResultSet rsInsert = stmt.executeQuery(insertSql);
      rsInsert.next();
      int insertedId = rsInsert.getInt("id");

      // Init entity
      var entity = new DynamicEntity();
      var context = new TaskExecutionContext();
      var entityConfiguration = new EntityConfiguration();
      var access = new HashMap<String, Object>();

      // Configure access to the users table
      var selectAccess = new HashMap<String, Object>();
      selectAccess.put("table", "users");
      access.put("select", selectAccess);
      entityConfiguration.setAccess(access);

      // Configure attributes (id, name, email)
      List<AttributeConfiguration> attributeConfigs = createUserAttributeConfigurations();
      entityConfiguration.setAttributes(attributeConfigs);
      entity.setConfiguration(entityConfiguration);

      // Call findById function
      DynamicEntity result = provider.findById(context, providerConfiguration, String.valueOf(insertedId), entity);

      // Verify the result
      assertEquals("testFindById", result.getAttributes().get("userName"));
      assertEquals("testfindbyid@example.com", result.getAttributes().get("userEmail"));

      // Delete the row created for the test
      stmt.executeUpdate("DELETE FROM users WHERE id = " + insertedId);

    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  /**
   * Create user attribute configurations for id, name, and email.
   *
   * @return List of configured attributes
   */
  private List<AttributeConfiguration> createUserAttributeConfigurations() {
    List<AttributeConfiguration> attributeConfigs = new ArrayList<>();

    AttributeConfiguration idAttr = new AttributeConfiguration();
    idAttr.setName("id");
    Map<String, Object> idAccess = new HashMap<>();
    idAccess.put("primaryKey", true);
    idAccess.put("column", "id");
    idAttr.setAccess(idAccess);
    attributeConfigs.add(idAttr);

    AttributeConfiguration nameAttr = new AttributeConfiguration();
    nameAttr.setName("userName");
    Map<String, Object> nameAccess = new HashMap<>();
    nameAccess.put("column", "name");
    nameAttr.setAccess(nameAccess);
    attributeConfigs.add(nameAttr);

    AttributeConfiguration emailAttr = new AttributeConfiguration();
    emailAttr.setName("userEmail");
    Map<String, Object> emailAccess = new HashMap<>();
    emailAccess.put("column", "email");
    emailAttr.setAccess(emailAccess);
    attributeConfigs.add(emailAttr);

    return attributeConfigs;
  }

  /**
   * Mock TaskEngine for testing that doesn't do anything.
   */
  static class TaskEngineTest implements TaskEngine {

    @Override
    public void execute(DynamicEntity dynamicEntity, TaskExecutionContext context, String phase) {
      System.out.println("TaskEngine.mock : " + phase);
    }
  }
}
