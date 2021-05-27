package com.vmware.vsphere.client.vsan.health.historical;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanClusterHealthGroup;
import com.vmware.vim.vsan.binding.vim.cluster.VsanClusterHealthSummary;
import com.vmware.vim.vsan.binding.vim.cluster.VsanClusterHealthTest;
import com.vmware.vim.vsan.binding.vim.cluster.VsanHistoricalHealthQuerySpec;
import com.vmware.vim.vsan.binding.vim.cluster.VsanHistoricalHealthTest;
import com.vmware.vim.vsan.binding.vim.cluster.VsanVcClusterHealthSystem;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsphere.client.vsan.base.util.BaseUtils;
import com.vmware.vsphere.client.vsan.health.VsanHealthData;
import com.vmware.vsphere.client.vsan.health.util.VsanHealthUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;

public class VsanHistoricalHealthService {
   @Autowired
   private VsanClient vsanClient;

   @TsService
   public VsanHealthData getHistoricalHealthSummary(ManagedObjectReference clusterRef, Calendar from, Calendar to) {
      VsanClusterHealthSummary healthSummary = this.requestHistoricalHealth(clusterRef, from, to, (String)null, (String)null);
      return VsanHealthUtil.getVsanHealthData(healthSummary, clusterRef, false, false);
   }

   @TsService
   public List getHistoricalHealthForTest(ManagedObjectReference clusterRef, String groupId, String testId, Calendar from, Calendar to) {
      VsanClusterHealthTest healthTest = this.requestHistoricalTest(clusterRef, groupId, testId, from, to);
      return (List)(healthTest == null ? new ArrayList() : VsanHealthUtil.createTestInstancesDetails(healthTest.getHistoricalResults(), clusterRef.getServerGuid()));
   }

   @TsService
   public List getHistoricalHealthForTestInstance(ManagedObjectReference clusterRef, String groupId, String testId, Calendar timestamp) {
      VsanClusterHealthTest healthTest = this.requestHistoricalTest(clusterRef, groupId, testId, timestamp, timestamp);
      if (healthTest != null && !ArrayUtils.isEmpty(healthTest.getHistoricalResults())) {
         Optional historicalTestOpt = Arrays.stream(healthTest.getHistoricalResults()).filter((historicalTest) -> {
            return timestamp.compareTo(historicalTest.timestamp) == 0;
         }).findFirst();
         return (List)(!historicalTestOpt.isPresent() ? new ArrayList() : VsanHealthUtil.createTestTables(((VsanHistoricalHealthTest)historicalTestOpt.get()).testDetails, clusterRef.getServerGuid()));
      } else {
         return new ArrayList();
      }
   }

   private VsanClusterHealthSummary requestHistoricalHealth(ManagedObjectReference clusterRef, Calendar from, Calendar to, String groupId, String testId) {
      if (!VsanCapabilityUtils.isHistoricalHealthSupported(clusterRef)) {
         throw new VsanUiLocalizableException("vsan.common.error.notSupported");
      } else {
         BaseUtils.setUTCTimeZone(from);
         BaseUtils.setUTCTimeZone(to);
         VsanHistoricalHealthQuerySpec spec = new VsanHistoricalHealthQuerySpec(new ManagedObjectReference[]{clusterRef}, from, to, testId, groupId);

         VsanClusterHealthSummary[] healthSummaries;
         try {
            VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
            Throwable var9 = null;

            try {
               VsanVcClusterHealthSystem healthSystem = conn.getVsanVcClusterHealthSystem();
               Measure measure = new Measure("VsanVcClusterHealthSystem.queryClusterHistoricalHealth");
               Throwable var12 = null;

               try {
                  healthSummaries = healthSystem.queryClusterHistoricalHealth(spec);
               } catch (Throwable var37) {
                  var12 = var37;
                  throw var37;
               } finally {
                  if (measure != null) {
                     if (var12 != null) {
                        try {
                           measure.close();
                        } catch (Throwable var36) {
                           var12.addSuppressed(var36);
                        }
                     } else {
                        measure.close();
                     }
                  }

               }
            } catch (Throwable var39) {
               var9 = var39;
               throw var39;
            } finally {
               if (conn != null) {
                  if (var9 != null) {
                     try {
                        conn.close();
                     } catch (Throwable var35) {
                        var9.addSuppressed(var35);
                     }
                  } else {
                     conn.close();
                  }
               }

            }
         } catch (Exception var41) {
            throw new VsanUiLocalizableException("vsan.health.retrieve.historical.summary.error", var41);
         }

         if (ArrayUtils.isEmpty(healthSummaries)) {
            return null;
         } else {
            Optional healthSummaryOpt = Arrays.stream(healthSummaries).filter((summary) -> {
               return this.isEqualsToServiceResultMoRef(summary.cluster, clusterRef);
            }).findFirst();
            return !healthSummaryOpt.isPresent() ? null : (VsanClusterHealthSummary)healthSummaryOpt.get();
         }
      }
   }

   private boolean isEqualsToServiceResultMoRef(ManagedObjectReference serviceResultMoRef, ManagedObjectReference moRef) {
      return serviceResultMoRef.getType().equals(moRef.getType()) && serviceResultMoRef.getValue().equals(moRef.getValue());
   }

   private VsanClusterHealthTest requestHistoricalTest(ManagedObjectReference clusterRef, String groupId, String testId, Calendar from, Calendar to) {
      VsanClusterHealthSummary healthSummary = this.requestHistoricalHealth(clusterRef, from, to, groupId, testId);
      if (healthSummary == null) {
         return null;
      } else {
         VsanClusterHealthTest healthTest = this.getTestById(healthSummary, groupId, testId);
         return healthTest;
      }
   }

   private VsanClusterHealthTest getTestById(VsanClusterHealthSummary healthSummary, String groupId, String testId) {
      Optional groupOpt = Arrays.stream(healthSummary.getGroups()).filter((group) -> {
         return groupId.equals(group.getGroupId());
      }).findFirst();
      if (!groupOpt.isPresent()) {
         return null;
      } else {
         Optional testOpt = Arrays.stream(((VsanClusterHealthGroup)groupOpt.get()).getGroupTests()).filter((test) -> {
            return testId.equals(test.testId);
         }).findFirst();
         return testOpt.isPresent() ? (VsanClusterHealthTest)testOpt.get() : null;
      }
   }
}
