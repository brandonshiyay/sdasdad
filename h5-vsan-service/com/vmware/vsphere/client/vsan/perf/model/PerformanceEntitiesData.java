package com.vmware.vsphere.client.vsan.perf.model;

import com.vmware.proxygen.ts.TsModel;
import java.util.Map;

@TsModel
public class PerformanceEntitiesData {
   public Map entityRefIdToEntityRefData;

   public PerformanceEntitiesData() {
   }

   public PerformanceEntitiesData(Map entityRefIdToEntityRefData) {
      this.entityRefIdToEntityRefData = entityRefIdToEntityRefData;
   }
}
