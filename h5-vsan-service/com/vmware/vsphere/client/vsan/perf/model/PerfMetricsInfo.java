package com.vmware.vsphere.client.vsan.perf.model;

import com.vmware.vim.vsan.binding.vim.cluster.VsanPerfEntityMetricCSV;
import com.vmware.vim.vsan.binding.vim.cluster.VsanPerfMetricSeriesCSV;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.ArrayUtils;

public class PerfMetricsInfo {
   public Map entityRefIdToIntervalMap;
   public Map entityRefIdToMetricIdMap;

   public static PerfMetricsInfo extractMetricsInfo(VsanPerfEntityMetricCSV[] metrics) {
      if (ArrayUtils.isEmpty(metrics)) {
         return new PerfMetricsInfo();
      } else {
         Map intervals = new HashMap();
         Map metricIds = new HashMap();

         for(int metricIndex = 0; metricIndex < metrics.length; ++metricIndex) {
            String entityRefId = metrics[metricIndex].entityRefId;
            if (metricIds.get(entityRefId) == null && ArrayUtils.isNotEmpty(metrics[metricIndex].value)) {
               List ids = new ArrayList();
               VsanPerfMetricSeriesCSV[] var6 = metrics[metricIndex].value;
               int var7 = var6.length;

               for(int var8 = 0; var8 < var7; ++var8) {
                  VsanPerfMetricSeriesCSV value = var6[var8];
                  ids.add(value.metricId.label);
               }

               metricIds.put(entityRefId, ids);
            }

            if (intervals.get(entityRefId) == null) {
               int interval = PerfEntityStateData.getMetricsCollectInterval(metrics[metricIndex]);
               if (interval != 0) {
                  intervals.put(entityRefId, interval);
               }
            }
         }

         PerfMetricsInfo metricsInfo = new PerfMetricsInfo();
         metricsInfo.entityRefIdToIntervalMap = intervals;
         metricsInfo.entityRefIdToMetricIdMap = metricIds;
         return metricsInfo;
      }
   }
}
