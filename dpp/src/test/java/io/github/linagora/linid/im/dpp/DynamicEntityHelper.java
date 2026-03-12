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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.github.linagora.linid.im.corelib.plugin.config.dto.EntityConfiguration;
import io.github.linagora.linid.im.corelib.plugin.entity.DynamicEntity;
import java.io.IOException;

/**
 * Helper class to load {@link DynamicEntity} from YAML configuration files
 * for testing purposes.
 */
public final class DynamicEntityHelper {

  private DynamicEntityHelper() {
  }

  /**
   * Loads a {@link DynamicEntity} from a YAML configuration file.
   *
   * @param filePath the path of the YAML file in classpath
   * @return the constructed DynamicEntity
   * @throws IOException if the file cannot be read
   */
  public static DynamicEntity getEntity(final String filePath) throws IOException {
    EntityConfiguration entityConfiguration = loadConfiguration(filePath);

    DynamicEntity dynamicEntity = new DynamicEntity();
    dynamicEntity.setConfiguration(entityConfiguration);

    return dynamicEntity;
  }

  /**
   * Loads an {@link EntityConfiguration} from a YAML configuration file.
   *
   * @param filePath the path of the YAML file in classpath
   * @return the parsed EntityConfiguration
   * @throws IOException if the file cannot be read
   */
  public static EntityConfiguration loadConfiguration(final String filePath) throws IOException {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    return mapper.readValue(DynamicEntityHelper.class.getClassLoader().getResourceAsStream(filePath),
        EntityConfiguration.class);
  }
}
