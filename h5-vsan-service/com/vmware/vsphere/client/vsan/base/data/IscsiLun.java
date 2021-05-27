package com.vmware.vsphere.client.vsan.base.data;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.vsan.binding.vim.cluster.StorageComplianceResult;
import com.vmware.vim.vsan.binding.vim.cluster.VsanIscsiLUN;
import com.vmware.vsphere.client.vsan.iscsi.utils.VsanIscsiUtil;
import com.vmware.vsphere.client.vsan.util.Utils;
import java.util.Date;
import java.util.Map;

@TsModel
public class IscsiLun extends VsanObject {
   public Integer lunId;
   public String alias;
   public String targetAlias;
   public String targetIqn;
   public long lunSize;
   public long actualSize;
   public IscsiLunStatus status;
   public String vmStoragePolicyUuid;
   public Date lastChecked;

   public IscsiLun() {
      this.objectType = VsanObjectType.iscsiLun;
   }

   public IscsiLun(VsanIscsiLUN lun, Map storageProfiles) {
      this.objectType = VsanObjectType.iscsiLun;
      this.lunId = lun.lunId;
      this.alias = lun.alias;
      this.targetAlias = lun.targetAlias;
      this.name = Utils.getLocalizedString("vsan.virtualObjects.iscsiLun", this.alias, this.lunId.toString()).trim();
      this.lunSize = lun.lunSize;
      this.actualSize = lun.actualSize;
      if (lun.status.equals("Online")) {
         this.status = IscsiLunStatus.Online;
      } else {
         this.status = IscsiLunStatus.Offline;
      }

      if (lun.objectInformation != null) {
         this.vsanObjectUuid = lun.objectInformation.vsanObjectUuid;
         this.healthState = VsanObjectHealthState.fromServerLocalizedString(lun.objectInformation.vsanHealth);
         if (lun.objectInformation.spbmProfileUuid != null) {
            this.vmStoragePolicyUuid = lun.objectInformation.spbmProfileUuid;
            this.storagePolicy = storageProfiles.containsKey(this.vmStoragePolicyUuid) ? (String)storageProfiles.get(this.vmStoragePolicyUuid) : this.vmStoragePolicyUuid;
         }

         StorageComplianceResult storageStatus = lun.objectInformation.spbmComplianceResult;
         if (storageStatus != null) {
            this.complianceResult = VsanIscsiUtil.toComplianceResult(storageStatus);
            this.complianceStatus = VsanIscsiUtil.getComplianceStatus(this.complianceResult);
            this.lastChecked = storageStatus.checkTime.getTime();
         }
      }

   }

   public IscsiLun(VsanIscsiLUN lun, Map storageProfiles, String targetIqnString) {
      this(lun, storageProfiles);
      this.targetIqn = targetIqnString;
   }
}
