package com.vmware.vsan.client.services.fileanalytics.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.vsan.binding.vim.vsan.FileAnalyticsFileShareDashboardLabels;
import com.vmware.vim.vsan.binding.vim.vsan.VsanFileAnalyticsReport;
import com.vmware.vim.vsan.binding.vim.vsan.VsanFileAnalyticsReportData;
import com.vmware.vsan.client.services.fileanalytics.FileAnalyticsUtil;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@TsModel
public class FileAnalyticsFastestGrowingShareData {
   public String shareName;
   public List growthData;
   private static final Log logger = LogFactory.getLog(FileAnalyticsFastestGrowingShareData.class);

   public FileAnalyticsFastestGrowingShareData() {
   }

   public FileAnalyticsFastestGrowingShareData(String shareName, List data) {
      this.shareName = shareName;
      this.growthData = data;
   }

   public static FileAnalyticsFastestGrowingShareData fromVmodl(VsanFileAnalyticsReport report) {
      if (report != null && report.reportGroupedBy != null && !ArrayUtils.isEmpty(report.reportData) && !ArrayUtils.isEmpty(report.categories)) {
         String shareName = report.reportGroupedBy;
         List shareGrowthData = getShareGrowthData(report);
         return new FileAnalyticsFastestGrowingShareData(shareName, shareGrowthData);
      } else {
         logger.error("Invalid report data received for fastest growing file shares.");
         return null;
      }
   }

   private static List getShareGrowthData(VsanFileAnalyticsReport report) {
      List shareGrowthData = new ArrayList();
      String[] var2 = report.categories;
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         String category = var2[var4];
         Date shareReportDate = FileAnalyticsUtil.parseReportDate(category);
         long fileShareUsage = 0L;
         long fileShareGrowth = 0L;
         VsanFileAnalyticsReportData[] var11 = report.reportData;
         int var12 = var11.length;

         for(int var13 = 0; var13 < var12; ++var13) {
            VsanFileAnalyticsReportData reportData = var11[var13];
            if (!ArrayUtils.isEmpty(reportData.values) && reportData.label != null) {
               if (reportData.values.length > 1) {
                  logger.warn("Additional report data received for share: " + report.reportGroupedBy);
               }

               switch(FileAnalyticsFileShareDashboardLabels.valueOf(reportData.label)) {
               case usage:
                  fileShareUsage = (Long)FileAnalyticsUtil.parseStringsToLongs(reportData.values).get(0);
                  break;
               case growth:
                  fileShareGrowth = (Long)FileAnalyticsUtil.parseStringsToLongs(reportData.values).get(0);
                  break;
               default:
                  logger.warn("Unexpected report label received: " + reportData.label);
               }
            } else {
               logger.error("Invalid report data received for share: " + report.reportGroupedBy);
            }
         }

         shareGrowthData.add(new FileAnalyticsFileShareGrowthData(shareReportDate, fileShareUsage, fileShareGrowth, FileAnalyticsUtil.getGrowthPercentage(fileShareUsage, fileShareGrowth)));
      }

      return shareGrowthData;
   }

   public String toString() {
      return "FileAnalyticsFastestGrowingShareData(shareName=" + this.shareName + ", growthData=" + this.growthData + ")";
   }
}
