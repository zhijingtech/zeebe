/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.es.report.process.processinstance.duration.groupby.none;

import com.google.common.collect.Lists;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.optimize.dto.optimize.ReportConstants;
import org.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.SingleProcessReportDefinitionDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.filter.util.ProcessFilterBuilder;
import org.camunda.optimize.dto.optimize.query.report.single.process.group.ProcessGroupByType;
import org.camunda.optimize.dto.optimize.query.report.single.process.result.duration.AggregationResultDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.result.duration.ProcessDurationReportNumberResultDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.view.ProcessViewEntity;
import org.camunda.optimize.dto.optimize.query.report.single.process.view.ProcessViewProperty;
import org.camunda.optimize.dto.optimize.rest.report.ProcessReportEvaluationResultDto;
import org.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
import org.camunda.optimize.service.es.report.process.AbstractProcessDefinitionIT;
import org.camunda.optimize.test.util.ProcessReportDataBuilder;
import org.camunda.optimize.test.util.ProcessReportDataBuilderHelper;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.camunda.optimize.test.util.ProcessReportDataType.PROC_INST_DUR_GROUP_BY_NONE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

public class ProcessInstanceDurationByNoneReportEvaluationIT extends AbstractProcessDefinitionIT {

  public static final String PROCESS_DEFINITION_KEY = "123";
  private static final String TEST_ACTIVITY = "testActivity";

  @Test
  public void reportEvaluationForOneProcess() throws Exception {

    // given
    OffsetDateTime startDate = OffsetDateTime.now();
    OffsetDateTime endDate = startDate.plusSeconds(1);
    ProcessInstanceEngineDto processInstanceDto = deployAndStartSimpleServiceTaskProcess();
    engineDatabaseRule.changeProcessInstanceStartDate(processInstanceDto.getId(), startDate);
    engineDatabaseRule.changeProcessInstanceEndDate(processInstanceDto.getId(), endDate);
    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshAllOptimizeIndices();

    // when
    ProcessReportDataDto reportData = ProcessReportDataBuilder
      .createReportData()
      .setProcessDefinitionKey(processInstanceDto.getProcessDefinitionKey())
      .setProcessDefinitionVersion(processInstanceDto.getProcessDefinitionVersion())
      .setReportDataType(PROC_INST_DUR_GROUP_BY_NONE)
      .build();

    ProcessReportEvaluationResultDto<ProcessDurationReportNumberResultDto> evaluationResponse =
      evaluateDurationNumberReport(reportData);

    // then
    ProcessReportDataDto resultReportDataDto = evaluationResponse.getReportDefinition().getData();
    assertThat(resultReportDataDto.getProcessDefinitionKey(), is(processInstanceDto.getProcessDefinitionKey()));
    assertThat(resultReportDataDto.getProcessDefinitionVersion(), is(processInstanceDto.getProcessDefinitionVersion()));
    assertThat(resultReportDataDto.getView(), is(notNullValue()));
    assertThat(resultReportDataDto.getView().getEntity(), is(ProcessViewEntity.PROCESS_INSTANCE));
    assertThat(resultReportDataDto.getView().getProperty(), is(ProcessViewProperty.DURATION));
    assertThat(resultReportDataDto.getGroupBy().getType(), is(ProcessGroupByType.NONE));
    assertThat(resultReportDataDto.getParameters().getProcessPart(), is(nullValue()));

    assertThat(evaluationResponse.getResult().getProcessInstanceCount(), is(1L));
    AggregationResultDto calculatedResult = evaluationResponse.getResult().getData();
    assertThat(calculatedResult, is(notNullValue()));
    assertThat(calculatedResult.getAvg(), is(1000L));
    assertThat(calculatedResult.getMax(), is(1000L));
    assertThat(calculatedResult.getMin(), is(1000L));
    assertThat(calculatedResult.getMedian(), is(1000L));
  }

