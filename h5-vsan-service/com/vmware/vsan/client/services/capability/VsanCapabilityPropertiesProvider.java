package com.vmware.vsan.client.services.capability;

import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vim.Datacenter;
import com.vmware.vim.binding.vim.Datastore;
import com.vmware.vim.binding.vim.Folder;
import com.vmware.vim.binding.vim.HostSystem;
import com.vmware.vim.binding.vim.VirtualMachine;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vise.data.query.DataServiceExtensionRegistry;
import com.vmware.vise.data.query.PropertyRequestSpec;
import com.vmware.vise.data.query.PropertyValue;
import com.vmware.vise.data.query.ResultItem;
import com.vmware.vise.data.query.ResultSet;
import com.vmware.vise.data.query.TypeInfo;
import com.vmware.vsan.client.services.common.VsanBasePropertyProviderAdapter;
import com.vmware.vsan.client.util.VmodlHelper;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;

public class VsanCapabilityPropertiesProvider extends VsanBasePropertyProviderAdapter {
   private static final String PROPERTY_IS_VM_LEVEL_CAPACITY_MONITORING_SUPPORTED = "isVmLevelCapacityMonitoringSupported";
   private static final String PROPERTY_CNS_SUPPORTED_ON_VC = "isCnsVolumesSupportedOnVc";
   private static final String PROPERTY_IS_PERSISTENCE_SERVICE_SUPPORTED = "isPersistenceServiceSupported";
   private static final String PROPERTY_IS_VM_IO_DIAGNOSTICS_SUPPORTED = "isVmIoDiagnosticsSupported";
   private static final String PROPERTY_IS_DISK_MGMT_REDESIGN_SUPPORTED = "isDiskMgmtRedesignSupported";
   private static final String PROPERTY_IS_VSAN_NESTED_FDS_SUPPORTED = "isVsanNestedFdsSupported";
   private static final String PROPERTY_IS_IO_INSIGHT_SUPPORTED_ON_VC = "isIoInsightSupportedOnVC";
   private static final String PROPERTY_IS_IO_INSIGHT_SUPPORTED = "isIoInsightSupported";
   private static final String PROPERTY_IS_HISTORICAL_HEALTH_SUPPORTED_ON_VC = "isHistoricalHealthSupportedOnVc";
   private static final String PROPERTY_IS_HISTORICAL_HEALTH_SUPPORTED = "isHistoricalHealthSupported";
   private static final String PROPERTY_IS_HARDWARE_MANAGEMENT_SUPPORTED_ON_HOST = "isHardwareManagementSupportedOnHost";
   private static final String PROPERTY_IS_CSD_SUPPORTED_ON_VC = "isCsdSupportedOnVC";
   private static final String PROPERTY_FILE_ANALYTICS_SUPPORTED = "isFileAnalyticsSupported";
   private static final String PROPERTY_IS_VSAN_HCI_MESH_POLICY_SUPPORTED = "isVsanHciMeshPolicySupported";
   private static final String PROPERTY_IS_DELETE_VSAN_DIRECT_DATASTORE_AVAILABLE = "isDeleteVsandDatastoreActionAvailable";
   @Autowired
   protected VmodlHelper vmodlHelper;

   public VsanCapabilityPropertiesProvider(DataServiceExtensionRegistry registry) {
      Validate.notNull(registry);
      List capabilityTypeInfos = new ArrayList();
      capabilityTypeInfos.addAll(this.getCapacityTypeInfos());
      capabilityTypeInfos.addAll(this.getCnsTypeInfos());
      capabilityTypeInfos.addAll(this.getPersistenceServiceInfos());
      capabilityTypeInfos.addAll(this.getNestedFdsInfos());
      capabilityTypeInfos.addAll(this.getIoInsightInfos());
      capabilityTypeInfos.addAll(this.getHistoricalHealthOnVcInfos());
      capabilityTypeInfos.addAll(this.getHistoricalHealthInfos());
      capabilityTypeInfos.addAll(this.getCsdOnVCInfos());
      capabilityTypeInfos.addAll(this.getHardwareManagementOnHostInfos());
      capabilityTypeInfos.addAll(this.getIoDiagnosticsInfos());
      capabilityTypeInfos.addAll(this.getDiskMgmtRedesignInfos());
      capabilityTypeInfos.addAll(this.getFileAnalyticsInfos());
      capabilityTypeInfos.addAll(this.getHciMeshPolicyInfos());
      capabilityTypeInfos.addAll(this.getVsanDirectDatastoreDeleteActionInfos());
      registry.registerDataAdapter(this, (TypeInfo[])capabilityTypeInfos.toArray(new TypeInfo[0]));
   }

