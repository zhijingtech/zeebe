/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.es.report.process.single.user_task.frequency.groupby.candidate_group;

import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.assertj.core.groups.Tuple;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.optimize.dto.engine.definition.ProcessDefinitionEngineDto;
import org.camunda.optimize.dto.optimize.ReportConstants;
import org.camunda.optimize.dto.optimize.query.report.single.configuration.FlowNodeExecutionState;
import org.camunda.optimize.dto.optimize.query.report.single.filter.data.FilterOperator;
import org.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.filter.ProcessFilterDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.filter.util.ProcessFilterBuilder;
import org.camunda.optimize.dto.optimize.query.report.single.process.view.ProcessViewEntity;
import org.camunda.optimize.dto.optimize.query.report.single.process.view.ProcessViewProperty;
import org.camunda.optimize.dto.optimize.query.report.single.result.ReportMapResultDto;
import org.camunda.optimize.dto.optimize.query.report.single.result.hyper.MapResultEntryDto;
import org.camunda.optimize.dto.optimize.query.sorting.ReportSortingDto;
import org.camunda.optimize.dto.optimize.query.sorting.SortOrder;
import org.camunda.optimize.dto.optimize.rest.report.AuthorizedProcessReportEvaluationResultDto;
import org.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
import org.camunda.optimize.service.es.report.process.AbstractProcessDefinitionIT;
import org.camunda.optimize.service.security.util.LocalDateUtil;
import org.camunda.optimize.test.util.ProcessReportDataType;
import org.camunda.optimize.test.util.TemplatedProcessReportDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.optimize.dto.optimize.query.report.single.filter.data.FilterOperator.IN;
import static org.camunda.optimize.dto.optimize.query.report.single.filter.data.FilterOperator.NOT_IN;
import static org.camunda.optimize.dto.optimize.query.sorting.ReportSortingDto.SORT_BY_KEY;
import static org.camunda.optimize.dto.optimize.query.sorting.ReportSortingDto.SORT_BY_LABEL;
import static org.camunda.optimize.dto.optimize.query.sorting.ReportSortingDto.SORT_BY_VALUE;
import static org.camunda.optimize.test.it.extension.TestEmbeddedCamundaOptimize.DEFAULT_PASSWORD;
import static org.camunda.optimize.test.it.extension.TestEmbeddedCamundaOptimize.DEFAULT_USERNAME;
import static org.camunda.optimize.util.BpmnModels.getDoubleUserTaskDiagram;
import static org.camunda.optimize.util.BpmnModels.getSingleUserTaskDiagram;

public class UserTaskFrequencyByCandidateGroupReportEvaluationIT extends AbstractProcessDefinitionIT {

  private static final String PROCESS_DEFINITION_KEY = "123";

  @BeforeEach
  public void init() {
    engineIntegrationExtension.createGroup(FIRST_CANDIDATE_GROUP);
    engineIntegrationExtension.createGroup(SECOND_CANDIDATE_GROUP);
  }

  @Test
  public void reportEvaluationForOneProcess() {
    // given
    ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
    ProcessInstanceEngineDto processInstanceDto =
      engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    finishTwoUserTasksOneWithFirstAndSecondGroup(processInstanceDto);

    importAllEngineEntitiesFromScratch();

    final ProcessReportDataDto reportData = createReport(processDefinition);

    // when
    final AuthorizedProcessReportEvaluationResultDto<ReportMapResultDto> evaluationResponse =
      reportClient.evaluateMapReport(reportData);

    // then
    final ProcessReportDataDto resultReportDataDto = evaluationResponse.getReportDefinition().getData();
    assertThat(resultReportDataDto.getProcessDefinitionKey()).isEqualTo(processDefinition.getKey());
    assertThat(resultReportDataDto.getDefinitionVersions()).contains(processDefinition.getVersionAsString());
    assertThat(resultReportDataDto.getView()).isNotNull();
    assertThat(resultReportDataDto.getView().getEntity()).isEqualTo(ProcessViewEntity.USER_TASK);
    assertThat(resultReportDataDto.getView().getProperty()).isEqualTo(ProcessViewProperty.FREQUENCY);

    final ReportMapResultDto result = evaluationResponse.getResult();
    assertThat(result.getData()).isNotNull();
    assertThat(result.getData()).hasSize(2);
    assertThat(getExecutedFlowNodeCount(result)).isEqualTo(2L);
    assertThat(result.getEntryForKey(FIRST_CANDIDATE_GROUP).get().getValue()).isEqualTo(1.);
    assertThat(result.getEntryForKey(SECOND_CANDIDATE_GROUP).get().getValue()).isEqualTo(1.);
    assertThat(result.getInstanceCount()).isEqualTo(1L);
  }

