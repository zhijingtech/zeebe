/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.camunda.zeebe.client.api.search.filter;

import io.camunda.zeebe.client.api.search.filter.builder.DateTimeProperty;
import io.camunda.zeebe.client.api.search.filter.builder.IntegerProperty;
import io.camunda.zeebe.client.api.search.filter.builder.LongProperty;
import io.camunda.zeebe.client.api.search.filter.builder.ProcessInstanceStateProperty;
import io.camunda.zeebe.client.api.search.filter.builder.StringProperty;
import io.camunda.zeebe.client.api.search.query.TypedSearchQueryRequest.SearchRequestFilter;
import io.camunda.zeebe.client.protocol.rest.ProcessInstanceVariableFilterRequest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface ProcessInstanceFilter extends SearchRequestFilter {

  /** Filter by processInstanceKey */
  ProcessInstanceFilter processInstanceKey(final Long processInstanceKey);

  /** Filter by processInstanceKey using {@link LongProperty} consumer */
  ProcessInstanceFilter processInstanceKey(final Consumer<LongProperty> fn);

  /** Filter by processDefinitionId */
  ProcessInstanceFilter processDefinitionId(final String processDefinitionId);

  /** Filter by processDefinitionId using {@link StringProperty} */
  ProcessInstanceFilter processDefinitionId(final Consumer<StringProperty> fn);

  /** Filter by processDefinitionName */
  ProcessInstanceFilter processDefinitionName(final String processDefinitionName);

  /** Filter by processDefinitionName using {@link StringProperty} consumer */
  ProcessInstanceFilter processDefinitionName(final Consumer<StringProperty> fn);

  /** Filter by processDefinitionVersion */
  ProcessInstanceFilter processDefinitionVersion(final Integer processDefinitionVersion);

  /** Filter by processDefinitionVersion using {@link IntegerProperty} consumer */
  ProcessInstanceFilter processDefinitionVersion(final Consumer<IntegerProperty> fn);

  /** Filter by processDefinitionVersionTag */
  ProcessInstanceFilter processDefinitionVersionTag(final String processDefinitionVersionTag);

  /** Filter by processDefinitionVersionTag using {@link StringProperty} consumer */
  ProcessInstanceFilter processDefinitionVersionTag(final Consumer<StringProperty> fn);

  /** Filter by processDefinitionKey */
  ProcessInstanceFilter processDefinitionKey(final Long processDefinitionKey);

  /** Filter by processDefinitionKey using {@link LongProperty} consumer */
  ProcessInstanceFilter processDefinitionKey(final Consumer<LongProperty> fn);

  /** Filter by parentProcessInstanceKey */
  ProcessInstanceFilter parentProcessInstanceKey(final Long parentProcessInstanceKey);

  /** Filter by parentProcessInstanceKey using {@link LongProperty} consumer */
  ProcessInstanceFilter parentProcessInstanceKey(final Consumer<LongProperty> fn);

  /** Filter by parentFlowNodeInstanceKey */
  ProcessInstanceFilter parentFlowNodeInstanceKey(final Long parentFlowNodeInstanceKey);

  /** Filter by parentFlowNodeInstanceKey using {@link LongProperty} consumer */
  ProcessInstanceFilter parentFlowNodeInstanceKey(final Consumer<LongProperty> fn);

  /** Filter by treePath */
  ProcessInstanceFilter treePath(final String treePath);

  /** Filter by treePath using {@link StringProperty} consumer */
  ProcessInstanceFilter treePath(final Consumer<StringProperty> fn);

  /** Filter by startDate */
  ProcessInstanceFilter startDate(final OffsetDateTime startDate);

  /** Filter by startDate using {@link DateTimeProperty} consumer */
  ProcessInstanceFilter startDate(final Consumer<DateTimeProperty> fn);

  /** Filter by endDate */
  ProcessInstanceFilter endDate(final OffsetDateTime endDate);

  /** Filter by endDate using {@link DateTimeProperty} consumer */
  ProcessInstanceFilter endDate(final Consumer<DateTimeProperty> fn);

  /** Filter by state */
  ProcessInstanceFilter state(final String state);

  /** Filter by state using {@link ProcessInstanceStateProperty} consumer */
  ProcessInstanceFilter state(final Consumer<ProcessInstanceStateProperty> fn);

  /** Filter by hasIncident */
  ProcessInstanceFilter hasIncident(final Boolean hasIncident);

  /** Filter by tenantId */
  ProcessInstanceFilter tenantId(final String tenantId);

  /** Filter by tenantId using {@link StringProperty} consumer */
  ProcessInstanceFilter tenantId(final Consumer<StringProperty> fn);

  /** Filter by variables */
  ProcessInstanceFilter variables(
      final List<ProcessInstanceVariableFilterRequest> variableValueFilters);

  /** Filter by variables map */
  ProcessInstanceFilter variables(final Map<String, String> variableValueFilters);
}