  @Test
  public void reportEvaluationById() throws Exception {
    // given
    OffsetDateTime startDate = OffsetDateTime.now();
    ProcessInstanceEngineDto processInstanceDto = deployAndStartSimpleServiceTaskProcess();

    engineDatabaseRule.changeProcessInstanceStartDate(processInstanceDto.getId(), startDate);
    engineDatabaseRule.changeProcessInstanceEndDate(processInstanceDto.getId(), startDate.plusSeconds(1));
    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshAllOptimizeIndices();
    ProcessReportDataDto reportDataDto = ProcessReportDataBuilder
      .createReportData()
      .setProcessDefinitionKey(processInstanceDto.getProcessDefinitionKey())
      .setProcessDefinitionVersion(processInstanceDto.getProcessDefinitionVersion())
      .setReportDataType(PROC_INST_DUR_GROUP_BY_NONE)
      .build();

    String reportId = createAndStoreDefaultReportDefinition(reportDataDto);

    // when
    ProcessReportEvaluationResultDto<ProcessDurationReportNumberResultDto> evaluationResponse =
      evaluateDurationNumberReportById(reportId);

    // then
    ProcessReportDataDto resultReportDataDto = evaluationResponse.getReportDefinition().getData();
    assertThat(resultReportDataDto.getProcessDefinitionKey(), is(processInstanceDto.getProcessDefinitionKey()));
    assertThat(resultReportDataDto.getProcessDefinitionVersion(), is(processInstanceDto.getProcessDefinitionVersion()));
    assertThat(resultReportDataDto.getView(), is(notNullValue()));
    assertThat(resultReportDataDto.getView().getEntity(), is(ProcessViewEntity.PROCESS_INSTANCE));
    assertThat(resultReportDataDto.getView().getProperty(), is(ProcessViewProperty.DURATION));
    assertThat(resultReportDataDto.getGroupBy().getType(), is(ProcessGroupByType.NONE));

    AggregationResultDto calculatedResult = evaluationResponse.getResult().getData();
    assertThat(calculatedResult, is(notNullValue()));
    assertThat(calculatedResult.getAvg(), is(1000L));
    assertThat(calculatedResult.getMax(), is(1000L));
    assertThat(calculatedResult.getMin(), is(1000L));
    assertThat(calculatedResult.getMedian(), is(1000L));
  }

  @Test
  public void evaluateReportForMultipleEvents() throws Exception {
    // given
    OffsetDateTime startDate = OffsetDateTime.now();
    ProcessInstanceEngineDto processInstanceDto = deployAndStartSimpleServiceTaskProcess();
    ProcessInstanceEngineDto processInstanceDto2 =
      engineRule.startProcessInstance(processInstanceDto.getDefinitionId());
    ProcessInstanceEngineDto processInstanceDto3 =
      engineRule.startProcessInstance(processInstanceDto.getDefinitionId());
    Map<String, OffsetDateTime> startDatesToUpdate = new HashMap<>();
    startDatesToUpdate.put(processInstanceDto.getId(), startDate);
    startDatesToUpdate.put(processInstanceDto2.getId(), startDate);
    startDatesToUpdate.put(processInstanceDto3.getId(), startDate);
    engineDatabaseRule.updateProcessInstanceStartDates(startDatesToUpdate);
    Map<String, OffsetDateTime> endDatesToUpdate = new HashMap<>();
    endDatesToUpdate.put(processInstanceDto.getId(), startDate.plusSeconds(1));
    endDatesToUpdate.put(processInstanceDto2.getId(), startDate.plusSeconds(2));
    endDatesToUpdate.put(processInstanceDto3.getId(), startDate.plusSeconds(9));
    engineDatabaseRule.updateProcessInstanceEndDates(endDatesToUpdate);
    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshAllOptimizeIndices();

    // when
    ProcessReportDataDto reportDataDto = ProcessReportDataBuilder
      .createReportData()
      .setProcessDefinitionKey(processInstanceDto.getProcessDefinitionKey())
      .setProcessDefinitionVersion(processInstanceDto.getProcessDefinitionVersion())
      .setReportDataType(PROC_INST_DUR_GROUP_BY_NONE)
      .build();
    ProcessDurationReportNumberResultDto resultDto = evaluateDurationNumberReport(reportDataDto).getResult();

    // then
    assertThat(resultDto.getData(), is(notNullValue()));
    AggregationResultDto calculatedResult = resultDto.getData();
    assertThat(calculatedResult, is(notNullValue()));
    assertThat(calculatedResult.getAvg(), is(4000L));
    assertThat(calculatedResult.getMin(), is(1000L));
    assertThat(calculatedResult.getMax(), is(9000L));
    assertThat(calculatedResult.getMedian(), is(2000L));
  }

  @Test
  public void noAvailableProcessInstancesReturnsZero() {
    // when
    ProcessReportDataDto reportData = ProcessReportDataBuilder
      .createReportData()
      .setProcessDefinitionKey("fooProcDef")
      .setProcessDefinitionVersion("1")
      .setReportDataType(PROC_INST_DUR_GROUP_BY_NONE)
      .build();

    ProcessDurationReportNumberResultDto resultDto = evaluateDurationNumberReport(reportData).getResult();

    // then
    AggregationResultDto calculatedResult = resultDto.getData();
    assertThat(calculatedResult, is(notNullValue()));
    assertThat(calculatedResult.getAvg(), is(0L));
    assertThat(calculatedResult.getMax(), is(0L));
    assertThat(calculatedResult.getMin(), is(0L));
    assertThat(calculatedResult.getMedian(), is(0L));
  }

