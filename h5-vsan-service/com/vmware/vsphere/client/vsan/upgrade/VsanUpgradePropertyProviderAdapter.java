package com.vmware.vsphere.client.vsan.upgrade;

import com.vmware.vim.binding.vim.HostSystem;
import com.vmware.vim.binding.vim.host.ScsiDisk;
import com.vmware.vim.binding.vim.vsan.host.DiskMapping;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vise.data.query.DataServiceExtensionRegistry;
import com.vmware.vise.data.query.PropertyRequestSpec;
import com.vmware.vise.data.query.PropertyValue;
import com.vmware.vise.data.query.ResultItem;
import com.vmware.vise.data.query.ResultSet;
import com.vmware.vise.data.query.TypeInfo;
import com.vmware.vsan.client.services.common.VsanBasePropertyProviderAdapter;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class VsanUpgradePropertyProviderAdapter extends VsanBasePropertyProviderAdapter {
   private static final String DISK_MAPPINGS = "config.vsanHostConfig.storageInfo.diskMapping";
   private static final Log _logger = LogFactory.getLog(VsanUpgradePropertyProviderAdapter.class);

   public VsanUpgradePropertyProviderAdapter(DataServiceExtensionRegistry registry) {
      Validate.notNull(registry);
      TypeInfo hostInfo = new TypeInfo();
      hostInfo.type = HostSystem.class.getSimpleName();
      hostInfo.properties = new String[]{"vsanDiskVersionsData"};
      TypeInfo[] providedProperties = new TypeInfo[]{hostInfo};
      registry.registerDataAdapter(this, providedProperties);
   }

   protected ResultSet getResult(PropertyRequestSpec propertyRequest) {
      ResultSet resultSet;
      if (!this.isValidRequest(propertyRequest)) {
         resultSet = new ResultSet();
         resultSet.totalMatchedObjectCount = 0;
         return resultSet;
      } else {
         ArrayList resultItems = new ArrayList();

         try {
            ManagedObjectReference[] moRefs = (ManagedObjectReference[])Arrays.copyOf(propertyRequest.objects, propertyRequest.objects.length, ManagedObjectReference[].class);
            PropertyValue[] propValues = QueryUtil.getProperties(moRefs, new String[]{"config.vsanHostConfig.storageInfo.diskMapping"}).getPropertyValues();
            PropertyValue[] var6 = propValues;
            int var7 = propValues.length;

            for(int var8 = 0; var8 < var7; ++var8) {
               PropertyValue propValue = var6[var8];
               DiskMapping[] diskMappings = (DiskMapping[])((DiskMapping[])propValue.value);
               ManagedObjectReference hostRef = (ManagedObjectReference)propValue.resourceObject;
               VsanDiskVersionData[] hostDiskVersionData = this.getHostDiskVersionsData(hostRef, diskMappings);
               PropertyValue resultPropValue = QueryUtil.newProperty("vsanDiskVersionsData", hostDiskVersionData);
               resultPropValue.resourceObject = hostRef;
               ResultItem resultItem = QueryUtil.newResultItem(hostRef, resultPropValue);
               resultItems.add(resultItem);
            }
         } catch (Exception var15) {
            _logger.error("Failed to retrieve properties from DS. ", var15);
            resultSet = new ResultSet();
            resultSet.error = var15;
            return resultSet;
         }

         return QueryUtil.newResultSet((ResultItem[])resultItems.toArray(new ResultItem[0]));
      }
   }

   private VsanDiskVersionData getDiskVersionData(ScsiDisk scsiDisk) {
      return scsiDisk.vsanDiskInfo == null ? new VsanDiskVersionData() : new VsanDiskVersionData(scsiDisk.vsanDiskInfo);
   }

   private VsanDiskVersionData[] getHostDiskVersionsData(ManagedObjectReference host, DiskMapping[] diskGroups) {
      if (ArrayUtils.isEmpty(diskGroups)) {
         return null;
      } else {
         List disksData = new ArrayList();
         DiskMapping[] var4 = diskGroups;
         int var5 = diskGroups.length;

         for(int var6 = 0; var6 < var5; ++var6) {
            DiskMapping diskGroup = var4[var6];
            disksData.add(this.getDiskVersionData(diskGroup.ssd));
            ScsiDisk[] var8 = diskGroup.nonSsd;
            int var9 = var8.length;

            for(int var10 = 0; var10 < var9; ++var10) {
               ScsiDisk disk = var8[var10];
               disksData.add(this.getDiskVersionData(disk));
            }
         }

         return (VsanDiskVersionData[])disksData.toArray(new VsanDiskVersionData[disksData.size()]);
      }
   }

   private boolean isValidRequest(PropertyRequestSpec propertyRequest) {
      if (propertyRequest == null) {
         return false;
      } else if (!ArrayUtils.isEmpty(propertyRequest.objects) && !ArrayUtils.isEmpty(propertyRequest.properties)) {
         if (!(propertyRequest.objects[0] instanceof ManagedObjectReference)) {
            _logger.error("VsanUpgradePropertyProviderAdapter got a list of objects that are not of type ManagedObjectReferences");
            return false;
         } else {
            return true;
         }
      } else {
         _logger.error("VsanUpgradePropertyProviderAdapter got a null or empty list of properties or objects");
         return false;
      }
   }
}
