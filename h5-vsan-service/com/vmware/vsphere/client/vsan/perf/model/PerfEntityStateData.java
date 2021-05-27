package com.vmware.vsphere.client.vsan.perf.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.vsan.binding.vim.cluster.VsanPerfEntityMetricCSV;
import com.vmware.vim.vsan.binding.vim.cluster.VsanPerfMetricSeriesCSV;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

@TsModel
public class PerfEntityStateData {
   private static final String NONE = "None";
   public List timeStamps;
   public List metricsSeries;
   public int metricsCollectInterval;
   public String entityRefId;

   public static PerfEntityStateData parsePerfEntityMetricCSV(VsanPerfEntityMetricCSV metric, long rangeStart, long rangeEnd) {
      PerfEntityStateData stateData = new PerfEntityStateData();
      stateData.entityRefId = metric.entityRefId;
      stateData.timeStamps = new ArrayList();
      stateData.metricsSeries = new ArrayList();
      if (!ArrayUtils.isEmpty(metric.value) && !StringUtils.isEmpty(metric.sampleInfo)) {
         List rawTimestamps = Arrays.asList(StringUtils.split(metric.sampleInfo, ","));
         int metricsCollectInterval = getMetricsCollectInterval(metric);
         stateData.metricsCollectInterval = metricsCollectInterval;
         boolean canFormatData = metricsCollectInterval > 0;
         if (canFormatData) {
            stateData.timeStamps = generateTimestamps(rangeStart, rangeEnd, metricsCollectInterval);
         } else {
            stateData.timeStamps = rawTimestamps;
         }

         VsanPerfMetricSeriesCSV[] var9 = metric.value;
         int var10 = var9.length;

         for(int var11 = 0; var11 < var10; ++var11) {
            VsanPerfMetricSeriesCSV metricSeries = var9[var11];
            PerfGraphMetricsData metricsData = new PerfGraphMetricsData();
            metricsData.key = metricSeries.metricId.label;
            if (metricSeries.threshold != null) {
               metricsData.threshold = new PerfGraphThreshold();
               metricsData.threshold.direction = PerfGraphThresholdDirection.fromVmodl(metricSeries.threshold.direction);
               metricsData.threshold.yellow = StringUtils.isBlank(metricSeries.threshold.yellow) ? null : Integer.parseInt(metricSeries.threshold.yellow);
               metricsData.threshold.red = StringUtils.isBlank(metricSeries.threshold.red) ? null : Integer.parseInt(metricSeries.threshold.red);
            }

            String[] rawValues = metricSeries.values.split(",");
            List newValues = new ArrayList();
            int index;
            String value;
            if (!canFormatData) {
               String[] var20 = rawValues;
               index = rawValues.length;

               for(int var22 = 0; var22 < index; ++var22) {
                  value = var20[var22];
                  newValues.add(formatValue(value));
               }
            } else {
               Map timestampToValueMapOfRawData = new HashMap();

               for(index = 0; index < rawValues.length; ++index) {
                  timestampToValueMapOfRawData.put(rawTimestamps.get(index), rawValues[index]);
               }

               Iterator var21 = stateData.timeStamps.iterator();

               while(var21.hasNext()) {
                  String timestamp = (String)var21.next();
                  value = (String)timestampToValueMapOfRawData.get(timestamp);
                  newValues.add(formatValue(value));
               }
            }

            metricsData.values = newValues;
            stateData.metricsSeries.add(metricsData);
         }

         return stateData;
      } else {
         return stateData;
      }
   }

   protected static int getMetricsCollectInterval(VsanPerfEntityMetricCSV metric) {
      int metricsCollectInterval = 0;
      if (ArrayUtils.isEmpty(metric.value)) {
         return metricsCollectInterval;
      } else {
         VsanPerfMetricSeriesCSV[] var2 = metric.value;
         int var3 = var2.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            VsanPerfMetricSeriesCSV value = var2[var4];
            if (value.metricId != null && value.metricId.metricsCollectInterval != null && value.metricId.metricsCollectInterval > 0) {
               return value.metricId.metricsCollectInterval;
            }
         }

         return metricsCollectInterval;
      }
   }

   private static Double formatValue(String valueStr) {
      return !"None".equalsIgnoreCase(valueStr) && !StringUtils.isBlank(valueStr) ? Double.parseDouble(valueStr) : null;
   }

   public static PerfEntityStateData parsePerfEntityMetricCSV(VsanPerfEntityMetricCSV metric, long rangeStart, long rangeEnd, int interval, List metricIds) {
      PerfEntityStateData stateData = new PerfEntityStateData();
      stateData.metricsCollectInterval = interval;
      stateData.entityRefId = metric.entityRefId;
      stateData.timeStamps = generateTimestamps(rangeStart, rangeEnd, interval);
      stateData.metricsSeries = new ArrayList();
      Iterator var8 = metricIds.iterator();

      while(var8.hasNext()) {
         String id = (String)var8.next();
         PerfGraphMetricsData metricsData = new PerfGraphMetricsData();
         metricsData.key = id;

         for(int index = 0; index < stateData.timeStamps.size(); ++index) {
            if (metricsData.values == null) {
               metricsData.values = new ArrayList();
            }

            metricsData.values.add((Object)null);
         }

         stateData.metricsSeries.add(metricsData);
      }

      return stateData;
   }

   private static List generateTimestamps(long rangeStart, long rangeEnd, int interval) {
      DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
      List timestamps = new ArrayList();

      for(rangeStart %= (long)(interval * 1000); rangeStart <= rangeEnd; rangeStart += (long)(interval * 1000)) {
         timestamps.add(dateFormat.format(new Date(rangeStart)));
      }

      return timestamps;
   }
}
