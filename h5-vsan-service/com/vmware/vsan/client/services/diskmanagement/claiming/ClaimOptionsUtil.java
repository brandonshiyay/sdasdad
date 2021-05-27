package com.vmware.vsan.client.services.diskmanagement.claiming;

import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsphere.client.vsan.data.ClaimOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClaimOptionsUtil {
   public static Map getSupportedClaimOptions(final ManagedObjectReference hostRef) {
      return new HashMap() {
         {
            this.put(DiskType.HDD, ClaimOptionsUtil.getHddClaimOptions(hostRef));
            this.put(DiskType.FLASH, ClaimOptionsUtil.getFlashDiskClaimOptions(hostRef));
            this.put(DiskType.PMEM, ClaimOptionsUtil.getPmemStorageClaimOptions(hostRef));
         }
      };
   }

   private static ClaimOption[] getHddClaimOptions(ManagedObjectReference hostRef) {
      List hddDiskClaimOptions = new ArrayList();
      hddDiskClaimOptions.add(ClaimOption.DoNotClaim);
      hddDiskClaimOptions.add(ClaimOption.ClaimForStorage);
      if (isManagedVmfsSupported(hostRef)) {
         hddDiskClaimOptions.add(ClaimOption.VMFS);
      }

      return (ClaimOption[])hddDiskClaimOptions.toArray(new ClaimOption[0]);
   }

   private static ClaimOption[] getFlashDiskClaimOptions(ManagedObjectReference hostRef) {
      List flashDiskClaimOptions = new ArrayList();
      flashDiskClaimOptions.add(ClaimOption.DoNotClaim);
      flashDiskClaimOptions.add(ClaimOption.ClaimForStorage);
      flashDiskClaimOptions.add(ClaimOption.ClaimForCache);
      if (isManagedVmfsSupported(hostRef)) {
         flashDiskClaimOptions.add(ClaimOption.VMFS);
      }

      return (ClaimOption[])flashDiskClaimOptions.toArray(new ClaimOption[0]);
   }

   private static ClaimOption[] getPmemStorageClaimOptions(ManagedObjectReference hostRef) {
      List pmemStorageClaimOptions = new ArrayList();
      if (isManagedPmemSupported(hostRef)) {
         pmemStorageClaimOptions.add(ClaimOption.DoNotClaim);
         pmemStorageClaimOptions.add(ClaimOption.PMEM);
      }

      return (ClaimOption[])pmemStorageClaimOptions.toArray(new ClaimOption[0]);
   }

   private static boolean isManagedVmfsSupported(ManagedObjectReference hostRef) {
      return VsanCapabilityUtils.isManagedVmfsSupportedOnVC(hostRef) && VsanCapabilityUtils.isManagedVmfsSupported(hostRef);
   }

   private static boolean isManagedPmemSupported(ManagedObjectReference hostRef) {
      return VsanCapabilityUtils.isManagedPMemSupportedOnVC(hostRef) && VsanCapabilityUtils.isManagedPMemSupported(hostRef);
   }
}
