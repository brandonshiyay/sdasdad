package com.vmware.vsan.client.services.virtualobjects.data;

import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanManagedStorageObjUuidMapping;
import com.vmware.vsan.client.services.capacity.model.DatastoreType;
import com.vmware.vsan.client.services.diskmanagement.pmem.PmemStorage;
import com.vmware.vsan.client.services.virtualobjects.VirtualObjectsUtil;
import com.vmware.vsphere.client.vsan.util.DataServiceResponse;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class PmemObjectPlacementFactory {
   private Set hostRefs;
   private Map hostData;
   private Map hostToPmemStorage;
   private Map hostToStorageObjUuidMapping;

   public PmemObjectPlacementFactory(Set hostRefs, DataServiceResponse hostsPhysicalPlacementProps, Map hostToPmemStorage, Map hostToStorageObjUuidMapping) {
      this.hostRefs = hostRefs;
      this.hostData = hostsPhysicalPlacementProps.getMap();
      this.hostToPmemStorage = hostToPmemStorage;
      this.hostToStorageObjUuidMapping = hostToStorageObjUuidMapping;
   }

   public VirtualObjectPlacementModel create(String objectId) {
      Iterator var2 = this.hostRefs.iterator();

      ManagedObjectReference hostRef;
      PmemStorage pmemStorage;
      do {
         if (!var2.hasNext()) {
            return null;
         }

         hostRef = (ManagedObjectReference)var2.next();
         pmemStorage = this.findPmemStorage(objectId, hostRef);
      } while(pmemStorage == null);

      return this.buildObjectPlacement(objectId, pmemStorage, hostRef);
   }

   private PmemStorage findPmemStorage(String objectId, ManagedObjectReference hostRef) {
      if (this.hostToStorageObjUuidMapping != null && this.hostToPmemStorage != null) {
         VsanManagedStorageObjUuidMapping[] storageToObjectUuids = (VsanManagedStorageObjUuidMapping[])this.hostToStorageObjUuidMapping.get(hostRef);
         String storageId = VirtualObjectsUtil.getPmemStorageId(storageToObjectUuids, objectId);
         return StringUtils.isEmpty(storageId) ? null : this.findPmemStorage(storageId, (List)this.hostToPmemStorage.get(hostRef));
      } else {
         return null;
      }
   }

   private PmemStorage findPmemStorage(String storageId, List hostPmemStorage) {
      return CollectionUtils.isEmpty(hostPmemStorage) ? null : (PmemStorage)hostPmemStorage.stream().filter((storage) -> {
         return storage.dsRef.getValue().equalsIgnoreCase(storageId);
      }).findFirst().orElse((Object)null);
   }

   private VirtualObjectPlacementModel buildObjectPlacement(String objectId, PmemStorage pmemStorage, ManagedObjectReference hostRef) {
      VirtualObjectPlacementModel objectPlacement = new VirtualObjectPlacementModel();
      objectPlacement.nodeUuid = objectId;
      objectPlacement.host = this.hostData.containsKey(hostRef) ? VirtualObjectsUtil.buildHostPlacement((Map)this.hostData.get(hostRef)) : null;
      objectPlacement.capacityDisk = this.buildPmemPlacement(pmemStorage);
      objectPlacement.datastoreType = DatastoreType.PMEM;
      return objectPlacement;
   }

   private VirtualObjectPlacementModel buildPmemPlacement(PmemStorage pmemStorage) {
      VirtualObjectPlacementModel diskPlacement = new VirtualObjectPlacementModel();
      diskPlacement.nodeUuid = pmemStorage.uuid;
      diskPlacement.label = pmemStorage.name;
      return diskPlacement;
   }
}
