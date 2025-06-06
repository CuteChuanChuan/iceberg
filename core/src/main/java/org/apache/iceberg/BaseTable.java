/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.iceberg;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.iceberg.encryption.EncryptionManager;
import org.apache.iceberg.io.FileIO;
import org.apache.iceberg.io.LocationProvider;
import org.apache.iceberg.metrics.LoggingMetricsReporter;
import org.apache.iceberg.metrics.MetricsReporter;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;

/**
 * Base {@link Table} implementation.
 *
 * <p>This can be extended by providing a {@link TableOperations} to the constructor.
 *
 * <p>Serializing and deserializing a BaseTable object returns a read only implementation of the
 * BaseTable using a {@link StaticTableOperations}. This way no Catalog related calls are needed
 * when reading the table data after deserialization.
 */
public class BaseTable implements Table, HasTableOperations, Serializable {
  private final TableOperations ops;
  private final String name;
  private final MetricsReporter reporter;

  public BaseTable(TableOperations ops, String name) {
    this(ops, name, LoggingMetricsReporter.instance());
  }

  public BaseTable(TableOperations ops, String name, MetricsReporter reporter) {
    Preconditions.checkNotNull(reporter, "reporter cannot be null");
    this.ops = ops;
    this.name = name;
    this.reporter = reporter;
  }

  public MetricsReporter reporter() {
    return reporter;
  }

  @Override
  public TableOperations operations() {
    return ops;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public void refresh() {
    ops.refresh();
  }

  @Override
  public TableScan newScan() {
    return new DataTableScan(
        this, schema(), ImmutableTableScanContext.builder().metricsReporter(reporter).build());
  }

  @Override
  public IncrementalAppendScan newIncrementalAppendScan() {
    return new BaseIncrementalAppendScan(
        this, schema(), ImmutableTableScanContext.builder().metricsReporter(reporter).build());
  }

  @Override
  public IncrementalChangelogScan newIncrementalChangelogScan() {
    return new BaseIncrementalChangelogScan(this);
  }

  @Override
  public Schema schema() {
    return ops.current().schema();
  }

  @Override
  public Map<Integer, Schema> schemas() {
    return ops.current().schemasById();
  }

  @Override
  public PartitionSpec spec() {
    return ops.current().spec();
  }

  @Override
  public Map<Integer, PartitionSpec> specs() {
    return ops.current().specsById();
  }

  @Override
  public SortOrder sortOrder() {
    return ops.current().sortOrder();
  }

  @Override
  public Map<Integer, SortOrder> sortOrders() {
    return ops.current().sortOrdersById();
  }

  @Override
  public Map<String, String> properties() {
    return ops.current().properties();
  }

  @Override
  public String location() {
    return ops.current().location();
  }

  @Override
  public Snapshot currentSnapshot() {
    return ops.current().currentSnapshot();
  }

  @Override
  public Snapshot snapshot(long snapshotId) {
    return ops.current().snapshot(snapshotId);
  }

  @Override
  public Iterable<Snapshot> snapshots() {
    return ops.current().snapshots();
  }

  @Override
  public List<HistoryEntry> history() {
    return ops.current().snapshotLog();
  }

  @Override
  public UpdateSchema updateSchema() {
    return new SchemaUpdate(ops);
  }

  @Override
  public UpdatePartitionSpec updateSpec() {
    return new BaseUpdatePartitionSpec(ops);
  }

  @Override
  public UpdateProperties updateProperties() {
    return new PropertiesUpdate(ops);
  }

  @Override
  public ReplaceSortOrder replaceSortOrder() {
    return new BaseReplaceSortOrder(ops);
  }

  @Override
  public UpdateLocation updateLocation() {
    return new SetLocation(ops);
  }

  @Override
  public AppendFiles newAppend() {
    return new MergeAppend(name, ops).reportWith(reporter);
  }

  @Override
  public AppendFiles newFastAppend() {
    return new FastAppend(name, ops).reportWith(reporter);
  }

  @Override
  public RewriteFiles newRewrite() {
    return new BaseRewriteFiles(name, ops).reportWith(reporter);
  }

  @Override
  public RewriteManifests rewriteManifests() {
    return new BaseRewriteManifests(name, ops).reportWith(reporter);
  }

  @Override
  public OverwriteFiles newOverwrite() {
    return new BaseOverwriteFiles(name, ops).reportWith(reporter);
  }

  @Override
  public RowDelta newRowDelta() {
    return new BaseRowDelta(name, ops).reportWith(reporter);
  }

  @Override
  public ReplacePartitions newReplacePartitions() {
    return new BaseReplacePartitions(name, ops).reportWith(reporter);
  }

  @Override
  public DeleteFiles newDelete() {
    return new StreamingDelete(name, ops).reportWith(reporter);
  }

  @Override
  public UpdateStatistics updateStatistics() {
    return new SetStatistics(ops);
  }

  @Override
  public UpdatePartitionStatistics updatePartitionStatistics() {
    return new SetPartitionStatistics(ops);
  }

  @Override
  public ExpireSnapshots expireSnapshots() {
    return new RemoveSnapshots(ops);
  }

  @Override
  public ManageSnapshots manageSnapshots() {
    return new SnapshotManager(name, ops, reporter);
  }

  @Override
  public Transaction newTransaction() {
    return Transactions.newTransaction(name, ops, reporter);
  }

  @Override
  public FileIO io() {
    return ops.io();
  }

  @Override
  public EncryptionManager encryption() {
    return ops.encryption();
  }

  @Override
  public LocationProvider locationProvider() {
    return ops.locationProvider();
  }

  @Override
  public List<StatisticsFile> statisticsFiles() {
    return ops.current().statisticsFiles();
  }

  @Override
  public List<PartitionStatisticsFile> partitionStatisticsFiles() {
    return ops.current().partitionStatisticsFiles();
  }

  @Override
  public Map<String, SnapshotRef> refs() {
    return ops.current().refs();
  }

  @Override
  public UUID uuid() {
    return UUID.fromString(ops.current().uuid());
  }

  @Override
  public String toString() {
    return name();
  }

  Object writeReplace() {
    return SerializableTable.copyOf(this);
  }
}
