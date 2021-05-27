package com.vmware.vsan.client.services.fileanalytics.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.vsan.binding.vim.vsan.FileAnalyticsFileShareDashboardLabels;
import com.vmware.vim.vsan.binding.vim.vsan.VsanFileAnalyticsReport;
import com.vmware.vim.vsan.binding.vim.vsan.VsanFileAnalyticsReportData;
import com.vmware.vsan.client.services.fileanalytics.FileAnalyticsUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@TsModel
public class FileAnalyticsShareCapacityTrending {
   public Date date;
   public long capacityAdded;
   public long capacityRemoved;
   public long sharesTotalCapacity;
   private static final Log logger = LogFactory.getLog(FileAnalyticsShareCapacityTrending.class);

   public FileAnalyticsShareCapacityTrending() {
   }

   public FileAnalyticsShareCapacityTrending(Date date, long capacityAdded, long capacityRemoved, long totalCapacity) {
      this.date = date;
      this.capacityAdded = capacityAdded;
      this.capacityRemoved = capacityRemoved;
      this.sharesTotalCapacity = totalCapacity;
   }

   public static List fromVmodl(VsanFileAnalyticsReport report) {
      if (report != null && !ArrayUtils.isEmpty(report.reportData) && !ArrayUtils.isEmpty(report.categories)) {
         List reportDates = (List)Arrays.stream(report.categories).map((category) -> {
            return FileAnalyticsUtil.parseReportDate(category);
         }).collect(Collectors.toList());
         List sharesTotalUsage = new ArrayList();
         List sharesAddedUsage = new ArrayList();
         List sharesDeletedUsage = new ArrayList();
         VsanFileAnalyticsReportData[] var5 = report.reportData;
         int i = var5.length;

         for(int var7 = 0; var7 < i; ++var7) {
            VsanFileAnalyticsReportData reportData = var5[var7];
            if (reportData != null && reportData.values != null) {
               if (EnumUtils.isValidEnum(FileAnalyticsFileShareDashboardLabels.class, reportData.label)) {
                  switch(FileAnalyticsFileShareDashboardLabels.valueOf(reportData.label)) {
                  case total_usage:
                     sharesTotalUsage.addAll(FileAnalyticsUtil.parseStringsToLongs(reportData.values));
                     break;
                  case files_added:
                     sharesAddedUsage.addAll(FileAnalyticsUtil.parseStringsToLongs(reportData.values));
                     break;
                  case files_deleted:
                     sharesDeletedUsage.addAll(FileAnalyticsUtil.parseStringsToLongs(reportData.values));
                     break;
                  default:
                     logger.warn("Unknown value '" + reportData.label + "' of FileAnalyticsFileShareDashboardLabels");
                  }
               }
            } else {
               logger.error("Invalid report data values received for file shares capacity trending: " + reportData);
            }
         }

         List result = new ArrayList();

         for(i = 0; i < reportDates.size(); ++i) {
            result.add(new FileAnalyticsShareCapacityTrending((Date)reportDates.get(i), (Long)sharesAddedUsage.get(i), (Long)sharesDeletedUsage.get(i), (Long)sharesTotalUsage.get(i)));
         }

         return result;
      } else {
         logger.error("Invalid report data received for file shares capacity trending.");
         return Collections.emptyList();
      }
   }

   public String toString() {
      return "FileAnalyticsShareCapacityTrending(date=" + this.date + ", capacityAdded=" + this.capacityAdded + ", capacityRemoved=" + this.capacityRemoved + ", sharesTotalCapacity=" + this.sharesTotalCapacity + ")";
   }
}
