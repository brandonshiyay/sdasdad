package com.vmware.vsphere.client.vsan.perf;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vim.fault.NotFound;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vmomi.core.Future;
import com.vmware.vim.vsan.binding.vim.cluster.VsanPerfDiagnoseFeedbackSpec;
import com.vmware.vim.vsan.binding.vim.cluster.VsanPerfDiagnoseQuerySpec;
import com.vmware.vim.vsan.binding.vim.cluster.VsanPerfDiagnosticException;
import com.vmware.vim.vsan.binding.vim.cluster.VsanPerfDiagnosticResult;
import com.vmware.vim.vsan.binding.vim.cluster.VsanPerfEntityInfo;
import com.vmware.vim.vsan.binding.vim.cluster.VsanPerfEntityMetricCSV;
import com.vmware.vim.vsan.binding.vim.cluster.VsanPerfEntityType;
import com.vmware.vim.vsan.binding.vim.cluster.VsanPerformanceManager;
import com.vmware.vise.data.query.PropertyValue;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsan.client.util.VmodlHelper;
import com.vmware.vsphere.client.vsan.base.util.BaseUtils;
import com.vmware.vsphere.client.vsan.base.util.VsanProfiler;
import com.vmware.vsphere.client.vsan.perf.model.DiagnosticException;
import com.vmware.vsphere.client.vsan.perf.model.EntityRefData;
import com.vmware.vsphere.client.vsan.perf.model.PerfDiagnosticQuerySpec;
import com.vmware.vsphere.client.vsan.perf.model.PerformanceDiagnosticData;
import com.vmware.vsphere.client.vsan.perf.model.PerformanceDiagnosticException;
import com.vmware.vsphere.client.vsan.perf.model.PerformanceEntitiesData;
import com.vmware.vsphere.client.vsan.perf.model.PerformanceExceptionsData;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class PerformanceDiagnosticsPropertyProvider {
   private static final Log _logger = LogFactory.getLog(PerformanceDiagnosticsPropertyProvider.class);
   private static final VsanProfiler _profiler = new VsanProfiler(PerformanceDiagnosticsPropertyProvider.class);
   @Autowired
   private VsanClient vsanClient;

   @TsService
   public boolean getPerfAnalysisSupported(ManagedObjectReference clusterRef) throws Exception {
      return VsanCapabilityUtils.isPerfAnalysisSupportedOnVc(clusterRef);
   }

   @TsService
   public PerformanceExceptionsData getPerformanceExceptionsData(ManagedObjectReference clusterRef) throws Exception {
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var3 = null;

      PerformanceExceptionsData var22;
      try {
         VsanPerformanceManager perfMgr = conn.getVsanPerformanceManager();
         VsanPerfDiagnosticException[] exceptions = this.getExceptions(perfMgr);
         Map idToExceptionMap = new HashMap();
         if (exceptions != null) {
            VsanPerfDiagnosticException[] var7 = exceptions;
            int var8 = exceptions.length;

            for(int var9 = 0; var9 < var8; ++var9) {
               VsanPerfDiagnosticException ex = var7[var9];
               idToExceptionMap.put(ex.exceptionId, new PerformanceDiagnosticException(ex.exceptionMessage, ex.exceptionDetails, ex.exceptionUrl));
            }
         }

         PerformanceExceptionsData exceptionsData = new PerformanceExceptionsData();
         exceptionsData.performanceExceptionIdToException = idToExceptionMap;
         PerformanceDiagnosticsPropertyProvider.EntityTypes types = this.getVsanPerfEntityTypes(perfMgr);
         exceptionsData.performanceEntityTypes = types.simpleTypes;
         exceptionsData.performanceAggregatedEntityTypes = types.aggregatedTypes;
         var22 = exceptionsData;
      } catch (Throwable var18) {
         var3 = var18;
         throw var18;
      } finally {
         if (conn != null) {
            if (var3 != null) {
               try {
                  conn.close();
               } catch (Throwable var17) {
                  var3.addSuppressed(var17);
               }
            } else {
               conn.close();
            }
         }

      }

      return var22;
   }

   private VsanPerfDiagnosticException[] getExceptions(VsanPerformanceManager perfMgr) {
      VsanPerfDiagnosticException[] exceptions = null;
      VsanProfiler.Point p = _profiler.point("perfMgr.getSupportedDiagnosticExceptions");
      Throwable var4 = null;

      try {
         exceptions = perfMgr.getSupportedDiagnosticExceptions();
      } catch (Throwable var13) {
         var4 = var13;
         throw var13;
      } finally {
         if (p != null) {
            if (var4 != null) {
               try {
                  p.close();
               } catch (Throwable var12) {
                  var4.addSuppressed(var12);
               }
            } else {
               p.close();
            }
         }

      }

      return exceptions;
   }

   @TsService
   public PerformanceDiagnosticData getPerformanceDiagnosticData(ManagedObjectReference clusterRef, ManagedObjectReference taskRef, PerfDiagnosticQuerySpec spec) {
      Validate.notNull(taskRef);
      VsanPerfDiagnosticResult[] perfDiagnosticResults = null;
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var6 = null;

      try {
         VsanPerformanceManager perfMgr = conn.getVsanPerformanceManager();
         perfDiagnosticResults = this.getDiagnosticResults(perfMgr, taskRef, clusterRef);
      } catch (Throwable var15) {
         var6 = var15;
         throw var15;
      } finally {
         if (conn != null) {
            if (var6 != null) {
               try {
                  conn.close();
               } catch (Throwable var14) {
                  var6.addSuppressed(var14);
               }
            } else {
               conn.close();
            }
         }

      }

      int totalPerfDiagIssues = perfDiagnosticResults == null ? 0 : perfDiagnosticResults.length;
      _logger.info("Total received number of performance issues is " + totalPerfDiagIssues);
      if (perfDiagnosticResults != null && perfDiagnosticResults.length != 0) {
         List issues = this.getIssues(perfDiagnosticResults, spec.startTime.getTimeInMillis(), spec.endTime.getTimeInMillis());
         PerformanceDiagnosticData result = new PerformanceDiagnosticData(issues, this.getAllEntityRefIds(perfDiagnosticResults));
         return result;
      } else {
         DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
         _logger.info(String.format("No performance issues were detected for the period between %s and %s for perspective: %s", dateFormat.format(spec.startTime.getTime()), dateFormat.format(spec.endTime.getTime()), spec.queryType.toString()));
         return new PerformanceDiagnosticData();
      }
   }

   @TsService
   public ManagedObjectReference getPerformanceDiagnosticTask(ManagedObjectReference clusterRef, PerfDiagnosticQuerySpec spec) {
      VsanPerfDiagnoseQuerySpec querySpec = new VsanPerfDiagnoseQuerySpec();
      querySpec.queryType = spec.queryType.toString();
      querySpec.startTime = spec.startTime;
      BaseUtils.setUTCTimeZone(querySpec.startTime);
      querySpec.endTime = spec.endTime;
      BaseUtils.setUTCTimeZone(querySpec.endTime);
      ManagedObjectReference perfDiagnosticTaskRef = null;
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var6 = null;

      ManagedObjectReference var39;
      try {
         VsanPerformanceManager perfMgr = conn.getVsanPerformanceManager();

         try {
            VsanProfiler.Point p = _profiler.point("perfMgr.vsanPerfDiagnoseTask");
            Throwable var9 = null;

            try {
               perfDiagnosticTaskRef = perfMgr.vsanPerfDiagnoseTask(querySpec, clusterRef);
               VmodlHelper.assignServerGuid(perfDiagnosticTaskRef, clusterRef.getServerGuid());
            } catch (Throwable var34) {
               var9 = var34;
               throw var34;
            } finally {
               if (p != null) {
                  if (var9 != null) {
                     try {
                        p.close();
                     } catch (Throwable var33) {
                        var9.addSuppressed(var33);
                     }
                  } else {
                     p.close();
                  }
               }

            }
         } catch (Exception var36) {
            _logger.error("Cannot trigger performance diagnose task", var36);
            throw new VsanUiLocalizableException("vsan.perf.query.task.error");
         }

         var39 = perfDiagnosticTaskRef;
      } catch (Throwable var37) {
         var6 = var37;
         throw var37;
      } finally {
         if (conn != null) {
            if (var6 != null) {
               try {
                  conn.close();
               } catch (Throwable var32) {
                  var6.addSuppressed(var32);
               }
            } else {
               conn.close();
            }
         }

      }

      return var39;
   }

   @TsService
   public PerformanceEntitiesData getPerfEntitiesInfo(ManagedObjectReference clusterRef, List entityRefIds) throws Exception {
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var4 = null;

      PerformanceEntitiesData var7;
      try {
         VsanPerformanceManager perfMgr = conn.getVsanPerformanceManager();
         Map entityRefIdToEntityRefDataMap = this.getEntityRefIdToEntityRefDataMap((String[])entityRefIds.toArray(new String[entityRefIds.size()]), perfMgr, clusterRef);
         var7 = new PerformanceEntitiesData(entityRefIdToEntityRefDataMap);
      } catch (Throwable var16) {
         var4 = var16;
         throw var16;
      } finally {
         if (conn != null) {
            if (var4 != null) {
               try {
                  conn.close();
               } catch (Throwable var15) {
                  var4.addSuppressed(var15);
               }
            } else {
               conn.close();
            }
         }

      }

      return var7;
   }

   @TsService
   public void submitFeedbackForDiagnosisResult(ManagedObjectReference clusterRef, VsanPerfDiagnoseFeedbackSpec feedbackSpec, boolean feedbackValue) {
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var5 = null;

      try {
         VsanPerformanceManager perfMgr = conn.getVsanPerformanceManager();
         if (!VsanCapabilityUtils.isPerfDiagnosticsFeedbackSupportedOnVc(clusterRef)) {
            throw new VsanUiLocalizableException("vsan.perf.feedback.submit.error");
         }

         boolean isFeedbackSubmitted = false;

         try {
            VsanProfiler.Point p = _profiler.point("perfMgr.submitFeedbackForDiagnosisResult");
            Throwable var9 = null;

            try {
               isFeedbackSubmitted = perfMgr.submitFeedbackForDiagnosisResult(feedbackSpec, feedbackValue, (String)null, clusterRef);
               if (!isFeedbackSubmitted) {
                  _logger.error("Could not submit feedback: return false");
                  throw new VsanUiLocalizableException("vsan.perf.feedback.submit.error");
               }
            } catch (Throwable var34) {
               var9 = var34;
               throw var34;
            } finally {
               if (p != null) {
                  if (var9 != null) {
                     try {
                        p.close();
                     } catch (Throwable var33) {
                        var9.addSuppressed(var33);
                     }
                  } else {
                     p.close();
                  }
               }

            }
         } catch (Exception var36) {
            _logger.error("Could not submit feedback", var36);
            throw new VsanUiLocalizableException("vsan.perf.feedback.submit.error");
         }
      } catch (Throwable var37) {
         var5 = var37;
         throw var37;
      } finally {
         if (conn != null) {
            if (var5 != null) {
               try {
                  conn.close();
               } catch (Throwable var32) {
                  var5.addSuppressed(var32);
               }
            } else {
               conn.close();
            }
         }

      }

   }

   private List getIssues(VsanPerfDiagnosticResult[] perfDiagnosticResults, long rangeStart, long rangeEnd) {
      List issues = new ArrayList();
      Map idToExceptionMap = new HashMap();
      VsanPerfDiagnosticResult[] var8 = perfDiagnosticResults;
      int var9 = perfDiagnosticResults.length;

      for(int var10 = 0; var10 < var9; ++var10) {
         VsanPerfDiagnosticResult diagnosticResult = var8[var10];
         _logger.info(String.format("Preparing perf diag issue for exceptionId: %s, and recommendation: %s", diagnosticResult.exceptionId, diagnosticResult.recommendation));
         DiagnosticException diagEx = (DiagnosticException)idToExceptionMap.get(diagnosticResult.exceptionId);
         if (diagEx == null) {
            diagEx = new DiagnosticException(diagnosticResult.exceptionId);
            idToExceptionMap.put(diagEx.exceptionId, diagEx);
            issues.add(diagEx);
         }

         diagEx.addEntities(diagnosticResult, rangeStart, rangeEnd);
      }

      return issues;
   }

   private List getAllEntityRefIds(VsanPerfDiagnosticResult[] perfDiagnosticResults) {
      List entityRefIds = new ArrayList();
      VsanPerfDiagnosticResult[] var3 = perfDiagnosticResults;
      int var4 = perfDiagnosticResults.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         VsanPerfDiagnosticResult diagResult = var3[var5];
         if (!ArrayUtils.isEmpty(diagResult.exceptionData)) {
            VsanPerfEntityMetricCSV[] var7 = diagResult.exceptionData;
            int var8 = var7.length;

            for(int var9 = 0; var9 < var8; ++var9) {
               VsanPerfEntityMetricCSV metricCsv = var7[var9];
               if (!entityRefIds.contains(metricCsv.entityRefId)) {
                  entityRefIds.add(metricCsv.entityRefId);
               }
            }
         }
      }

      return entityRefIds;
   }

   protected VsanPerfDiagnosticResult[] getDiagnosticResults(VsanPerformanceManager perfMgr, ManagedObjectReference taskRef, ManagedObjectReference clusterRef) {
      VsanPerfDiagnosticResult[] perfDiagnosticResults = null;

      try {
         VsanProfiler.Point p = _profiler.point("perfMgr.getVsanPerfDiagnosisResult");
         Throwable var6 = null;

         try {
            if (taskRef != null) {
               perfDiagnosticResults = perfMgr.getVsanPerfDiagnosisResult(taskRef, clusterRef);
            }
         } catch (Throwable var17) {
            var6 = var17;
            throw var17;
         } finally {
            if (p != null) {
               if (var6 != null) {
                  try {
                     p.close();
                  } catch (Throwable var16) {
                     var6.addSuppressed(var16);
                  }
               } else {
                  p.close();
               }
            }

         }
      } catch (NotFound var19) {
         _logger.info("There is no diagnostic data in the selected time period.", var19);
      } catch (Exception var20) {
         _logger.error("Could not retrieve performance diagnostic issues", var20);
         throw new VsanUiLocalizableException("vsan.perf.query.issues.error");
      }

      return perfDiagnosticResults;
   }

   private Map getEntityRefIdToEntityRefDataMap(String[] entityRefIds, VsanPerformanceManager perfMgr, ManagedObjectReference clusterRef) throws Exception {
      Map result = new HashMap();
      if (ArrayUtils.isEmpty(entityRefIds)) {
         return result;
      } else {
         VsanPerfEntityInfo[] entityInfos = perfMgr.getVcMoRefFromPerfEntityRefId(clusterRef, entityRefIds);
         if (entityInfos != null) {
            VsanPerfEntityInfo[] var6 = entityInfos;
            int var7 = entityInfos.length;

            for(int var8 = 0; var8 < var7; ++var8) {
               VsanPerfEntityInfo entityInfo = var6[var8];
               EntityRefData refData = new EntityRefData(entityInfo, clusterRef);
               if (refData.managedObjectRef == null) {
                  _logger.info(String.format("Skipping entity with entityRefId %s as missing", entityInfo.entityRefId));
               }

               result.put(entityInfo.entityRefId, refData);
            }
         }

         if (!result.isEmpty()) {
            Map refToNameMap = this.getMoRefToNameMap(result.values());

            EntityRefData refData;
            for(Iterator var12 = result.values().iterator(); var12.hasNext(); refData.managedObjectName = (String)refToNameMap.get(refData.managedObjectRef)) {
               refData = (EntityRefData)var12.next();
            }
         }

         return result;
      }
   }

   private Map getMoRefToNameMap(Collection refDatas) throws Exception {
      Set mos = new HashSet();
      Iterator var3 = refDatas.iterator();

      while(var3.hasNext()) {
         EntityRefData refData = (EntityRefData)var3.next();
         if (!refData.isEntityMissing) {
            mos.add(refData.managedObjectRef);
         }
      }

      Map refToNameMap = new HashMap();
      if (mos.size() > 0) {
         PropertyValue[] propValues = QueryUtil.getProperties((ManagedObjectReference[])mos.toArray(new ManagedObjectReference[0]), new String[]{"name"}).getPropertyValues();
         PropertyValue[] var5 = propValues;
         int var6 = propValues.length;

         for(int var7 = 0; var7 < var6; ++var7) {
            PropertyValue propValue = var5[var7];
            refToNameMap.put((ManagedObjectReference)propValue.resourceObject, (String)propValue.value);
         }
      }

      return refToNameMap;
   }

   private PerformanceDiagnosticsPropertyProvider.EntityTypes getVsanPerfEntityTypes(VsanPerformanceManager performanceManager) throws Exception {
      Measure measure = new Measure("Retrieving performance diagnostics entity types");
      Throwable var3 = null;

      PerformanceDiagnosticsPropertyProvider.EntityTypes var8;
      try {
         Future entityTypesFuture = this.getEntityTypesFuture(performanceManager, measure);
         Future aggregatedEntityTypesFuture = this.getAggregatedEntityTypesFuture(performanceManager, measure);
         Map entityTypes = this.getEntityTypesFromFuture(entityTypesFuture);
         Map aggregatedEntityTypes = this.getEntityTypesFromFuture(aggregatedEntityTypesFuture);
         var8 = new PerformanceDiagnosticsPropertyProvider.EntityTypes(entityTypes, aggregatedEntityTypes);
      } catch (Throwable var17) {
         var3 = var17;
         throw var17;
      } finally {
         if (measure != null) {
            if (var3 != null) {
               try {
                  measure.close();
               } catch (Throwable var16) {
                  var3.addSuppressed(var16);
               }
            } else {
               measure.close();
            }
         }

      }

      return var8;
   }

   private Future getEntityTypesFuture(VsanPerformanceManager performanceManager, Measure measure) {
      Future future = measure.newFuture("VsanPerformanceManager.getSupportedEntityTypes");
      performanceManager.getSupportedEntityTypes(future);
      return future;
   }

   private Future getAggregatedEntityTypesFuture(VsanPerformanceManager perfMgr, Measure measure) {
      Future future = measure.newFuture("VsanPerformanceManager.getAggregatedEntityTypes");
      perfMgr.getAggregatedEntityTypes(future);
      return future;
   }

   private Map getEntityTypesFromFuture(Future future) {
      Object entityTypeMap = new HashMap();

      try {
         VsanPerfEntityType[] entityTypes = (VsanPerfEntityType[])future.get();
         entityTypeMap = createNameToTypeMap(entityTypes);
      } catch (Exception var4) {
         _logger.error("Cannot load supported entity types: ", var4);
      }

      return (Map)entityTypeMap;
   }

   private static Map createNameToTypeMap(VsanPerfEntityType[] types) {
      Map map = new HashMap();
      if (types != null) {
         VsanPerfEntityType[] var2 = types;
         int var3 = types.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            VsanPerfEntityType perfEntityType = var2[var4];
            map.put(perfEntityType.name, perfEntityType);
         }
      }

      return map;
   }

   private static class EntityTypes {
      Map simpleTypes;
      Map aggregatedTypes;

      public EntityTypes(Map simpleTypes, Map aggregatedTyped) {
         this.simpleTypes = simpleTypes;
         this.aggregatedTypes = aggregatedTyped;
      }
   }
}
