package com.vmware.vsphere.client.vsan.base.data;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.vsan.binding.vim.cluster.StorageComplianceResult;
import com.vmware.vim.vsan.binding.vim.cluster.VsanIscsiLUN;
import com.vmware.vim.vsan.binding.vim.cluster.VsanIscsiTarget;
import com.vmware.vim.vsan.binding.vim.cluster.VsanIscsiTargetAuthSpec;
import com.vmware.vsphere.client.vsan.iscsi.models.config.VsanIscsiAuthSpec;
import com.vmware.vsphere.client.vsan.iscsi.utils.VsanIscsiUtil;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.CollectionUtils;

@TsModel
public class IscsiTarget extends VsanObject {
   public String iqn;
   public String alias;
   public Integer lunCount;
   public String networkInterface;
   public Integer port;
   public String ioOwnerHost;
   public String authType;
   public AffinitySiteLocation affinityLocation;
   public String vmStoragePolicyUuid;
   public Date lastChecked;
   public List luns = new ArrayList();
   public VsanIscsiAuthSpec authSpec;

   public IscsiTarget() {
      this.objectType = VsanObjectType.iscsiTarget;
   }

   public IscsiTarget(VsanIscsiTarget iscsi, List iscsiLuns, Map storageProfiles, boolean isIscsiStretchedClusterSupported) {
      this.objectType = VsanObjectType.iscsiTarget;
      this.iqn = iscsi.iqn;
      this.alias = iscsi.alias;
      this.name = iscsi.alias;
      this.lunCount = iscsi.lunCount;
      this.networkInterface = iscsi.networkInterface;
      this.port = iscsi.port;
      this.ioOwnerHost = iscsi.ioOwnerHost;
      this.authType = iscsi.authSpec.authType;
      this.authSpec = this.getVsanIscsiAuthSpec(iscsi.authSpec);
      if (isIscsiStretchedClusterSupported) {
         this.affinityLocation = AffinitySiteLocation.parse(iscsi.affinityLocation);
      }

      if (iscsi.objectInformation != null) {
         this.vsanObjectUuid = iscsi.objectInformation.vsanObjectUuid;
         this.healthState = VsanObjectHealthState.fromServerLocalizedString(iscsi.objectInformation.vsanHealth);
         if (iscsi.objectInformation.spbmProfileUuid != null) {
            this.vmStoragePolicyUuid = iscsi.objectInformation.spbmProfileUuid;
            this.storagePolicy = storageProfiles.containsKey(this.vmStoragePolicyUuid) ? (String)storageProfiles.get(this.vmStoragePolicyUuid) : this.vmStoragePolicyUuid;
         }

         StorageComplianceResult storageStatus = iscsi.objectInformation.spbmComplianceResult;
         if (storageStatus != null) {
            this.complianceResult = VsanIscsiUtil.toComplianceResult(storageStatus);
            this.complianceStatus = VsanIscsiUtil.getComplianceStatus(this.complianceResult);
            this.lastChecked = storageStatus.checkTime.getTime();
         }
      }

      if (!CollectionUtils.isEmpty(iscsiLuns)) {
         Iterator var7 = iscsiLuns.iterator();

         while(var7.hasNext()) {
            VsanIscsiLUN lun = (VsanIscsiLUN)var7.next();
            this.luns.add(new IscsiLun(lun, storageProfiles, this.iqn));
         }

      }
   }

   private VsanIscsiAuthSpec getVsanIscsiAuthSpec(VsanIscsiTargetAuthSpec sourceAuthSpec) {
      if (sourceAuthSpec == null) {
         return null;
      } else {
         VsanIscsiAuthSpec authSpec = new VsanIscsiAuthSpec();
         authSpec.authType = sourceAuthSpec.authType;
         authSpec.initiatorSecret = sourceAuthSpec.userSecretAttachToInitiator;
         authSpec.initiatorUsername = sourceAuthSpec.userNameAttachToInitiator;
         authSpec.targetSecret = sourceAuthSpec.userSecretAttachToTarget;
         authSpec.targetUsername = sourceAuthSpec.userNameAttachToTarget;
         return authSpec;
      }
   }
}
