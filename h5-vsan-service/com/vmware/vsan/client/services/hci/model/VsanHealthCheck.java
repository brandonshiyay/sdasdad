package com.vmware.vsan.client.services.hci.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsphere.client.vsan.health.VsanHealthStatus;

@TsModel
public class VsanHealthCheck {
   public String perspective;
   public String healthGroup;
   public String healthTestId;
   public String healthTest;
   public String healthCheckLabel;
   public VsanHealthStatus status;

   public VsanHealthCheck() {
   }

   public VsanHealthCheck(String perspective, String healthGroup, String healthTestId, String healthTest, String healthCheckLabel, String healthStatus) {
      this.perspective = perspective;
      this.healthGroup = healthGroup;
      this.healthTestId = healthTestId;
      this.healthTest = healthTest;
      this.healthCheckLabel = healthCheckLabel;
      this.status = VsanHealthStatus.valueOf(healthStatus);
   }
}
