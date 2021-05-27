package com.vmware.vsan.client.services.virtualobjects.data;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vsan.client.services.capacity.model.DatastoreType;
import com.vmware.vsphere.client.vsan.base.data.VsanComponentState;
import java.util.List;

@TsModel
public class VirtualObjectPlacementModel {
   public String nodeUuid;
   public String label;
   public String iconId;
   public VsanComponentState state;
   public VirtualObjectPlacementModel host;
   public ManagedObjectReference navigationTarget;
   public String faultDomain;
   public VirtualObjectPlacementModel cacheDisk;
   public VirtualObjectPlacementModel capacityDisk;
   public List children;
   public DatastoreType datastoreType;
}
