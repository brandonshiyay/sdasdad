package com.vmware.vsan.client.services.diskmanagement;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanManagedStorageObjUuidMapping;
import com.vmware.vim.vsan.binding.vim.vsan.host.VsanManagedDisksInfo;
import com.vmware.vsan.client.services.diskmanagement.claiming.AvailabilityState;
import com.vmware.vsan.client.services.diskmanagement.pmem.PmemStorage;
import com.vmware.vsan.client.services.virtualobjects.VirtualObjectsUtil;
import com.vmware.vsphere.client.vsan.data.ClaimOption;
import java.util.List;

@TsModel
public class PmemDiskData implements IStorageData {
   public ManagedObjectReference dsRef;
   public String name;
   public String uuid;
   public StorageCapacity capacity;
   public List objectUuids;
   public ClaimOption claimOption;
   public DiskStatus diskStatus;

   public PmemDiskData() {
   }

   public PmemDiskData(PmemStorage storage, VsanManagedStorageObjUuidMapping[] storageToObjectUuids) {
      this.dsRef = storage.dsRef;
      this.uuid = storage.uuid;
      this.name = storage.name;
      this.capacity = storage.capacity;
      this.objectUuids = VirtualObjectsUtil.getObjectUuidsOnPmem(storageToObjectUuids, storage.dsRef.getValue());
      this.diskStatus = DiskStatusUtil.getPmemStorageStatus(storage);
   }

   public AvailabilityState getAvailabilityState(VsanManagedDisksInfo managedDisks) {
      return DiskManagementUtil.isPmemStorageInUse(this.dsRef, managedDisks) ? AvailabilityState.ONLY_MANAGED_BY_VSAN : AvailabilityState.ELIGIBLE;
   }

   public StorageCapacity getCapacity() {
      return this.capacity;
   }

   public List getObjectUuids() {
      return this.objectUuids;
   }
}
