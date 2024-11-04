/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.exporter.archiver;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch._types.Conflicts;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.Slices;
import co.elastic.clients.elasticsearch._types.SlicesCalculation;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.AggregationBuilders;
import co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval;
import co.elastic.clients.elasticsearch._types.aggregations.DateHistogramBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.elasticsearch.async_search.SubmitRequest;
import co.elastic.clients.elasticsearch.async_search.SubmitResponse;
import co.elastic.clients.elasticsearch.core.DeleteByQueryRequest;
import co.elastic.clients.elasticsearch.core.DeleteByQueryResponse;
import co.elastic.clients.elasticsearch.core.ReindexRequest;
import co.elastic.clients.elasticsearch.core.reindex.Source;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.elasticsearch.indices.PutIndicesSettingsRequest;
import co.elastic.clients.json.JsonData;
import io.camunda.exporter.config.ExporterConfiguration.ArchiverConfiguration;
import io.camunda.exporter.config.ExporterConfiguration.RetentionConfiguration;
import io.camunda.exporter.metrics.CamundaExporterMetrics;
import io.camunda.webapps.schema.descriptors.operate.template.ListViewTemplate;
import io.micrometer.core.instrument.Timer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.annotation.WillCloseWhenClosed;
import org.slf4j.Logger;

public final class ElasticsearchRepository implements ArchiverRepository {
  private static final String DATES_AGG = "datesAgg";
  private static final String INSTANCES_AGG = "instancesAgg";
  private static final String DATES_SORTED_AGG = "datesSortedAgg";
  private static final Time REINDEX_SCROLL_TIMEOUT = Time.of(t -> t.time("30s"));
  private static final Slices AUTO_SLICES =
      Slices.of(slices -> slices.computed(SlicesCalculation.Auto));

  private final int partitionId;
  private final ArchiverConfiguration config;
  private final RetentionConfiguration retention;
  private final ListViewTemplate template;
  private final ElasticsearchAsyncClient client;
  private final Executor executor;
  private final CamundaExporterMetrics metrics;
  private final Logger logger;

  private final CalendarInterval rolloverInterval;

  public ElasticsearchRepository(
      final int partitionId,
      final ArchiverConfiguration config,
      final RetentionConfiguration retention,
      final ListViewTemplate template,
      @WillCloseWhenClosed final ElasticsearchAsyncClient client,
      final Executor executor,
      final CamundaExporterMetrics metrics,
      final Logger logger) {
    this.partitionId = partitionId;
    this.config = config;
    this.retention = retention;
    this.template = template;
    this.client = client;
    this.executor = executor;
    this.metrics = metrics;
    this.logger = logger;

    rolloverInterval = mapCalendarInterval(config.getRolloverInterval());
  }

  @Override
  public CompletableFuture<ArchiveBatch> getProcessInstancesNextBatch() {
    final var aggregation = createFinishedInstancesAggregation();
    final var searchRequest = createFinishedInstancesSearchRequest(aggregation);

    final var timer = Timer.start();
    return client
        .asyncSearch()
        .submit(searchRequest, Object.class)
        .whenCompleteAsync((ignored, error) -> metrics.measureArchiverSearch(timer), executor)
        .thenApplyAsync(this::createArchiveBatch, executor);
  }

  @Override
  public CompletableFuture<Void> setIndexLifeCycle(final String destinationIndexName) {
    if (!retention.isEnabled()) {
      return CompletableFuture.completedFuture(null);
    }

    final var existsRequest = new ExistsRequest.Builder().index(destinationIndexName).build();
    final var settingsRequest =
        new PutIndicesSettingsRequest.Builder()
            .settings(
                settings ->
                    settings.lifecycle(lifecycle -> lifecycle.name(retention.getPolicyName())))
            .build();

    return client
        .indices()
        .exists(existsRequest)
        .thenComposeAsync(
            response -> {
              if (!response.value()) {
                // TODO: verify this is the expected behavior
                return CompletableFuture.completedFuture(null);
              }

              return client.indices().putSettings(settingsRequest);
            },
            executor)
        .thenApplyAsync(ok -> null, executor);
  }

  @Override
  public CompletableFuture<Void> deleteDocuments(
      final String sourceIndexName,
      final String idFieldName,
      final List<String> processInstanceKeys) {
    final TermsQuery termsQuery = buildIdTermsQuery(idFieldName, processInstanceKeys);
    final var request =
        new DeleteByQueryRequest.Builder()
            .index(sourceIndexName)
            .slices(AUTO_SLICES)
            .conflicts(Conflicts.Proceed)
            .query(q -> q.terms(termsQuery))
            .build();

    final var timer = Timer.start();
    return client
        .deleteByQuery(request)
        .whenCompleteAsync((ignored, error) -> metrics.measureArchiverDelete(timer), executor)
        .thenApplyAsync(DeleteByQueryResponse::total, executor)
        .thenApplyAsync(ok -> null, executor);
  }

