package com.vmware.vsphere.client.vsan.data;

import com.vmware.proxygen.ts.TsModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@TsModel
public class VsanSemiAutoClaimDisksData {
   public VsanDiskData[] notInUseDisks;
   public VsanPmemDiskData[] notInUsePmemStorage;
   public int numNotInUseSsdDisks = 0;
   public int numNotInUseDataDisks = 0;
   public int numAllFlashGroups = 0;
   public int numHybridGroups = 0;
   public int numAllFlashCapacityDisks = 0;
   public int numHybridCapacityDisks = 0;
   public boolean hybridDiskGroupExist = false;
   public boolean allFlashDiskGroupExist = false;
   public boolean isAllFlashAvailable = true;
   public long claimedCapacity = 0L;
   public long claimedCache = 0L;
   public List claimedDisksSummary = new ArrayList();

   public VsanSemiAutoClaimDisksData() {
   }

   public VsanSemiAutoClaimDisksData(VsanDiskData[] eligibleScsiDisks, VsanPmemDiskData[] eligiblePmemStorage) {
      this.setNotInUseDisks(eligibleScsiDisks);
      this.setNotInUsePmemStorage(eligiblePmemStorage);
   }

   private void setNotInUseDisks(VsanDiskData[] eligibleScsiDisks) {
      this.notInUseDisks = eligibleScsiDisks;
      this.numNotInUseSsdDisks = (int)Arrays.stream(this.notInUseDisks).filter((diskData) -> {
         return diskData.disk.ssd;
      }).count();
      this.numNotInUseDataDisks = (int)Arrays.stream(this.notInUseDisks).filter((diskData) -> {
         return !diskData.disk.ssd;
      }).count();
   }

   public void setNotInUsePmemStorage(VsanPmemDiskData[] eligiblePmemStorage) {
      this.notInUsePmemStorage = eligiblePmemStorage;
   }
}
