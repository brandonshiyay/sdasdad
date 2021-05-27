package com.vmware.vsan.client.sessionmanager.vlsi.client.vsan;

import com.vmware.vim.binding.vim.VsanUpgradeSystem;
import com.vmware.vim.binding.vmodl.ManagedObject;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.VsanPhoneHomeSystem;
import com.vmware.vim.vsan.binding.vim.VsanUpgradeSystemEx;
import com.vmware.vim.vsan.binding.vim.VsanVcFileAnalyticsSystem;
import com.vmware.vim.vsan.binding.vim.VsanVcPrecheckerSystem;
import com.vmware.vim.vsan.binding.vim.cluster.VsanCapabilitySystem;
import com.vmware.vim.vsan.binding.vim.cluster.VsanClusterMgmtInternalSystem;
import com.vmware.vim.vsan.binding.vim.cluster.VsanDiagnosticsSystem;
import com.vmware.vim.vsan.binding.vim.cluster.VsanIoInsightManager;
import com.vmware.vim.vsan.binding.vim.cluster.VsanIscsiTargetSystem;
import com.vmware.vim.vsan.binding.vim.cluster.VsanObjectSystem;
import com.vmware.vim.vsan.binding.vim.cluster.VsanPerformanceManager;
import com.vmware.vim.vsan.binding.vim.cluster.VsanRemoteDatastoreSystem;
import com.vmware.vim.vsan.binding.vim.cluster.VsanSpaceReportSystem;
import com.vmware.vim.vsan.binding.vim.cluster.VsanVcClusterConfigSystem;
import com.vmware.vim.vsan.binding.vim.cluster.VsanVcClusterHealthSystem;
import com.vmware.vim.vsan.binding.vim.cluster.VsanVcDiskManagementSystem;
import com.vmware.vim.vsan.binding.vim.cluster.VsanVcHardwareManagementSystem;
import com.vmware.vim.vsan.binding.vim.cluster.VsanVcStretchedClusterSystem;
import com.vmware.vim.vsan.binding.vim.cluster.VsanVumSystem;
import com.vmware.vim.vsan.binding.vim.cns.VolumeManager;
import com.vmware.vim.vsan.binding.vim.host.VsanSystemEx;
import com.vmware.vim.vsan.binding.vim.host.VsanUpdateManager;
import com.vmware.vim.vsan.binding.vim.vsan.VsanFileServiceSystem;
import com.vmware.vim.vsan.binding.vim.vsan.VsanPolicyManager;
import com.vmware.vim.vsan.binding.vim.vsan.VsanResourceCheckSystem;
import com.vmware.vim.vsan.binding.vim.vsan.VsanVdsSystem;
import com.vmware.vsan.client.sessionmanager.vlsi.client.VlsiConnection;
import com.vmware.vsan.client.sessionmanager.vlsi.util.RequestContextUtil;

public class VsanConnection extends VlsiConnection {
   public VsanVcClusterConfigSystem getVsanConfigSystem() {
      return (VsanVcClusterConfigSystem)this.createStub(VsanVcClusterConfigSystem.class, VsanManagedObject.CLUSTER_CONFIG_SYSTEM);
   }

   public VsanVcDiskManagementSystem getVsanDiskManagementSystem() {
      return (VsanVcDiskManagementSystem)this.createStub(VsanVcDiskManagementSystem.class, VsanManagedObject.DISK_MANAGEMENT_SYSTEM);
   }

   public VsanPerformanceManager getVsanPerformanceManager() {
      return (VsanPerformanceManager)this.createStub(VsanPerformanceManager.class, VsanManagedObject.PERFORMANCE_MANAGER);
   }

   public VsanIoInsightManager getVsanIoInsightManager() {
      return (VsanIoInsightManager)this.createStub(VsanIoInsightManager.class, VsanManagedObject.IO_INSIGHT_MANAGER);
   }

   public VsanUpgradeSystem getVsanUpgradeSystem() {
      return (VsanUpgradeSystem)this.createStub(VsanUpgradeSystem.class, VsanManagedObject.UPGRADE_SYSTEM);
   }

   public VsanUpgradeSystem getVsanLegacyUpgradeSystem() {
      return (VsanUpgradeSystem)this.createStub(VsanUpgradeSystem.class, VsanManagedObject.LEGACY_UPGRADE_SYSTEM);
   }

   public VsanUpgradeSystemEx getVsanUpgradeSystemEx() {
      return (VsanUpgradeSystemEx)this.createStub(VsanUpgradeSystemEx.class, VsanManagedObject.UPGRADE_SYSTEM_EX);
   }

   public VsanVcClusterHealthSystem getVsanVcClusterHealthSystem() {
      return (VsanVcClusterHealthSystem)this.createStub(VsanVcClusterHealthSystem.class, VsanManagedObject.VC_CLUSTER_HEALTH_SYSTEM);
   }

   public VsanObjectSystem getVsanObjectSystem() {
      return (VsanObjectSystem)this.createStub(VsanObjectSystem.class, VsanManagedObject.OBJECT_SYSTEM);
   }

   public VsanIscsiTargetSystem getVsanIscsiSystem() {
      return (VsanIscsiTargetSystem)this.createStub(VsanIscsiTargetSystem.class, VsanManagedObject.ISCSI_TARGET_SYSTEM);
   }

