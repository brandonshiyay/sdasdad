package com.vmware.vsphere.client.vsan.data;

import com.vmware.proxygen.ts.TsModel;
import java.util.List;
import java.util.Map;

@TsModel
public class VsanDirectDiskGroupData {
   public List disks;
   public Map diskToCapacity;
   public Map diskToObjectUuids;
}
