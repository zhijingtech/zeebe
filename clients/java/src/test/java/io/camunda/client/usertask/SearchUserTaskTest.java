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
package io.camunda.client.usertask;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.camunda.client.protocol.rest.IntegerFilterProperty;
import io.camunda.client.protocol.rest.UserTaskFilterRequest;
import io.camunda.client.protocol.rest.UserTaskSearchQueryRequest;
import io.camunda.client.protocol.rest.UserTaskVariableFilterRequest;
import io.camunda.client.util.ClientRestTest;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

public final class SearchUserTaskTest extends ClientRestTest {

  @Test
  void shouldSearchUserTask() {
    // when
    client.newUserTaskQuery().send().join();

    // then
    final UserTaskSearchQueryRequest request =
        gatewayService.getLastRequest(UserTaskSearchQueryRequest.class);
    assertThat(request.getFilter()).isNull();
  }

  @Test
  void shouldSearchUserTaskByAssignee() {
    // when
    client.newUserTaskQuery().filter(f -> f.assignee("demo")).send().join();

    // then
    final UserTaskSearchQueryRequest request =
        gatewayService.getLastRequest(UserTaskSearchQueryRequest.class);
    assertThat(request.getFilter().getAssignee().get$Eq()).isEqualTo("demo");
  }

  @Test
  void shouldSearchUserTaskByAssigneeStringFilter() {
    // when
    client.newUserTaskQuery().filter(f -> f.assignee(b -> b.neq("that"))).send().join();

    // then
    final UserTaskSearchQueryRequest request =
        gatewayService.getLastRequest(UserTaskSearchQueryRequest.class);
    assertThat(request.getFilter().getAssignee().get$Neq()).isEqualTo("that");
  }

  @Test
  void shouldSearchUserTaskByState() {
    // when
    client.newUserTaskQuery().filter(f -> f.state("COMPLETED")).send().join();

    // then
    final UserTaskSearchQueryRequest request =
        gatewayService.getLastRequest(UserTaskSearchQueryRequest.class);
    assertThat(request.getFilter().getState()).isEqualTo(UserTaskFilterRequest.StateEnum.COMPLETED);
  }

  @Test
  void shouldSearchUserTaskByKey() {
    // when
    client.newUserTaskQuery().filter(f -> f.userTaskKey(12345L)).send().join();

    // then
    final UserTaskSearchQueryRequest request =
        gatewayService.getLastRequest(UserTaskSearchQueryRequest.class);
    assertThat(request.getFilter().getUserTaskKey()).isEqualTo(12345L);
  }

  @Test
  void shouldSearchUserTaskByTaskDefinitionId() {
    // when
    client.newUserTaskQuery().filter(f -> f.elementId("task-def-id")).send().join();

    // then
    final UserTaskSearchQueryRequest request =
        gatewayService.getLastRequest(UserTaskSearchQueryRequest.class);
    assertThat(request.getFilter().getElementId()).isEqualTo("task-def-id");
  }

  @Test
  void shouldSearchUserTaskByCandidateGroup() {
    // when
    client.newUserTaskQuery().filter(f -> f.candidateGroup("group1")).send().join();

    // then
    final UserTaskSearchQueryRequest request =
        gatewayService.getLastRequest(UserTaskSearchQueryRequest.class);
    assertThat(request.getFilter().getCandidateGroup().get$Eq()).isEqualTo("group1");
  }

  @Test
  void shouldSearchUserTaskByCandidateUser() {
    // when
    client.newUserTaskQuery().filter(f -> f.candidateUser("user1")).send().join();

    // then
    final UserTaskSearchQueryRequest request =
        gatewayService.getLastRequest(UserTaskSearchQueryRequest.class);
    assertThat(request.getFilter().getCandidateUser().get$Eq()).isEqualTo("user1");
  }

