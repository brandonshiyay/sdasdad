package com.vmware.vsan.client.services.csd.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsphere.client.vsan.health.VsanHealthStatus;
import java.util.List;

@TsModel
public class MountPrecheckTest {
   public VsanHealthStatus status;
   public String description;
   public List reasons;

   public String toString() {
      return "MountPrecheckTest{status=" + this.status + ", description=" + this.description + ", reasons=" + this.reasons + '}';
   }
}
