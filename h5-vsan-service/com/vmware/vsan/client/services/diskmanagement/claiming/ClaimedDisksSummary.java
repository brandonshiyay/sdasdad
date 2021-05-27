package com.vmware.vsan.client.services.diskmanagement.claiming;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsphere.client.vsan.data.ClaimOption;

@TsModel
public class ClaimedDisksSummary {
   public ClaimOption claimOption;
   public int claimedDisksCount;
   public long claimedCapacity;
}
