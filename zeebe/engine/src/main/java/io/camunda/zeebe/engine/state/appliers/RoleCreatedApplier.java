/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.engine.state.appliers;

import io.camunda.zeebe.engine.state.TypedEventApplier;
import io.camunda.zeebe.engine.state.mutable.MutableAuthorizationState;
import io.camunda.zeebe.engine.state.mutable.MutableRoleState;
import io.camunda.zeebe.protocol.impl.record.value.authorization.RoleRecord;
import io.camunda.zeebe.protocol.record.intent.RoleIntent;
import io.camunda.zeebe.protocol.record.value.AuthorizationOwnerType;

public class RoleCreatedApplier implements TypedEventApplier<RoleIntent, RoleRecord> {

  private final MutableRoleState roleState;
  private final MutableAuthorizationState authorizationState;

  public RoleCreatedApplier(
      final MutableRoleState roleState, final MutableAuthorizationState authorizationState) {
    this.roleState = roleState;
    this.authorizationState = authorizationState;
  }

  @Override
  public void applyState(final long key, final RoleRecord value) {
    roleState.create(value);
    authorizationState.insertOwnerTypeByKey(value.getRoleKey(), AuthorizationOwnerType.ROLE);
  }
}
