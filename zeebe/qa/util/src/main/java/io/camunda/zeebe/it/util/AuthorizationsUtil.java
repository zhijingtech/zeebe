/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.it.util;

import static io.camunda.security.configuration.InitializationConfiguration.DEFAULT_USER_PASSWORD;
import static io.camunda.security.configuration.InitializationConfiguration.DEFAULT_USER_USERNAME;

import io.camunda.client.CamundaClient;
import io.camunda.client.CredentialsProvider;
import io.camunda.client.protocol.rest.PermissionTypeEnum;
import io.camunda.client.protocol.rest.ResourceTypeEnum;
import io.camunda.search.clients.SearchClients;
import io.camunda.search.query.AuthorizationQuery;
import io.camunda.search.query.SearchQueryResult;
import io.camunda.search.query.TenantQuery;
import io.camunda.search.query.UserQuery;
import io.camunda.zeebe.protocol.record.value.PermissionType;
import io.camunda.zeebe.qa.util.cluster.TestGateway;
import io.camunda.zeebe.util.CloseableSilently;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.function.Supplier;
import org.awaitility.Awaitility;

public class AuthorizationsUtil implements CloseableSilently {

  private final TestGateway<?> gateway;
  private final CamundaClient client;
  private final SearchClients searchClients;

  public AuthorizationsUtil(
      final TestGateway<?> gateway, final CamundaClient client, final String elasticsearchUrl) {
    this(gateway, client, SearchClientsUtil.createSearchClients(elasticsearchUrl));
  }

  public AuthorizationsUtil(
      final TestGateway<?> gateway, final CamundaClient client, final SearchClients searchClients) {
    this.gateway = gateway;
    this.client = client;
    this.searchClients = searchClients;
  }

  public static AuthorizationsUtil create(
      final TestGateway<?> gateway, final String elasticsearchUrl) {
    final var authorizationUtil =
        new AuthorizationsUtil(
            gateway,
            createClient(gateway, DEFAULT_USER_USERNAME, DEFAULT_USER_PASSWORD),
            SearchClientsUtil.createSearchClients(elasticsearchUrl));
    authorizationUtil.awaitUserExistsInElasticsearch(DEFAULT_USER_USERNAME);
    return authorizationUtil;
  }

  public long createUser(final String username, final String password) {
    return createUserWithPermissions(username, password);
  }

  public long createUserWithPermissions(
      final String username, final String password, final Permissions... permissions) {
    final var userCreateResponse =
        client
            .newUserCreateCommand()
            .username(username)
            .password(password)
            .name("name")
            .email("foo@bar.com")
            .send()
            .join();
    awaitUserExistsInElasticsearch(username);
    createPermissions(userCreateResponse.getUserKey(), permissions);
    return userCreateResponse.getUserKey();
  }

  public void createPermissions(final long userKey, final Permissions... permissions) {
    for (final Permissions permission : permissions) {
      client
          .newAddPermissionsCommand(userKey)
          .resourceType(permission.resourceType())
          .permission(permission.permissionType())
          .resourceIds(permission.resourceIds())
          .send()
          .join();
    }
    if (permissions != null && permissions.length > 0) {
      awaitPermissionExistsInElasticsearch(userKey, Arrays.asList(permissions).getLast());
    }
  }

  public long createTenant(final String tenantId, final String tenantName, final long... userKeys) {
    final long tenantKey =
        client
            .newCreateTenantCommand()
            .tenantId(tenantId)
            .name(tenantName)
            .send()
            .join()
            .getTenantKey();
    for (final long userKey : userKeys) {
      client.newAssignUserToTenantCommand(tenantKey).userKey(userKey).send().join();
    }
    awaitTenantExistsInElasticsearch(tenantId);
    return tenantKey;
  }

  public CamundaClient createClient(final String username, final String password) {
    return createClient(gateway, username, password);
  }

  public CamundaClient createClientGrpc(final String username, final String password) {
    return createClientGrpc(gateway, username, password);
  }

  public CamundaClient createUserAndClient(
      final String username, final String password, final Permissions... permissions) {
    createUserWithPermissions(username, password, permissions);
    return createClient(gateway, username, password);
  }

  public static CamundaClient createClient(
      final TestGateway<?> gateway, final String username, final String password) {
    return gateway
        .newClientBuilder()
        .preferRestOverGrpc(true)
        .defaultRequestTimeout(Duration.ofSeconds(15))
        .credentialsProvider(
            new CredentialsProvider() {
              @Override
              public void applyCredentials(final CredentialsApplier applier) {
                applier.put(
                    "Authorization",
                    "Basic %s"
                        .formatted(
                            Base64.getEncoder()
                                .encodeToString("%s:%s".formatted(username, password).getBytes())));
              }

              @Override
              public boolean shouldRetryRequest(final StatusCode statusCode) {
                return false;
              }
            })
        .build();
  }

  public static CamundaClient createClientGrpc(
      final TestGateway<?> gateway, final String username, final String password) {
    return gateway
        .newClientBuilder()
        .defaultRequestTimeout(Duration.ofSeconds(15))
        .preferRestOverGrpc(false)
        .credentialsProvider(
            new CredentialsProvider() {
              @Override
              public void applyCredentials(final CredentialsApplier applier) {
                applier.put(
                    "Authorization",
                    "Basic %s"
                        .formatted(
                            Base64.getEncoder()
                                .encodeToString("%s:%s".formatted(username, password).getBytes())));
              }

              @Override
              public boolean shouldRetryRequest(final StatusCode statusCode) {
                return false;
              }
            })
        .build();
  }

  private void awaitTenantExistsInElasticsearch(final String tenantId) {
    final var tenantQuery = TenantQuery.of(b -> b.filter(f -> f.tenantId(tenantId)));
    awaitEntityExistsInElasticsearch(() -> searchClients.searchTenants(tenantQuery));
  }

  public void awaitUserExistsInElasticsearch(final String username) {
    final var userQuery = UserQuery.of(b -> b.filter(f -> f.username(username)));
    awaitEntityExistsInElasticsearch(() -> searchClients.searchUsers(userQuery));
  }

  private void awaitPermissionExistsInElasticsearch(
      final long userKey, final Permissions permissions) {
    final var resourceType = permissions.resourceType().getValue();
    final var permissionType = PermissionType.valueOf(permissions.permissionType().getValue());
    final var resourceIds = permissions.resourceIds();

    final var permissionQuery =
        AuthorizationQuery.of(
            b ->
                b.filter(
                    f ->
                        f.ownerKeys(userKey)
                            .resourceType(resourceType)
                            .permissionType(permissionType)
                            .resourceIds(resourceIds)));

    awaitEntityExistsInElasticsearch(() -> searchClients.searchAuthorizations(permissionQuery));
  }

  private void awaitEntityExistsInElasticsearch(
      final Supplier<SearchQueryResult<?>> searchQueryResultSupplier) {
    Awaitility.await()
        .atMost(Duration.ofSeconds(15))
        .ignoreExceptions()
        .until(() -> searchQueryResultSupplier.get().total() > 0);
  }

  public CamundaClient getDefaultClient() {
    return client;
  }

  @Override
  public void close() {
    client.close();
    searchClients.close();
  }

  public record Permissions(
      ResourceTypeEnum resourceType, PermissionTypeEnum permissionType, List<String> resourceIds) {}
}
