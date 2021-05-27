package com.vmware.vsphere.client.vsan.base.data;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.pbm.compliance.ComplianceResult;
import com.vmware.vsan.client.services.virtualobjects.data.VsanObjectHealthData;
import java.util.List;
import java.util.Map;

@TsModel
public class VsanObject {
   public String vsanObjectUuid;
   public String name;
   public VsanObjectType objectType;
   public String storagePolicy;
   public VsanComplianceStatus complianceStatus;
   public ComplianceResult complianceResult;
   public VsanObjectHealthState healthState;
   public VsanRootConfig rootConfig;

   public VsanObject() {
   }

   public VsanObject(String objectUuid) {
      this.vsanObjectUuid = objectUuid;
   }

   public VsanObject(String objectUuid, List components) {
      this.vsanObjectUuid = objectUuid;
      this.rootConfig = new VsanRootConfig();
      this.rootConfig.children = components;
   }

   public void updateHealthData(Map vsanHealthData) {
      if (vsanHealthData.containsKey(this.vsanObjectUuid)) {
         VsanObjectHealthData vsanHealthInfo = (VsanObjectHealthData)vsanHealthData.get(this.vsanObjectUuid);
         this.healthState = VsanObjectHealthState.fromString(vsanHealthInfo.vsanHealthState);
         this.storagePolicy = vsanHealthInfo.policyName;
      }
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         VsanObject that = (VsanObject)o;
         return this.vsanObjectUuid.equals(that.vsanObjectUuid);
      } else {
         return false;
      }
   }

   public int hashCode() {
      return this.vsanObjectUuid.hashCode();
   }
}
