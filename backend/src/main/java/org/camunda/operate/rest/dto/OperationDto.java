/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.operate.rest.dto;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.camunda.operate.entities.IncidentEntity;
import org.camunda.operate.entities.OperationEntity;
import org.camunda.operate.entities.OperationState;
import org.camunda.operate.entities.OperationType;

public class OperationDto {

  private OperationType type;

  private OffsetDateTime startDate;
  private OffsetDateTime endDate;

  private OperationState state;

  public OperationType getType() {
    return type;
  }

  public void setType(OperationType type) {
    this.type = type;
  }

  public OffsetDateTime getStartDate() {
    return startDate;
  }

  public void setStartDate(OffsetDateTime startDate) {
    this.startDate = startDate;
  }

  public OffsetDateTime getEndDate() {
    return endDate;
  }

  public void setEndDate(OffsetDateTime endDate) {
    this.endDate = endDate;
  }

  public OperationState getState() {
    return state;
  }

  public void setState(OperationState state) {
    this.state = state;
  }

  public static OperationDto createFrom(OperationEntity operationEntity) {
    if (operationEntity == null) {
      return null;
    }
    OperationDto operation = new OperationDto();
    operation.setType(operationEntity.getType());
    operation.setStartDate(operationEntity.getStartDate());
    operation.setEndDate(operationEntity.getEndDate());
    operation.setState(operationEntity.getState());
    return operation;
  }

  public static List<OperationDto> createFrom(List<OperationEntity> operationEntities) {
    List<OperationDto> result = new ArrayList<>();
    if (operationEntities != null) {
      for (OperationEntity operationEntity: operationEntities) {
        if (operationEntity != null) {
          result.add(createFrom(operationEntity));
        }
      }
    }
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    OperationDto that = (OperationDto) o;

    if (type != that.type)
      return false;
    if (startDate != null ? !startDate.equals(that.startDate) : that.startDate != null)
      return false;
    if (endDate != null ? !endDate.equals(that.endDate) : that.endDate != null)
      return false;
    return state == that.state;
  }

  @Override
  public int hashCode() {
    int result = type != null ? type.hashCode() : 0;
    result = 31 * result + (startDate != null ? startDate.hashCode() : 0);
    result = 31 * result + (endDate != null ? endDate.hashCode() : 0);
    result = 31 * result + (state != null ? state.hashCode() : 0);
    return result;
  }
}