  @Test
  public void reportEvaluationForOneProcessWithUnassignedTasks() {
    // given
    ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
    ProcessInstanceEngineDto processInstanceDto =
      engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    finishOneUserTaskOneWithFirstAndLeaveSecondUnassigned(processInstanceDto);

    importAllEngineEntitiesFromScratch();

    final ProcessReportDataDto reportData = createReport(processDefinition);

    // when
    final AuthorizedProcessReportEvaluationResultDto<ReportMapResultDto> evaluationResponse =
      reportClient.evaluateMapReport(reportData);

    // then
    final ProcessReportDataDto resultReportDataDto = evaluationResponse.getReportDefinition().getData();
    assertThat(resultReportDataDto.getProcessDefinitionKey()).isEqualTo(processDefinition.getKey());
    assertThat(resultReportDataDto.getDefinitionVersions()).contains(processDefinition.getVersionAsString());
    assertThat(resultReportDataDto.getView()).isNotNull();
    assertThat(resultReportDataDto.getView().getEntity()).isEqualTo(ProcessViewEntity.USER_TASK);
    assertThat(resultReportDataDto.getView().getProperty()).isEqualTo(ProcessViewProperty.FREQUENCY);

    final ReportMapResultDto result = evaluationResponse.getResult();
    assertThat(result.getData()).isNotNull();
    assertThat(result.getData()).hasSize(2);
    assertThat(getExecutedFlowNodeCount(result)).isEqualTo(2L);
    assertThat(result.getEntryForKey(FIRST_CANDIDATE_GROUP).get().getValue()).isEqualTo(1.);
    assertThat(result.getEntryForKey(getLocalisedUnassignedLabel()).get().getValue()).isEqualTo(1.);
    assertThat(result.getInstanceCount()).isEqualTo(1L);
  }

  @Test
  public void reportEvaluationForMultipleCandidateGroups() {
    // given
    ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
    ProcessInstanceEngineDto processInstanceDto =
      engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    // finish first task
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(FIRST_CANDIDATE_GROUP);
    engineIntegrationExtension.finishAllRunningUserTasks(processInstanceDto.getId());
    // finish second task with
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(FIRST_CANDIDATE_GROUP);
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(SECOND_CANDIDATE_GROUP);
    engineIntegrationExtension.finishAllRunningUserTasks(processInstanceDto.getId());

    importAllEngineEntitiesFromScratch();

    final ProcessReportDataDto reportData = createReport(processDefinition);

    // when
    final AuthorizedProcessReportEvaluationResultDto<ReportMapResultDto> evaluationResponse =
      reportClient.evaluateMapReport(reportData);

    // then
    final ProcessReportDataDto resultReportDataDto = evaluationResponse.getReportDefinition().getData();
    final ReportMapResultDto result = evaluationResponse.getResult();
    assertThat(result.getData()).isNotNull();
    assertThat(result.getData()).hasSize(2);
    assertThat(result.getEntryForKey(FIRST_CANDIDATE_GROUP).get().getValue()).isEqualTo(2.);
    assertThat(result.getEntryForKey(SECOND_CANDIDATE_GROUP).get().getValue()).isEqualTo(1.);
    assertThat(result.getInstanceCount()).isEqualTo(1L);
  }

  @Test
  public void reportEvaluationForSeveralProcesses() {
    // given
    final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
    final ProcessInstanceEngineDto processInstanceDto1 = engineIntegrationExtension.startProcessInstance(
      processDefinition.getId());
    finishTwoUserTasksOneWithFirstAndSecondGroup(processInstanceDto1);

    final ProcessInstanceEngineDto processInstanceDto2 = engineIntegrationExtension.startProcessInstance(
      processDefinition.getId());
    finishOneUserTaskOneWithFirstAndLeaveSecondUnassigned(processInstanceDto2);

    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData = createReport(processDefinition);
    final ReportMapResultDto result = reportClient.evaluateMapReport(reportData).getResult();

    // then
    assertThat(result.getData()).hasSize(3);
    assertThat(getExecutedFlowNodeCount(result)).isEqualTo(3L);
    assertThat(result.getEntryForKey(FIRST_CANDIDATE_GROUP).get().getValue()).isEqualTo(2.);
    assertThat(result.getEntryForKey(SECOND_CANDIDATE_GROUP).get().getValue()).isEqualTo(1.);
    assertThat(result.getEntryForKey(getLocalisedUnassignedLabel()).get().getValue()).isEqualTo(1.);
    assertThat(result.getInstanceCount()).isEqualTo(2L);
  }

