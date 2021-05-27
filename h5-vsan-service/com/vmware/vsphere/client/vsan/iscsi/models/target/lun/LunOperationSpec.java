package com.vmware.vsphere.client.vsan.iscsi.models.target.lun;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vim.vm.DefinedProfileSpec;
import com.vmware.vim.vsan.binding.vim.cluster.VsanIscsiLUNSpec;

@TsModel
public class LunOperationSpec {
   public String targetAlias;
   public String lunAlias;
   public int lunId;
   public long lunSize;
   public int newLunId;
   public String status;
   public String policyId;

   public LunOperationSpec() {
   }

   public LunOperationSpec(String targetAlias, String lunAlias, int lunId, long lunSize, int newLunId, String status, String policyId) {
      this.targetAlias = targetAlias;
      this.lunAlias = lunAlias;
      this.lunId = lunId;
      this.lunSize = lunSize;
      this.newLunId = newLunId;
      this.status = status;
      this.policyId = policyId;
   }

   public VsanIscsiLUNSpec toVmodlVsanIscsiLUNSpec() {
      VsanIscsiLUNSpec lunSpec = new VsanIscsiLUNSpec();
      lunSpec.alias = this.lunAlias;
      lunSpec.lunId = this.lunId;
      lunSpec.lunSize = this.lunSize;
      lunSpec.newLunId = this.newLunId;
      lunSpec.status = this.status;
      lunSpec.storagePolicy = this.getDefinedProfileSpec();
      return lunSpec;
   }

   private DefinedProfileSpec getDefinedProfileSpec() {
      DefinedProfileSpec profile = new DefinedProfileSpec();
      profile.profileId = this.policyId;
      return profile;
   }
}
