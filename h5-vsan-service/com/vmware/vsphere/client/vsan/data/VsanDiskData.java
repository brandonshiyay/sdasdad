package com.vmware.vsphere.client.vsan.data;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vim.host.ScsiDisk;
import com.vmware.vim.binding.vim.vsan.host.DiskResult;
import com.vmware.vim.binding.vim.vsan.host.DiskResult.State;
import com.vmware.vsan.client.services.diskmanagement.DiskManagementUtil;
import com.vmware.vsan.client.services.diskmanagement.DiskStatus;
import com.vmware.vsan.client.services.diskmanagement.DiskStatusUtil;
import com.vmware.vsan.client.services.diskmanagement.claiming.DiskType;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.CollectionUtils;

@TsModel
public class VsanDiskData {
   public ScsiDisk disk;
   public boolean inUse;
   public boolean ineligible;
   public String stateReason;
   public String[] issues;
   public String vsanUuid;
   public String diskGroupUuid;
   public boolean isCacheDisk;
   public boolean markedAsCapacityFlash;
   public DiskLocalityType diskLocality;
   public ClaimOption recommendedAllFlashClaimOption;
   public ClaimOption recommendedHybridClaimOption;
   public ClaimOption[] possibleClaimOptions;
   public ClaimOption[] possibleClaimOptionsIfMarkedAsOppositeType;
   public DiskStatus diskStatus;

   public VsanDiskData() {
      this.recommendedAllFlashClaimOption = ClaimOption.DoNotClaim;
      this.recommendedHybridClaimOption = ClaimOption.DoNotClaim;
   }

   public VsanDiskData(DiskResult diskResult, Map diskToIssues, Map vsanDiskStatuses) {
      this(diskResult);
      if (this.inUse) {
         this.diskStatus = (DiskStatus)vsanDiskStatuses.get(diskResult.vsanUuid);
      }

      if (diskToIssues != null && diskToIssues.containsKey(this.disk.uuid)) {
         List diskIssues = (List)diskToIssues.get(this.disk.uuid);
         if (!CollectionUtils.isEmpty(diskIssues)) {
            this.issues = (String[])diskIssues.toArray(new String[0]);
         }
      }

   }

   public VsanDiskData(DiskResult diskResult, Map claimOptionsPerDiskType) {
      this(diskResult);
      this.possibleClaimOptions = (ClaimOption[])claimOptionsPerDiskType.get(this.getDiskType(this.disk.ssd));
      this.possibleClaimOptionsIfMarkedAsOppositeType = (ClaimOption[])claimOptionsPerDiskType.get(this.getDiskType(!this.disk.ssd));
   }

   public VsanDiskData(DiskResult diskResult) {
      this.recommendedAllFlashClaimOption = ClaimOption.DoNotClaim;
      this.recommendedHybridClaimOption = ClaimOption.DoNotClaim;
      State diskState = (State)Enum.valueOf(State.class, diskResult.state);
      this.disk = diskResult.disk;
      this.vsanUuid = diskResult.vsanUuid;
      this.inUse = diskState == State.inUse;
      this.ineligible = diskState == State.ineligible;
      this.diskLocality = DiskManagementUtil.getDiskLocality(diskResult.disk);
      this.markedAsCapacityFlash = false;
      if (diskState != State.inUse) {
         this.diskStatus = DiskStatusUtil.getNotClaimedDiskStatus(diskResult.disk, diskResult.error);
      }

      if (diskResult.error != null) {
         this.stateReason = diskResult.error.getLocalizedMessage();
      }

   }

   private DiskType getDiskType(boolean isSsd) {
      return isSsd ? DiskType.FLASH : DiskType.HDD;
   }
}
