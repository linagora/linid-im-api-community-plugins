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

package io.github.linagora.linid.im.hpp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import io.github.linagora.linid.im.corelib.plugin.config.JinjaService;
import io.github.linagora.linid.im.corelib.plugin.config.dto.ProviderConfiguration;
import io.github.linagora.linid.im.corelib.plugin.entity.DynamicEntity;
import io.github.linagora.linid.im.corelib.plugin.provider.ProviderPlugin;
import io.github.linagora.linid.im.corelib.plugin.task.TaskEngine;
import io.github.linagora.linid.im.corelib.plugin.task.TaskExecutionContext;
import io.github.linagora.linid.im.hpp.model.EndpointConfiguration;
import io.github.linagora.linid.im.hpp.service.HttpService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

/**
 * HTTP Provider Plugin implementation for LinID Directory Manager.
 *
 * <p>This plugin allows CRUD operations over HTTP endpoints defined via dynamic configurations.
 * It supports templated URIs, request bodies, headers, and response mappings using Jinja templates.
 *
 * <p>Operations include create, update, patch, delete, findById, and findAll.
 * The plugin delegates actual HTTP calls to the {@link HttpService} and uses {@link JinjaService} for template rendering.
 *
 * <p>Lifecycle tasks can be executed before and after key operations using the {@link TaskEngine}.
 */
@Component
public class HttpProviderPlugin implements ProviderPlugin {

  /**
   * Engine to execute lifecycle tasks before and after validation and CRUD operations. Supports running custom logic at various
   * points in the entity handling process.
   */
  private final TaskEngine taskEngine;

  /**
   * Service responsible for performing HTTP requests based on configured endpoints.
   */
  private final HttpService httpService;

  /**
   * Service used to render Jinja templates within URIs, request bodies, headers, and response mappings.
   */
  private final JinjaService jinjaService;

  /**
   * Jackson ObjectMapper used to convert configuration maps into {@link EndpointConfiguration} instances.
   */
  private final ObjectMapper mapper = new ObjectMapper();

  private static final String RESPONSE = "response";

  /**
   * Default constructor.
   *
   * @param taskEngine  Engine to execute lifecycle tasks before and after validation and CRUD operations.
   * @param httpService Service responsible for performing HTTP requests based on configured endpoints.
   * @param jinjaService Service used to render Jinja templates within URIs, request bodies, headers, and response mappings.
   */
  @Autowired
  public HttpProviderPlugin(final TaskEngine taskEngine,
                            final HttpService httpService,
                            final JinjaService jinjaService) {
    this.taskEngine = taskEngine;
    this.httpService = httpService;
    this.jinjaService = jinjaService;
  }

  @Override
  public boolean supports(@NonNull String type) {
    return "http".equalsIgnoreCase(type);
  }

  @Override
  public DynamicEntity create(TaskExecutionContext context,
                              ProviderConfiguration providerConfiguration,
                              DynamicEntity dynamicEntity) {
    var endpointConfiguration = getEndpointConfiguration("create", dynamicEntity);
    String response = httpService.request(context, providerConfiguration, endpointConfiguration, "create", dynamicEntity);
    context.put(RESPONSE, response);

    taskEngine.execute(dynamicEntity, context, "beforeResponseMappingCreate");
    var entity = mappingEntity(context, endpointConfiguration, dynamicEntity);
    taskEngine.execute(dynamicEntity, context, "afterResponseMappingCreate");

    return entity;
  }

  @Override
  public DynamicEntity update(TaskExecutionContext context,
                              ProviderConfiguration providerConfiguration,
                              String id,
                              DynamicEntity dynamicEntity) {
    var endpointConfiguration = getEndpointConfiguration("update", dynamicEntity);
    context.put("id", id);
    String response = httpService.request(context, providerConfiguration, endpointConfiguration, "update", dynamicEntity);
    context.put(RESPONSE, response);

    taskEngine.execute(dynamicEntity, context, "beforeResponseMappingUpdate");
    var entity = mappingEntity(context, endpointConfiguration, dynamicEntity);
    taskEngine.execute(dynamicEntity, context, "afterResponseMappingUpdate");

    return entity;
  }

  @Override
  public DynamicEntity patch(TaskExecutionContext context,
                             ProviderConfiguration providerConfiguration,
                             String id,
                             DynamicEntity dynamicEntity) {
    var endpointConfiguration = getEndpointConfiguration("patch", dynamicEntity);
    context.put("id", id);
    String response = httpService.request(context, providerConfiguration, endpointConfiguration, "patch", dynamicEntity);
    context.put(RESPONSE, response);

    taskEngine.execute(dynamicEntity, context, "beforeResponseMappingPatch");
    var entity = mappingEntity(context, endpointConfiguration, dynamicEntity);
    taskEngine.execute(dynamicEntity, context, "afterResponseMappingPatch");

    return entity;
  }