  @Test
  public void evaluateReportForMultipleEvents_resultLimitedByConfig() {
    // given
    final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();

    final ProcessInstanceEngineDto processInstanceDto1 = engineIntegrationExtension.startProcessInstance(
      processDefinition.getId());
    finishTwoUserTasksOneWithFirstAndSecondGroup(processInstanceDto1);

    final ProcessInstanceEngineDto processInstanceDto2 = engineIntegrationExtension.startProcessInstance(
      processDefinition.getId());
    finishTwoUserTasksOneWithFirstAndSecondGroup(processInstanceDto2);

    importAllEngineEntitiesFromScratch();

    embeddedOptimizeExtension.getConfigurationService().setEsAggregationBucketLimit(1);

    // when
    final ProcessReportDataDto reportData = createReport(processDefinition);
    final ReportMapResultDto resultDto = reportClient.evaluateMapReport(reportData).getResult();

    // then
    assertThat(resultDto.getInstanceCount()).isEqualTo(2L);
    assertThat(resultDto.getData()).isNotNull();
    assertThat(resultDto.getData()).hasSize(1);
    assertThat(getExecutedFlowNodeCount(resultDto)).isEqualTo(1L);
    assertThat(resultDto.getIsComplete()).isFalse();
  }

  @Test
  public void testCustomOrderOnResultKeyIsApplied() {
    // given
    final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();

    final ProcessInstanceEngineDto processInstanceDto1 = engineIntegrationExtension.startProcessInstance(
      processDefinition.getId());
    finishTwoUserTasksOneWithFirstAndSecondGroup(processInstanceDto1);

    final ProcessInstanceEngineDto processInstanceDto2 = engineIntegrationExtension.startProcessInstance(
      processDefinition.getId());
    finishOneUserTaskOneWithFirstAndLeaveSecondUnassigned(processInstanceDto2);

    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData = createReport(processDefinition);
    reportData.getConfiguration().setSorting(new ReportSortingDto(SORT_BY_KEY, SortOrder.DESC));
    final ReportMapResultDto result = reportClient.evaluateMapReport(reportData).getResult();

    // then
    final List<MapResultEntryDto> resultData = result.getData();
    assertThat(resultData).hasSize(3);
    assertThat(getExecutedFlowNodeCount(result)).isEqualTo(3L);
    final List<String> resultKeys = resultData.stream()
      .map(entry -> entry.getKey().toLowerCase())
      .collect(Collectors.toList());
    // expect descending order ignoring case
    assertThat(resultKeys).isSortedAccordingTo(Comparator.reverseOrder());
  }

  @Test
  public void testCustomOrderOnResultLabelIsApplied() {
    // given
    final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();

    final ProcessInstanceEngineDto processInstanceDto1 = engineIntegrationExtension.startProcessInstance(
      processDefinition.getId());
    finishTwoUserTasksOneWithFirstAndSecondGroup(processInstanceDto1);

    final ProcessInstanceEngineDto processInstanceDto2 = engineIntegrationExtension.startProcessInstance(
      processDefinition.getId());
    finishOneUserTaskOneWithFirstAndLeaveSecondUnassigned(processInstanceDto2);

    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData = createReport(processDefinition);
    reportData.getConfiguration().setSorting(new ReportSortingDto(SORT_BY_LABEL, SortOrder.DESC));
    final ReportMapResultDto result = reportClient.evaluateMapReport(reportData).getResult();

    // then
    final List<MapResultEntryDto> resultData = result.getData();
    assertThat(resultData).hasSize(3);
    assertThat(getExecutedFlowNodeCount(result)).isEqualTo(3L);
    final List<String> resultLabels = resultData.stream()
      .map(entry -> entry.getLabel().toLowerCase())
      .collect(Collectors.toList());
    // expect descending order ignoring case
    assertThat(resultLabels).isSortedAccordingTo(Comparator.reverseOrder());
  }

