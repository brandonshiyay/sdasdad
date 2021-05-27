package com.vmware.vsan.client.services.config.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsan.client.services.config.VsanServiceData;

@TsModel
public class VsanServicesStatus {
   public VsanServiceData dataAtRestEncryptionStatus;
   public VsanServiceData dataInTransitEncryptionStatus;
   public VsanServiceData spaceEfficiencyStatus;
   public VsanServiceData supportInsightStatus;
   public VsanServiceData advancedOptionsStatus;
   public VsanServiceData performanceServiceStatus;
   public VsanServiceData iscsiTargetConfigStatus;
}
