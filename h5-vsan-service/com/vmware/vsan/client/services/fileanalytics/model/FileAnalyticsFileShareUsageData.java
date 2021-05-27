package com.vmware.vsan.client.services.fileanalytics.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.vsan.binding.vim.vsan.FileAnalyticsFileShareDashboardLabels;
import com.vmware.vim.vsan.binding.vim.vsan.VsanFileAnalyticsReport;
import com.vmware.vim.vsan.binding.vim.vsan.VsanFileAnalyticsReportData;
import com.vmware.vsan.client.services.fileanalytics.FileAnalyticsUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@TsModel
public class FileAnalyticsFileShareUsageData {
   private static final Log logger = LogFactory.getLog(FileAnalyticsFileShareUsageData.class);
   public String name;
   public long usage;
   public double usagePercentage;

   public FileAnalyticsFileShareUsageData() {
   }

   public FileAnalyticsFileShareUsageData(String name, long usage) {
      this.name = name;
      this.usage = usage;
   }

   public static List fromVmodl(VsanFileAnalyticsReport report) {
      if (report != null && report.reportData != null && report.categories != null) {
         List fileShareNames = Arrays.asList(report.categories);
         List fileUsages = new ArrayList();
         VsanFileAnalyticsReportData[] var3 = report.reportData;
         int i = var3.length;

         for(int var5 = 0; var5 < i; ++var5) {
            VsanFileAnalyticsReportData rowData = var3[var5];
            if (rowData != null && rowData.values != null) {
               switch(FileAnalyticsFileShareDashboardLabels.valueOf(rowData.label)) {
               case usage:
                  fileUsages.addAll(FileAnalyticsUtil.parseStringsToLongs(rowData.values));
                  break;
               default:
                  logger.warn("Unknown value '" + rowData.label + "' of FileAnalyticsFieShareDashboardLabels");
               }
            }
         }

         if (fileShareNames.size() != fileUsages.size()) {
            logger.warn("Cannot fetch all shares properties.");
            return Collections.emptyList();
         } else {
            List models = new ArrayList();

            for(i = 0; i < fileShareNames.size(); ++i) {
               models.add(new FileAnalyticsFileShareUsageData((String)fileShareNames.get(i), (Long)fileUsages.get(i)));
            }

            return models;
         }
      } else {
         return Collections.emptyList();
      }
   }

   public String toString() {
      return "FileAnalyticsFileShareUsageData(name=" + this.name + ", usage=" + this.usage + ", usagePercentage=" + this.usagePercentage + ")";
   }
}