  @Test
  public void testCustomOrderOnResultValueIsApplied() {
    // given
    final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();

    final ProcessInstanceEngineDto processInstanceDto1 = engineIntegrationExtension.startProcessInstance(
      processDefinition.getId());
    finishTwoUserTasksOneWithFirstAndSecondGroup(processInstanceDto1);

    final ProcessInstanceEngineDto processInstanceDto2 = engineIntegrationExtension.startProcessInstance(
      processDefinition.getId());
    finishOneUserTaskOneWithFirstAndLeaveSecondUnassigned(processInstanceDto2);

    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData = createReport(processDefinition);
    reportData.getConfiguration().setSorting(new ReportSortingDto(SORT_BY_VALUE, SortOrder.ASC));
    final ReportMapResultDto result = reportClient.evaluateMapReport(reportData).getResult();

    // then
    assertThat(result.getData()).hasSize(3);
    assertThat(getExecutedFlowNodeCount(result)).isEqualTo(3L);
    assertCorrectValueOrdering(result);
  }

  @Test
  public void otherProcessDefinitionsDoNotInfluenceResult() {
    // given
    final ProcessDefinitionEngineDto processDefinition1 = deployTwoUserTasksDefinition();
    final ProcessInstanceEngineDto processInstanceDto1 = engineIntegrationExtension.startProcessInstance(
      processDefinition1.getId());
    finishTwoUserTasksOneWithFirstAndSecondGroup(processInstanceDto1);
    final ProcessInstanceEngineDto processInstanceDto2 = engineIntegrationExtension.startProcessInstance(
      processDefinition1.getId());
    finishTwoUserTasksOneWithFirstAndSecondGroup(processInstanceDto2);

    final ProcessDefinitionEngineDto processDefinition2 = deployTwoUserTasksDefinition();
    final ProcessInstanceEngineDto processInstanceDto3 = engineIntegrationExtension.startProcessInstance(
      processDefinition2.getId());
    finishOneUserTaskOneWithFirstAndLeaveSecondUnassigned(processInstanceDto3);

    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData1 = createReport(processDefinition1);
    final ReportMapResultDto result1 = reportClient.evaluateMapReport(reportData1).getResult();
    final ProcessReportDataDto reportData2 = createReport(processDefinition2);
    final ReportMapResultDto result2 = reportClient.evaluateMapReport(reportData2).getResult();

    // then
    assertThat(result1.getData()).hasSize(2);
    assertThat(getExecutedFlowNodeCount(result1)).isEqualTo(2L);
    assertThat(result1.getEntryForKey(FIRST_CANDIDATE_GROUP).get().getValue()).isEqualTo(2.);
    assertThat(result1.getEntryForKey(SECOND_CANDIDATE_GROUP).get().getValue()).isEqualTo(2.);

    assertThat(result2.getData()).hasSize(2);
    assertThat(getExecutedFlowNodeCount(result2)).isEqualTo(2L);
    assertThat(result2.getEntryForKey(FIRST_CANDIDATE_GROUP).get().getValue()).isEqualTo(1.);
    assertThat(result2.getEntryForKey(getLocalisedUnassignedLabel()).get().getValue()).isEqualTo(1.);
  }

  @Test
  public void reportEvaluationSingleBucketFilteredBySingleTenant() {
    // given
    final String tenantId1 = "tenantId1";
    final String tenantId2 = "tenantId2";
    final List<String> selectedTenants = Collections.singletonList(tenantId1);
    final String processKey = deployAndStartMultiTenantUserTaskProcess(
      Arrays.asList(null, tenantId1, tenantId2)
    );

    importAllEngineEntitiesFromScratch();

    // when
    ProcessReportDataDto reportData = createReport(processKey, ReportConstants.ALL_VERSIONS);
    reportData.setTenantIds(selectedTenants);
    ReportMapResultDto result = reportClient.evaluateMapReport(reportData).getResult();

    // then
    assertThat(result.getInstanceCount()).isEqualTo((long) selectedTenants.size());
  }

  @Test
  public void noUserTaskMatchesReturnsEmptyResult() {
    // when
    final ProcessReportDataDto reportData = createReport(
      "nonExistingProcessDefinitionId", "1"
    );
    final ReportMapResultDto result = reportClient.evaluateMapReport(reportData).getResult();

    // then
    assertThat(result.getData()).isEmpty();
  }

  public static Stream<Arguments> assigneeFilterScenarios() {
    return Stream.of(
      Arguments.of(IN, new String[]{SECOND_USER}, Lists.newArrayList(Tuple.tuple(SECOND_CANDIDATE_GROUP, 1.))),
      Arguments.of(
        IN,
        new String[]{DEFAULT_USERNAME, SECOND_USER},
        Lists.newArrayList(Tuple.tuple(FIRST_CANDIDATE_GROUP, 1.), Tuple.tuple(SECOND_CANDIDATE_GROUP, 1.))
      ),
      Arguments.of(NOT_IN, new String[]{SECOND_USER}, Lists.newArrayList(Tuple.tuple(FIRST_CANDIDATE_GROUP, 1.))),
      Arguments.of(NOT_IN, new String[]{DEFAULT_USERNAME, SECOND_USER}, Lists.newArrayList())
    );
  }

