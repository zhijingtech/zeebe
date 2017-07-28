package org.camunda.optimize.service.importing.impl;

import org.camunda.optimize.dto.engine.ProcessDefinitionXmlEngineDto;
import org.camunda.optimize.dto.optimize.importing.ProcessDefinitionXmlOptimizeDto;
import org.camunda.optimize.service.es.writer.ProcessDefinitionWriter;
import org.camunda.optimize.service.exceptions.OptimizeException;
import org.camunda.optimize.service.importing.diff.MissingEntitiesFinder;
import org.camunda.optimize.service.importing.diff.MissingProcessDefinitionXmlFinder;
import org.camunda.optimize.service.importing.fetcher.IdBasedProcessDefinitionXmlFetcher;
import org.camunda.optimize.service.importing.fetcher.TotalityBasedProcessDefinitionXmlFetcher;
import org.camunda.optimize.service.importing.index.ImportIndexHandler;
import org.camunda.optimize.service.importing.index.TotalityBasedImportIndexHandler;
import org.camunda.optimize.service.importing.job.importing.ProcessDefinitionXmlImportJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProcessDefinitionXmlImportService extends PaginatedImportService<ProcessDefinitionXmlEngineDto, ProcessDefinitionXmlOptimizeDto> {
  private final Logger logger = LoggerFactory.getLogger(ProcessDefinitionXmlImportService.class);

  @Autowired
  private ProcessDefinitionWriter procDefWriter;

  @Autowired
  private MissingProcessDefinitionXmlFinder xmlFinder;

  @Autowired
  private TotalityBasedProcessDefinitionXmlFetcher processDefinitionXmlFetcher;

  @Autowired
  private TotalityBasedImportIndexHandler totalityBasedImportIndexHandler;

  @Override
  protected ImportIndexHandler initializeImportIndexHandler() {
    totalityBasedImportIndexHandler.initializeImportIndex(getElasticsearchType());
    return totalityBasedImportIndexHandler;
  }

  @Override
  protected MissingEntitiesFinder<ProcessDefinitionXmlEngineDto> getMissingEntitiesFinder() {
    return xmlFinder;
  }

  @Override
  protected List<ProcessDefinitionXmlEngineDto> queryEngineRestPoint() throws OptimizeException {
    return processDefinitionXmlFetcher.fetchProcessDefinitionXmls(
      importIndexHandler.getAbsoluteImportIndex()
    );
  }

  @Override
  public int getEngineEntityCount() throws OptimizeException {
    return processDefinitionXmlFetcher
      .fetchProcessDefinitionCount();
  }

  @Override
  public void importToElasticSearch(List<ProcessDefinitionXmlOptimizeDto> newOptimizeEntriesToImport) {
    ProcessDefinitionXmlImportJob procDefXmlImportJob = new ProcessDefinitionXmlImportJob(procDefWriter);
    procDefXmlImportJob.setEntitiesToImport(newOptimizeEntriesToImport);
    try {
      importJobExecutor.executeImportJob(procDefXmlImportJob);
    } catch (InterruptedException e) {
      logger.error("Interruption during import of process definition xml import job!", e);
    }
  }

  @Override
  public ProcessDefinitionXmlOptimizeDto mapToOptimizeDto(ProcessDefinitionXmlEngineDto entry) {
    return mapDefaults(entry);
  }

  private ProcessDefinitionXmlOptimizeDto mapDefaults(ProcessDefinitionXmlEngineDto dto) {
    ProcessDefinitionXmlOptimizeDto optimizeDto = new ProcessDefinitionXmlOptimizeDto();
    optimizeDto.setBpmn20Xml(dto.getBpmn20Xml());
    optimizeDto.setId(dto.getId());
    return optimizeDto;
  }

  @Override
  public String getElasticsearchType() {
    return configurationService.getProcessDefinitionXmlType();
  }
}