   private List getCapacityTypeInfos() {
      String[] capacityProperties = new String[]{"isVmLevelCapacityMonitoringSupported"};
      final TypeInfo vmTypeInfo = this.getTypeInfo(VirtualMachine.class, capacityProperties);
      final TypeInfo clusterTypeInfo = this.getTypeInfo(ClusterComputeResource.class, capacityProperties);
      return new ArrayList() {
         {
            this.add(vmTypeInfo);
            this.add(clusterTypeInfo);
         }
      };
   }

   private List getNestedFdsInfos() {
      String[] nestedFdsProperties = new String[]{"isVsanNestedFdsSupported"};
      final TypeInfo folderTypeInfo = this.getTypeInfo(Folder.class, nestedFdsProperties);
      return new ArrayList() {
         {
            this.add(folderTypeInfo);
         }
      };
   }

   private List getHciMeshPolicyInfos() {
      String[] hciMeshPolicyProperties = new String[]{"isVsanHciMeshPolicySupported"};
      final TypeInfo folderTypeInfo = this.getTypeInfo(Folder.class, hciMeshPolicyProperties);
      return new ArrayList() {
         {
            this.add(folderTypeInfo);
         }
      };
   }

   private List getCnsTypeInfos() {
      String[] cnsProperties = new String[]{"isCnsVolumesSupportedOnVc"};
      final TypeInfo folderTypeInfo = this.getTypeInfo(Folder.class, cnsProperties);
      final TypeInfo datacenterTypeInfo = this.getTypeInfo(Datacenter.class, cnsProperties);
      final TypeInfo clusterTypeInfo = this.getTypeInfo(ClusterComputeResource.class, cnsProperties);
      final TypeInfo datastoreTypeInfo = this.getTypeInfo(Datastore.class, cnsProperties);
      return new ArrayList() {
         {
            this.add(folderTypeInfo);
            this.add(datacenterTypeInfo);
            this.add(clusterTypeInfo);
            this.add(datastoreTypeInfo);
         }
      };
   }

   private List getPersistenceServiceInfos() {
      String[] persistenceServiceProperties = new String[]{"isPersistenceServiceSupported"};
      final TypeInfo folderTypeInfo = this.getTypeInfo(Folder.class, persistenceServiceProperties);
      final TypeInfo clusterTypeInfo = this.getTypeInfo(ClusterComputeResource.class, persistenceServiceProperties);
      return new ArrayList() {
         {
            this.add(folderTypeInfo);
            this.add(clusterTypeInfo);
         }
      };
   }

   private List getIoInsightInfos() {
      String[] ioInsightClusterProperties = new String[]{"isIoInsightSupportedOnVC"};
      String[] ioInsightHostProperties = new String[]{"isIoInsightSupported"};
      final TypeInfo hostTypeInfo = this.getTypeInfo(HostSystem.class, ioInsightHostProperties);
      final TypeInfo clusterTypeInfo = this.getTypeInfo(ClusterComputeResource.class, ioInsightClusterProperties);
      return new ArrayList() {
         {
            this.add(hostTypeInfo);
            this.add(clusterTypeInfo);
         }
      };
   }

   private List getHistoricalHealthOnVcInfos() {
      String[] historicalHealthProperties = new String[]{"isHistoricalHealthSupportedOnVc"};
      final TypeInfo hostTypeInfo = this.getTypeInfo(HostSystem.class, historicalHealthProperties);
      final TypeInfo clusterTypeInfo = this.getTypeInfo(ClusterComputeResource.class, historicalHealthProperties);
      final TypeInfo folderTypeInfo = this.getTypeInfo(Folder.class, historicalHealthProperties);
      return new ArrayList() {
         {
            this.add(hostTypeInfo);
            this.add(clusterTypeInfo);
            this.add(folderTypeInfo);
         }
      };
   }

   private List getHistoricalHealthInfos() {
      String[] historicalHealthProperties = new String[]{"isHistoricalHealthSupported"};
      final TypeInfo hostTypeInfo = this.getTypeInfo(HostSystem.class, historicalHealthProperties);
      final TypeInfo clusterTypeInfo = this.getTypeInfo(ClusterComputeResource.class, historicalHealthProperties);
      return new ArrayList() {
         {
            this.add(hostTypeInfo);
            this.add(clusterTypeInfo);
         }
      };
   }

