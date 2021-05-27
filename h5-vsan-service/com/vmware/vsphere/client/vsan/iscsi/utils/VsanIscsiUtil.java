package com.vmware.vsphere.client.vsan.iscsi.utils;

import com.vmware.vim.binding.pbm.capability.CapabilityInstance;
import com.vmware.vim.binding.pbm.capability.ConstraintInstance;
import com.vmware.vim.binding.pbm.capability.Operator;
import com.vmware.vim.binding.pbm.capability.PropertyInstance;
import com.vmware.vim.binding.pbm.capability.CapabilityMetadata.UniqueId;
import com.vmware.vim.binding.pbm.compliance.ComplianceResult;
import com.vmware.vim.binding.pbm.compliance.PolicyStatus;
import com.vmware.vim.binding.pbm.compliance.ComplianceResult.ComplianceStatus;
import com.vmware.vim.binding.pbm.profile.ProfileId;
import com.vmware.vim.vsan.binding.vim.cluster.StorageComplianceResult;
import com.vmware.vim.vsan.binding.vim.cluster.StoragePolicyStatus;
import com.vmware.vsphere.client.vsan.base.data.VsanComplianceStatus;
import java.util.ArrayList;
import java.util.List;

public class VsanIscsiUtil {
   public static final String iscsiEnableInProgressErrMsg = "vSAN iSCSI Target Service is not enabled or the enable task is in progress.";

   public static VsanComplianceStatus getComplianceStatus(ComplianceResult complianceResult) {
      if (complianceResult == null) {
         return VsanComplianceStatus.UNKNOWN;
      } else if (complianceResult.mismatch) {
         return VsanComplianceStatus.OUT_OF_DATE;
      } else if (complianceResult.complianceStatus.equals(ComplianceStatus.compliant.name())) {
         return VsanComplianceStatus.COMPLIANT;
      } else if (complianceResult.complianceStatus.equals(ComplianceStatus.nonCompliant.name())) {
         return VsanComplianceStatus.NOT_COMPLIANT;
      } else {
         return complianceResult.complianceStatus.equals(ComplianceStatus.notApplicable.name()) ? VsanComplianceStatus.NOT_APPLICABLE : VsanComplianceStatus.UNKNOWN;
      }
   }

   public static ComplianceResult toComplianceResult(StorageComplianceResult storageComplianceResult) {
      if (storageComplianceResult == null) {
         return null;
      } else {
         ComplianceResult result = new ComplianceResult();
         result.checkTime = storageComplianceResult.checkTime;
         result.mismatch = storageComplianceResult.mismatch;
         List violatedPolicies = new ArrayList();
         if (storageComplianceResult.violatedPolicies != null) {
            StoragePolicyStatus[] var3 = storageComplianceResult.violatedPolicies;
            int var4 = var3.length;

            for(int var5 = 0; var5 < var4; ++var5) {
               StoragePolicyStatus status = var3[var5];
               String id = status.id == null ? "" : status.id;
               CapabilityInstance expInstance = new CapabilityInstance();
               expInstance.id = new UniqueId("VSAN", id);
               expInstance.constraint = new ConstraintInstance[]{new ConstraintInstance(new PropertyInstance[]{new PropertyInstance(status.id, Operator.NOT.toString(), status.expectedValue)})};
               CapabilityInstance currInstance = new CapabilityInstance();
               currInstance.id = new UniqueId("VSAN", id);
               currInstance.constraint = new ConstraintInstance[]{new ConstraintInstance(new PropertyInstance[]{new PropertyInstance(status.id, Operator.NOT.toString(), status.currentValue)})};
               PolicyStatus newStatus = new PolicyStatus(expInstance, currInstance);
               violatedPolicies.add(newStatus);
            }
         }

         result.violatedPolicies = (PolicyStatus[])violatedPolicies.toArray(new PolicyStatus[violatedPolicies.size()]);
         result.complianceStatus = storageComplianceResult.complianceStatus;
         result.profile = new ProfileId(storageComplianceResult.profile);
         return result;
      }
   }
}
