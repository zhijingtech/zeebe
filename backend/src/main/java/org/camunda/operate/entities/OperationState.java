/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.operate.entities;

public enum OperationState {

  SCHEDULED,
  LOCKED,
  SENT,
  FAILED,
  COMPLETED

}
