package com.vmware.vsan.client.services.virtualobjects.data;

import com.vmware.vim.binding.vim.host.ScsiDisk;
import com.vmware.vim.binding.vim.vsan.host.DiskResult;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vsan.client.services.capacity.model.DatastoreType;
import com.vmware.vsan.client.services.virtualobjects.VirtualObjectsUtil;
import com.vmware.vsphere.client.vsan.util.DataServiceResponse;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class VsanDirectObjectPlacementFactory {
   private static final Log logger = LogFactory.getLog(VsanDirectObjectPlacementFactory.class);
   private Set hostRefs;
   private Map hostData;
   private Map hostsToDisks;

   public VsanDirectObjectPlacementFactory(Set hostRefs, DataServiceResponse hostsPhysicalPlacementProps, Map disks) {
      this.hostRefs = hostRefs;
      this.hostData = hostsPhysicalPlacementProps.getMap();
      this.hostsToDisks = this.flatMapHostsToDisks(disks);
   }

   public VirtualObjectPlacementModel create(VirtualObjectBasicModel object) {
      Iterator var2 = this.hostRefs.iterator();

      ManagedObjectReference hostRef;
      do {
         if (!var2.hasNext()) {
            logger.warn("Could not find disk with uuid: " + object.diskUuid);
            return null;
         }

         hostRef = (ManagedObjectReference)var2.next();
      } while(!this.hostsToDisks.containsKey(hostRef) || !((Map)this.hostsToDisks.get(hostRef)).containsKey(object.diskUuid));

      return this.buildObjectPlacement(object, hostRef);
   }

   private Map flatMapHostsToDisks(Map hostsToDisks) {
      return (Map)hostsToDisks.entrySet().stream().collect(Collectors.toMap(Entry::getKey, (entry) -> {
         return this.mapUuidToDisk((DiskResult[])entry.getValue());
      }));
   }

   private Map mapUuidToDisk(DiskResult[] disks) {
      return (Map)Arrays.stream(disks).collect(Collectors.toMap((diskResult) -> {
         return diskResult.disk.uuid;
      }, (diskResult) -> {
         return diskResult.disk;
      }));
   }

   private VirtualObjectPlacementModel buildObjectPlacement(VirtualObjectBasicModel object, ManagedObjectReference hostRef) {
      VirtualObjectPlacementModel objectPlacement = new VirtualObjectPlacementModel();
      objectPlacement.label = object.name;
      objectPlacement.nodeUuid = object.uid;
      objectPlacement.host = this.hostData.containsKey(hostRef) ? VirtualObjectsUtil.buildHostPlacement((Map)this.hostData.get(hostRef)) : null;
      objectPlacement.capacityDisk = this.buildDiskPlacement(hostRef, object.diskUuid);
      objectPlacement.datastoreType = DatastoreType.VMFS;
      return objectPlacement;
   }

   private VirtualObjectPlacementModel buildDiskPlacement(ManagedObjectReference hostRef, String diskUuid) {
      if (this.hostsToDisks.containsKey(hostRef) && ((Map)this.hostsToDisks.get(hostRef)).containsKey(diskUuid)) {
         VirtualObjectPlacementModel diskPlacement = new VirtualObjectPlacementModel();
         ScsiDisk disk = (ScsiDisk)((Map)this.hostsToDisks.get(hostRef)).get(diskUuid);
         diskPlacement.nodeUuid = disk.uuid;
         diskPlacement.label = disk.displayName;
         diskPlacement.iconId = BooleanUtils.isTrue(disk.ssd) ? "ssd-disk-icon" : "disk-icon";
         return diskPlacement;
      } else {
         return null;
      }
   }
}
