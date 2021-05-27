package com.vmware.vsphere.client.vsan.perf.model;

import com.vmware.proxygen.ts.TsModel;
import java.util.Calendar;

@TsModel
public class PerfDiagnosticQuerySpec {
   public Calendar startTime;
   public Calendar endTime;
   public PerfDiagnosticType queryType;
}