  @ParameterizedTest
  @MethodSource("assigneeFilterScenarios")
  public void filterByAssigneeOnlyCountsCandidateGroupsFromThoseUserTasks(final FilterOperator filterOperator,
                                                                          final String[] filterValues,
                                                                          final List<Tuple> expectedResult) {
    // given
    engineIntegrationExtension.addUser(SECOND_USER, SECOND_USERS_PASSWORD);
    engineIntegrationExtension.grantAllAuthorizations(SECOND_USER);
    final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
    final ProcessInstanceEngineDto processInstanceDto = engineIntegrationExtension
      .startProcessInstance(processDefinition.getId());
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(FIRST_CANDIDATE_GROUP);
    engineIntegrationExtension.finishAllRunningUserTasks(
      DEFAULT_USERNAME, DEFAULT_PASSWORD, processInstanceDto.getId()
    );
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(SECOND_CANDIDATE_GROUP);
    engineIntegrationExtension.finishAllRunningUserTasks(
      SECOND_USER, SECOND_USERS_PASSWORD, processInstanceDto.getId()
    );

    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData = createReport(processDefinition);
    final List<ProcessFilterDto<?>> inclusiveAssigneeFilter = ProcessFilterBuilder
      .filter().assignee().ids(filterValues).operator(filterOperator).add().buildList();
    reportData.setFilter(inclusiveAssigneeFilter);
    final ReportMapResultDto result = reportClient.evaluateMapReport(reportData).getResult();

    // then
    assertThat(result.getData())
      .extracting(MapResultEntryDto::getKey, MapResultEntryDto::getValue)
      .containsExactlyInAnyOrderElementsOf(expectedResult);
  }

  public static Stream<Arguments> candidateGroupFilterScenarios() {
    return Stream.of(
      Arguments.of(
        IN, new String[]{SECOND_CANDIDATE_GROUP}, Lists.newArrayList(Tuple.tuple(SECOND_CANDIDATE_GROUP, 1.))
      ),
      Arguments.of(
        IN,
        new String[]{FIRST_CANDIDATE_GROUP, SECOND_CANDIDATE_GROUP},
        Lists.newArrayList(Tuple.tuple(FIRST_CANDIDATE_GROUP, 1.), Tuple.tuple(SECOND_CANDIDATE_GROUP, 1.))
      ),
      Arguments.of(
        NOT_IN, new String[]{SECOND_CANDIDATE_GROUP}, Lists.newArrayList(Tuple.tuple(FIRST_CANDIDATE_GROUP, 1.))
      ),
      Arguments.of(NOT_IN, new String[]{FIRST_CANDIDATE_GROUP, SECOND_CANDIDATE_GROUP}, Lists.newArrayList())
    );
  }

  @ParameterizedTest
  @MethodSource("candidateGroupFilterScenarios")
  public void filterByCandidateGroupOnlyCountsThoseCandidateGroups(final FilterOperator filterOperator,
                                                                   final String[] filterValues,
                                                                   final List<Tuple> expectedResult) {
    // given
    final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
    final ProcessInstanceEngineDto processInstanceDto = engineIntegrationExtension
      .startProcessInstance(processDefinition.getId());
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(FIRST_CANDIDATE_GROUP);
    engineIntegrationExtension.finishAllRunningUserTasks(processInstanceDto.getId());
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(SECOND_CANDIDATE_GROUP);
    engineIntegrationExtension.finishAllRunningUserTasks(processInstanceDto.getId());

    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData = createReport(processDefinition);
    final List<ProcessFilterDto<?>> inclusiveAssigneeFilter = ProcessFilterBuilder
      .filter().candidateGroups().ids(filterValues).operator(filterOperator).add().buildList();
    reportData.setFilter(inclusiveAssigneeFilter);
    final ReportMapResultDto result = reportClient.evaluateMapReport(reportData).getResult();

    // then
    assertThat(result.getData())
      .extracting(MapResultEntryDto::getKey, MapResultEntryDto::getValue)
      .containsExactlyInAnyOrderElementsOf(expectedResult);
  }

