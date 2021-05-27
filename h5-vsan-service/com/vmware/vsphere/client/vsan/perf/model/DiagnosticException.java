package com.vmware.vsphere.client.vsan.perf.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.vsan.binding.vim.cluster.VsanPerfDiagnosticResult;
import com.vmware.vim.vsan.binding.vim.cluster.VsanPerfEntityMetricCSV;
import java.util.ArrayList;
import java.util.List;

@TsModel
public class DiagnosticException {
   private static final String AGGREGATED_REF_ID_SEPARATOR = "/";
   public String exceptionId;
   public List exceptionEntities;

   public DiagnosticException() {
      this.exceptionEntities = new ArrayList();
   }

   public DiagnosticException(String exceptionId) {
      this();
      this.exceptionId = exceptionId;
   }

   public void addEntities(VsanPerfDiagnosticResult diagnosticResult, long rangeStart, long rangeEnd) {
      if (diagnosticResult.aggregationData != null) {
         AggregatedDiagnosticIssueEntity aggregatedEntity = this.createAggregatedEntity(diagnosticResult, rangeStart, rangeEnd);
         if (!aggregatedEntity.entities.isEmpty()) {
            this.exceptionEntities.add(aggregatedEntity);
         }
      } else {
         VsanPerfEntityMetricCSV[] var11 = diagnosticResult.exceptionData;
         int var7 = var11.length;

         for(int var8 = 0; var8 < var7; ++var8) {
            VsanPerfEntityMetricCSV entityMetric = var11[var8];
            SingleDiagnosticIssueEntity issueEntity = new SingleDiagnosticIssueEntity(diagnosticResult.recommendation, PerfEntityStateData.parsePerfEntityMetricCSV(entityMetric, rangeStart, rangeEnd));
            this.exceptionEntities.add(issueEntity);
         }
      }

   }

   private AggregatedDiagnosticIssueEntity createAggregatedEntity(VsanPerfDiagnosticResult diagnosticResult, long rangeStart, long rangeEnd) {
      String[] aggregatedEntityRefIds = diagnosticResult.aggregationData.entityRefId.split("/");

      for(int i = 0; i < aggregatedEntityRefIds.length; ++i) {
         String[] parts = aggregatedEntityRefIds[i].split(":");
         aggregatedEntityRefIds[i] = parts[0];
      }

      AggregatedDiagnosticIssueEntity issue = new AggregatedDiagnosticIssueEntity(diagnosticResult.recommendation, diagnosticResult.aggregationData, diagnosticResult.exceptionData, rangeStart, rangeEnd);
      return issue;
   }
}
