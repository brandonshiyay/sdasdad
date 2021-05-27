package com.vmware.vsphere.client.vsan.perf.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vim.host.ScsiDisk;
import com.vmware.vim.binding.vim.vsan.host.DiskMapping;
import java.util.ArrayList;
import java.util.List;

@TsModel
public class DiskGroup {
   public String diskGroupUuid;
   public String diskGroupName;
   public DiskInfo cacheDisk;
   public List capacityDisks;

   public static DiskGroup fromDiskMapping(DiskMapping diskMapping) {
      DiskGroup group = new DiskGroup();
      group.diskGroupName = diskMapping.ssd.vsanDiskInfo.vsanUuid;
      group.diskGroupUuid = diskMapping.ssd.vsanDiskInfo.vsanUuid;
      group.cacheDisk = new DiskInfo();
      group.cacheDisk.diskUuid = diskMapping.ssd.vsanDiskInfo.vsanUuid;
      group.cacheDisk.diskName = diskMapping.ssd.displayName;
      List capacityDisks = new ArrayList();
      ScsiDisk[] var3 = diskMapping.nonSsd;
      int var4 = var3.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         ScsiDisk disk = var3[var5];
         DiskInfo capacityDisk = new DiskInfo();
         capacityDisk.diskName = disk.displayName;
         capacityDisk.diskUuid = disk.vsanDiskInfo.vsanUuid;
         capacityDisks.add(capacityDisk);
      }

      group.capacityDisks = capacityDisks;
      return group;
   }
}
