package com.vmware.vsan.client.services.evacuationstatus.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsan.client.services.config.SpaceEfficiencyConfig;

@TsModel
public class EvacuationStatusData {
   public boolean isHostResourcePrecheckSupported;
   public boolean isDiskResourcePrecheckSupported;
   public SpaceEfficiencyConfig spaceEfficiencyConfig;
   public EvacuationEntity[] evacuationEntities;
   public String errorMessage;
}
