package com.vmware.vsphere.client.vsan.data;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsan.client.services.diskmanagement.DiskStatus;
import com.vmware.vsan.client.services.diskmanagement.DiskStatusUtil;
import com.vmware.vsan.client.services.diskmanagement.claiming.DiskType;
import com.vmware.vsan.client.services.diskmanagement.pmem.PmemStorage;
import java.util.Map;

@TsModel
public class VsanPmemDiskData {
   public String uuid;
   public String name;
   public long capacity;
   public ClaimOption[] possibleClaimOptions;
   public DiskStatus diskStatus;
   public boolean isManageableByVsan;

   public VsanPmemDiskData() {
   }

   public VsanPmemDiskData(PmemStorage storage, Map claimOptionsPerDiskType) {
      this.uuid = storage.uuid;
      this.name = storage.name;
      this.capacity = storage.capacity.total;
      this.possibleClaimOptions = (ClaimOption[])claimOptionsPerDiskType.get(DiskType.PMEM);
      this.diskStatus = DiskStatusUtil.getPmemStorageStatus(storage);
      this.isManageableByVsan = storage.isManageableByVsan;
   }
}
