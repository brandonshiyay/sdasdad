package com.vmware.vsan.client.services.hci.model;

import com.vmware.proxygen.ts.TsModel;
import java.util.Map;

@TsModel
public class BasicClusterConfigData {
   public int hosts;
   public int notConfiguredHosts;
   public HciWorkflowState hciWorkflowState;
   public Map dvsDataByService;
   public boolean isComputeOnlyCluster;
   public boolean haEnabled;
   public boolean drsEnabled;
   public boolean vsanEnabled;
   public boolean pmanEnabled;
}