   public VsanSpaceReportSystem getVsanSpaceReportSystem() {
      return (VsanSpaceReportSystem)this.createStub(VsanSpaceReportSystem.class, VsanManagedObject.SPACE_REPORTING_SYSTEM);
   }

   public VsanCapabilitySystem getVsanCapabilitySystem() {
      return (VsanCapabilitySystem)this.createStub(VsanCapabilitySystem.class, VsanManagedObject.CAPABILITY_SYSTEM);
   }

   public VsanSystemEx getVsanSystemEx(ManagedObjectReference moRef) {
      ManagedObjectReference vsanSystemExRef = new ManagedObjectReference("VsanSystemEx", moRef.getValue().replace("host", "vsanSystemEx"), moRef.getServerGuid());
      return (VsanSystemEx)this.createStub(VsanSystemEx.class, vsanSystemExRef);
   }

   public VsanUpdateManager getUpdateManager() {
      return (VsanUpdateManager)this.createStub(VsanUpdateManager.class, VsanManagedObject.VSAN_UPDATE_MANAGER);
   }

   public VsanDiagnosticsSystem getDiagnosticsSystem() {
      return (VsanDiagnosticsSystem)this.createStub(VsanDiagnosticsSystem.class, VsanManagedObject.VSAN_DIAGNOSTICS_SYSTEM);
   }

   public VsanVdsSystem getVdsSystem() {
      return (VsanVdsSystem)this.createStub(VsanVdsSystem.class, VsanManagedObject.VSAN_VDS_SYSTEM);
   }

   public VsanVcPrecheckerSystem getVsanPreCheckerSystem() {
      return (VsanVcPrecheckerSystem)this.createStub(VsanVcPrecheckerSystem.class, VsanManagedObject.VSAN_VC_PRECHECKER_SYSTEM);
   }

   public VsanPhoneHomeSystem getPhoneHomeSystem() {
      return (VsanPhoneHomeSystem)this.createStub(VsanPhoneHomeSystem.class, VsanManagedObject.VSAN_PHONEHOME_SYSTEM);
   }

   public VsanClusterMgmtInternalSystem getVsanClusterMgmtInternalSystem() {
      return (VsanClusterMgmtInternalSystem)this.createStub(VsanClusterMgmtInternalSystem.class, VsanManagedObject.VSAN_CLUSTER_MGMT_INTERNAL_SYSTEM);
   }

   public VsanFileServiceSystem getVsanFileServiceSystem() {
      return (VsanFileServiceSystem)this.createStub(VsanFileServiceSystem.class, VsanManagedObject.VSAN_FILE_SERVICE_SYSTEM);
   }

   public VsanVcFileAnalyticsSystem getVsanFileAnalyticsSystem() {
      return (VsanVcFileAnalyticsSystem)this.createStub(VsanVcFileAnalyticsSystem.class, VsanManagedObject.VSAN_FILE_ANALYTICS_SYSTEM);
   }

   public VsanVumSystem getVsanVumSystem() {
      return (VsanVumSystem)this.createStub(VsanVumSystem.class, VsanManagedObject.VSAN_VUM_SYSTEM);
   }

   public VolumeManager getCnsVolumeManager() {
      return (VolumeManager)this.createStub(VolumeManager.class, VsanManagedObject.CNS_VOLUME_MANAGER);
   }

   public VsanResourceCheckSystem getVsanResourceCheckSystem() {
      return (VsanResourceCheckSystem)this.createStub(VsanResourceCheckSystem.class, VsanManagedObject.VSAN_CLUSTER_RESOURCE_CHECK_SYSTEM);
   }

   public VsanVcStretchedClusterSystem getVcStretchedClusterSystem() {
      return (VsanVcStretchedClusterSystem)this.createStub(VsanVcStretchedClusterSystem.class, VsanManagedObject.STRETCHED_CLUSTER);
   }

   public VsanVcHardwareManagementSystem getVcHardwareManagementSystem() {
      return (VsanVcHardwareManagementSystem)this.createStub(VsanVcHardwareManagementSystem.class, VsanManagedObject.VSAN_HARDWARE_MANAGEMENT_SYSTEM);
   }

   public VsanRemoteDatastoreSystem getVsanRemoteDatastoreSystem() {
      return (VsanRemoteDatastoreSystem)this.createStub(VsanRemoteDatastoreSystem.class, VsanManagedObject.VSAN_REMOTE_DATASTORE_SYSTEM);
   }

   public VsanPolicyManager getVsanPolicyManager() {
      return (VsanPolicyManager)this.createStub(VsanPolicyManager.class, VsanManagedObject.VSAN_POLICY_MANAGER);
   }

   private ManagedObject createStub(Class clazz, VsanManagedObject vsanMo) {
      return this.createStub(clazz, new ManagedObjectReference(vsanMo.type, vsanMo.id, (String)null));
   }

   public ManagedObject createStub(Class clazz, String moId) {
      return RequestContextUtil.setVcSessionCookie(super.createStub(clazz, moId), this.settings.getSessionCookie());
   }

   public ManagedObject createStub(Class clazz, ManagedObjectReference moRef) {
      return RequestContextUtil.setVcSessionCookie(super.createStub(clazz, moRef), this.settings.getSessionCookie());
   }

   public String toString() {
      return this.settings != null ? String.format("VsanConnection(host=%s)", this.settings.getHttpSettings().getHost()) : "VsanConnection(initializing)";
   }
}
