package com.vmware.vsan.client.services.capacity;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vim.HostSystem.ConnectionState;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.vsan.ConfigInfoEx;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.services.capacity.model.CapacityBasicInfo;
import com.vmware.vsan.client.services.config.ConfigInfoService;
import com.vmware.vsan.client.services.csd.CsdService;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsphere.client.vsan.base.util.BaseUtils;
import com.vmware.vsphere.client.vsan.util.DataServiceResponse;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CapacityBasicInfoService {
   private final Log logger = LogFactory.getLog(this.getClass());
   @Autowired
   private ConfigInfoService configInfoService;
   @Autowired
   private CsdService csdService;
   private static final String[] HOST_CONNECTIVITY_PROPERTIES = new String[]{"runtime.connectionState", "runtime.inMaintenanceMode"};

   @TsService
   public CapacityBasicInfo getCapacityBasicInfo(ManagedObjectReference objectRef) {
      ManagedObjectReference clusterRef = BaseUtils.getCluster(objectRef);
      Validate.notNull(clusterRef);
      CapacityBasicInfo basicInfo = new CapacityBasicInfo();
      basicInfo.clusterRef = clusterRef;

      try {
         Measure measure = new Measure("Get capacity basic info");
         Throwable var5 = null;

         CapacityBasicInfo var8;
         try {
            CompletableFuture configInfo = this.configInfoService.getVsanConfigInfoAsync(clusterRef);
            DataServiceResponse hostProperties = this.queryHostProperties(clusterRef);
            basicInfo.isComputeOnlyCluster = this.csdService.isComputeOnlyClusterByConfigInfoEx((ConfigInfoEx)configInfo.get());
            if (hostProperties != null && !ArrayUtils.isEmpty(hostProperties.getPropertyValues())) {
               basicInfo.isCsdSupported = this.csdService.isCsdSupported(objectRef);
               basicInfo.hostCount = hostProperties.getResourceObjects().size();
               basicInfo.faultyHostsTotalCount = this.getFaultyHostsCount(hostProperties);
               basicInfo.isHistoricalCapacitySupported = this.isHistoricalCapacitySupported((ManagedObjectReference[])hostProperties.getResourceObjects().toArray(new ManagedObjectReference[0]));
               return basicInfo;
            }

            var8 = basicInfo;
         } catch (Throwable var19) {
            var5 = var19;
            throw var19;
         } finally {
            if (measure != null) {
               if (var5 != null) {
                  try {
                     measure.close();
                  } catch (Throwable var18) {
                     var5.addSuppressed(var18);
                  }
               } else {
                  measure.close();
               }
            }

         }

         return var8;
      } catch (Exception var21) {
         this.logger.error("Unable to extract cluster's configuration for capacity view.", var21);
         throw new VsanUiLocalizableException(var21);
      }
   }

   private boolean isHistoricalCapacitySupported(ManagedObjectReference[] hosts) {
      ManagedObjectReference[] var2 = hosts;
      int var3 = hosts.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         ManagedObjectReference host = var2[var4];
         boolean isSupported = VsanCapabilityUtils.isHistoricalCapacitySupportedOnHost(host);
         if (!isSupported) {
            return false;
         }
      }

      return true;
   }

   private DataServiceResponse queryHostProperties(ManagedObjectReference clusterRef) {
      try {
         return QueryUtil.getPropertiesForRelatedObjects(clusterRef, "allVsanHosts", ClusterComputeResource.class.getSimpleName(), HOST_CONNECTIVITY_PROPERTIES);
      } catch (Exception var3) {
         throw new VsanUiLocalizableException("vsan.cluster.monitor.capacity.hostProperties.error");
      }
   }

   private int getFaultyHostsCount(DataServiceResponse hostPropertiesResponse) {
      int faultyHostsTotalCount = 0;
      Iterator var3 = hostPropertiesResponse.getResourceObjects().iterator();

      while(true) {
         ConnectionState connectionState;
         Boolean isInMaintenanceMode;
         do {
            if (!var3.hasNext()) {
               return faultyHostsTotalCount;
            }

            ManagedObjectReference hostRef = (ManagedObjectReference)var3.next();
            connectionState = (ConnectionState)hostPropertiesResponse.getProperty(hostRef, "runtime.connectionState");
            isInMaintenanceMode = (Boolean)hostPropertiesResponse.getProperty(hostRef, "runtime.inMaintenanceMode");
         } while(!Boolean.TRUE.equals(isInMaintenanceMode) && com.vmware.vsan.client.services.common.data.ConnectionState.fromHostState(connectionState).equals(com.vmware.vsan.client.services.common.data.ConnectionState.connected));

         ++faultyHostsTotalCount;
      }
   }

   private boolean areAllHostsFaulty(int hostCount, int faultyHostsTotalCount) {
      return hostCount == faultyHostsTotalCount;
   }
}
