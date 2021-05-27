package com.vmware.vsphere.client.vsan.stretched;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vsan.client.services.stretchedcluster.model.SharedWitnessValidationData;
import com.vmware.vsphere.client.vsan.data.VsanSemiAutoClaimDisksData;

@TsModel
public class WitnessHostValidationResult {
   public ManagedObjectReference witnessHostRef;
   public SharedWitnessValidationData witnessValidationResult;
   public VsanSemiAutoClaimDisksData hostDisksData;
   public boolean isHostInTheSameCluster;
   public boolean isHostInVsanEnabledCluster;
   public boolean isWitnessHost;
   public boolean hasVsanEnabledNic;
   public boolean isHostDisconnected;
   public boolean isPoweredOn;
   public boolean isHostInMaintenanceMode;
   public boolean isStretchedClusterSupported;
   public boolean isDitSharedWitnessInteroperabilitySupported;
   public boolean isEncrypted;
   public boolean isWitnessvLCM;
   public boolean vLCMToBeDisabled;
}
