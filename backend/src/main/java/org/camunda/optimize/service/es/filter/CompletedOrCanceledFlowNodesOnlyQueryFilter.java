/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.es.filter;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.camunda.optimize.dto.optimize.query.report.single.process.filter.data.CompletedOrCanceledFlowNodesOnlyFilterDataDto;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.List;

import static org.camunda.optimize.service.es.filter.util.modelelement.FlowNodeFilterQueryUtil.createCompletedOrCanceledFlowNodesOnlyFilterQuery;
import static org.camunda.optimize.service.es.schema.index.ProcessInstanceIndex.EVENTS;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;

@Component
public class CompletedOrCanceledFlowNodesOnlyQueryFilter
  implements QueryFilter<CompletedOrCanceledFlowNodesOnlyFilterDataDto> {

  @Override
  public void addFilters(final BoolQueryBuilder query,
                         final List<CompletedOrCanceledFlowNodesOnlyFilterDataDto> completedOrCanceledFilterData,
                         final ZoneId timezone) {
    if (!CollectionUtils.isEmpty(completedOrCanceledFilterData)) {
      List<QueryBuilder> filters = query.filter();
      filters.add(nestedQuery(EVENTS, createCompletedOrCanceledFlowNodesOnlyFilterQuery(boolQuery()), ScoreMode.None));
    }
  }

}