  @Test
  void shouldSearchUserTaskByProcessDefinitionKey() {
    // when
    client.newUserTaskQuery().filter(f -> f.processDefinitionKey(123L)).send().join();

    // then
    final UserTaskSearchQueryRequest request =
        gatewayService.getLastRequest(UserTaskSearchQueryRequest.class);
    assertThat(request.getFilter().getProcessDefinitionKey()).isEqualTo(123L);
  }

  @Test
  void shouldSearchUserTaskByProcessInstanceKey() {
    // when
    client.newUserTaskQuery().filter(f -> f.processInstanceKey(456L)).send().join();

    // then
    final UserTaskSearchQueryRequest request =
        gatewayService.getLastRequest(UserTaskSearchQueryRequest.class);
    assertThat(request.getFilter().getProcessInstanceKey()).isEqualTo(456L);
  }

  @Test
  void shouldSearchUserTaskByTenantId() {
    // when
    client.newUserTaskQuery().filter(f -> f.tenantId("tenant1")).send().join();

    // then
    final UserTaskSearchQueryRequest request =
        gatewayService.getLastRequest(UserTaskSearchQueryRequest.class);
    assertThat(request.getFilter().getTenantId()).isEqualTo("tenant1");
  }

  @Test
  void shouldSearchUserTaskByPriority() {
    // when
    client.newUserTaskQuery().filter(f -> f.priority(10)).send().join();

    // then
    final UserTaskSearchQueryRequest request =
        gatewayService.getLastRequest(UserTaskSearchQueryRequest.class);
    assertThat(request.getFilter().getPriority().get$Eq()).isEqualTo(10);
  }

  @Test
  void shouldSearchUserTaskByPriorityLongFilter() {
    // when
    client.newUserTaskQuery().filter(f -> f.priority(b -> b.gt(1).lt(10))).send().join();

    // then
    final UserTaskSearchQueryRequest request =
        gatewayService.getLastRequest(UserTaskSearchQueryRequest.class);
    final UserTaskFilterRequest filter = request.getFilter();
    assertThat(filter).isNotNull();
    final IntegerFilterProperty priority = filter.getPriority();
    assertThat(priority).isNotNull();
    assertThat(priority.get$Gt()).isEqualTo(1);
    assertThat(priority.get$Lt()).isEqualTo(10);
  }

  @Test
  void shouldSearchUserTaskByProcessInstanceVariable() {
    // when
    final UserTaskVariableFilterRequest userTaskVariableFilterRequest =
        new UserTaskVariableFilterRequest().name("test").value("test");
    final ArrayList<UserTaskVariableFilterRequest> listFilter = new ArrayList<>();

    listFilter.add(userTaskVariableFilterRequest);

    client.newUserTaskQuery().filter(f -> f.processInstanceVariables(listFilter)).send().join();

    // then
    final UserTaskSearchQueryRequest request =
        gatewayService.getLastRequest(UserTaskSearchQueryRequest.class);
    assertThat(request.getFilter().getProcessInstanceVariables()).isEqualTo(listFilter);
  }

  @Test
  void shouldSearchUserTaskByLocalVariable() {
    // when
    final UserTaskVariableFilterRequest userTaskVariableFilterRequest =
        new UserTaskVariableFilterRequest().name("test").value("test");
    final ArrayList<UserTaskVariableFilterRequest> listFilter = new ArrayList<>();

    listFilter.add(userTaskVariableFilterRequest);

    client.newUserTaskQuery().filter(f -> f.localVariables(listFilter)).send().join();

    // then
    final UserTaskSearchQueryRequest request =
        gatewayService.getLastRequest(UserTaskSearchQueryRequest.class);
    assertThat(request.getFilter().getLocalVariables()).isEqualTo(listFilter);
  }

  @Test
  void shouldReturnFormByUserTaskKey() {
    // when
    final long userTaskKey = 1L;
    client.newUserTaskGetFormRequest(userTaskKey).send().join();

    // then
    final LoggedRequest request = gatewayService.getLastRequest();
    assertThat(request.getUrl()).isEqualTo("/v2/user-tasks/" + userTaskKey + "/form");
    assertThat(request.getMethod()).isEqualTo(RequestMethod.GET);
  }
}
