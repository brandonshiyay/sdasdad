package com.vmware.vsphere.client.vsan.perf.model;

import com.vmware.proxygen.ts.TsModel;
import java.util.Map;

@TsModel
public class PerformanceExceptionsData {
   public Map performanceExceptionIdToException;
   public Map performanceEntityTypes;
   public Map performanceAggregatedEntityTypes;
}
