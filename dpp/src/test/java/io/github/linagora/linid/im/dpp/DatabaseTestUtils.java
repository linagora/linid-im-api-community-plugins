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

import io.github.linagora.linid.im.corelib.plugin.entity.DynamicEntity;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility methods to build {@link DynamicEntity} instances for tests.
 */
public final class DatabaseTestUtils {

  /**
   * Private constructor to fetch one line in a certain table.
   *
   * @param stmt   the SQL statement to execute
   * @param table  the name of the table to query
   * @param column the name of the column to filter on
   * @param value  the value to filter on
   * @return a map representing the first row of the result set, with column names as keys and column values as values
   * @throws SQLException
   */
  public static Map<String, Object> fetchOne(Statement stmt, String table, String column, String value)
      throws SQLException {
    ResultSet result = stmt
        .executeQuery(String.format("SELECT * FROM %s WHERE %s = '%s' LIMIT 1", table, column, value));
    ResultSetMetaData meta = result.getMetaData();
    int columnCount = meta.getColumnCount();

    if (!result.next()) {
      return Map.of();
    }

    var row = new HashMap<String, Object>();

    for (int i = 1; i <= columnCount; i++) {
      row.put(meta.getColumnLabel(i), result.getObject(i));
    }

    return row;
  }

  /**
   * Deletes one line in a certain table.
   *
   * @param stmt   the SQL statement to execute
   * @param table  the name of the table to query
   * @param column the name of the column to filter on
   * @param value  the value to filter on
   * @throws SQLException
   */
  public static void deleteOne(Statement stmt, String table, String column, String value) throws SQLException {
    stmt.executeUpdate(String.format("DELETE FROM %s WHERE %s = '%s'", table, column, value));
  }

  /**
   * Inserts one line in a certain table and returns the generated id.
   *
   * @param stmt    the SQL statement to execute
   * @param table   the name of the table to query
   * @param columns the names of the columns to insert
   * @param values  the values to insert, in the same order as the columns
   * @return the generated id of the inserted row
   * @throws SQLException
   */
  public static Object insertOne(Statement stmt, String table, String[] columns, String[] values) throws SQLException {
    String queryColumns = String.join(", ", columns);
    String queryValues = String.join(", ", values);

    ResultSet result = stmt
        .executeQuery(String.format("INSERT INTO %s (%s) VALUES (%s) RETURNING id", table, queryColumns, queryValues));
    result.next();
    return result.getObject("id");
  }

}
