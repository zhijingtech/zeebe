/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.es.report.command.decision.frequency;

import org.camunda.optimize.dto.optimize.query.report.single.decision.DecisionReportDataDto;
import org.camunda.optimize.dto.optimize.query.report.single.decision.group.DecisionGroupByDto;
import org.camunda.optimize.dto.optimize.query.report.single.decision.group.value.DecisionGroupByVariableValueDto;
import org.camunda.optimize.dto.optimize.query.report.single.decision.result.DecisionReportMapResultDto;
import org.camunda.optimize.dto.optimize.query.report.single.result.MapResultEntryDto;
import org.camunda.optimize.service.es.report.command.decision.DecisionReportCommand;
import org.camunda.optimize.service.es.report.command.util.MapResultSortingUtility;
import org.camunda.optimize.service.es.report.result.decision.SingleDecisionMapReportResult;
import org.camunda.optimize.service.exceptions.OptimizeRuntimeException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.Filter;
import org.elasticsearch.search.aggregations.bucket.nested.Nested;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.camunda.optimize.service.es.schema.OptimizeIndexNameHelper.getOptimizeIndexAliasForType;
import static org.camunda.optimize.service.util.DecisionVariableHelper.getVariableIdField;
import static org.camunda.optimize.service.util.DecisionVariableHelper.getVariableStringValueField;
import static org.camunda.optimize.upgrade.es.ElasticsearchConstants.DECISION_INSTANCE_TYPE;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

public abstract class CountDecisionFrequencyGroupByVariableCommand
  extends DecisionReportCommand<SingleDecisionMapReportResult> {

  private static final String NESTED_AGGREGATION = "nested";
  private static final String FILTERED_VARIABLES_AGGREGATION = "filteredVariables";
  private static final String VARIABLE_VALUE_TERMS_AGGREGATION = "variableValueTerms";

  private final String variablePath;

  public CountDecisionFrequencyGroupByVariableCommand(final String variablePath) {
    this.variablePath = variablePath;
  }

  @Override
  protected SingleDecisionMapReportResult evaluate() {
    final DecisionReportDataDto reportData = getReportData();
    logger.debug(
      "Evaluating count decision instance frequency grouped by {} report " +
        "for decision definition with key [{}] and version [{}]",
      variablePath,
      reportData.getDecisionDefinitionKey(),
      reportData.getDecisionDefinitionVersion()
    );

    final BoolQueryBuilder query = setupBaseQuery(reportData);

    DecisionGroupByVariableValueDto groupBy =
      ((DecisionGroupByDto<DecisionGroupByVariableValueDto>) reportData.getGroupBy()).getValue();

    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
      .query(query)
      .fetchSource(false)
      .aggregation(createAggregation(groupBy.getId()))
      .size(0);
    SearchRequest searchRequest =
      new SearchRequest(getOptimizeIndexAliasForType(DECISION_INSTANCE_TYPE))
        .types(DECISION_INSTANCE_TYPE)
        .source(searchSourceBuilder);

    SearchResponse response;
    try {
      response = esClient.search(searchRequest, RequestOptions.DEFAULT);
    } catch (IOException e) {
      String reason =
        String.format(
          "Could not evaluate count decision instance frequency grouped by {} report " +
            "for decision definition with key [%s] and version [%s]",
          reportData.getDecisionDefinitionKey(),
          reportData.getDecisionDefinitionVersion()
        );
      logger.error(reason, e);
      throw new OptimizeRuntimeException(reason, e);
    }

    final DecisionReportMapResultDto mapResultDto = mapToReportResult(response);
    return new SingleDecisionMapReportResult(mapResultDto, reportDefinition);
  }

  @Override
  protected void sortResultData(final SingleDecisionMapReportResult evaluationResult) {
    ((DecisionReportDataDto) getReportData()).getParameters().getSorting().ifPresent(
      sorting -> MapResultSortingUtility.sortResultData(sorting, evaluationResult)
    );
  }

  private AggregationBuilder createAggregation(final String variableId) {
    return AggregationBuilders
      .nested(NESTED_AGGREGATION, variablePath)
      .subAggregation(
        AggregationBuilders
          .filter(
            FILTERED_VARIABLES_AGGREGATION,
            boolQuery().filter(termQuery(getVariableIdField(variablePath), variableId))
          )
          .subAggregation(
            AggregationBuilders
              .terms(VARIABLE_VALUE_TERMS_AGGREGATION)
              .size(configurationService.getEsAggregationBucketLimit())
              .field(getVariableStringValueField(variablePath))
          )
      );
  }

  private DecisionReportMapResultDto mapToReportResult(final SearchResponse response) {
    final DecisionReportMapResultDto resultDto = new DecisionReportMapResultDto();

    final Nested nested = response.getAggregations().get(NESTED_AGGREGATION);
    final Filter filteredVariables = nested.getAggregations().get(FILTERED_VARIABLES_AGGREGATION);
    final Terms variableTerms = filteredVariables.getAggregations().get(VARIABLE_VALUE_TERMS_AGGREGATION);

    final List<MapResultEntryDto<Long>> resultData = new ArrayList<>();
    for (Terms.Bucket b : variableTerms.getBuckets()) {
      resultData.add(new MapResultEntryDto<>(b.getKeyAsString(), b.getDocCount()));
    }

    resultDto.setData(resultData);
    resultDto.setIsComplete(variableTerms.getSumOfOtherDocCounts() == 0L);
    resultDto.setDecisionInstanceCount(response.getHits().getTotalHits());

    return resultDto;
  }

}
