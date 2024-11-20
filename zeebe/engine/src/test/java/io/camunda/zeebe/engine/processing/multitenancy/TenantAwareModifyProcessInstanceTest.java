/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.engine.processing.multitenancy;

import static io.camunda.zeebe.protocol.record.Assertions.assertThat;

import io.camunda.zeebe.engine.util.EngineRule;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.protocol.record.intent.ProcessInstanceIntent;
import io.camunda.zeebe.protocol.record.intent.ProcessInstanceModificationIntent;
import io.camunda.zeebe.protocol.record.value.TenantOwned;
import io.camunda.zeebe.test.util.record.RecordingExporter;
import io.camunda.zeebe.test.util.record.RecordingExporterTestWatcher;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;

public class TenantAwareModifyProcessInstanceTest {

  @ClassRule public static final EngineRule ENGINE = EngineRule.singlePartition();

  @Rule public final TestWatcher watcher = new RecordingExporterTestWatcher();

  @Test
  public void shouldModifyInstanceForDefaultTenant() {
    // given
    ENGINE
        .deployment()
        .withXmlResource(
            Bpmn.createExecutableProcess("process")
                .startEvent()
                .serviceTask("task", t -> t.zeebeJobType("test"))
                .endEvent()
                .done())
        .withTenantId(TenantOwned.DEFAULT_TENANT_IDENTIFIER)
        .deploy();

    final long processInstanceKey =
        ENGINE
            .processInstance()
            .ofBpmnProcessId("process")
            .withTenantId(TenantOwned.DEFAULT_TENANT_IDENTIFIER)
            .create();

    final var task =
        RecordingExporter.processInstanceRecords(ProcessInstanceIntent.ELEMENT_ACTIVATED)
            .withProcessInstanceKey(processInstanceKey)
            .withElementId("task")
            .getFirst();

    // when
    final var modified =
        ENGINE
            .processInstance()
            .withInstanceKey(processInstanceKey)
            .forAuthorizedTenants(TenantOwned.DEFAULT_TENANT_IDENTIFIER)
            .modification()
            .activateElement("task")
            .terminateElement(task.getKey())
            .modify();

    // then
    assertThat(modified)
        .describedAs("Expect that modification was successful")
        .hasIntent(ProcessInstanceModificationIntent.MODIFIED);
  }

  @Test
  public void shouldModifyInstanceForSpecificTenant() {
    // given
    ENGINE
        .deployment()
        .withXmlResource(
            Bpmn.createExecutableProcess("process")
                .startEvent()
                .serviceTask("task", t -> t.zeebeJobType("test"))
                .endEvent()
                .done())
        .withTenantId("custom-tenant")
        .deploy();

    final long processInstanceKey =
        ENGINE.processInstance().ofBpmnProcessId("process").withTenantId("custom-tenant").create();

    final var task =
        RecordingExporter.processInstanceRecords(ProcessInstanceIntent.ELEMENT_ACTIVATED)
            .withProcessInstanceKey(processInstanceKey)
            .withElementId("task")
            .getFirst();

    // when
    final var modified =
        ENGINE
            .processInstance()
            .withInstanceKey(processInstanceKey)
            .forAuthorizedTenants("custom-tenant")
            .modification()
            .activateElement("task")
            .terminateElement(task.getKey())
            .modify();

    // then
    assertThat(modified)
        .describedAs("Expect that modification was successful")
        .hasIntent(ProcessInstanceModificationIntent.MODIFIED);
  }
}