  @Data
  @AllArgsConstructor
  static class ExecutionStateTestValues {
    FlowNodeExecutionState executionState;
    Map<String, Double> expectedFrequencyValues;
  }

  private static Map<String, Double> getExpectedResultsMap(Double userTask1Results, Double userTask2Results) {
    Map<String, Double> result = new HashMap<>();
    result.put(FIRST_CANDIDATE_GROUP, userTask1Results);
    result.put(SECOND_CANDIDATE_GROUP, userTask2Results);
    return result;
  }

  protected static Stream<ExecutionStateTestValues> getExecutionStateExpectedValues() {
    return Stream.of(
      new ExecutionStateTestValues(FlowNodeExecutionState.RUNNING, getExpectedResultsMap(2., 1.)),
      new ExecutionStateTestValues(FlowNodeExecutionState.COMPLETED, getExpectedResultsMap(1., null)),
      new ExecutionStateTestValues(FlowNodeExecutionState.ALL, getExpectedResultsMap(3., 1.))
    );
  }

  @ParameterizedTest
  @MethodSource("getExecutionStateExpectedValues")
  public void evaluateReportWithExecutionState(ExecutionStateTestValues executionStateTestValues) {
    // given
    OffsetDateTime now = OffsetDateTime.now();
    LocalDateUtil.setCurrentTime(now);

    final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
    final ProcessInstanceEngineDto processInstanceDto = engineIntegrationExtension.startProcessInstance(
      processDefinition.getId());
    // finish first running task, second now runs but unclaimed
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(FIRST_CANDIDATE_GROUP);
    engineIntegrationExtension.finishAllRunningUserTasks(processInstanceDto.getId());
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(SECOND_CANDIDATE_GROUP);

    final ProcessInstanceEngineDto processInstanceDto2 = engineIntegrationExtension.startProcessInstance(
      processDefinition.getId());
    // claim first running task
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(FIRST_CANDIDATE_GROUP);
    engineIntegrationExtension.claimAllRunningUserTasks(processInstanceDto2.getId());

    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData = createReport(processDefinition);
    reportData.getConfiguration().setFlowNodeExecutionState(executionStateTestValues.executionState);
    final ReportMapResultDto result = reportClient.evaluateMapReport(reportData).getResult();

    // then
    assertThat((long) result.getData().size()).isEqualTo(
      executionStateTestValues.getExpectedFrequencyValues().values().stream().filter(Objects::nonNull).count());
    assertThat(result.getEntryForKey(FIRST_CANDIDATE_GROUP).get().getValue()).isEqualTo(
      executionStateTestValues.getExpectedFrequencyValues().get(FIRST_CANDIDATE_GROUP));
    assertThat(result.getEntryForKey(SECOND_CANDIDATE_GROUP).map(MapResultEntryDto::getValue).orElse(null))
      .isEqualTo(executionStateTestValues.getExpectedFrequencyValues().get(SECOND_CANDIDATE_GROUP));
    assertThat(result.getInstanceCount()).isEqualTo(2L);
  }

  @Test
  public void evaluateReportWithExecutionStateCanceled() {
    // given
    OffsetDateTime now = OffsetDateTime.now();
    LocalDateUtil.setCurrentTime(now);

    final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
    final ProcessInstanceEngineDto firstInstance = engineIntegrationExtension.startProcessInstance(
      processDefinition.getId());
    // finish first running task, second now runs unclaimed and canceled
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(FIRST_CANDIDATE_GROUP);
    engineIntegrationExtension.finishAllRunningUserTasks(firstInstance.getId());
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(SECOND_CANDIDATE_GROUP);
    engineIntegrationExtension.cancelActivityInstance(firstInstance.getId(), USER_TASK_2);

    final ProcessInstanceEngineDto secondInstance = engineIntegrationExtension.startProcessInstance(
      processDefinition.getId());
    // claim and cancel first running task
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(FIRST_CANDIDATE_GROUP);
    engineIntegrationExtension.claimAllRunningUserTasks(secondInstance.getId());
    engineIntegrationExtension.cancelActivityInstance(secondInstance.getId(), USER_TASK_1);

    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData = createReport(processDefinition);
    reportData.getConfiguration().setFlowNodeExecutionState(FlowNodeExecutionState.CANCELED);
    final ReportMapResultDto result = reportClient.evaluateMapReport(reportData).getResult();

    // then
    assertThat(result.getData()).hasSize(2);
    assertThat(result.getEntryForKey(FIRST_CANDIDATE_GROUP)).isPresent().get()
      .extracting(MapResultEntryDto::getValue).isEqualTo(1.);
    assertThat(result.getEntryForKey(SECOND_CANDIDATE_GROUP)).isPresent().get()
      .extracting(MapResultEntryDto::getValue).isEqualTo(1.);
    assertThat(result.getInstanceCount()).isEqualTo(2L);
    assertThat(result.getInstanceCountWithoutFilters()).isEqualTo(2L);
  }

