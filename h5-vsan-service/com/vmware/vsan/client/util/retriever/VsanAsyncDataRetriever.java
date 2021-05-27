package com.vmware.vsan.client.util.retriever;

import com.vmware.vim.binding.pbm.capability.provider.CapabilityObjectSchema;
import com.vmware.vim.binding.pbm.compliance.ComplianceResult;
import com.vmware.vim.binding.pbm.profile.Profile;
import com.vmware.vim.binding.vim.option.OptionValue;
import com.vmware.vim.binding.vim.vsan.host.ConfigInfo;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VSANWitnessHostInfo;
import com.vmware.vim.vsan.binding.vim.cluster.VsanCapability;
import com.vmware.vim.vsan.binding.vim.cluster.VsanClusterHclInfo;
import com.vmware.vim.vsan.binding.vim.cluster.VsanIscsiLUN;
import com.vmware.vim.vsan.binding.vim.cluster.VsanIscsiTarget;
import com.vmware.vim.vsan.binding.vim.cluster.VsanObjectIdentityAndHealth;
import com.vmware.vim.vsan.binding.vim.cluster.VsanObjectInformation;
import com.vmware.vim.vsan.binding.vim.cluster.VsanPerfEntityMetricCSV;
import com.vmware.vim.vsan.binding.vim.cluster.VsanPerfEntityType;
import com.vmware.vim.vsan.binding.vim.cluster.VsanPerfNodeInformation;
import com.vmware.vim.vsan.binding.vim.cluster.VsanPerfTopQuerySpec;
import com.vmware.vim.vsan.binding.vim.cluster.VsanSpaceUsageWithDatastoreType;
import com.vmware.vim.vsan.binding.vim.cluster.VsanVcClusterConfigSystem;
import com.vmware.vim.vsan.binding.vim.cluster.VsanVcClusterHealthSystem;
import com.vmware.vim.vsan.binding.vim.vsan.ConfigInfoEx;
import com.vmware.vim.vsan.binding.vim.vsan.DataEfficiencyCapacityState;
import com.vmware.vim.vsan.binding.vim.vsan.VsanFileServicePreflightCheckResult;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.services.capacity.model.DatastoreType;
import com.vmware.vsan.client.services.cns.model.Volume;
import com.vmware.vsan.client.services.common.PermissionService;
import com.vmware.vsan.client.services.fileservice.model.FileSharesPaginationSpec;
import com.vmware.vsan.client.sessionmanager.vlsi.client.pbm.PbmClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsan.client.util.VmodlHelper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class VsanAsyncDataRetriever {
   private static final Log logger = LogFactory.getLog(VsanAsyncDataRetriever.class);
   private static final String CONFIG_INFO_EX = "VsanVcClusterConfigSystem.getConfigInfoEx";
   private static final String CLUSTER_HCL_INFO = "VsanVcClusterHealthSystem.getClusterHclInfo";
   private Measure measure;
   private ManagedObjectReference objRef;
   private Map dataRetrievers;
   private final VcClient vcClient;
   private final VsanClient vsanClient;
   private final VmodlHelper vmodlHelper;
   private final PbmClient pbmClient;
   private final PermissionService permissionService;

   public VsanAsyncDataRetriever(Measure measure, ManagedObjectReference objRef, VcClient vcClient, VsanClient vsanClient, VmodlHelper vmodlHelper, PbmClient pbmClient, PermissionService permissionService) {
      Validate.notNull(measure);
      Validate.notNull(vcClient);
      Validate.notNull(vsanClient);
      Validate.notNull(vmodlHelper);
      Validate.notNull(pbmClient);
      this.measure = measure;
      this.objRef = objRef;
      this.vcClient = vcClient;
      this.vsanClient = vsanClient;
      this.vmodlHelper = vmodlHelper;
      this.pbmClient = pbmClient;
      this.permissionService = permissionService;
      this.dataRetrievers = new HashMap();
   }

   public VsanAsyncDataRetriever loadData(String name, ClosureDataRetriever.ClosureWithFuture closure) {
      this.addDataRetriever(name, new ClosureDataRetriever(name, this.measure, closure));
      return this;
   }

   public VsanAsyncDataRetriever loadData(String name, ClosureDataRetriever.ClosureWithoutFuture closure) {
      this.addDataRetriever(name, new ClosureDataRetriever(name, this.measure, closure));
      return this;
   }

   public Object getData(String name) {
      return this.getResult(name);
   }

   public VsanAsyncDataRetriever loadObjectIdentitiesAndHealth(Set uuids) {
      return this.loadObjectIdentities(uuids, true);
   }

   public VsanAsyncDataRetriever loadObjectIdentities(Set uuids) {
      return this.loadObjectIdentities((Set)null, false);
   }

   private VsanAsyncDataRetriever loadObjectIdentities(Set uuids, boolean queryHealth) {
      if (!VsanCapabilityUtils.getCapabilities(this.objRef).isObjectIdentitiesSupported) {
         throw new VsanUiLocalizableException("vsan.virtualObjects.error.hostVersion");
      } else {
         this.addDataRetriever(ObjectIdentitiesDataRetriever.class, new ObjectIdentitiesDataRetriever(this.objRef, this.measure, uuids, queryHealth, this.vsanClient));
         return this;
      }
   }

   public VsanAsyncDataRetriever loadExtensionIdentities() {
      this.addDataRetriever(ExtensionIdentitiesDataRetriever.class, new ExtensionIdentitiesDataRetriever(this.objRef, this.measure, this.vsanClient));
      return this;
   }

   public VsanAsyncDataRetriever loadTargetVrConfigIdentities() {
      this.addDataRetriever(HbrConfigIdentitiesDataRetriever.class, new HbrConfigIdentitiesDataRetriever(this.objRef, this.measure, this.vsanClient));
      return this;
   }

   public VsanAsyncDataRetriever loadObjectInformation(Set uuids) {
      Validate.notNull(uuids);
      this.addDataRetriever(ObjectInformationDataRetriever.class, new ObjectInformationDataRetriever(this.objRef, this.measure, uuids, this.vsanClient));
      return this;
   }

   public VsanAsyncDataRetriever loadIscsiTargets() {
      this.addDataRetriever(IscsiTargetsDataRetriever.class, new IscsiTargetsDataRetriever(this.objRef, this.measure, this.vsanClient));
      return this;
   }

   public VsanAsyncDataRetriever loadIscsiLuns() {
      this.addDataRetriever(IscsiLunsDataRetriever.class, new IscsiLunsDataRetriever(this.objRef, this.measure, this.vsanClient));
      return this;
   }

   public VsanAsyncDataRetriever loadFileShares(FileSharesPaginationSpec spec) {
      this.addDataRetriever(FileSharesDataRetriever.class, new FileSharesDataRetriever(this.objRef, this.measure, this.vsanClient, spec));
      return this;
   }

   public VsanAsyncDataRetriever loadFileShares() {
      this.addDataRetriever(FileSharesDataRetriever.class, new FileSharesDataRetriever(this.objRef, this.measure, this.vsanClient, (FileSharesPaginationSpec)null));
      return this;
   }

   public VsanAsyncDataRetriever loadFileServicePrecheckResult() {
      this.addDataRetriever(FileServicePrecheckDataRetriever.class, new FileServicePrecheckDataRetriever(this.objRef, this.measure, this.vsanClient));
      return this;
   }

   public VsanAsyncDataRetriever loadFileSharesCount() {
      this.addDataRetriever(FileSharesCountRetriever.class, new FileSharesCountRetriever(this.objRef, this.measure, this.vsanClient, (FileSharesPaginationSpec)null));
      return this;
   }

   public VsanAsyncDataRetriever loadFileSharesCount(FileSharesPaginationSpec paginationSpec) {
      this.addDataRetriever(FileSharesCountRetriever.class, new FileSharesCountRetriever(this.objRef, this.measure, this.vsanClient, paginationSpec));
      return this;
   }

   public VsanAsyncDataRetriever loadClusterUuids() {
      this.addDataRetriever(ClusterUuidsDataRetriever.class, new ClusterUuidsDataRetriever(this.objRef, this.measure, this.vcClient, this.vmodlHelper));
      return this;
   }

   public VsanAsyncDataRetriever loadComplianceResults(Volume volume) {
      this.addDataRetriever(ComplianceResultDataRetriever.class, new ComplianceResultDataRetriever(this.objRef, this.measure, this.pbmClient, volume));
      return this;
   }

   public VsanAsyncDataRetriever loadCapabilityObjectSchema() {
      this.addDataRetriever(CapabilityObjectSchemaDataRetriever.class, new CapabilityObjectSchemaDataRetriever(this.objRef, this.measure, this.pbmClient));
      return this;
   }

   public VsanAsyncDataRetriever loadSupportedEntityTypes() {
      this.addDataRetriever(SupportedEntityTypesDataRetriever.class, new SupportedEntityTypesDataRetriever(this.objRef, this.measure, this.vsanClient));
      return this;
   }

   public VsanAsyncDataRetriever loadNodeInformation() {
      this.addDataRetriever(NodeInformationDataRetriever.class, new NodeInformationDataRetriever(this.objRef, this.measure, this.vsanClient));
      return this;
   }

   public VsanAsyncDataRetriever loadConfigInfoEx() {
      this.loadData("VsanVcClusterConfigSystem.getConfigInfoEx", (future) -> {
         VsanConnection conn = this.vsanClient.getConnection(this.objRef.getServerGuid());
         Throwable var3 = null;

         try {
            VsanVcClusterConfigSystem vsanConfigSystem = conn.getVsanConfigSystem();
            vsanConfigSystem.getConfigInfoEx(this.objRef, future);
         } catch (Throwable var12) {
            var3 = var12;
            throw var12;
         } finally {
            if (conn != null) {
               if (var3 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var11) {
                     var3.addSuppressed(var11);
                  }
               } else {
                  conn.close();
               }
            }

         }

      });
      return this;
   }

   public VsanAsyncDataRetriever loadSpaceUsageData(DatastoreType datastoreType) {
      this.addDataRetriever(SpaceUsageDataRetriever.class, new SpaceUsageDataRetriever(this.objRef, this.measure, this.vsanClient, datastoreType));
      return this;
   }

   public VsanAsyncDataRetriever loadDataEfficiencyCapacitySpace() {
      this.addDataRetriever(CapacityEfficiencyUsageDataRetriever.class, new CapacityEfficiencyUsageDataRetriever(this.objRef, this.measure, this.vsanClient));
      return this;
   }

   public VsanAsyncDataRetriever loadStatsObjectInformation() {
      this.addDataRetriever(StatsObjectInformationDataRetriever.class, new StatsObjectInformationDataRetriever(this.objRef, this.measure, this.vsanClient));
      return this;
   }

   public VsanAsyncDataRetriever loadStoragePolicies() {
      this.addDataRetriever(PoliciesDataRetriever.class, new PoliciesDataRetriever(this.objRef, this.measure, this.pbmClient, this.permissionService));
      return this;
   }

   public VsanAsyncDataRetriever loadDisksProperties(String[] propertiesToGet, List hosts) {
      this.addDataRetriever(PhysicalVsanDisksDataRetriever.class, new PhysicalVsanDisksDataRetriever(this.objRef, this.measure, this.vcClient, hosts, propertiesToGet));
      return this;
   }

   public VsanAsyncDataRetriever loadDisksProperties(String requestKey, String[] propertiesToGet, List hosts) {
      this.addDataRetriever(requestKey, new PhysicalVsanDisksDataRetriever(this.objRef, this.measure, this.vcClient, hosts, propertiesToGet));
      return this;
   }

   public VsanAsyncDataRetriever loadDisksStatuses(List hosts) {
      if (VsanCapabilityUtils.isRealTimePhysicalDiskHealthSupported(this.objRef)) {
         this.addDataRetriever(PhysicalDisksStatusDataRetriever.class, new PhysicalDisksStatusDataRetriever(this.objRef, this.measure, this.vsanClient, hosts));
      } else {
         this.addDataRetriever(LegacyPhysicalDisksStatusDataRetriever.class, new LegacyPhysicalDisksStatusDataRetriever(this.objRef, this.measure, this.vcClient, hosts));
      }

      return this;
   }

   public VsanAsyncDataRetriever loadManagedDisks(List hosts, boolean processPartialResults) {
      this.addDataRetriever(ManagedDisksFacadeDataRetriever.class, new ManagedDisksFacadeDataRetriever(this.objRef, this.measure, this.vsanClient, hosts, processPartialResults));
      return this;
   }

   public VsanAsyncDataRetriever loadManagedDisks(List hosts) {
      return this.loadManagedDisks(hosts, true);
   }

   public VsanAsyncDataRetriever loadManagedDisks(final ManagedObjectReference hostRef, boolean processPartialResults) {
      this.addDataRetriever(ManagedDisksFacadeDataRetriever.class, new ManagedDisksFacadeDataRetriever(this.objRef, this.measure, this.vsanClient, new ArrayList() {
         {
            this.add(hostRef);
         }
      }, processPartialResults));
      return this;
   }

   public VsanAsyncDataRetriever loadManagedDisks(ManagedObjectReference hostRef) {
      return this.loadManagedDisks(hostRef, true);
   }

   public VsanAsyncDataRetriever loadDiskMappings(List hosts, boolean processPartialResults) {
      this.addDataRetriever(DiskMappingsDataRetriever.class, new DiskMappingsDataRetriever(this.objRef, this.measure, this.vsanClient, hosts, processPartialResults));
      return this;
   }

   public VsanAsyncDataRetriever loadDiskMappings(List hosts) {
      return this.loadDiskMappings(hosts, true);
   }

   public VsanAsyncDataRetriever loadTopContributors(VsanPerfTopQuerySpec spec) {
      this.addDataRetriever(TopContributorsDataRetriever.class, new TopContributorsDataRetriever(this.objRef, this.measure, this.vsanClient, spec));
      return this;
   }

   public VsanAsyncDataRetriever loadDisks(List hosts, boolean processPartialResults) {
      this.addDataRetriever(DisksDataRetriever.class, new DisksDataRetriever(this.objRef, this.measure, this.vcClient, hosts, processPartialResults));
      return this;
   }

   public VsanAsyncDataRetriever loadDisks(List hosts) {
      return this.loadDisks(hosts, true);
   }

   public VsanAsyncDataRetriever loadDisks(final ManagedObjectReference hostRef, boolean processPartialResults) {
      this.addDataRetriever(DisksDataRetriever.class, new DisksDataRetriever(this.objRef, this.measure, this.vcClient, new ArrayList() {
         {
            this.add(hostRef);
         }
      }, processPartialResults));
      return this;
   }

   public VsanAsyncDataRetriever loadDisks(ManagedObjectReference hostRef) {
      return this.loadDisks(hostRef, true);
   }

   public VsanAsyncDataRetriever loadHostStorageDeviceInfos(List hosts) {
      this.addDataRetriever(HostStorageDeviceInfosDataRetriever.class, new HostStorageDeviceInfosDataRetriever(this.objRef, this.measure, this.vcClient, this.vmodlHelper, hosts));
      return this;
   }

   public VsanAsyncDataRetriever loadHostClusterStatus(List hosts) {
      if (VsanCapabilityUtils.isSharedWitnessSupported(hosts)) {
         this.addDataRetriever(HostClusterStatusesDataRetriever.class, new HostClusterStatusesDataRetriever(this.measure, this.vsanClient, hosts));
      } else {
         this.addDataRetriever(LegacyHostClusterStatusDataRetriever.class, new LegacyHostClusterStatusDataRetriever(this.objRef, this.measure, this.vcClient, hosts));
      }

      return this;
   }

   public VsanAsyncDataRetriever loadHostCapabilities(List hosts) {
      this.addDataRetriever(HostCapabilitiesDataRetriever.class, new HostCapabilitiesDataRetriever(this.objRef, this.measure, this.vsanClient, hosts));
      return this;
   }

   public VsanAsyncDataRetriever loadWitnessHosts() {
      this.addDataRetriever(WitnessHostsDataRetriever.class, new WitnessHostsDataRetriever(this.objRef, this.measure, this.vsanClient));
      return this;
   }

   public VsanAsyncDataRetriever loadIsWitnessHost() {
      this.addDataRetriever(IsWitnessHostDataRetriever.class, new IsWitnessHostDataRetriever(this.objRef, this.measure, this.vsanClient));
      return this;
   }

   public VsanAsyncDataRetriever loadAssignedSharedWitnessClusters(List hosts) {
      this.addDataRetriever(AssignedSharedWitnessClustersDataRetriever.class, new AssignedSharedWitnessClustersDataRetriever(this.objRef, this.measure, this.vsanClient, hosts));
      return this;
   }

   public VsanAsyncDataRetriever loadAssignedSharedWitnessClusters(ManagedObjectReference host) {
      return this.loadAssignedSharedWitnessClusters(Arrays.asList(host));
   }

   public VsanAsyncDataRetriever loadAreHostsWitnessVirtualAppliances(List hosts) {
      this.addDataRetriever(HostIsWitnessApplianceDataRetriever.class, new HostIsWitnessApplianceDataRetriever(this.objRef, this.measure, this.vcClient, hosts));
      return this;
   }

   public VsanAsyncDataRetriever loadOptionsValues(String queryView) {
      this.addDataRetriever(OptionsDataRetriever.class, new OptionsDataRetriever(this.objRef, queryView, this.measure, this.vcClient));
      return this;
   }

   public VsanAsyncDataRetriever loadIscsiHomeObject() {
      this.addDataRetriever(IscsiHomeObjectDataRetriever.class, new IscsiHomeObjectDataRetriever(this.objRef, this.measure, this.vsanClient));
      return this;
   }

   public VsanAsyncDataRetriever loadHostConfig() {
      this.addDataRetriever(HostConfigDataRetriever.class, new HostConfigDataRetriever(this.objRef, this.measure, this.vcClient));
      return this;
   }

   public VsanAsyncDataRetriever loadClusterHclInfo() {
      this.loadData("VsanVcClusterHealthSystem.getClusterHclInfo", (future) -> {
         VsanConnection vsanConnection = this.vsanClient.getConnection(this.objRef.getServerGuid());
         Throwable var3 = null;

         try {
            VsanVcClusterHealthSystem vsanVcClusterHealthSystem = vsanConnection.getVsanVcClusterHealthSystem();
            vsanVcClusterHealthSystem.getClusterHclInfo(this.objRef, true, false, (String)null, future);
         } catch (Throwable var12) {
            var3 = var12;
            throw var12;
         } finally {
            if (vsanConnection != null) {
               if (var3 != null) {
                  try {
                     vsanConnection.close();
                  } catch (Throwable var11) {
                     var3.addSuppressed(var11);
                  }
               } else {
                  vsanConnection.close();
               }
            }

         }

      });
      return this;
   }

   public VsanObjectIdentityAndHealth getObjectIdentities() {
      return (VsanObjectIdentityAndHealth)this.getResult(ObjectIdentitiesDataRetriever.class);
   }

   public VsanObjectIdentityAndHealth getExtensionIdentities() {
      return (VsanObjectIdentityAndHealth)this.getResult(ExtensionIdentitiesDataRetriever.class);
   }

   public VsanObjectIdentityAndHealth getTargetVrConfigIdentities() {
      return (VsanObjectIdentityAndHealth)this.getResult(HbrConfigIdentitiesDataRetriever.class);
   }

   public VsanObjectInformation[] getObjectInformation() {
      return (VsanObjectInformation[])this.getResult(ObjectInformationDataRetriever.class);
   }

   public VsanIscsiTarget[] getIscsiTargets() {
      return (VsanIscsiTarget[])this.getResult(IscsiTargetsDataRetriever.class);
   }

   public VsanIscsiLUN[] getIscsiLuns() {
      return (VsanIscsiLUN[])this.getResult(IscsiLunsDataRetriever.class);
   }

   public List getFileShares() {
      return (List)this.getResult(FileSharesDataRetriever.class);
   }

   public int getFileSharesCountResult() {
      return (Integer)this.getResult(FileSharesCountRetriever.class);
   }

   public VsanFileServicePreflightCheckResult getFileServicePrecheckResult() {
      return (VsanFileServicePreflightCheckResult)this.getResult(FileServicePrecheckDataRetriever.class);
   }

   public Set getClusterUuids() {
      return (Set)this.getResult(ClusterUuidsDataRetriever.class);
   }

   public ComplianceResult[] getComplianceResults() {
      return (ComplianceResult[])this.getResult(ComplianceResultDataRetriever.class);
   }

   public VsanPerfEntityType[] getSupportedEntityTypes() {
      return (VsanPerfEntityType[])this.getResult(SupportedEntityTypesDataRetriever.class);
   }

   public VsanPerfNodeInformation[] getNodeInformation() {
      return (VsanPerfNodeInformation[])this.getResult(NodeInformationDataRetriever.class);
   }

   public ConfigInfoEx getConfigInfoEx() {
      return (ConfigInfoEx)this.getResult("VsanVcClusterConfigSystem.getConfigInfoEx");
   }

   public VsanSpaceUsageWithDatastoreType[] getSpaceUsageData() {
      return (VsanSpaceUsageWithDatastoreType[])this.getResult(SpaceUsageDataRetriever.class);
   }

   public DataEfficiencyCapacityState getDataEfficiencyCapacitySpace() {
      return (DataEfficiencyCapacityState)this.getResult(CapacityEfficiencyUsageDataRetriever.class);
   }

   public VsanObjectInformation getStatsObjectInformation() {
      return (VsanObjectInformation)this.getResult(StatsObjectInformationDataRetriever.class);
   }

   public CapabilityObjectSchema[] getCabalityObjectSchema() {
      return (CapabilityObjectSchema[])this.getResult(CapabilityObjectSchemaDataRetriever.class);
   }

   public Profile[] getStoragePolicies() {
      return (Profile[])this.getResult(PoliciesDataRetriever.class);
   }

   public Map getDisksProperties() {
      return (Map)this.getResult(PhysicalVsanDisksDataRetriever.class);
   }

   public Map getDisksProperties(String requestKey) {
      return (Map)this.getResult(requestKey);
   }

   public Map getDisksStatuses() {
      return VsanCapabilityUtils.isRealTimePhysicalDiskHealthSupported(this.objRef) ? (Map)this.getResult(PhysicalDisksStatusDataRetriever.class) : (Map)this.getResult(LegacyPhysicalDisksStatusDataRetriever.class);
   }

   public Map getManagedDisks() {
      return (Map)this.getResult(ManagedDisksFacadeDataRetriever.class);
   }

   public Map getDiskMappings() {
      return (Map)this.getResult(DiskMappingsDataRetriever.class);
   }

   public VsanPerfEntityMetricCSV[] getTopContributors() {
      return (VsanPerfEntityMetricCSV[])this.getResult(TopContributorsDataRetriever.class);
   }

   public Map getDisks() {
      return (Map)this.getResult(DisksDataRetriever.class);
   }

   public Map getHostStorageDeviceInfos() {
      return (Map)this.getResult(HostStorageDeviceInfosDataRetriever.class);
   }

   public Map getHostClusterStatus() {
      return this.dataRetrievers.containsKey(HostClusterStatusesDataRetriever.class) ? (Map)this.getResult(HostClusterStatusesDataRetriever.class) : (Map)this.getResult(LegacyHostClusterStatusDataRetriever.class);
   }

   public VsanCapability[] getHostCapabilities() {
      return (VsanCapability[])this.getResult(HostCapabilitiesDataRetriever.class);
   }

   public VSANWitnessHostInfo[] getWitnessHosts() {
      return (VSANWitnessHostInfo[])this.getResult(WitnessHostsDataRetriever.class);
   }

   public Boolean getIsWitnessHost() {
      return (Boolean)this.getResult(IsWitnessHostDataRetriever.class);
   }

   public Map getAssignedSharedWitnessClusters() {
      return (Map)this.getResult(AssignedSharedWitnessClustersDataRetriever.class);
   }

   public Map getAreHostsWitnessVirtualAppliances() {
      return (Map)this.getResult(HostIsWitnessApplianceDataRetriever.class);
   }

   public VsanObjectInformation getIscsiHomeObject() {
      return (VsanObjectInformation)this.getResult(IscsiHomeObjectDataRetriever.class);
   }

   public OptionValue[] getOptionsValues() {
      return (OptionValue[])this.getResult(OptionsDataRetriever.class);
   }

   public ConfigInfo getHostConfig() {
      return (ConfigInfo)this.getResult(HostConfigDataRetriever.class);
   }

   public VsanClusterHclInfo getClusterHclInfo() {
      return (VsanClusterHclInfo)this.getResult("VsanVcClusterHealthSystem.getClusterHclInfo");
   }

   private void addDataRetriever(Object type, DataRetriever dataRetriever) {
      if (this.dataRetrievers.containsKey(type)) {
         throw new IllegalStateException("'" + type + "' data retriever has alread been registered!");
      } else {
         dataRetriever.start();
         this.dataRetrievers.put(type, dataRetriever);
         logger.debug("Registered retriever: " + type);
      }
   }

   private Object getResult(Object type) {
      try {
         return this.getDataRetriever(type).getResult();
      } catch (Exception var3) {
         throw new RuntimeException(var3);
      }
   }

   private DataRetriever getDataRetriever(Object type) {
      DataRetriever dataRetriever = (DataRetriever)this.dataRetrievers.get(type);
      if (dataRetriever == null) {
         throw new IllegalStateException("No '" + type.toString() + "'DataRetriever found!");
      } else {
         return dataRetriever;
      }
   }
}
