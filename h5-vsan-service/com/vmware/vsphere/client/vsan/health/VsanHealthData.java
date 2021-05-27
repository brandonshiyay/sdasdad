package com.vmware.vsphere.client.vsan.health;

import com.vmware.proxygen.ts.TsModel;
import java.util.Calendar;
import java.util.List;

@TsModel
public class VsanHealthData {
   public VsanHealthStatus status;
   public String description;
   public String helpId;
   public List testsData;
   public Calendar timestamp;
}
