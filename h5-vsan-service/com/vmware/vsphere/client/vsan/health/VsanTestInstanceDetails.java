package com.vmware.vsphere.client.vsan.health;

import com.vmware.proxygen.ts.TsModel;
import java.util.Calendar;
import java.util.List;

@TsModel
public class VsanTestInstanceDetails {
   public Calendar timestamp;
   public VsanHealthStatus status;
   public List details;
}
