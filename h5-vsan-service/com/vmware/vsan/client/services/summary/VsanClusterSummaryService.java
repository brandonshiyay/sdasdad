package com.vmware.vsan.client.services.summary;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanPerfEntityType;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.services.summary.model.SummaryPerformanceData;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsan.client.util.retriever.VsanAsyncDataRetriever;
import com.vmware.vsan.client.util.retriever.VsanDataRetrieverFactory;
import com.vmware.vsphere.client.vsan.perf.VsanPerfPropertyProvider;
import com.vmware.vsphere.client.vsan.perf.model.PerfEntityStateData;
import com.vmware.vsphere.client.vsan.perf.model.PerfQuerySpec;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class VsanClusterSummaryService {
   private static final String CLUSTER_VM_CONSUMPTION_ENTITY = "com.vmware.vsan.perf.entity.cluster-domclient";
   @Autowired
   private VsanDataRetrieverFactory dataRetrieverFactory;
   @Autowired
   private VsanPerfPropertyProvider perfPropertyProvider;

   @TsService
   public SummaryPerformanceData getSummaryPerformanceData(ManagedObjectReference clusterRef) {
      SummaryPerformanceData data = new SummaryPerformanceData();
      data.isPerfEnabled = this.perfPropertyProvider.getPerfServiceEnabled(clusterRef);
      if (!data.isPerfEnabled) {
         return data;
      } else {
         data.isTopContributorsSupported = VsanCapabilityUtils.isTopContributorsSupported(clusterRef);

         try {
            Measure measure = new Measure("Retrieving performance entity types: ");
            Throwable var4 = null;

            SummaryPerformanceData var10;
            try {
               VsanAsyncDataRetriever dataRetriever = this.dataRetrieverFactory.createVsanAsyncDataRetriever(measure, clusterRef).loadSupportedEntityTypes();
               String entityUuid = (String)QueryUtil.getProperty(clusterRef, "configurationEx[@type='ClusterConfigInfoEx'].vsanConfigInfo.defaultConfig.uuid");
               Map entityTypes = this.perfPropertyProvider.handlePerfEntityTypes(dataRetriever.getSupportedEntityTypes());
               data.clusterDomClientEntity = (VsanPerfEntityType)entityTypes.get("com.vmware.vsan.perf.entity.cluster-domclient");
               PerfQuerySpec spec = this.createPerQuerySpec(data.clusterDomClientEntity.name, entityUuid);
               List chartsData = this.perfPropertyProvider.getEntityPerfState(clusterRef, new PerfQuerySpec[]{spec});
               if (!chartsData.isEmpty()) {
                  data.chartsData = (PerfEntityStateData)chartsData.get(0);
               }

               var10 = data;
            } catch (Throwable var20) {
               var4 = var20;
               throw var20;
            } finally {
               if (measure != null) {
                  if (var4 != null) {
                     try {
                        measure.close();
                     } catch (Throwable var19) {
                        var4.addSuppressed(var19);
                     }
                  } else {
                     measure.close();
                  }
               }

            }

            return var10;
         } catch (Exception var22) {
            throw new VsanUiLocalizableException("vsan.summary.perf.data.error", "Failed to extract performance charts data for cluster " + clusterRef, var22, new Object[0]);
         }
      }
   }

   PerfQuerySpec createPerQuerySpec(String entityType, String entityUuid) {
      Date now = this.currentTimeWithoutSeconds();
      PerfQuerySpec spec = new PerfQuerySpec();
      spec.entityType = entityType;
      spec.entityUuid = entityUuid;
      spec.startTime = now.getTime() - 7200000L;
      spec.endTime = now.getTime();
      return spec;
   }

   Date currentTimeWithoutSeconds() {
      Calendar now = Calendar.getInstance();
      now.set(13, 0);
      now.set(14, 0);
      return now.getTime();
   }
}
