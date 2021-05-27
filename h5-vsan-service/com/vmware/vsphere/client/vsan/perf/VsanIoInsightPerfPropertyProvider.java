package com.vmware.vsphere.client.vsan.perf;

import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vim.HostSystem;
import com.vmware.vim.binding.vim.VirtualMachine;
import com.vmware.vim.binding.vim.HostSystem.ConnectionState;
import com.vmware.vim.binding.vim.VirtualMachine.PowerState;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.vsan.ConfigInfoEx;
import com.vmware.vise.data.query.DataServiceExtensionRegistry;
import com.vmware.vise.data.query.PropertyRequestSpec;
import com.vmware.vise.data.query.PropertyValue;
import com.vmware.vise.data.query.ResultItem;
import com.vmware.vise.data.query.ResultSet;
import com.vmware.vise.data.query.TypeInfo;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.services.common.PermissionService;
import com.vmware.vsan.client.services.common.VsanBasePropertyProviderAdapter;
import com.vmware.vsan.client.services.config.ConfigInfoService;
import com.vmware.vsan.client.util.VsanInventoryHelper;
import com.vmware.vsphere.client.vsan.util.DataServiceResponse;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class VsanIoInsightPerfPropertyProvider extends VsanBasePropertyProviderAdapter {
   private static final Log logger = LogFactory.getLog(VsanIoInsightPerfPropertyProvider.class);
   private static final String PROPERTY_IS_IO_INSIGHT_AVAILABLE_ON_CLUSTER = "isIoInsightAvailableOnCluster";
   private static final String PROPERTY_IS_IO_INSIGHT_AVAILABLE_ON_HOST = "isIoInsightAvailableOnHost";
   private static final String PROPERTY_IS_IO_INSIGHT_AVAILABLE_ON_VM = "isIoInsightAvailableOnVM";
   @Autowired
   private ConfigInfoService configInfoService;
   @Autowired
   private VsanInventoryHelper vsanInventoryHelper;
   @Autowired
   private PermissionService permissionService;

   public VsanIoInsightPerfPropertyProvider(DataServiceExtensionRegistry registry) {
      Validate.notNull(registry);
      List ioInsightTypeInfos = this.getIoInsightInfos();
      registry.registerDataAdapter(this, (TypeInfo[])ioInsightTypeInfos.toArray(new TypeInfo[0]));
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
            if (QueryUtil.isAnyPropertyRequested(propertyRequest.properties, "isIoInsightAvailableOnCluster")) {
               propValue = QueryUtil.newProperty("isIoInsightAvailableOnCluster", this.isIoInsightAvailableOnCluster(moRef));
               propValue.resourceObject = moRef;
               propValues.add(propValue);
            }

            if (QueryUtil.isAnyPropertyRequested(propertyRequest.properties, "isIoInsightAvailableOnHost")) {
               propValue = QueryUtil.newProperty("isIoInsightAvailableOnHost", this.isIoInsightAvailableOnHost(moRef));
               propValue.resourceObject = moRef;
               propValues.add(propValue);
            }

            if (QueryUtil.isAnyPropertyRequested(propertyRequest.properties, "isIoInsightAvailableOnVM")) {
               propValue = QueryUtil.newProperty("isIoInsightAvailableOnVM", this.isIoInsightAvailableOnVM(moRef));
               propValue.resourceObject = moRef;
               propValues.add(propValue);
            }

            resultItems.add(QueryUtil.newResultItem(objectRef, (PropertyValue[])propValues.toArray(new PropertyValue[0])));
         }
      }

      return QueryUtil.newResultSet((ResultItem[])resultItems.toArray(new ResultItem[0]));
   }

   private Boolean isIoInsightAvailableOnCluster(ManagedObjectReference clusterRef) {
      if (!VsanCapabilityUtils.isIoInsightSupported(clusterRef)) {
         return false;
      } else {
         ConfigInfoEx configInfoEx = null;

         try {
            configInfoEx = this.configInfoService.getVsanConfigInfo(clusterRef);
         } catch (Exception var4) {
            logger.error("Failed to check if IoInsight is supported on the cluster.", var4);
         }

         return configInfoEx != null && configInfoEx.enabled && configInfoEx.perfsvcConfig != null && configInfoEx.perfsvcConfig.enabled;
      }
   }

   private Boolean isIoInsightAvailableOnHost(ManagedObjectReference hostRef) {
      boolean isIoInsightAvailableOnCluster = false;
      boolean isHostInMmode = true;
      boolean isHostConnected = true;
      boolean hasPrivileges = true;

      try {
         DataServiceResponse hostProperties = QueryUtil.getProperties(hostRef, new String[]{"cluster", "runtime.inMaintenanceMode", "runtime.connectionState"});
         ManagedObjectReference clusterRef = (ManagedObjectReference)hostProperties.getProperty(hostRef, "cluster");
         if (clusterRef == null) {
            return false;
         }

         isIoInsightAvailableOnCluster = this.isIoInsightAvailableOnCluster(clusterRef);
         isHostInMmode = (Boolean)hostProperties.getProperty(hostRef, "runtime.inMaintenanceMode");
         isHostConnected = hostProperties.getProperty(hostRef, "runtime.connectionState").equals(ConnectionState.connected);
         hasPrivileges = this.permissionService.hasPermissions(clusterRef, new String[]{"Global.Diagnostics"});
      } catch (Exception var8) {
         logger.error("Failed to load host's properties", var8);
      }

      boolean isIoInsightSupportedOnHost = VsanCapabilityUtils.isIoInsightSupported(hostRef);
      return isIoInsightAvailableOnCluster && !isHostInMmode && isHostConnected && isIoInsightSupportedOnHost && hasPrivileges;
   }

   private Boolean isIoInsightAvailableOnVM(ManagedObjectReference vmRef) {
      boolean isIoInsightSupportedOnHost = false;
      boolean isVmPoweredOn = false;
      boolean isVmOnVsanDatastore = false;
      boolean hasPrivileges = true;

      try {
         DataServiceResponse vmProperties = QueryUtil.getProperties(vmRef, new String[]{"cluster", "summary.runtime.host", "powerState"});
         ManagedObjectReference clusterRef = (ManagedObjectReference)vmProperties.getProperty(vmRef, "cluster");
         if (clusterRef == null) {
            return false;
         }

         hasPrivileges = this.permissionService.hasPermissions(clusterRef, new String[]{"Global.Diagnostics"});
         ManagedObjectReference hostRef = (ManagedObjectReference)vmProperties.getProperty(vmRef, "summary.runtime.host");
         isIoInsightSupportedOnHost = this.isIoInsightAvailableOnHost(hostRef);
         isVmPoweredOn = vmProperties.getProperty(vmRef, "powerState").equals(PowerState.poweredOn);
         isVmOnVsanDatastore = this.vsanInventoryHelper.getVsanDatastore(vmRef) != null;
      } catch (Exception var9) {
         logger.error("Failed to get vm's properties", var9);
      }

      return isIoInsightSupportedOnHost && isVmPoweredOn && isVmOnVsanDatastore && hasPrivileges;
   }

   private List getIoInsightInfos() {
      String[] ioInsightClusterProperties = new String[]{"isIoInsightAvailableOnCluster"};
      String[] ioInsightHostProperties = new String[]{"isIoInsightAvailableOnHost"};
      String[] ioInsightVMProperties = new String[]{"isIoInsightAvailableOnVM"};
      final TypeInfo hostTypeInfo = this.getTypeInfo(HostSystem.class, ioInsightHostProperties);
      final TypeInfo clusterTypeInfo = this.getTypeInfo(ClusterComputeResource.class, ioInsightClusterProperties);
      final TypeInfo vmTypeInfo = this.getTypeInfo(VirtualMachine.class, ioInsightVMProperties);
      return new ArrayList() {
         {
            this.add(hostTypeInfo);
            this.add(clusterTypeInfo);
            this.add(vmTypeInfo);
         }
      };
   }

   private TypeInfo getTypeInfo(Class typeInfoType, String[] properties) {
      TypeInfo typeInfo = new TypeInfo();
      typeInfo.type = typeInfoType.getSimpleName();
      typeInfo.properties = properties;
      return typeInfo;
   }
}
