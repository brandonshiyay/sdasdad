package com.vmware.vsphere.client.vsan.iscsi.models.target;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsphere.client.vsan.iscsi.models.config.VsanIscsiTargetConfig;
import java.util.List;

@TsModel
public class TargetModificationData {
   public String[] networks;
   public List domains;
   public List policies;
   public VsanIscsiTargetConfig iscsiTargetConfig;
   public boolean isTargetLocationSupported;

   public String toString() {
      return String.format("TargetModificationData [networks=%s, domains=%s, policies=%s, iscsiTargetConfig=%s, isTargetLocationSupported=%s]", this.networks, this.domains, this.policies, this.iscsiTargetConfig, this.isTargetLocationSupported);
   }
}