  @Test
  public void processDefinitionContainsMultiInstanceBody() {
    // given
    BpmnModelInstance processWithMultiInstanceUserTask = Bpmn
      // @formatter:off
        .createExecutableProcess("processWithMultiInstanceUserTask")
        .startEvent()
          .userTask(USER_TASK_1).multiInstance().cardinality("2").multiInstanceDone()
        .endEvent()
        .done();
    // @formatter:on

    final ProcessDefinitionEngineDto processDefinition =
      engineIntegrationExtension.deployProcessAndGetProcessDefinition(
        processWithMultiInstanceUserTask
      );
    final ProcessInstanceEngineDto processInstanceDto = engineIntegrationExtension.startProcessInstance(
      processDefinition.getId());
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(FIRST_CANDIDATE_GROUP);
    engineIntegrationExtension.finishAllRunningUserTasks(processInstanceDto.getId());

    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData = createReport(processDefinition);
    final ReportMapResultDto result = reportClient.evaluateMapReport(reportData).getResult();

    // then
    assertThat(result.getData()).hasSize(1);
    assertThat(getExecutedFlowNodeCount(result)).isEqualTo(1L);
    assertThat(result.getEntryForKey(FIRST_CANDIDATE_GROUP).get().getValue()).isEqualTo(2.);
  }

  @Test
  public void evaluateReportForMoreThanTenEvents() {
    // given
    final ProcessDefinitionEngineDto processDefinition = deployOneUserTasksDefinition();

    for (int i = 0; i < 11; i++) {
      final ProcessInstanceEngineDto processInstanceDto = engineIntegrationExtension.startProcessInstance(
        processDefinition.getId());
      engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(FIRST_CANDIDATE_GROUP);
      engineIntegrationExtension.finishAllRunningUserTasks(processInstanceDto.getId());
    }

    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData = createReport(processDefinition);
    final ReportMapResultDto result = reportClient.evaluateMapReport(reportData).getResult();

    // then
    assertThat(result.getData()).hasSize(1);
    assertThat(getExecutedFlowNodeCount(result)).isEqualTo(1L);
    assertThat(result.getEntryForKey(FIRST_CANDIDATE_GROUP).get().getValue()).isEqualTo(11.);
  }

  @Test
  public void filterInReport() {
    // given
    final ProcessDefinitionEngineDto processDefinition = deployOneUserTasksDefinition();
    final ProcessInstanceEngineDto processInstanceDto = engineIntegrationExtension.startProcessInstance(
      processDefinition.getId());
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(FIRST_CANDIDATE_GROUP);

    final OffsetDateTime processStartTime = engineIntegrationExtension.getHistoricProcessInstance(processInstanceDto
                                                                                                    .getId())
      .getStartTime();

    importAllEngineEntitiesFromScratch();

    // when
    ProcessReportDataDto reportData = createReport(processDefinition);
    reportData.setFilter(createStartDateFilter(null, processStartTime.minusSeconds(1L)));
    ReportMapResultDto result = reportClient.evaluateMapReport(reportData).getResult();

    // then
    assertThat(result.getData()).isNotNull();
    assertThat(result.getData()).isEmpty();
    assertThat(getExecutedFlowNodeCount(result)).isEqualTo(0L);

    // when
    reportData = createReport(processDefinition);
    reportData.setFilter(createStartDateFilter(processStartTime, null));
    result = reportClient.evaluateMapReport(reportData).getResult();

    // then
    assertThat(result.getData()).isNotNull();
    assertThat(result.getData()).hasSize(1);
    assertThat(getExecutedFlowNodeCount(result)).isEqualTo(1L);
    assertThat(result.getEntryForKey(FIRST_CANDIDATE_GROUP).get().getValue()).isEqualTo(1.);
  }