  @Override
  public boolean delete(TaskExecutionContext context,
                        ProviderConfiguration providerConfiguration,
                        String id,
                        DynamicEntity dynamicEntity) {
    var endpointConfiguration = getEndpointConfiguration("delete", dynamicEntity);
    context.put("id", id);
    String response = httpService.request(context, providerConfiguration, endpointConfiguration, "delete", dynamicEntity);
    context.put(RESPONSE, response);

    taskEngine.execute(dynamicEntity, context, "beforeResponseMappingDelete");
    boolean state = "true".equalsIgnoreCase(jinjaService.render(context, dynamicEntity, endpointConfiguration.getResult()));
    taskEngine.execute(dynamicEntity, context, "afterResponseMappingDelete");

    if (!state) {
      throw new ApiException(400, I18nMessage.of("hpp.error.delete"));
    }

    return true;
  }

  @Override
  public DynamicEntity findById(TaskExecutionContext context,
                                ProviderConfiguration providerConfiguration,
                                String id,
                                DynamicEntity dynamicEntity) {
    context.put("id", id);
    var endpointConfiguration = getEndpointConfiguration("findById", dynamicEntity);
    String response = httpService.request(context, providerConfiguration, endpointConfiguration, "findById", dynamicEntity);
    context.put(RESPONSE, response);

    taskEngine.execute(dynamicEntity, context, "beforeResponseMappingFindById");
    var entity = mappingEntity(context, endpointConfiguration, dynamicEntity);
    taskEngine.execute(dynamicEntity, context, "afterResponseMappingFindById");

    return entity;
  }

  @Override
  public Page<DynamicEntity> findAll(TaskExecutionContext context,
                                     ProviderConfiguration providerConfiguration,
                                     MultiValueMap<String, String> multiValueMap,
                                     Pageable pageable,
                                     DynamicEntity dynamicEntity) {
    var endpointConfiguration = getEndpointConfiguration("findAll", dynamicEntity);
    String response = httpService.request(context, providerConfiguration, endpointConfiguration, "findAll", dynamicEntity);
    context.put(RESPONSE, response);

    taskEngine.execute(dynamicEntity, context, "beforeResponseMappingFindAll");
    Page<DynamicEntity> entities = mappingEntities(context, endpointConfiguration, dynamicEntity);
    taskEngine.execute(dynamicEntity, context, "afterResponseMappingFindAll");

    return entities;
  }

  /**
   * Extracts and converts the endpoint configuration for the specified action from the dynamic entity's configuration.
   *
   * @param action the action name (e.g., "create", "update").
   * @param dynamicEntity the entity containing configuration maps.
   * @return the endpoint configuration or an empty configuration if none found.
   */
  public EndpointConfiguration getEndpointConfiguration(String action, DynamicEntity dynamicEntity) {
    var access = dynamicEntity.getConfiguration().getAccess();

    if (!access.containsKey(action)) {
      return new EndpointConfiguration();
    }

    return mapper.convertValue(
        access.get(action),
        new TypeReference<>() {
        }
    );
  }

  /**
   * Maps a single entity from the HTTP response according to the entity mapping configuration.
   *
   * @param taskContext the current task execution context.
   * @param configuration the endpoint configuration containing mapping definitions.
   * @param dynamicEntity the original dynamic entity context.
   * @return the mapped entity.
   */
  public DynamicEntity mappingEntity(TaskExecutionContext taskContext, EndpointConfiguration configuration,
                                     DynamicEntity dynamicEntity) {
    var result = new DynamicEntity();

    result.setConfiguration(dynamicEntity.getConfiguration());
    result.setAttributes(new HashMap<>());

    configuration.getEntityMapping().forEach((key, template) -> {
      String value = jinjaService.render(taskContext, dynamicEntity, template);
      result.getAttributes().put(key, value);
    });

    return result;
  }

  /**
   * Maps a page of entities from the HTTP response using pagination and entity mapping configuration.
   *
   * @param taskContext the current task execution context.
   * @param configuration the endpoint configuration containing pagination and mapping details.
   * @param dynamicEntity the original dynamic entity context.
   * @return a pageable list of mapped entities.
   */
  public Page<DynamicEntity> mappingEntities(TaskExecutionContext taskContext, EndpointConfiguration configuration,
                                             DynamicEntity dynamicEntity) {
    int page = Integer.parseInt(jinjaService.render(taskContext, dynamicEntity,
        Optional.ofNullable(configuration.getPage()).orElse("0")));
    int size = Integer.parseInt(jinjaService.render(taskContext, dynamicEntity,
        Optional.ofNullable(configuration.getSize()).orElse("0")));
    int itemsSize = Integer.parseInt(jinjaService.render(taskContext, dynamicEntity,
        Optional.ofNullable(configuration.getItemsCount()).orElse("0")));
    long total = Long.parseLong(jinjaService.render(taskContext, dynamicEntity,
        Optional.ofNullable(configuration.getTotal()).orElse("0")));

    List<DynamicEntity> content = new ArrayList<>();

    for (int i = 0; i < itemsSize; i++) {
      final int index = i;
      var result = new DynamicEntity();

      result.setConfiguration(dynamicEntity.getConfiguration());
      result.setAttributes(new HashMap<>());

      configuration.getEntityMapping().forEach((key, template) -> {
        String value = jinjaService.render(taskContext, dynamicEntity, Map.of("index", index), template);
        result.getAttributes().put(key, value);
      });
      content.add(result);
    }

    return new PageImpl<>(content, PageRequest.of(page, size), total);
  }
}
