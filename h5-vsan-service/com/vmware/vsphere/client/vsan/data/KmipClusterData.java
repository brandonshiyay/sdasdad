package com.vmware.vsphere.client.vsan.data;

import com.vmware.proxygen.ts.TsModel;
import java.util.ArrayList;
import java.util.List;

@TsModel
public class KmipClusterData {
   public List availableKmipClusters = new ArrayList();
   public String defaultKmipCluster;
   public boolean hasManageKeyServersPermissions;
}
