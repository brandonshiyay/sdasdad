package com.vmware.vsphere.client.vsan.data;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class VsanDiskGroupData {
   public VsanDiskData ssd;
   public VsanDiskData[] disks;
   public boolean mounted = true;
   public boolean unlockedEncrypted;
   public boolean encrypted;
   public boolean isAllFlash;
}