  @Test
  public void reportAcrossAllVersions() throws Exception {
    //given
    OffsetDateTime startDate = OffsetDateTime.now();
    ProcessInstanceEngineDto processInstanceDto = deployAndStartSimpleServiceTaskProcess();

    engineDatabaseRule.changeProcessInstanceStartDate(processInstanceDto.getId(), startDate);
    engineDatabaseRule.changeProcessInstanceEndDate(processInstanceDto.getId(), startDate.plusSeconds(1));
    processInstanceDto = engineRule.startProcessInstance(processInstanceDto.getDefinitionId());
    engineDatabaseRule.changeProcessInstanceStartDate(processInstanceDto.getId(), startDate);
    engineDatabaseRule.changeProcessInstanceEndDate(processInstanceDto.getId(), startDate.plusSeconds(9));
    processInstanceDto = deployAndStartSimpleServiceTaskProcess();
    engineDatabaseRule.changeProcessInstanceStartDate(processInstanceDto.getId(), startDate);
    engineDatabaseRule.changeProcessInstanceEndDate(processInstanceDto.getId(), startDate.plusSeconds(2));
    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshAllOptimizeIndices();


    // when
    ProcessReportDataDto reportDataDto = ProcessReportDataBuilder
      .createReportData()
      .setProcessDefinitionKey(processInstanceDto.getProcessDefinitionKey())
      .setProcessDefinitionVersion(ReportConstants.ALL_VERSIONS)
      .setReportDataType(PROC_INST_DUR_GROUP_BY_NONE)
      .build();

    ProcessDurationReportNumberResultDto resultDto = evaluateDurationNumberReport(reportDataDto).getResult();

    // then
    AggregationResultDto calculatedResult = resultDto.getData();
    assertThat(calculatedResult, is(notNullValue()));
    assertThat(calculatedResult.getAvg(), is(4000L));
    assertThat(calculatedResult.getMax(), is(9000L));
    assertThat(calculatedResult.getMin(), is(1000L));
    assertThat(calculatedResult.getMedian(), is(2000L));
  }

  @Test
  public void otherProcessDefinitionsDoNoAffectResult() throws Exception {
    // given
    OffsetDateTime startDate = OffsetDateTime.now();
    ProcessInstanceEngineDto processInstanceDto = deployAndStartSimpleServiceTaskProcess();

    String processDefinitionKey = processInstanceDto.getProcessDefinitionKey();
    String processDefinitionVersion = processInstanceDto.getProcessDefinitionVersion();

    engineDatabaseRule.changeProcessInstanceStartDate(processInstanceDto.getId(), startDate);
    engineDatabaseRule.changeProcessInstanceEndDate(processInstanceDto.getId(), startDate.plusSeconds(1));
    processInstanceDto = engineRule.startProcessInstance(processInstanceDto.getDefinitionId());
    engineDatabaseRule.changeProcessInstanceStartDate(processInstanceDto.getId(), startDate);
    engineDatabaseRule.changeProcessInstanceEndDate(processInstanceDto.getId(), startDate.plusSeconds(9));
    processInstanceDto = engineRule.startProcessInstance(processInstanceDto.getDefinitionId());
    engineDatabaseRule.changeProcessInstanceStartDate(processInstanceDto.getId(), startDate);
    engineDatabaseRule.changeProcessInstanceEndDate(processInstanceDto.getId(), startDate.plusSeconds(2));
    deployAndStartSimpleServiceTaskProcess();
    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshAllOptimizeIndices();

    // when
    ProcessReportDataDto reportDataDto = ProcessReportDataBuilder
      .createReportData()
      .setProcessDefinitionKey(processDefinitionKey)
      .setProcessDefinitionVersion(processDefinitionVersion)
      .setReportDataType(PROC_INST_DUR_GROUP_BY_NONE)
      .build();

    ProcessDurationReportNumberResultDto resultDto = evaluateDurationNumberReport(reportDataDto).getResult();

    // then
    AggregationResultDto calculatedResult = resultDto.getData();
    assertThat(calculatedResult, is(notNullValue()));
    assertThat(calculatedResult.getAvg(), is(4000L));
    assertThat(calculatedResult.getMax(), is(9000L));
    assertThat(calculatedResult.getMin(), is(1000L));
    assertThat(calculatedResult.getMedian(), is(2000L));
  }

  @Test
  public void reportEvaluationSingleBucketFilteredBySingleTenant() {
    // given
    final String tenantId1 = "tenantId1";
    final String tenantId2 = "tenantId2";
    final List<String> selectedTenants = Lists.newArrayList(tenantId1);
    final String processKey = deployAndStartMultiTenantSimpleServiceTaskProcess(
      Lists.newArrayList(null, tenantId1, tenantId2)
    );

    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshAllOptimizeIndices();

    // when
    ProcessReportDataDto reportData = ProcessReportDataBuilderHelper.createProcessInstanceDurationGroupByNone(
      processKey, ReportConstants.ALL_VERSIONS
    );
    reportData.setTenantIds(selectedTenants);
    ProcessDurationReportNumberResultDto result = evaluateDurationNumberReport(reportData).getResult();

    // then
    assertThat(result.getProcessInstanceCount(), is((long) selectedTenants.size()));
  }

