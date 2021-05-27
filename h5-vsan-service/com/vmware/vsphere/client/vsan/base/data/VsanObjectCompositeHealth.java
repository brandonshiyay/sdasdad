package com.vmware.vsphere.client.vsan.base.data;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.vsan.binding.vim.host.VsanObjectHealthStateV2;
import com.vmware.vsphere.client.vsan.util.EnumUtils;

@TsModel
public class VsanObjectCompositeHealth {
   public ObjectHealthComplianceState complianceState;
   public ObjectHealthIncomplianceReason incomplianceReason;
   public ObjectHealthRebuildState rebuildState;
   public ObjectHealthPolicyState policyState;

   public VsanObjectCompositeHealth() {
   }

   public VsanObjectCompositeHealth(VsanObjectHealthStateV2 healthStateV2) {
      if (healthStateV2.getComplianceLevel() != null) {
         this.complianceState = (ObjectHealthComplianceState)EnumUtils.fromStringIgnoreCase(ObjectHealthComplianceState.class, healthStateV2.getComplianceLevel(), ObjectHealthComplianceState.COMPLIANCE_UNKNOWN);
      }

      if (healthStateV2.getIncomplianceReason() != null) {
         this.incomplianceReason = (ObjectHealthIncomplianceReason)EnumUtils.fromStringIgnoreCase(ObjectHealthIncomplianceReason.class, healthStateV2.getIncomplianceReason(), ObjectHealthIncomplianceReason.INCOMPLIANCE_UNKNOWN);
      }

      if (healthStateV2.getRebuildState() != null) {
         this.rebuildState = (ObjectHealthRebuildState)EnumUtils.fromStringIgnoreCase(ObjectHealthRebuildState.class, healthStateV2.getRebuildState(), ObjectHealthRebuildState.REBUILD_UNKNOWN);
      }

      if (healthStateV2.getPolicyApplicationState() != null) {
         this.policyState = (ObjectHealthPolicyState)EnumUtils.fromStringIgnoreCase(ObjectHealthPolicyState.class, healthStateV2.getPolicyApplicationState(), ObjectHealthPolicyState.POLICY_UNKNOWN);
      }

   }

   public String toString() {
      return "VsanObjectCompositeHealth(complianceState=" + this.complianceState + ", incomplianceReason=" + this.incomplianceReason + ", rebuildState=" + this.rebuildState + ", policyState=" + this.policyState + ")";
   }
}