  @Test
  public void optimizeExceptionOnViewEntityIsNull() {
    // given
    final ProcessReportDataDto dataDto = createReport(PROCESS_DEFINITION_KEY, "1");
    dataDto.getView().setEntity(null);

    // when
    final Response response = reportClient.evaluateReportAndReturnResponse(dataDto);

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  @Test
  public void optimizeExceptionOnViewPropertyIsNull() {
    // given
    final ProcessReportDataDto dataDto = createReport(PROCESS_DEFINITION_KEY, "1");
    dataDto.getView().setProperty(null);

    // when
    final Response response = reportClient.evaluateReportAndReturnResponse(dataDto);

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  @Test
  public void optimizeExceptionOnGroupByTypeIsNull() {
    // given
    final ProcessReportDataDto dataDto = createReport(PROCESS_DEFINITION_KEY, "1");
    dataDto.getGroupBy().setType(null);

    // when
    final Response response = reportClient.evaluateReportAndReturnResponse(dataDto);

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  protected ProcessReportDataDto createReport(final String processDefinitionKey, final String version) {
    return TemplatedProcessReportDataBuilder
      .createReportData()
      .setProcessDefinitionKey(processDefinitionKey)
      .setProcessDefinitionVersion(version)
      .setReportDataType(ProcessReportDataType.USER_TASK_FREQUENCY_GROUP_BY_CANDIDATE)
      .build();
  }

  private ProcessReportDataDto createReport(final ProcessDefinitionEngineDto processDefinition) {
    return createReport(processDefinition.getKey(), String.valueOf(processDefinition.getVersion()));
  }

  private List<ProcessFilterDto<?>> createStartDateFilter(OffsetDateTime startDate, OffsetDateTime endDate) {
    return ProcessFilterBuilder.filter().fixedStartDate().start(startDate).end(endDate).add().buildList();
  }

  private void finishTwoUserTasksOneWithFirstAndSecondGroup(final ProcessInstanceEngineDto processInstanceDto) {
    // finish first task
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(FIRST_CANDIDATE_GROUP);
    engineIntegrationExtension.finishAllRunningUserTasks(processInstanceDto.getId());
    // finish second task with
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(SECOND_CANDIDATE_GROUP);
    engineIntegrationExtension.finishAllRunningUserTasks(processInstanceDto.getId());
  }

  private void finishOneUserTaskOneWithFirstAndLeaveSecondUnassigned(final ProcessInstanceEngineDto processInstanceDto) {
    // finish first task
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(FIRST_CANDIDATE_GROUP);
    engineIntegrationExtension.finishAllRunningUserTasks(processInstanceDto.getId());
  }

  private String deployAndStartMultiTenantUserTaskProcess(final List<String> deployedTenants) {
    final String processKey = "multiTenantProcess";
    deployedTenants.stream()
      .filter(Objects::nonNull)
      .forEach(tenantId -> engineIntegrationExtension.createTenant(tenantId));
    deployedTenants
      .forEach(tenant -> {
        final ProcessDefinitionEngineDto processDefinitionEngineDto = deployOneUserTasksDefinition(processKey, tenant);
        engineIntegrationExtension.startProcessInstance(processDefinitionEngineDto.getId());
      });

    return processKey;
  }

  private ProcessDefinitionEngineDto deployOneUserTasksDefinition() {
    return deployOneUserTasksDefinition("aProcess", null);
  }

  private ProcessDefinitionEngineDto deployOneUserTasksDefinition(String key, String tenantId) {
    return engineIntegrationExtension.deployProcessAndGetProcessDefinition(getSingleUserTaskDiagram(key), tenantId);
  }

  private ProcessDefinitionEngineDto deployTwoUserTasksDefinition() {
    return engineIntegrationExtension.deployProcessAndGetProcessDefinition(getDoubleUserTaskDiagram());
  }

  private long getExecutedFlowNodeCount(ReportMapResultDto resultList) {
    return resultList.getData()
      .stream()
      .map(MapResultEntryDto::getValue)
      .filter(Objects::nonNull)
      .count();
  }

  private void assertCorrectValueOrdering(ReportMapResultDto result) {
    List<MapResultEntryDto> resultData = result.getData();
    final List<Double> bucketValues = resultData.stream()
      .map(MapResultEntryDto::getValue)
      .collect(Collectors.toList());
    final List<Double> bucketValuesWithoutNullValue = bucketValues.stream()
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
    assertThat(bucketValuesWithoutNullValue).isSortedAccordingTo(Comparator.naturalOrder());
    for (int i = resultData.size() - 1; i > getExecutedFlowNodeCount(result) - 1; i--) {
      assertThat(bucketValues.get(i)).isNull();
    }
  }

  private String getLocalisedUnassignedLabel() {
    return embeddedOptimizeExtension.getLocalizationService()
      .getDefaultLocaleMessageForMissingAssigneeLabel();
  }

}