   private List getCsdOnVCInfos() {
      String[] csdProperties = new String[]{"isCsdSupportedOnVC"};
      final TypeInfo clusterTypeInfo = this.getTypeInfo(ClusterComputeResource.class, csdProperties);
      return new ArrayList() {
         {
            this.add(clusterTypeInfo);
         }
      };
   }

   private List getHardwareManagementOnHostInfos() {
      String[] hardwareManagementProperties = new String[]{"isHardwareManagementSupportedOnHost"};
      final TypeInfo hostTypeInfo = this.getTypeInfo(HostSystem.class, hardwareManagementProperties);
      return new ArrayList() {
         {
            this.add(hostTypeInfo);
         }
      };
   }

   private List getIoDiagnosticsInfos() {
      String[] ioDiagnosticsProperties = new String[]{"isVmIoDiagnosticsSupported"};
      final TypeInfo vmTypeInfo = this.getTypeInfo(VirtualMachine.class, ioDiagnosticsProperties);
      return new ArrayList() {
         {
            this.add(vmTypeInfo);
         }
      };
   }

   private List getDiskMgmtRedesignInfos() {
      String[] diskMgmtRedesignProperties = new String[]{"isDiskMgmtRedesignSupported"};
      final TypeInfo clusterTypeInfo = this.getTypeInfo(ClusterComputeResource.class, diskMgmtRedesignProperties);
      return new ArrayList() {
         {
            this.add(clusterTypeInfo);
         }
      };
   }

   private List getFileAnalyticsInfos() {
      String[] fileAnalyticsPropertis = new String[]{"isFileAnalyticsSupported"};
      final TypeInfo clusterTypeInfo = this.getTypeInfo(ClusterComputeResource.class, fileAnalyticsPropertis);
      return new ArrayList() {
         {
            this.add(clusterTypeInfo);
         }
      };
   }

   private List getVsanDirectDatastoreDeleteActionInfos() {
      String[] vsanDirectDatastoreDeleteActionProperties = new String[]{"isDeleteVsandDatastoreActionAvailable"};
      final TypeInfo datastoreTypeInfo = this.getTypeInfo(Datastore.class, vsanDirectDatastoreDeleteActionProperties);
      return new ArrayList() {
         {
            this.add(datastoreTypeInfo);
         }
      };
   }

   private TypeInfo getTypeInfo(Class typeInfoType, String[] properties) {
      TypeInfo typeInfo = new TypeInfo();
      typeInfo.type = typeInfoType.getSimpleName();
      typeInfo.properties = properties;
      return typeInfo;
   }

