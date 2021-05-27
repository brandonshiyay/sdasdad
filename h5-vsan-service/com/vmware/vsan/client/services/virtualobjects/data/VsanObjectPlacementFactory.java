package com.vmware.vsan.client.services.virtualobjects.data;

import com.vmware.vim.binding.vim.host.ScsiDisk;
import com.vmware.vim.binding.vim.vsan.host.DiskResult;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vsan.client.services.capacity.model.DatastoreType;
import com.vmware.vsan.client.services.virtualobjects.VirtualObjectsUtil;
import com.vmware.vsphere.client.vsan.base.data.VsanComponent;
import com.vmware.vsphere.client.vsan.base.data.VsanObject;
import com.vmware.vsphere.client.vsan.base.data.VsanRaidConfig;
import com.vmware.vsphere.client.vsan.util.DataServiceResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class VsanObjectPlacementFactory {
   private static final Log logger = LogFactory.getLog(VsanObjectPlacementFactory.class);
   private static final String INVALID_DISK_UUID = "Object not found";
   private Map hostData;
   private Map diskByUuid = new HashMap();

   public VsanObjectPlacementFactory(DataServiceResponse hostData, Map disks) {
      this.hostData = hostData.getMap();
      List diskResults = (List)disks.values().stream().flatMap(Arrays::stream).collect(Collectors.toList());
      Iterator var4 = diskResults.iterator();

      while(var4.hasNext()) {
         DiskResult disk = (DiskResult)var4.next();
         this.diskByUuid.put(disk.vsanUuid, disk);
      }

   }

   public List create(VsanObject virtualObject) {
      if (virtualObject.rootConfig != null && !CollectionUtils.isEmpty(virtualObject.rootConfig.children)) {
         List models = new ArrayList();
         Iterator var3 = virtualObject.rootConfig.children.iterator();

         while(var3.hasNext()) {
            VsanComponent component = (VsanComponent)var3.next();
            if (component instanceof VsanRaidConfig) {
               models.add(this.buildRaid((VsanRaidConfig)component));
            } else {
               models.add(this.buildComponent(component));
            }
         }

         return models;
      } else {
         return Collections.emptyList();
      }
   }

   private VirtualObjectPlacementModel buildRaid(VsanRaidConfig raidConfig) {
      VirtualObjectPlacementModel result = new VirtualObjectPlacementModel();
      result.label = raidConfig.type;
      if (CollectionUtils.isEmpty(raidConfig.children)) {
         return result;
      } else {
         result.children = new ArrayList();
         Iterator var3 = raidConfig.children.iterator();

         while(var3.hasNext()) {
            VsanComponent component = (VsanComponent)var3.next();
            if (component instanceof VsanRaidConfig) {
               result.children.add(this.buildRaid((VsanRaidConfig)component));
            } else {
               result.children.add(this.buildComponent(component));
            }
         }

         return result;
      }
   }

   private VirtualObjectPlacementModel buildComponent(VsanComponent component) {
      VirtualObjectPlacementModel result = new VirtualObjectPlacementModel();
      result.nodeUuid = component.componentUuid;
      result.label = component.type;
      result.state = component.state;
      result.host = StringUtils.isNotEmpty(component.hostUuid) ? VirtualObjectsUtil.buildHostPlacement(this.getHostData(component.hostUuid), component.hostUuid) : null;
      result.cacheDisk = this.buildDisk(component.cacheDiskUuid);
      result.capacityDisk = this.buildDisk(component.capacityDiskUuid);
      result.datastoreType = DatastoreType.VSAN;
      return result;
   }

   private VirtualObjectPlacementModel buildDisk(String nodeUuid) {
      if (!StringUtils.isEmpty(nodeUuid) && !"Object not found".equals(nodeUuid)) {
         VirtualObjectPlacementModel result = new VirtualObjectPlacementModel();
         result.nodeUuid = nodeUuid;
         if (this.diskByUuid.containsKey(nodeUuid)) {
            ScsiDisk disk = ((DiskResult)this.diskByUuid.get(nodeUuid)).disk;
            result.label = disk.displayName;
            if (BooleanUtils.isTrue(disk.ssd)) {
               result.iconId = "ssd-disk-icon";
            } else {
               result.iconId = "disk-icon";
            }
         } else {
            result.label = nodeUuid;
         }

         return result;
      } else {
         return null;
      }
   }

   private Map getHostData(String vsanUuid) {
      Iterator var2 = this.hostData.keySet().iterator();

      Map hostProperties;
      do {
         if (!var2.hasNext()) {
            logger.warn("Host data not found: nodeUuid=" + vsanUuid);
            return null;
         }

         ManagedObjectReference hostRef = (ManagedObjectReference)var2.next();
         hostProperties = (Map)this.hostData.get(hostRef);
      } while(!vsanUuid.equals(hostProperties.get("config.vsanHostConfig.clusterInfo.nodeUuid")));

      return hostProperties;
   }
}
