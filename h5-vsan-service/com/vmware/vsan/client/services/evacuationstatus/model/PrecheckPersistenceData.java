package com.vmware.vsan.client.services.evacuationstatus.model;

import com.vmware.proxygen.ts.TsModel;
import java.util.List;

@TsModel
public class PrecheckPersistenceData {
   public long dataToRebuild;
   public List persistentInstances;
}
