package org.camunda.optimize.service.es.writer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.camunda.optimize.dto.optimize.importing.index.DefinitionBasedImportIndexDto;
import org.camunda.optimize.service.util.EsHelper;
import org.camunda.optimize.service.util.configuration.ConfigurationService;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class DefinitionBasedImportIndexWriter {

  private final Logger logger = LoggerFactory.getLogger(DefinitionBasedImportIndexWriter.class);

  @Autowired
  private Client esclient;
  @Autowired
  private ConfigurationService configurationService;
  @Autowired
  private ObjectMapper objectMapper;

  public void importIndex(DefinitionBasedImportIndexDto importStartIndex, String typeIndexComesFrom) throws IOException {
    logger.debug("Writing definition based import index '{}' of type '{}' to elasticsearch",
      importStartIndex.getCurrentProcessDefinition().getDefinitionBasedImportIndex(), typeIndexComesFrom);
    esclient
      .prepareIndex(
        configurationService.getOptimizeIndex(),
        configurationService.getProcessDefinitionImportIndexType(),
        EsHelper.constructKey(typeIndexComesFrom, importStartIndex.getEngine()))
      .setSource(
        objectMapper.writeValueAsString(importStartIndex), XContentType.JSON)
      .get();
  }

}
