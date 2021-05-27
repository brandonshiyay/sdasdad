package com.vmware.vsan.client.services.stretchedcluster;

import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vim.HostSystem;
import com.vmware.vim.binding.vim.HostSystem.ConnectionState;
import com.vmware.vim.binding.vim.HostSystem.PowerState;
import com.vmware.vim.binding.vim.host.VirtualNic;
import com.vmware.vim.binding.vim.host.VirtualNicManagerInfo;
import com.vmware.vim.binding.vim.host.VirtualNicManager.NetConfig;
import com.vmware.vim.binding.vim.host.VirtualNicManager.NicType;
import com.vmware.vim.binding.vim.vsan.host.ConfigInfo;
import com.vmware.vim.binding.vim.vsan.host.DiskResult;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.vsan.host.ConfigInfoEx;
import com.vmware.vim.vsan.binding.vim.vsan.host.EncryptionInfo;
import com.vmware.vim.vsan.binding.vim.vsan.host.VsanManagedDisksInfo;
import com.vmware.vise.data.query.QuerySpec;
import com.vmware.vise.data.query.RequestSpec;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.services.diskmanagement.DiskManagementUtil;
import com.vmware.vsan.client.services.stretchedcluster.model.SharedWitnessClusterValidationData;
import com.vmware.vsan.client.services.stretchedcluster.model.SharedWitnessValidationData;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsan.client.util.VmodlHelper;
import com.vmware.vsan.client.util.dataservice.query.QueryBuilder;
import com.vmware.vsan.client.util.dataservice.query.QueryExecutor;
import com.vmware.vsan.client.util.dataservice.query.QueryExecutorResult;
import com.vmware.vsan.client.util.dataservice.query.QueryResult;
import com.vmware.vsan.client.util.retriever.VsanAsyncDataRetriever;
import com.vmware.vsan.client.util.retriever.VsanDataRetrieverFactory;
import com.vmware.vsphere.client.vsan.data.VsanSemiAutoClaimDisksData;
import com.vmware.vsphere.client.vsan.stretched.WitnessHostValidationResult;
import com.vmware.vsphere.client.vsan.util.DataServiceResponse;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import com.vmware.vsphere.client.vsan.util.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class WitnessHostValidationService {
   @Autowired
   VsanDataRetrieverFactory dataRetrieverFactory;
   @Autowired
   SharedWitnessValidationService sharedWitnessValidationService;
   @Autowired
   VmodlHelper vmodlHelper;
   @Autowired
   private QueryExecutor queryExecutor;
   @Autowired
   VsanClient vsanClient;
   private static final Log logger = LogFactory.getLog(WitnessHostValidationService.class);
   private static final String VNIC_PREFIX = "vim.host.VirtualNic-";
   private static final String VIRTUAL_NIC_MANAGER_INFO_PROPERTY = "config.virtualNicManagerInfo";
   private static final String POWER_STATE_PROPERTY = "runtime.powerState";
   private static final String DISABLED_METHODS_PROPERTY = "disabledMethod";
   private static final String ENTER_MAINTENANCE_MODE_DISABLED_METHOD = "EnterMaintenanceMode_Task";
   private static final String EXIT_MAINTENANCE_MODE_DISABLED_METHOD = "ExitMaintenanceMode_Task";

   public WitnessHostValidationResult validateWitnessHost(ManagedObjectReference[] clustersRefs, ManagedObjectReference witnessHost, boolean queryHostDisks) {
      try {
         Measure measure = new Measure("Validate witness host");
         Throwable var5 = null;

         WitnessHostValidationResult var8;
         try {
            VsanAsyncDataRetriever dataRetriever = this.dataRetrieverFactory.createVsanAsyncDataRetriever(measure, witnessHost).loadIsWitnessHost().loadHostConfig();
            if (queryHostDisks) {
               dataRetriever.loadDisks(witnessHost, false).loadManagedDisks(witnessHost, false);
            }

            WitnessHostValidationResult hostValidationResult = this.getWitnessHostDataServiceData(clustersRefs, witnessHost);
            if (this.isInvalidWitnessCandidate(hostValidationResult)) {
               var8 = hostValidationResult;
               return var8;
            }

            hostValidationResult.isDitSharedWitnessInteroperabilitySupported = VsanCapabilityUtils.isDitSharedWitnessInteroperabilitySupported(clustersRefs);
            hostValidationResult.isStretchedClusterSupported = VsanCapabilityUtils.isStretchedClusterSupportedOnHost(witnessHost, this.vsanClient);
            hostValidationResult.isWitnessHost = dataRetriever.getIsWitnessHost();
            hostValidationResult.isEncrypted = this.isHostEncrypted(dataRetriever.getHostConfig());
            hostValidationResult.witnessValidationResult = this.validateWitnessHost(hostValidationResult, clustersRefs);
            if (queryHostDisks && hostValidationResult.witnessValidationResult.isValidationSuccessful()) {
               hostValidationResult.hostDisksData = this.getHostDiskData(dataRetriever, witnessHost);
            }

            var8 = hostValidationResult;
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
         logger.error("Unable to validate the witness host.");
         throw new VsanUiLocalizableException("vsan.faultDomains.strechedCluster.witnessHost.validate.error", var21);
      }
   }

   private WitnessHostValidationResult getWitnessHostDataServiceData(ManagedObjectReference[] clustersRefs, ManagedObjectReference witnessHost) throws Exception {
      WitnessHostValidationResult result = new WitnessHostValidationResult();
      result.witnessHostRef = witnessHost;
      DataServiceResponse hostProperties = QueryUtil.getProperties(witnessHost, new String[]{"config.virtualNicManagerInfo", "config.network.vnic", "parent", "runtime.connectionState", "runtime.powerState", "runtime.inMaintenanceMode", "disabledMethod", "isVsanWitnessLifecycleManaged"});
      ManagedObjectReference hostParent = (ManagedObjectReference)hostProperties.getProperty(witnessHost, "parent");
      result.isHostInTheSameCluster = Arrays.asList(clustersRefs).stream().anyMatch((clusterRef) -> {
         return clusterRef.equals(hostParent);
      });
      if (!result.isHostInTheSameCluster && this.vmodlHelper.isOfType(hostParent, ClusterComputeResource.class)) {
         result.isHostInVsanEnabledCluster = (Boolean)QueryUtil.getProperty(hostParent, "configurationEx[@type='ClusterConfigInfoEx'].vsanConfigInfo.enabled");
      }

      VirtualNic[] vnic = (VirtualNic[])hostProperties.getProperty(witnessHost, "config.network.vnic");
      VirtualNicManagerInfo nicManagerInfo = (VirtualNicManagerInfo)hostProperties.getProperty(witnessHost, "config.virtualNicManagerInfo");
      result.hasVsanEnabledNic = this.isVsanNetworkEnabled(vnic, nicManagerInfo);
      ConnectionState connectionState = (ConnectionState)hostProperties.getProperty(witnessHost, "runtime.connectionState");
      result.isHostDisconnected = !connectionState.equals(ConnectionState.connected);
      PowerState powerState = (PowerState)hostProperties.getProperty(witnessHost, "runtime.powerState");
      result.isPoweredOn = powerState.equals(PowerState.poweredOn);
      result.isHostInMaintenanceMode = (Boolean)hostProperties.getProperty(witnessHost, "runtime.inMaintenanceMode");
      List disabledMethods = Arrays.asList((Object[])hostProperties.getProperty(witnessHost, "disabledMethod"));
      result.isHostInMaintenanceMode |= disabledMethods.containsAll(Arrays.asList("EnterMaintenanceMode_Task", "ExitMaintenanceMode_Task"));
      result.isWitnessvLCM = BooleanUtils.isTrue((Boolean)hostProperties.getProperty(witnessHost, "isVsanWitnessLifecycleManaged"));
      return result;
   }

   private boolean isVsanNetworkEnabled(VirtualNic[] vnic, VirtualNicManagerInfo nicInfo) {
      if (vnic != null && nicInfo != null && nicInfo.netConfig != null) {
         VirtualNic[] var3 = vnic;
         int var4 = vnic.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            VirtualNic virtualNic = var3[var5];
            String vnicDevice = virtualNic.device;
            if (vnicDevice == null) {
               return false;
            }

            NetConfig[] var8 = nicInfo.netConfig;
            int var9 = var8.length;

            for(int var10 = 0; var10 < var9; ++var10) {
               NetConfig netConfig = var8[var10];
               if ((NicType.vsan.toString().equals(netConfig.nicType) || NicType.vsanWitness.toString().equals(netConfig.nicType)) && netConfig.selectedVnic != null) {
                  String[] var12 = netConfig.selectedVnic;
                  int var13 = var12.length;

                  for(int var14 = 0; var14 < var13; ++var14) {
                     String selectedVnic = var12[var14];
                     if (vnicDevice.equals(this.getVnicDeviceName(selectedVnic))) {
                        return true;
                     }
                  }
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private String getVnicDeviceName(String deviceKey) {
      if (deviceKey == null) {
         return deviceKey;
      } else {
         int vnicPrefix = deviceKey.indexOf("vim.host.VirtualNic-");
         return vnicPrefix < 0 ? deviceKey : deviceKey.substring(vnicPrefix + "vim.host.VirtualNic-".length());
      }
   }

   private boolean isHostEncrypted(ConfigInfo configInfo) {
      if (configInfo instanceof ConfigInfoEx) {
         EncryptionInfo encryptionInfo = ((ConfigInfoEx)configInfo).encryptionInfo;
         if (encryptionInfo != null) {
            return BooleanUtils.isTrue(encryptionInfo.enabled);
         }

         logger.error("ConfigInfo is null or is not an instance of ConfigInfoEx");
      }

      return false;
   }

   private boolean isInvalidWitnessCandidate(WitnessHostValidationResult hostValidationResult) {
      return hostValidationResult.isHostInTheSameCluster || hostValidationResult.isHostInVsanEnabledCluster || hostValidationResult.isHostInMaintenanceMode || !hostValidationResult.hasVsanEnabledNic || hostValidationResult.isHostDisconnected || !hostValidationResult.isPoweredOn;
   }

   private SharedWitnessValidationData validateWitnessHost(WitnessHostValidationResult hostValidationResult, ManagedObjectReference[] clustersRefs) {
      SharedWitnessValidationData clustersCountValidationResult = this.validateClustersCount(clustersRefs, hostValidationResult.isWitnessHost);
      if (!clustersCountValidationResult.isValidationSuccessful()) {
         return clustersCountValidationResult;
      } else {
         return !hostValidationResult.isWitnessHost && clustersRefs.length <= 1 ? this.getValidWitnessResult(clustersRefs) : this.validateSharedWitnessHost(hostValidationResult, clustersRefs);
      }
   }

   private SharedWitnessValidationData validateSharedWitnessHost(WitnessHostValidationResult hostValidationResult, ManagedObjectReference[] clustersRefs) {
      if (VsanCapabilityUtils.isSharedWitnessSupportedOnVc(hostValidationResult.witnessHostRef) && VsanCapabilityUtils.isSharedWitnessSupported(hostValidationResult.witnessHostRef)) {
         hostValidationResult.vLCMToBeDisabled = hostValidationResult.isWitnessvLCM;
         return this.sharedWitnessValidationService.querySharedWitnessCompatibility(hostValidationResult.witnessHostRef, clustersRefs);
      } else {
         SharedWitnessValidationData result = new SharedWitnessValidationData();
         final String errorKey = VsanCapabilityUtils.isSharedWitnessSupportedOnVc(hostValidationResult.witnessHostRef) ? "vsan.sharedWitness.validation.notSupported" : "vsan.faultDomains.stretchedCluster.witnessHost.validation.hostIsExternalWitness";
         result.witnessHostValidationErrors = new ArrayList() {
            {
               this.add(Utils.getLocalizedString(errorKey));
            }
         };
         return result;
      }
   }

   private SharedWitnessValidationData validateClustersCount(ManagedObjectReference[] clusterRefs, boolean isWitnessHost) {
      SharedWitnessValidationData result = new SharedWitnessValidationData();
      result.clustersValidation = new HashMap(clusterRefs.length);
      Map clustersCount = this.getClustersConnectedHostCount(clusterRefs);
      ManagedObjectReference[] var5 = clusterRefs;
      int var6 = clusterRefs.length;

      for(int var7 = 0; var7 < var6; ++var7) {
         ManagedObjectReference clusterRef = var5[var7];
         SharedWitnessClusterValidationData clusterValidation = new SharedWitnessClusterValidationData(clusterRef);
         clusterValidation.compatible = false;
         Integer hostsCount = (Integer)clustersCount.get(clusterRef);
         if (hostsCount < 2) {
            clusterValidation.validationMessage = Utils.getLocalizedString("vsan.sharedWitness.validation.cluster.insufficientNodes");
         } else if (hostsCount > 2 && isWitnessHost) {
            String errorKey = VsanCapabilityUtils.isSharedWitnessSupportedOnVc(clusterRef) ? "vsan.sharedWitness.validation.cluster.tooManyNodes" : "vsan.faultDomains.stretchedCluster.witnessHost.validation.hostIsExternalWitness";
            clusterValidation.validationMessage = Utils.getLocalizedString(errorKey);
         } else {
            clusterValidation.compatible = true;
         }

         result.clustersValidation.put(clusterRef.getValue(), clusterValidation);
      }

      return result;
   }

   private Map getClustersConnectedHostCount(ManagedObjectReference[] clustersRefs) {
      try {
         QueryBuilder queryBuilder = new QueryBuilder();
         ManagedObjectReference[] var3 = clustersRefs;
         int var4 = clustersRefs.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            ManagedObjectReference clusterRef = var3[var5];
            queryBuilder.addQuery(this.getConnectedHostsQuery(clusterRef));
         }

         RequestSpec rs = queryBuilder.build();
         QueryExecutorResult queryExecutorResult = this.queryExecutor.execute(rs);
         Map result = new HashMap();
         ManagedObjectReference[] var15 = clustersRefs;
         int var7 = clustersRefs.length;

         for(int var8 = 0; var8 < var7; ++var8) {
            ManagedObjectReference clusterRef = var15[var8];
            QueryResult clusterResult = queryExecutorResult.getQueryResult(clusterRef.getValue());
            result.put(clusterRef, clusterResult.totalItemsCount);
         }

         return result;
      } catch (Exception var11) {
         return new HashMap(0);
      }
   }

   private QuerySpec getConnectedHostsQuery(ManagedObjectReference clusterRef) {
      return (QuerySpec)(new QueryBuilder()).newQuery(clusterRef.getValue()).select("runtime.connectionState").from(clusterRef).join(HostSystem.class).on("host").where().propertyEquals("runtime.connectionState", ConnectionState.connected).end().getQueries().get(0);
   }

   private SharedWitnessValidationData getValidWitnessResult(ManagedObjectReference[] clustersRefs) {
      SharedWitnessValidationData result = new SharedWitnessValidationData();
      result.clustersValidation = (Map)Arrays.asList(clustersRefs).stream().collect(Collectors.toMap((c) -> {
         return c.getValue();
      }, (c) -> {
         return new SharedWitnessClusterValidationData(c);
      }));
      return result;
   }

   private VsanSemiAutoClaimDisksData getHostDiskData(VsanAsyncDataRetriever dataRetriever, ManagedObjectReference hostRef) throws InterruptedException, ExecutionException {
      try {
         Map hostToDisks = dataRetriever.getDisks();
         Map hostToManagedDisks = dataRetriever.getManagedDisks();
         return DiskManagementUtil.getNotClaimedDisksData(hostRef, (DiskResult[])hostToDisks.get(hostRef), (List)null, (VsanManagedDisksInfo)hostToManagedDisks.get(hostRef));
      } catch (Exception var5) {
         logger.error("Unable to get the host disks data");
         throw var5;
      }
   }
}
