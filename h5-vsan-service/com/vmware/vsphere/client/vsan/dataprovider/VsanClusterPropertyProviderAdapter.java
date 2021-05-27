package com.vmware.vsphere.client.vsan.dataprovider;

import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.vsan.ConfigInfoEx;
import com.vmware.vise.data.PropertySpec;
import com.vmware.vise.data.query.DataServiceExtensionRegistry;
import com.vmware.vise.data.query.PropertyRequestSpec;
import com.vmware.vise.data.query.PropertyValue;
import com.vmware.vise.data.query.ResultItem;
import com.vmware.vise.data.query.ResultSet;
import com.vmware.vise.data.query.TypeInfo;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.services.common.VsanBasePropertyProviderAdapter;
import com.vmware.vsan.client.services.config.VsanConfigService;
import com.vmware.vsan.client.services.csd.CsdService;
import com.vmware.vsan.client.util.VsanInventoryHelper;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class VsanClusterPropertyProviderAdapter extends VsanBasePropertyProviderAdapter {
   private static final Log logger = LogFactory.getLog(VsanClusterPropertyProviderAdapter.class);
   private static final String PROPERTY_IS_COMPUTE_ONLY_CLUSTER = "isComputeOnlyCluster";
   private static final String PROPERTY_IS_VSAN_ENABLED_OR_COMPUTE_ONLY_CLUSTER = "isVsanEnabledOrComputeOnlyCluster";
   private static final String PROPERTY_IS_FILE_SERVICE_ENABLED = "isFileServiceEnabled";
   private static final String PROPERTY_HAS_ELIGIBLE_HOSTS = "hasVsanEligibleHosts";
   private static final String PROPERTY_IS_ISCSI_TARGETS_ENABLED = "isIscsiTargetsEnabled";
   private static final String PROPERTY_VSAN_CONFIG_INFO = "vsanConfigInfo";
   private static final String PROPERTY_VSAN_RESYNC_THROTTLING = "vsanResyncThrottling";
   @Autowired
   private VsanConfigService vsanConfigService;
   @Autowired
   private VsanInventoryHelper vsanInventoryHelper;
   @Autowired
   private CsdService csdService;

   public VsanClusterPropertyProviderAdapter(DataServiceExtensionRegistry registry) {
      Validate.notNull(registry);
      String[] clusterProperties = new String[]{"isComputeOnlyCluster", "isVsanEnabledOrComputeOnlyCluster", "hasVsanEligibleHosts", "isFileServiceEnabled", "isIscsiTargetsEnabled", "vsanConfigInfo", "vsanResyncThrottling"};
      TypeInfo clusterInfo = new TypeInfo();
      clusterInfo.type = ClusterComputeResource.class.getSimpleName();
      clusterInfo.properties = clusterProperties;
      TypeInfo[] providedProperties = new TypeInfo[]{clusterInfo};
      registry.registerDataAdapter(this, providedProperties);
   }

   protected ResultSet getResult(PropertyRequestSpec propertyRequest) {
      List resultItems = new ArrayList();
      Object[] var3 = propertyRequest.objects;
      int var4 = var3.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         Object objectRef = var3[var5];
         ManagedObjectReference moRef = (ManagedObjectReference)objectRef;
         if (objectRef != null) {
            ResultItem resultItem = null;
            if (ClusterComputeResource.class.getSimpleName().equals(moRef.getType())) {
               PropertyValue[] clusterProperties = this.getClusterProperties(propertyRequest.properties, objectRef);
               resultItem = QueryUtil.newResultItem(objectRef, clusterProperties);
            }

            resultItems.add(resultItem);
         }
      }

      return QueryUtil.newResultSet((ResultItem[])resultItems.toArray(new ResultItem[0]));
   }

   private PropertyValue[] getClusterProperties(PropertySpec[] properties, Object objectRef) {
      List propValues = new ArrayList();
      ManagedObjectReference clusterRef = (ManagedObjectReference)objectRef;
      PropertyValue propValue;
      if (QueryUtil.isAnyPropertyRequested(properties, "hasVsanEligibleHosts")) {
         int hostCount = this.vsanInventoryHelper.getNumberOfClusterHosts(clusterRef);
         if (QueryUtil.isAnyPropertyRequested(properties, "hasVsanEligibleHosts")) {
            propValue = QueryUtil.newProperty("hasVsanEligibleHosts", hostCount > 0);
            propValue.resourceObject = objectRef;
            propValues.add(propValue);
         }
      }

      if (QueryUtil.isAnyPropertyRequested(properties, "isComputeOnlyCluster", "isVsanEnabledOrComputeOnlyCluster", "isFileServiceEnabled", "isIscsiTargetsEnabled", "vsanConfigInfo", "vsanResyncThrottling")) {
         ConfigInfoEx vsanConfig = null;

         try {
            vsanConfig = this.vsanConfigService.getConfigInfoEx(clusterRef);
         } catch (Exception var8) {
            logger.error("Cannot retrieve vSAN configuration for cluster: " + clusterRef, var8);
         }

         PropertyValue propValue;
         boolean result;
         if (QueryUtil.isAnyPropertyRequested(properties, "isComputeOnlyCluster")) {
            result = this.csdService.isComputeOnlyClusterByConfigInfoEx(vsanConfig);
            propValue = QueryUtil.newProperty("isComputeOnlyCluster", result);
            propValue.resourceObject = clusterRef;
            propValues.add(propValue);
         }

         if (QueryUtil.isAnyPropertyRequested(properties, "isVsanEnabledOrComputeOnlyCluster")) {
            propValue = QueryUtil.newProperty("isVsanEnabledOrComputeOnlyCluster", this.isVsanEnabledOrComputeOnlyCluster(vsanConfig, clusterRef));
            propValue.resourceObject = clusterRef;
            propValues.add(propValue);
         }

         if (QueryUtil.isAnyPropertyRequested(properties, "isFileServiceEnabled")) {
            result = vsanConfig != null && BooleanUtils.isTrue(vsanConfig.enabled) && vsanConfig.fileServiceConfig != null && BooleanUtils.isTrue(vsanConfig.fileServiceConfig.enabled) && ArrayUtils.isNotEmpty(vsanConfig.fileServiceConfig.domains);
            propValue = QueryUtil.newProperty("isFileServiceEnabled", result);
            propValue.resourceObject = clusterRef;
            propValues.add(propValue);
         }

         if (QueryUtil.isAnyPropertyRequested(properties, "isIscsiTargetsEnabled")) {
            result = vsanConfig != null && BooleanUtils.isTrue(vsanConfig.enabled) && vsanConfig.iscsiConfig != null && BooleanUtils.isTrue(vsanConfig.iscsiConfig.enabled);
            propValue = QueryUtil.newProperty("isIscsiTargetsEnabled", result);
            propValue.resourceObject = clusterRef;
            propValues.add(propValue);
         }

         if (QueryUtil.isAnyPropertyRequested(properties, "vsanConfigInfo")) {
            propValue = QueryUtil.newProperty("vsanConfigInfo", VsanCapabilityUtils.isClusterConfigSystemSupportedOnVc(clusterRef) ? vsanConfig : null);
            propValue.resourceObject = objectRef;
            propValues.add(propValue);
         }

         if (QueryUtil.isAnyPropertyRequested(properties, "vsanResyncThrottling")) {
            propValue = QueryUtil.newProperty("vsanResyncThrottling", this.getResyncThrottlingStatusValue(clusterRef, vsanConfig));
            propValue.resourceObject = objectRef;
            propValues.add(propValue);
         }
      }

      return (PropertyValue[])propValues.toArray(new PropertyValue[0]);
   }

   private boolean isVsanEnabledOrComputeOnlyCluster(ConfigInfoEx configInfoEx, ManagedObjectReference clusterRef) {
      if (configInfoEx == null) {
         try {
            Boolean isEnabled = (Boolean)QueryUtil.getProperty(clusterRef, "configurationEx[@type='ClusterConfigInfoEx'].vsanConfigInfo.enabled");
            if (BooleanUtils.isTrue(isEnabled)) {
               return true;
            }
         } catch (Exception var4) {
            logger.error("Cannot retrieve dataservice property configurationEx[@type='ClusterConfigInfoEx'].vsanConfigInfo.enabled", var4);
         }

         return false;
      } else {
         return BooleanUtils.isTrue(configInfoEx.enabled) ? true : this.csdService.isComputeOnlyClusterByConfigInfoEx(configInfoEx);
      }
   }

   private int getResyncThrottlingStatusValue(ManagedObjectReference clusterRef, ConfigInfoEx config) {
      int result = -1;
      if (!VsanCapabilityUtils.isResyncThrottlingSupported(clusterRef)) {
         return result;
      } else {
         if (config != null && config.resyncIopsLimitConfig != null) {
            result = config.resyncIopsLimitConfig.resyncIops;
         }

         return result;
      }
   }
}