  @Test
  public void filterInReportWorks() throws Exception {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("var", true);
    OffsetDateTime startDate = OffsetDateTime.now();
    ProcessInstanceEngineDto processInstanceDto = deployAndStartSimpleServiceTaskProcessWithVariables(variables);
    engineDatabaseRule.changeProcessInstanceStartDate(processInstanceDto.getId(), startDate);
    engineDatabaseRule.changeProcessInstanceEndDate(processInstanceDto.getId(), startDate.plusSeconds(1));
    String processDefinitionId = processInstanceDto.getDefinitionId();
    engineRule.startProcessInstance(processDefinitionId);
    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshAllOptimizeIndices();

    // when
    ProcessReportDataDto reportData = ProcessReportDataBuilder
      .createReportData()
      .setProcessDefinitionKey(processInstanceDto.getProcessDefinitionKey())
      .setProcessDefinitionVersion(processInstanceDto.getProcessDefinitionVersion())
      .setReportDataType(PROC_INST_DUR_GROUP_BY_NONE)
      .build();

    reportData.setFilter(ProcessFilterBuilder
                           .filter()
                           .variable()
                           .booleanTrue()
                           .name("var")
                           .add()
                           .buildList());
    ProcessDurationReportNumberResultDto resultDto = evaluateDurationNumberReport(reportData).getResult();

    // then
    AggregationResultDto calculatedResult = resultDto.getData();
    assertThat(calculatedResult, is(notNullValue()));
    assertThat(calculatedResult.getAvg(), is(1000L));
    assertThat(calculatedResult.getMax(), is(1000L));
    assertThat(calculatedResult.getMin(), is(1000L));
    assertThat(calculatedResult.getMedian(), is(1000L));

    // when
    reportData.setFilter(ProcessFilterBuilder
                           .filter()
                           .variable()
                           .booleanFalse()
                           .name("var")
                           .add()
                           .buildList());
    resultDto = evaluateDurationNumberReport(reportData).getResult();

    // then
    calculatedResult = resultDto.getData();
    assertThat(calculatedResult, is(notNullValue()));
    assertThat(calculatedResult.getAvg(), is(0L));
    assertThat(calculatedResult.getMax(), is(0L));
    assertThat(calculatedResult.getMin(), is(0L));
    assertThat(calculatedResult.getMedian(), is(0L));
  }

  @Test
  public void optimizeExceptionOnViewPropertyIsNull() {
    // given
    ProcessReportDataDto dataDto = ProcessReportDataBuilder
      .createReportData()
      .setProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .setProcessDefinitionVersion("1")
      .setReportDataType(PROC_INST_DUR_GROUP_BY_NONE)
      .build();

    dataDto.getView().setProperty(null);

    //when
    Response response = evaluateReportAndReturnResponse(dataDto);

    // then
    assertThat(response.getStatus(), is(500));
  }

  @Test
  public void optimizeExceptionOnGroupByTypeIsNull() {
    // given
    ProcessReportDataDto dataDto = ProcessReportDataBuilder
      .createReportData()
      .setProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .setProcessDefinitionVersion("1")
      .setReportDataType(PROC_INST_DUR_GROUP_BY_NONE)
      .build();

    dataDto.getGroupBy().setType(null);

    //when
    Response response = evaluateReportAndReturnResponse(dataDto);

    // then
    assertThat(response.getStatus(), is(400));
  }

  private ProcessInstanceEngineDto deployAndStartSimpleServiceTaskProcessWithVariables(Map<String, Object> variables) {
    BpmnModelInstance processModel = Bpmn.createExecutableProcess("aProcess")
      .name("aProcessName")
      .startEvent()
      .serviceTask(ProcessInstanceDurationByNoneReportEvaluationIT.TEST_ACTIVITY)
      .camundaExpression("${true}")
      .endEvent()
      .done();
    return engineRule.deployAndStartProcessWithVariables(processModel, variables);
  }

  private String createAndStoreDefaultReportDefinition(ProcessReportDataDto reportData) {
    String id = createNewReport();

    SingleProcessReportDefinitionDto report = new SingleProcessReportDefinitionDto();
    report.setData(reportData);
    report.setId(id);
    report.setLastModifier("something");
    report.setName("something");
    report.setCreated(OffsetDateTime.now());
    report.setLastModified(OffsetDateTime.now());
    report.setOwner("something");
    updateReport(id, report);
    return id;
  }

}
