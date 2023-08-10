/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under one or more contributor license agreements.
 * Licensed under a proprietary license. See the License.txt file for more information.
 * You may not use this file except in compliance with the proprietary license.
 */
package org.camunda.optimize.util;

import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.model.bpmn.BpmnModelInstance;
import io.camunda.zeebe.model.bpmn.builder.EndEventBuilder;
import io.camunda.zeebe.model.bpmn.builder.ProcessBuilder;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ZeebeBpmnModels {

  public static final String START_EVENT = "start";
  public static final String SERVICE_TASK = "service_task";
  public static final String SEND_TASK = "send_task";
  public static final String USER_TASK = "user_task";
  public static final String END_EVENT = "end";
  public static final String END_EVENT_2 = "end2";
  public static final String TERMINATE_END_EVENT = "terminate-end";
  public static final String CATCH_EVENT = "catchEvent";
  public static final String CONVERGING_GATEWAY = "converging_gateway";
  public static final String DIVERGING_GATEWAY = "diverging_gateway";

  public static BpmnModelInstance createStartEndProcess(final String processName) {
    return createStartEndProcess(processName, null);
  }

  public static BpmnModelInstance createStartEndProcess(final String processName, final String processId) {
    ProcessBuilder executableProcess = Bpmn.createExecutableProcess();
    if (processId != null) {
      executableProcess = executableProcess.id(processId);
    }
    return executableProcess
      .name(processName)
      .startEvent(START_EVENT).name(START_EVENT)
      .endEvent(END_EVENT).name(null)
      .done();
  }

  public static BpmnModelInstance createSimpleServiceTaskProcess(final String processName) {
    return Bpmn.createExecutableProcess(processName)
      .name(processName)
      .startEvent(START_EVENT).name(START_EVENT)
      .serviceTask(SERVICE_TASK).zeebeJobType(SERVICE_TASK).name(SERVICE_TASK)
      .endEvent(END_EVENT).name(null)
      .done();
  }

  public static BpmnModelInstance createSingleStartDoubleEndEventProcess(final String processName) {
    return Bpmn.createExecutableProcess(processName)
      .name(processName)
      .startEvent(START_EVENT)
      .parallelGateway()
      .endEvent(END_EVENT)
      .moveToLastGateway()
      .endEvent(END_EVENT_2)
      .done();
  }

  public static BpmnModelInstance createTerminateEndEventProcess(final String processName) {
    return Bpmn.createExecutableProcess(processName)
      .name(processName)
      .startEvent()
      .endEvent(TERMINATE_END_EVENT, EndEventBuilder::terminate)
      .done();
  }

  public static BpmnModelInstance createIncidentProcess(final String processName) {
    return Bpmn.createExecutableProcess()
      .name(processName)
      .startEvent(START_EVENT).name(START_EVENT)
      .intermediateCatchEvent(
        CATCH_EVENT,
        e -> e.message(m -> m.name("catch").zeebeCorrelationKeyExpression("orderId"))
      )
      .endEvent(END_EVENT_2).name(null)
      .done();
  }

  public static BpmnModelInstance createSimpleUserTaskProcess(final String processName) {
    return Bpmn.createExecutableProcess()
      .name(processName)
      .startEvent(START_EVENT).name(START_EVENT)
      .userTask(USER_TASK).id(USER_TASK).name(USER_TASK)
      .endEvent(END_EVENT).name(null)
      .done();
  }

  public static BpmnModelInstance createLoopingProcess(final String processName) {
    return Bpmn.createExecutableProcess()
      .name(processName)
      .startEvent(START_EVENT)
      .exclusiveGateway(CONVERGING_GATEWAY)
      .serviceTask(SERVICE_TASK).zeebeJobType(SERVICE_TASK)
      .exclusiveGateway(DIVERGING_GATEWAY).condition("End process", "=loop=false")
      .endEvent(END_EVENT)
      .moveToNode(DIVERGING_GATEWAY).condition("Do Loop", "=loop=true")
      .connectTo(CONVERGING_GATEWAY)
      .done();
  }

  public static BpmnModelInstance createInclusiveGatewayProcess(final String processName) {
    return Bpmn.createExecutableProcess()
      .name(processName)
      .startEvent(START_EVENT)
      .inclusiveGateway(DIVERGING_GATEWAY)
      .sequenceFlowId("s1")
      .conditionExpression("= contains(varName,\"a\")")
      .endEvent(END_EVENT)
      .moveToLastGateway()
      .sequenceFlowId("s2")
      .conditionExpression("= contains(varName,\"b\")")
      .endEvent(END_EVENT_2)
      .done();
  }

  public static BpmnModelInstance createInclusiveGatewayProcessWithConverging(final String processName) {
    return Bpmn.createExecutableProcess(processName)
      .startEvent(START_EVENT)
      .inclusiveGateway(DIVERGING_GATEWAY)
      .sequenceFlowId("s1")
      .conditionExpression("= contains(varName,\"a\")")
      .inclusiveGateway(CONVERGING_GATEWAY)
      .moveToNode(DIVERGING_GATEWAY)
      .conditionExpression("= contains(varName,\"b\")")
      .sequenceFlowId("s2")
      .connectTo(CONVERGING_GATEWAY)
      .endEvent(END_EVENT)
      .done();
  }

  public static BpmnModelInstance createSendTaskProcess(final String processName) {
    return Bpmn.createExecutableProcess()
      .name(processName)
      .startEvent(START_EVENT)
      .sendTask(SEND_TASK).zeebeJobType(SEND_TASK)
      .done();
  }

}
