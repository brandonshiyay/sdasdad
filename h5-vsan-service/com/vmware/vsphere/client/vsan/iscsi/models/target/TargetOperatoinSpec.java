package com.vmware.vsphere.client.vsan.iscsi.models.target;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vim.vm.DefinedProfileSpec;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanIscsiTargetSpec;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsphere.client.vsan.base.data.AffinitySiteLocation;
import com.vmware.vsphere.client.vsan.iscsi.models.config.VsanIscsiAuthSpec;

@TsModel
public class TargetOperatoinSpec {
   public String alias;
   public VsanIscsiAuthSpec authSpec;
   public String iqn;
   public String networkInterface;
   public String newAlias;
   public int port;
   public String policyId;
   public AffinitySiteLocation affinityLocation;

   public TargetOperatoinSpec() {
   }

   public TargetOperatoinSpec(String alias, VsanIscsiAuthSpec authSpec, String iqn, String networkInterface, String newAlias, int port, String policyId) {
      this.alias = alias;
      this.authSpec = authSpec;
      this.iqn = iqn;
      this.networkInterface = networkInterface;
      this.newAlias = newAlias;
      this.port = port;
      this.policyId = policyId;
   }

   public VsanIscsiTargetSpec toVmodlVsanIscsiTargetSpec(ManagedObjectReference clusterRef) {
      VsanIscsiTargetSpec targetSpec = new VsanIscsiTargetSpec();
      targetSpec.alias = this.alias;
      targetSpec.authSpec = VsanIscsiAuthSpec.getVsanIscsiTargetAuthSpec(this.authSpec);
      targetSpec.iqn = this.iqn;
      targetSpec.networkInterface = this.networkInterface;
      targetSpec.newAlias = this.newAlias;
      targetSpec.port = this.port;
      targetSpec.storagePolicy = this.getDefinedProfileSpec();
      if (VsanCapabilityUtils.isIscsiStretchedClusterSupportedOnCluster(clusterRef)) {
         targetSpec.affinityLocation = this.affinityLocation == null ? null : this.affinityLocation.toVmodl();
      }

      return targetSpec;
   }

   private DefinedProfileSpec getDefinedProfileSpec() {
      DefinedProfileSpec profile = new DefinedProfileSpec();
      profile.setProfileId(this.policyId);
      return profile;
   }
}
