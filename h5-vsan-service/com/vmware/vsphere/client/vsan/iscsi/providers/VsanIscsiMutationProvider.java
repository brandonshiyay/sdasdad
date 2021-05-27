package com.vmware.vsphere.client.vsan.iscsi.providers;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vim.vm.DefinedProfileSpec;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanIscsiTargetAuthSpec;
import com.vmware.vim.vsan.binding.vim.cluster.VsanIscsiTargetServiceDefaultConfigSpec;
import com.vmware.vim.vsan.binding.vim.cluster.VsanIscsiTargetServiceSpec;
import com.vmware.vim.vsan.binding.vim.vsan.ReconfigSpec;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsphere.client.vsan.impl.ConfigureVsanClusterMutationProvider;
import com.vmware.vsphere.client.vsan.iscsi.models.config.VsanIscsiConfigEditSpec;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;

public class VsanIscsiMutationProvider {
   @Autowired
   private ConfigureVsanClusterMutationProvider configureClusterService;

   @TsService
   public ManagedObjectReference editIscsiConfig(ManagedObjectReference clusterRef, VsanIscsiConfigEditSpec spec) {
      if (!VsanCapabilityUtils.isIscsiTargetsSupportedOnVc(clusterRef)) {
         return null;
      } else {
         Validate.notNull(spec);
         VsanIscsiTargetServiceSpec serviceSpec = new VsanIscsiTargetServiceSpec();
         serviceSpec.setEnabled(spec.enableIscsiTargetService);
         if (spec.enableIscsiTargetService) {
            VsanIscsiTargetServiceDefaultConfigSpec serviceConfig = new VsanIscsiTargetServiceDefaultConfigSpec();
            serviceConfig.setNetworkInterface(spec.network);
            serviceConfig.setPort(spec.port);
            serviceConfig.setIscsiTargetAuthSpec(this.createIscsiAuthSpec(spec));
            serviceSpec.setDefaultConfig(serviceConfig);
            serviceSpec.setHomeObjectStoragePolicy(this.createPofileSpec(spec));
         }

         ReconfigSpec reconfigSpec = new ReconfigSpec();
         reconfigSpec.iscsiSpec = serviceSpec;
         return this.configureClusterService.startReconfigureTask(clusterRef, reconfigSpec);
      }
   }

   private DefinedProfileSpec createPofileSpec(VsanIscsiConfigEditSpec spec) {
      DefinedProfileSpec profileSpec = null;
      if (spec.policy != null) {
         profileSpec = new DefinedProfileSpec();
         profileSpec.setProfileId(spec.policy.id);
      }

      return profileSpec;
   }

   private VsanIscsiTargetAuthSpec createIscsiAuthSpec(VsanIscsiConfigEditSpec spec) {
      if (spec.authSpec == null) {
         return null;
      } else {
         VsanIscsiTargetAuthSpec authSpec = new VsanIscsiTargetAuthSpec();
         authSpec.setAuthType(spec.authSpec.authType);
         authSpec.setUserNameAttachToInitiator(spec.authSpec.initiatorUsername);
         authSpec.setUserNameAttachToTarget(spec.authSpec.targetUsername);
         authSpec.setUserSecretAttachToInitiator(spec.authSpec.initiatorSecret);
         authSpec.setUserSecretAttachToTarget(spec.authSpec.targetSecret);
         return authSpec;
      }
   }
}
