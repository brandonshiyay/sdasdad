package com.vmware.vsan.client.services.hci.model;

import com.vmware.proxygen.ts.TsModel;
import java.util.List;

@TsModel
public class EvcModeConfigData {
   public boolean enabled;
   public boolean unsupportedEvcStatus;
   public EvcModeData selectedEvcMode;
   public List supportedIntelEvcMode;
   public List supportedAmdEvcMode;
}
