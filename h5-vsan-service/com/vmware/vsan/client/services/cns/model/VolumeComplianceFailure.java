package com.vmware.vsan.client.services.cns.model;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class VolumeComplianceFailure {
   public String propertyName;
   public String currentValue;
   public String expectedValue;
}
