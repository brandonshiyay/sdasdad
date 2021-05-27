package com.vmware.vsan.client.services.evacuationstatus.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsan.client.services.diskGroups.data.VsanDiskMapping;
import java.util.Map;

@TsModel
public class DataMigrationDiskGroupData {
   public boolean isLocked;
   public boolean isMounted;
   public VsanDiskMapping diskMapping;
   public Map disksStatuses;
}
