package com.vmware.vsan.client.services.configurecluster;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.vsan.host.DiskMapInfoEx;
import com.vmware.vise.data.query.ObjectReferenceService;
import com.vmware.vise.data.query.PropertyValue;
import com.vmware.vsan.client.services.common.data.ConnectionState;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsan.client.util.NoOpMeasure;
import com.vmware.vsan.client.util.retriever.VsanAsyncDataRetriever;
import com.vmware.vsan.client.util.retriever.VsanDataRetrieverFactory;
import com.vmware.vsphere.client.vsan.data.VsanConfigSpec;
import com.vmware.vsphere.client.vsan.impl.ConfigureVsanClusterMutationProvider;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import com.vmware.vsphere.client.vsan.util.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConfigureClusterService {
   private static final Logger logger = LoggerFactory.getLogger(ConfigureClusterService.class);
   private static final String HA_PROPERTY = "configurationEx[@type='ClusterConfigInfoEx'].dasConfig.enabled";
   private static final String DPM_PROPERTY = "configurationEx[@type='ClusterConfigInfoEx'].dpmConfigInfo.enabled";
   @Autowired
   private ObjectReferenceService refService;
   @Autowired
   private ConfigureVsanClusterMutationProvider mutationProvider;
   @Autowired
   private VsanDataRetrieverFactory dataRetrieverFactory;

   @TsService
   public List getClusterHostFaultDomainData(ManagedObjectReference clusterRef) throws Exception {
      PropertyValue[] props = QueryUtil.getPropertiesForRelatedObjects(clusterRef, "host", ClusterComputeResource.class.getSimpleName(), new String[]{"name", "primaryIconId", "runtime.connectionState", "config.product.version", "config.vsanHostConfig.faultDomainInfo.name"}).getPropertyValues();
      Map propsMap = QueryUtil.groupPropertiesByObject(props);
      List result = new ArrayList();
      Iterator var5 = propsMap.keySet().iterator();

      while(var5.hasNext()) {
         ManagedObjectReference mor = (ManagedObjectReference)var5.next();
         List objectProps = (List)propsMap.get(mor);
         HostFaultDomainData.Builder builder = new HostFaultDomainData.Builder();
         builder.hostUid(this.refService.getUid(mor));
         Iterator var9 = objectProps.iterator();

         while(var9.hasNext()) {
            PropertyValue property = (PropertyValue)var9.next();
            String var11 = property.propertyName;
            byte var12 = -1;
            switch(var11.hashCode()) {
            case -826278890:
               if (var11.equals("primaryIconId")) {
                  var12 = 1;
               }
               break;
            case 3373707:
               if (var11.equals("name")) {
                  var12 = 0;
               }
               break;
            case 707737491:
               if (var11.equals("config.vsanHostConfig.faultDomainInfo.name")) {
                  var12 = 4;
               }
               break;
            case 1445673005:
               if (var11.equals("config.product.version")) {
                  var12 = 3;
               }
               break;
            case 2004020797:
               if (var11.equals("runtime.connectionState")) {
                  var12 = 2;
               }
            }

            switch(var12) {
            case 0:
               builder.name((String)property.value);
               break;
            case 1:
               builder.primaryIconId((String)property.value);
               break;
            case 2:
               ConnectionState state = ConnectionState.valueOf(((com.vmware.vim.binding.vim.HostSystem.ConnectionState)property.value).name());
               builder.connectionState(state);
               break;
            case 3:
               builder.version((String)property.value);
               break;
            case 4:
               builder.faultDomainName((String)property.value);
            }
         }

         result.add(builder.createHostFaultDomainData());
      }

      return result;
   }

   @TsService
   public String getPrerequisitesWarning(ManagedObjectReference clusterRef) throws Exception {
      Map result = QueryUtil.getProperties(clusterRef, new String[]{"configurationEx[@type='ClusterConfigInfoEx'].dasConfig.enabled", "configurationEx[@type='ClusterConfigInfoEx'].dpmConfigInfo.enabled"}).getMap();
      Map properties = (Map)result.get(clusterRef);
      boolean haEnabled = Boolean.valueOf("" + properties.get("configurationEx[@type='ClusterConfigInfoEx'].dasConfig.enabled"));
      boolean dpmEnabled = Boolean.valueOf("" + properties.get("configurationEx[@type='ClusterConfigInfoEx'].dpmConfigInfo.enabled"));
      if (haEnabled && dpmEnabled) {
         return Utils.getLocalizedString("vsan.generalConfig.haAndDpm.enabled.warning");
      } else if (haEnabled) {
         return Utils.getLocalizedString("vsan.generalConfig.ha.enabled.warning");
      } else {
         return dpmEnabled ? Utils.getLocalizedString("vsan.generalConfig.dpm.enabled.warning") : null;
      }
   }

   @TsService
   public boolean hasHybridDiskGroups(ManagedObjectReference clusterRef) {
      try {
         ManagedObjectReference[] hosts = (ManagedObjectReference[])QueryUtil.getProperty(clusterRef, "host", (Object)null);
         VsanAsyncDataRetriever dataRetriever = this.dataRetrieverFactory.createVsanAsyncDataRetriever(new NoOpMeasure(), clusterRef).loadDiskMappings(Arrays.asList(hosts));
         Map hostToDiskMappings = dataRetriever.getDiskMappings();
         Iterator var5 = hostToDiskMappings.values().iterator();

         while(true) {
            DiskMapInfoEx[] diskGroups;
            do {
               if (!var5.hasNext()) {
                  return false;
               }

               diskGroups = (DiskMapInfoEx[])var5.next();
            } while(diskGroups == null);

            DiskMapInfoEx[] var7 = diskGroups;
            int var8 = diskGroups.length;

            for(int var9 = 0; var9 < var8; ++var9) {
               DiskMapInfoEx info = var7[var9];
               if (!info.isAllFlash) {
                  return true;
               }
            }
         }
      } catch (Exception var11) {
         logger.warn("Failed to check disk groups for cluster: " + clusterRef);
         return false;
      }
   }

   @TsService
   public boolean hasAnyDiskGroups(ManagedObjectReference clusterRef) {
      try {
         ManagedObjectReference[] hosts = (ManagedObjectReference[])QueryUtil.getProperty(clusterRef, "host", (Object)null);
         if (ArrayUtils.isEmpty(hosts)) {
            return false;
         } else {
            Measure measure = new Measure("ConfigureClusterService.hasAnyDiskGroups");
            Throwable var4 = null;

            boolean var5;
            try {
               var5 = this.dataRetrieverFactory.createVsanAsyncDataRetriever(measure, clusterRef).loadDiskMappings(Arrays.asList(hosts)).getDiskMappings().values().stream().anyMatch(ArrayUtils::isNotEmpty);
            } catch (Throwable var15) {
               var4 = var15;
               throw var15;
            } finally {
               if (measure != null) {
                  if (var4 != null) {
                     try {
                        measure.close();
                     } catch (Throwable var14) {
                        var4.addSuppressed(var14);
                     }
                  } else {
                     measure.close();
                  }
               }

            }

            return var5;
         }
      } catch (Exception var17) {
         logger.warn("Failed to check disk groups for cluster: " + clusterRef);
         return false;
      }
   }

   @TsService("configureClusterTask")
   public ManagedObjectReference configureCluster(ManagedObjectReference clusterRef, VsanConfigSpec vsanConfigSpec) {
      return this.mutationProvider.configure(clusterRef, vsanConfigSpec);
   }
}
