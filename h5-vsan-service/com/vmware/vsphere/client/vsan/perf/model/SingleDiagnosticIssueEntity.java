package com.vmware.vsphere.client.vsan.perf.model;

import com.vmware.proxygen.ts.TsModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@TsModel
public class SingleDiagnosticIssueEntity extends DiagnosticIssueEntity {
   private static final Log _logger = LogFactory.getLog(SingleDiagnosticIssueEntity.class);
   public String entityRefId;
   public String vsanUuid;
   public PerfEntityStateData metric;

   public SingleDiagnosticIssueEntity() {
   }

   public SingleDiagnosticIssueEntity(PerfEntityStateData metric) {
      this("", metric);
   }

   public SingleDiagnosticIssueEntity(String recommendation, PerfEntityStateData metric) {
      super(recommendation);
      _logger.info("Creating perf diag issue for entityRefId = " + metric.entityRefId);
      this.metric = metric;
      this.entityRefId = metric.entityRefId;
   }
}
