package com.vmware.vsan.client.services.evacuationstatus.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vsan.client.services.diskmanagement.DiskData;

@TsModel
public class EvacuationEntity {
   public ManagedObjectReference hostRef;
   public EvacuationEntityType type;
   public String name;
   public String primaryIconId;
   public String uuid;
   public boolean isHostConnected;
   public boolean isInMaintenanceMode;
   public DataMigrationDiskGroupData diskGroupData;
   public DiskData diskData;
   public EvacuationEntity[] children;
}
