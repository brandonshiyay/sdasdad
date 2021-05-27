package com.vmware.vsphere.client.vsan.perf.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.vsan.binding.vim.cluster.VsanPerfEntityMetricCSV;
import com.vmware.vim.vsan.binding.vim.cluster.VsanPerfMetricSeriesCSV;
import com.vmware.vim.vsan.binding.vim.cluster.VsanPerfThreshold;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

@TsModel
public class AggregatedDiagnosticIssueEntity extends DiagnosticIssueEntity {
   private static final String AGGREGATED_REF_ID_SEPARATOR = "/";
   public String[] aggregatedRefIds;
   public List entities;
   public SingleDiagnosticIssueEntity aggregatedEntity;
   public boolean hasSingleEntityInside;
   public boolean usingSingleMetricForAllEntities = false;
   public String metricIdLabel;
   public VsanPerfThreshold aggregationThreshold;

   public AggregatedDiagnosticIssueEntity() {
   }

   public AggregatedDiagnosticIssueEntity(String recommendation, VsanPerfEntityMetricCSV aggregationData, VsanPerfEntityMetricCSV[] metrics, long rangeStart, long rangeEnd) {
      super(recommendation);
      this.hasSingleEntityInside = this.isAggregatingMetricsOnSingleEntity(metrics);
      if (this.hasSingleEntityInside) {
         this.aggregatedRefIds = new String[]{aggregationData.entityRefId};
      } else {
         this.aggregatedRefIds = aggregationData.entityRefId.split("/");

         for(int i = 0; i < this.aggregatedRefIds.length; ++i) {
            String[] parts = this.aggregatedRefIds[i].split(":");
            this.aggregatedRefIds[i] = parts[0];
         }

         this.metricIdLabel = this.getCommonMetricLabelIfUsingCommonMetric(metrics);
         this.usingSingleMetricForAllEntities = StringUtils.isNotEmpty(this.metricIdLabel);
      }

      this.aggregatedEntity = new SingleDiagnosticIssueEntity(PerfEntityStateData.parsePerfEntityMetricCSV(aggregationData, rangeStart, rangeEnd));
      this.entities = this.createChildEntities(metrics, rangeStart, rangeEnd);
      this.aggregationThreshold = aggregationData.value[0].threshold;
   }

   private boolean isAggregatingMetricsOnSingleEntity(VsanPerfEntityMetricCSV[] metrics) {
      boolean isSingleEntity = true;
      String entityRefId = "";
      VsanPerfEntityMetricCSV[] var4 = metrics;
      int var5 = metrics.length;

      for(int var6 = 0; var6 < var5; ++var6) {
         VsanPerfEntityMetricCSV metric = var4[var6];
         if (StringUtils.isNotEmpty(entityRefId) && !entityRefId.equals(metric.entityRefId)) {
            isSingleEntity = false;
            break;
         }

         entityRefId = metric.entityRefId;
      }

      return isSingleEntity;
   }

   private List createChildEntities(VsanPerfEntityMetricCSV[] metrics, long rangeStart, long rangeEnd) {
      List _entities = new ArrayList();
      VsanPerfEntityMetricCSV[] var7 = metrics;
      int var8 = metrics.length;

      for(int var9 = 0; var9 < var8; ++var9) {
         VsanPerfEntityMetricCSV entityMetric = var7[var9];
         SingleDiagnosticIssueEntity entity = new SingleDiagnosticIssueEntity("", PerfEntityStateData.parsePerfEntityMetricCSV(entityMetric, rangeStart, rangeEnd));
         _entities.add(entity);
      }

      return _entities;
   }

   private String getCommonMetricLabelIfUsingCommonMetric(VsanPerfEntityMetricCSV[] metrics) {
      String metricIdLabel = null;
      boolean usingSingleMetricForAllEntities = true;
      VsanPerfEntityMetricCSV[] var4 = metrics;
      int var5 = metrics.length;

      for(int var6 = 0; var6 < var5; ++var6) {
         VsanPerfEntityMetricCSV entityMetric = var4[var6];
         VsanPerfMetricSeriesCSV[] var8 = entityMetric.value;
         int var9 = var8.length;

         for(int var10 = 0; var10 < var9; ++var10) {
            VsanPerfMetricSeriesCSV metricSeries = var8[var10];
            if (!StringUtils.isEmpty(metricIdLabel) && !metricIdLabel.equals(metricSeries.metricId.label)) {
               usingSingleMetricForAllEntities = false;
               break;
            }

            metricIdLabel = metricSeries.metricId.label;
         }

         if (!usingSingleMetricForAllEntities) {
            metricIdLabel = null;
            break;
         }
      }

      return metricIdLabel;
   }
}
