package org.camunda.operate.zeebe;

import org.camunda.operate.entities.OperateEntity;
import io.zeebe.client.api.record.Record;


public abstract class AbstractEventTransformer {

  protected void updateMetadataFields(OperateEntity operateEntity, Record zeebeRecord) {
    operateEntity.setPartitionId(zeebeRecord.getMetadata().getPartitionId());
    operateEntity.setPosition(zeebeRecord.getMetadata().getPosition());
  }

}
