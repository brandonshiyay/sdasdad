package com.vmware.vsphere.client.vsan.iscsi.models.target;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsphere.client.vsan.base.data.IscsiTarget;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@TsModel
public class TargetListData {
   public IscsiTarget[] targets;
   public boolean isTargetLocationSupported;
   public String preferredDomainName;
   public String secondaryDomainName;
   public List preferredDomainHostUids = new ArrayList();
   public List secondaryDomainHostUids = new ArrayList();
   public Map hostUidToHostIpMap = new HashMap();

   public String toString() {
      return String.format("TargetListData [targets=%s, isTargetLocationSupported=%s, preferredDomainName=%s, secondaryDomainName=%s,preferredDomainHostUids=%s, secondaryDomainHostUids=%s, hostUidToHostIpMap=%s]", this.targets, this.isTargetLocationSupported, this.preferredDomainName, this.secondaryDomainName, this.preferredDomainHostUids, this.secondaryDomainHostUids, this.hostUidToHostIpMap);
   }
}