   protected ResultSet getResult(PropertyRequestSpec propertyRequest) {
      List resultItems = new ArrayList();
      Object[] var3 = propertyRequest.objects;
      int var4 = var3.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         Object objectRef = var3[var5];
         ManagedObjectReference moRef = (ManagedObjectReference)objectRef;
         if (objectRef != null) {
            List propValues = new ArrayList();
            PropertyValue propValue;
            if (QueryUtil.isAnyPropertyRequested(propertyRequest.properties, "isVmLevelCapacityMonitoringSupported")) {
               propValue = QueryUtil.newProperty("isVmLevelCapacityMonitoringSupported", VsanCapabilityUtils.isVmLevelCapacityMonitoringSupportedOnVc(moRef));
               propValue.resourceObject = objectRef;
               propValues.add(propValue);
            }

            if (QueryUtil.isAnyPropertyRequested(propertyRequest.properties, "isCnsVolumesSupportedOnVc")) {
               propValue = QueryUtil.newProperty("isCnsVolumesSupportedOnVc", VsanCapabilityUtils.isCnsVolumesSupportedOnVc(moRef));
               propValue.resourceObject = objectRef;
               propValues.add(propValue);
            }

            if (QueryUtil.isAnyPropertyRequested(propertyRequest.properties, "isPersistenceServiceSupported")) {
               propValue = QueryUtil.newProperty("isPersistenceServiceSupported", VsanCapabilityUtils.isPersistenceServiceSupportedOnVc(moRef));
               propValue.resourceObject = objectRef;
               propValues.add(propValue);
            }

            if (QueryUtil.isAnyPropertyRequested(propertyRequest.properties, "isVsanNestedFdsSupported")) {
               propValue = QueryUtil.newProperty("isVsanNestedFdsSupported", VsanCapabilityUtils.isVsanNestedFdsSupportedOnVc((ManagedObjectReference)objectRef));
               propValue.resourceObject = objectRef;
               propValues.add(propValue);
            }

            if (QueryUtil.isAnyPropertyRequested(propertyRequest.properties, "isVsanHciMeshPolicySupported")) {
               propValue = QueryUtil.newProperty("isVsanHciMeshPolicySupported", VsanCapabilityUtils.isVsanHciMeshPolicySupportedOnVc((ManagedObjectReference)objectRef));
               propValue.resourceObject = objectRef;
               propValues.add(propValue);
            }

            if (QueryUtil.isAnyPropertyRequested(propertyRequest.properties, "isIoInsightSupportedOnVC")) {
               propValue = QueryUtil.newProperty("isIoInsightSupportedOnVC", VsanCapabilityUtils.isIoInsightSupportedOnVc(moRef));
               propValue.resourceObject = moRef;
               propValues.add(propValue);
            }

            if (QueryUtil.isAnyPropertyRequested(propertyRequest.properties, "isIoInsightSupported")) {
               propValue = QueryUtil.newProperty("isIoInsightSupported", VsanCapabilityUtils.isIoInsightSupported(moRef));
               propValue.resourceObject = moRef;
               propValues.add(propValue);
            }

            if (QueryUtil.isAnyPropertyRequested(propertyRequest.properties, "isHistoricalHealthSupportedOnVc")) {
               propValue = QueryUtil.newProperty("isHistoricalHealthSupportedOnVc", VsanCapabilityUtils.isHistoricalHealthSupportedOnVc(moRef));
               propValue.resourceObject = objectRef;
               propValues.add(propValue);
            }

            if (QueryUtil.isAnyPropertyRequested(propertyRequest.properties, "isHistoricalHealthSupported")) {
               propValue = QueryUtil.newProperty("isHistoricalHealthSupported", VsanCapabilityUtils.isHistoricalHealthSupported(moRef));
               propValue.resourceObject = objectRef;
               propValues.add(propValue);
            }

            if (QueryUtil.isAnyPropertyRequested(propertyRequest.properties, "isCsdSupportedOnVC")) {
               propValue = QueryUtil.newProperty("isCsdSupportedOnVC", VsanCapabilityUtils.isCsdSupportedOnVC(moRef));
               propValue.resourceObject = objectRef;
               propValues.add(propValue);
            }

            if (QueryUtil.isAnyPropertyRequested(propertyRequest.properties, "isHardwareManagementSupportedOnHost")) {
               propValue = QueryUtil.newProperty("isHardwareManagementSupportedOnHost", VsanCapabilityUtils.isHardwareManagementSupported(moRef));
               propValue.resourceObject = objectRef;
               propValues.add(propValue);
            }

            if (QueryUtil.isAnyPropertyRequested(propertyRequest.properties, "isVmIoDiagnosticsSupported")) {
               propValue = QueryUtil.newProperty("isVmIoDiagnosticsSupported", VsanCapabilityUtils.isVmIoDiagnosticsSupportedOnVc(moRef));
               propValue.resourceObject = moRef;
               propValues.add(propValue);
            }

            if (QueryUtil.isAnyPropertyRequested(propertyRequest.properties, "isDiskMgmtRedesignSupported")) {
               propValue = QueryUtil.newProperty("isDiskMgmtRedesignSupported", VsanCapabilityUtils.isDiskMgmtRedesignSupportedOnVc(moRef));
               propValue.resourceObject = moRef;
               propValues.add(propValue);
            }

            if (QueryUtil.isAnyPropertyRequested(propertyRequest.properties, "isFileAnalyticsSupported")) {
               propValue = QueryUtil.newProperty("isFileAnalyticsSupported", VsanCapabilityUtils.isFileAnalyticsSupported(moRef));
               propValue.resourceObject = moRef;
               propValues.add(propValue);
            }

            if (QueryUtil.isAnyPropertyRequested(propertyRequest.properties, "isDeleteVsandDatastoreActionAvailable")) {
               boolean isDecommissionModeSupported = VsanCapabilityUtils.isDecomModeForVsanDirectDisksSupportedOnVc((ManagedObjectReference)objectRef);
               PropertyValue propValue = QueryUtil.newProperty("isDeleteVsandDatastoreActionAvailable", !isDecommissionModeSupported);
               propValue.resourceObject = objectRef;
               propValues.add(propValue);
            }

            resultItems.add(QueryUtil.newResultItem(objectRef, (PropertyValue[])propValues.toArray(new PropertyValue[0])));
         }
      }

      return QueryUtil.newResultSet((ResultItem[])resultItems.toArray(new ResultItem[0]));
   }
}
