package com.vmware.vsan.client.services.hci.model;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class HciPermissionData {
   public boolean hasEditClusterBasics;
   public boolean hasAddHosts;
   public boolean hasConfigureCluster;
   public boolean hasEditCluster;
   public boolean hasRenameCluster;
   public boolean hasAddStandaloneHost;
   public boolean hasMoveHost;
   public boolean hasDvsCreate;
   public boolean hasDvsModify;
   public boolean hasCreatePortgroup;
   public boolean hasHostNetwrokConfig;
   public boolean hasNetworkAssign;
}