  @Override
  public CompletableFuture<Void> reindexDocuments(
      final String sourceIndexName,
      final String destinationIndexName,
      final String idFieldName,
      final List<String> processInstanceKeys) {
    final var source =
        new Source.Builder()
            .index(sourceIndexName)
            .query(q -> q.terms(buildIdTermsQuery(idFieldName, processInstanceKeys)))
            .build();
    final var request =
        new ReindexRequest.Builder()
            .source(source)
            .dest(dest -> dest.index(destinationIndexName))
            .conflicts(Conflicts.Proceed)
            .scroll(REINDEX_SCROLL_TIMEOUT)
            .slices(AUTO_SLICES)
            .build();

    final var timer = Timer.start();
    return client
        .reindex(request)
        .whenCompleteAsync((ignored, error) -> metrics.measureArchiverReindex(timer), executor)
        .thenApplyAsync(ignored -> null, executor);
  }

  @Override
  public void close() throws Exception {
    // TODO: verify if I close the transport that any pending futures are cancelled
    client._transport().close();
  }

  private Aggregation createFinishedInstancesAggregation() {
    final var dateAggregation =
        AggregationBuilders.dateHistogram()
            .field(ListViewTemplate.END_DATE)
            .calendarInterval(rolloverInterval)
            .format(config.getRolloverDateFormat())
            .keyed(false) // get result as an array (not a map)
            .build();
    final var sortAggregation =
        AggregationBuilders.bucketSort()
            .sort(sort -> sort.field(b -> b.field("_key")))
            .size(1) // we want to get only one bucket at a time
            .build();
    // we need process instance ids, also taking into account batch size
    final var instanceAggregation =
        AggregationBuilders.topHits()
            .size(config.getRolloverBatchSize())
            .sort(sort -> sort.field(b -> b.field(ListViewTemplate.ID).order(SortOrder.Asc)))
            .source(source -> source.filter(filter -> filter.includes(ListViewTemplate.ID)))
            .build();
    return new Aggregation.Builder()
        .dateHistogram(dateAggregation)
        .aggregations(DATES_SORTED_AGG, Aggregation.of(b -> b.bucketSort(sortAggregation)))
        .aggregations(INSTANCES_AGG, Aggregation.of(b -> b.topHits(instanceAggregation)))
        .build();
  }

  private SubmitRequest createFinishedInstancesSearchRequest(final Aggregation aggregation) {
    final var endDateQ =
        QueryBuilders.range(
            q ->
                q.field(ListViewTemplate.END_DATE)
                    .lte(JsonData.of(config.getArchivingTimePoint())));
    final var isProcessInstanceQ =
        QueryBuilders.term(
            q ->
                q.field(ListViewTemplate.JOIN_RELATION)
                    .value(ListViewTemplate.PROCESS_INSTANCE_JOIN_RELATION));
    final var partitionQ =
        QueryBuilders.term(q -> q.field(ListViewTemplate.PARTITION_ID).value(partitionId));
    final var combinedQuery =
        QueryBuilders.bool(q -> q.must(endDateQ, isProcessInstanceQ, partitionQ));

    logger.trace(
        "Finished process instances for archiving request: \n{}\n and aggregation: \n{}",
        combinedQuery.toString(),
        aggregation.toString());
    return new SubmitRequest.Builder()
        .index(template.getFullQualifiedName())
        .requestCache(false)
        .allowNoIndices(true)
        .ignoreUnavailable(true)
        .source(source -> source.fetch(false))
        .query(query -> query.constantScore(q -> q.filter(combinedQuery)))
        .aggregations(DATES_AGG, aggregation)
        .sort(
            sort ->
                sort.field(field -> field.field(ListViewTemplate.END_DATE).order(SortOrder.Asc)))
        .size(0)
        .build();
  }

  private ArchiveBatch createArchiveBatch(final SubmitResponse<?> search) {
    final List<DateHistogramBucket> buckets =
        search.response().aggregations().get(DATES_AGG).dateHistogram().buckets().array();

    if (buckets.isEmpty()) {
      return null;
    }

    final var bucket = buckets.getFirst();
    final var finishDate = bucket.keyAsString();
    final List<String> ids =
        bucket.aggregations().get(INSTANCES_AGG).topHits().hits().hits().stream()
            .map(Hit::id)
            .toList();
    return new ArchiveBatch(finishDate, ids);
  }

  private TermsQuery buildIdTermsQuery(final String idFieldName, final List<String> idValues) {
    return QueryBuilders.terms()
        .field(idFieldName)
        .terms(terms -> terms.value(idValues.stream().map(FieldValue::of).toList()))
        .build();
  }

  private CalendarInterval mapCalendarInterval(final String alias) {
    return Arrays.stream(CalendarInterval.values())
        .filter(c -> c.aliases() != null)
        .filter(c -> Arrays.binarySearch(c.aliases(), alias) >= 0)
        .findFirst()
        .orElseThrow();
  }

  // Visible for Jackson
  public record SearchResult(String id) {}
}
