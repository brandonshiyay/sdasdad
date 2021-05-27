package com.vmware.vsan.client.services.evacuationstatus.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vsan.client.services.diskGroups.data.DecommissionMode;

@TsModel
public class EvacuationTaskData {
   public ManagedObjectReference taskMoRef;
   public String name;
   public String uuid;
   public String hostName;
   public DecommissionMode decommissionMode;
   public PrecheckTaskType taskType;
}
