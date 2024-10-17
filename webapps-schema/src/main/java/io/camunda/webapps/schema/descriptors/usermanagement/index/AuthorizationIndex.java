/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.webapps.schema.descriptors.usermanagement.index;

import io.camunda.webapps.schema.descriptors.usermanagement.UserManagementIndexDescriptor;

public class AuthorizationIndex extends UserManagementIndexDescriptor {
  public static final String INDEX_NAME = "authorizations";
  public static final String INDEX_VERSION = "8.7.0";

  public static final String ID = "id";
  public static final String OWNER_KEY = "ownerKey";
  public static final String OWNER_TYPE = "ownerType";
  public static final String RESOURCE_TYPE = "resourceType";
  public static final String PERMISSIONS = "permissions";
  public static final String PERMISSIONS_TYPE = "type";
  public static final String PERMISSIONS_RESOURCEIDS = "resourceIds";

  public AuthorizationIndex(final String indexPrefix, final boolean isElasticsearch) {
    super(indexPrefix, isElasticsearch);
  }

  @Override
  public String getFullQualifiedName() {
    final var stringBuilder = new StringBuilder();

    if (getIndexPrefix() != null && !getIndexPrefix().isEmpty()) {
      stringBuilder.append(getIndexPrefix());
      stringBuilder.append("-");
    }

    stringBuilder.append(getIndexName());
    stringBuilder.append("-");
    stringBuilder.append(getVersion());
    stringBuilder.append("_");

    return stringBuilder.toString();
  }

  @Override
  public String getAlias() {
    return getFullQualifiedName() + "alias";
  }

  @Override
  public String getAllVersionsIndexNameRegexPattern() {
    return String.format("%s-%s-\\d.*", getIndexPrefix(), getIndexName());
  }

  @Override
  public String getVersion() {
    return INDEX_VERSION;
  }

  @Override
  public String getIndexName() {
    return INDEX_NAME;
  }
}
