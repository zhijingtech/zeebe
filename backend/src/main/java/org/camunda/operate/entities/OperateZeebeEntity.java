/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.operate.entities;

public abstract class OperateZeebeEntity extends OperateEntity {

  private long key;

  private int partitionId;

  public void setKey(long key) {
    this.key = key;
  }

  public void setPartitionId(int partitionId) {
    this.partitionId = partitionId;
  }

  public long getKey() {
    return key;
  }

  public int getPartitionId() {
    return partitionId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    if (!super.equals(o))
      return false;

    OperateZeebeEntity that = (OperateZeebeEntity) o;

    if (key != that.key)
      return false;
    return partitionId == that.partitionId;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (int) (key ^ (key >>> 32));
    result = 31 * result + partitionId;
    return result;
  }
}
