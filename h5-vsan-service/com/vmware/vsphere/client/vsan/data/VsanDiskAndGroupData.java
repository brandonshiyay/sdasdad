package com.vmware.vsphere.client.vsan.data;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsan.client.services.diskmanagement.PmemDiskData;

@TsModel
public class VsanDiskAndGroupData {
   public VsanDiskData[] connectedDisks;
   public VsanDiskData[] ineligibleDisks;
   public VsanDiskData[] disksNotInUse;
   public VsanDiskData[] vsanDisks;
   public VsanDiskGroupData[] vsanGroups;
   public VsanDirectDiskGroupData vsanDirectDiskGroupData;
   public PmemDiskData[] managedPmemStorage;
}
