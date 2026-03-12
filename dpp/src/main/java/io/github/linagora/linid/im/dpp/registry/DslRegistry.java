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

package io.github.linagora.linid.im.dpp.registry;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import io.github.linagora.linid.im.corelib.plugin.config.dto.ProviderConfiguration;
import jakarta.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/**
 * Registry responsible for creating and caching {@link DSLContext} instances
 * per provider configuration.
 *
 * <p>
 * If a provider configuration changes, the associated datasource and DSL
 * context
 * are recreated. Each provider maintains its own Hikari connection pool.
 * </p>
 */
@Slf4j
@Component
public class DslRegistry {

  /**
   * Map of provider name to its associated DSLContextHolder.
   */
  private final Map<String, DSLContextHolder> dslContexts = new HashMap<>();

  private static final String MISSING_OPTION = "error.plugin.default.missing.option";
  private static final String OPTION = "option";

  /**
   * Creates a new DSL registry.
   */
  public DslRegistry() {}

  /**
   * Returns a {@link DSLContext} associated with the given provider
   * configuration.
   * If the configuration has changed since the last call, the datasource and DSL
   * context are recreated.
   *
   * @param config the provider configuration
   * @return the DSLContext associated with the provider
   */
  public DSLContext getDsl(ProviderConfiguration config) {
    String name = config.getName();

    synchronized (name.intern()) {
      DSLContextHolder holder = dslContexts.get(name);

      // If the configuration didn't change we just return the current
      if (holder != null && config.equals(holder.getConfigSnapshot())) {
        return holder.getDsl();
      }

      // close previous datasource if exists
      if (holder != null) {
        log.info("Configuration changed for provider '{}', recreating connection pool", name);
        holder.getDatasource().close();
        dslContexts.remove(name);
      } else {
        log.info("Initializing connection pool for provider '{}'", name);
      }

      HikariConfig hikariConfig = new HikariConfig();

      String url = config.getOption("url")
          .orElseThrow(() -> new ApiException(
              500,
              I18nMessage.of(MISSING_OPTION, Map.of(OPTION, "url"))
        ));
      hikariConfig.setJdbcUrl(url);

      String username = config.getOption("username")
          .orElseThrow(() -> new ApiException(
              500,
              I18nMessage.of(MISSING_OPTION, Map.of(OPTION, "username"))
        ));
      hikariConfig.setUsername(username);

      String password = config.getOption("password")
          .orElseThrow(() -> new ApiException(
              500,
              I18nMessage.of(MISSING_OPTION, Map.of(OPTION, "password"))
        ));
      hikariConfig.setPassword(password);

      String maximumPoolSize = config.getOption("maximumPoolSize").orElse("10");
      hikariConfig.setMaximumPoolSize(Integer.parseInt(maximumPoolSize));

      String idleTimeout = config.getOption("idleTimeout").orElse("600000");
      hikariConfig.setIdleTimeout(Long.parseLong(idleTimeout));

      String connectionTimeout = config.getOption("connectionTimeout").orElse("30000");
      hikariConfig.setConnectionTimeout(Long.parseLong(connectionTimeout));

      HikariDataSource ds = new HikariDataSource(hikariConfig);
      DSLContext dsl = DSL.using(ds, SQLDialect.POSTGRES);

      DSLContextHolder newHolder = new DSLContextHolder(dsl, ds, config);
      dslContexts.put(name, newHolder);

      return dsl;
    }
  }

  /**
   * Closes all datasource pools when the application shuts down.
   */
  @PreDestroy
  public void shutdown() {
    dslContexts.values().forEach(holder -> holder.getDatasource().close());
  }
}
