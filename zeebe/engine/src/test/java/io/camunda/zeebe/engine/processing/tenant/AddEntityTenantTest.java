/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.engine.processing.tenant;

import static io.camunda.zeebe.protocol.record.Assertions.assertThat;
import static io.camunda.zeebe.protocol.record.value.EntityType.GROUP;
import static io.camunda.zeebe.protocol.record.value.EntityType.MAPPING;
import static io.camunda.zeebe.protocol.record.value.EntityType.USER;

import io.camunda.zeebe.engine.util.EngineRule;
import io.camunda.zeebe.protocol.record.RejectionType;
import io.camunda.zeebe.test.util.record.RecordingExporterTestWatcher;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;

public class AddEntityTenantTest {

  @Rule public final EngineRule engine = EngineRule.singlePartition();

  @Rule
  public final RecordingExporterTestWatcher recordingExporterTestWatcher =
      new RecordingExporterTestWatcher();

  @Test
  public void shouldAddMappingToTenant() {
    // given
    final var entityType = MAPPING;
    final var entityKey = createMapping();
    final var tenantId = UUID.randomUUID().toString();
    final var tenantKey =
        engine
            .tenant()
            .newTenant()
            .withTenantId(tenantId)
            .withName("Tenant 1")
            .create()
            .getValue()
            .getTenantKey();

    // when add user entity to tenant
    final var updatedTenant =
        engine
            .tenant()
            .addEntity(tenantKey)
            .withEntityKey(entityKey)
            .withEntityType(entityType)
            .add()
            .getValue();

    // then assert that the entity was added correctly
    Assertions.assertThat(updatedTenant)
        .describedAs(
            "Entity of type %s with key %s should be correctly added to tenant with key %s",
            entityType, entityKey, tenantKey)
        .isNotNull()
        .hasFieldOrPropertyWithValue("entityKey", entityKey)
        .hasFieldOrPropertyWithValue("tenantKey", tenantKey)
        .hasFieldOrPropertyWithValue("entityType", entityType);
  }

  @Test
  public void shouldAddUserToTenant() {
    // given
    final var entityType = USER;
    final var entityKey = createUser();
    final var tenantId = UUID.randomUUID().toString();
    final var tenantKey =
        engine
            .tenant()
            .newTenant()
            .withTenantId(tenantId)
            .withName("Tenant 1")
            .create()
            .getValue()
            .getTenantKey();

    // when add user entity to tenant
    final var updatedTenant =
        engine
            .tenant()
            .addEntity(tenantKey)
            .withEntityKey(entityKey)
            .withEntityType(entityType)
            .add()
            .getValue();

    // then assert that the entity was added correctly
    Assertions.assertThat(updatedTenant)
        .describedAs(
            "Entity of type %s with key %s should be correctly added to tenant with key %s",
            entityType, entityKey, tenantKey)
        .isNotNull()
        .hasFieldOrPropertyWithValue("entityKey", entityKey)
        .hasFieldOrPropertyWithValue("tenantKey", tenantKey)
        .hasFieldOrPropertyWithValue("entityType", entityType);
  }

  @Test
  public void shouldAddGroupToTenant() {
    // given
    final var entityType = GROUP;
    final var entityKey = createGroup();
    final var tenantId = UUID.randomUUID().toString();
    final var tenantKey =
        engine
            .tenant()
            .newTenant()
            .withTenantId(tenantId)
            .withName("Tenant 1")
            .create()
            .getValue()
            .getTenantKey();

    // when add user entity to tenant
    final var updatedTenant =
        engine
            .tenant()
            .addEntity(tenantKey)
            .withEntityKey(entityKey)
            .withEntityType(entityType)
            .add()
            .getValue();

    // then assert that the entity was added correctly
    Assertions.assertThat(updatedTenant)
        .describedAs(
            "Entity of type %s with key %s should be correctly added to tenant with key %s",
            entityType, entityKey, tenantKey)
        .isNotNull()
        .hasFieldOrPropertyWithValue("entityKey", entityKey)
        .hasFieldOrPropertyWithValue("tenantKey", tenantKey)
        .hasFieldOrPropertyWithValue("entityType", entityType);
  }

  @Test
  public void shouldRejectIfTenantIsNotPresentWhileAddingEntity() {
    // when try adding entity to a non-existent tenant
    final var entityKey = 1L;
    final var notPresentUpdateRecord = engine.tenant().addEntity(entityKey).expectRejection().add();
    // then assert that the rejection is for tenant not found
    assertThat(notPresentUpdateRecord)
        .hasRejectionType(RejectionType.NOT_FOUND)
        .hasRejectionReason(
            "Expected to add entity to tenant with key '%s', but no tenant with this key exists."
                .formatted(entityKey));
  }

  @Test
  public void shouldRejectIfEntityIsNotPresentWhileAddingToTenant() {
    // given
    final var tenantId = UUID.randomUUID().toString();
    final var tenantRecord =
        engine.tenant().newTenant().withTenantId(tenantId).withName("Tenant 1").create();

    // when try adding a non-existent entity to the tenant
    final var tenantKey = tenantRecord.getValue().getTenantKey();
    final var notPresentUpdateRecord =
        engine
            .tenant()
            .addEntity(tenantKey)
            .withEntityKey(1L)
            .withEntityType(USER)
            .expectRejection()
            .add();

    // then assert that the rejection is for entity not found
    assertThat(notPresentUpdateRecord)
        .hasRejectionType(RejectionType.NOT_FOUND)
        .hasRejectionReason(
            "Expected to add entity with key '1' to tenant with tenantId '%s', but the entity doesn't exist."
                .formatted(tenantId));
  }

  @Test
  public void shouldRejectIfEntityIsAlreadyAssignedToTenant() {
    // given
    final var userKey = createUser();
    final var tenantId = UUID.randomUUID().toString();
    final var tenantRecord =
        engine.tenant().newTenant().withTenantId(tenantId).withName("Tenant 1").create();
    final var tenantKey = tenantRecord.getValue().getTenantKey();
    engine.tenant().addEntity(tenantKey).withEntityKey(userKey).withEntityType(USER).add();

    // when try adding a non-existent entity to the tenant
    final var alreadyAssignedRecord =
        engine
            .tenant()
            .addEntity(tenantKey)
            .withEntityKey(userKey)
            .withEntityType(USER)
            .expectRejection()
            .add();

    // then assert that the rejection is for entity not found
    assertThat(alreadyAssignedRecord)
        .hasRejectionType(RejectionType.INVALID_ARGUMENT)
        .hasRejectionReason(
            "Expected to add entity with key '%s' to tenant with tenantId '%s', but the entity is already assigned to the tenant."
                .formatted(userKey, tenantId));
  }

  private Long createUser() {
    return engine
        .user()
        .newUser(UUID.randomUUID().toString())
        .withName("Foo Bar")
        .withEmail("foo@bar.com")
        .withPassword("password")
        .create()
        .getValue()
        .getUserKey();
  }

  private long createGroup() {
    return engine.group().newGroup("groupName").create().getValue().getGroupKey();
  }

  private long createMapping() {
    return engine
        .mapping()
        .newMapping("mappingName")
        .withClaimValue("claimValue")
        .create()
        .getValue()
        .getMappingKey();
  }
}
