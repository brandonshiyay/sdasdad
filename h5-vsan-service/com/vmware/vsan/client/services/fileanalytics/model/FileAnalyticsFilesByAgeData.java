package com.vmware.vsan.client.services.fileanalytics.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.vsan.binding.vim.vsan.FileAnalyticsCommonDashboardLabels;
import com.vmware.vim.vsan.binding.vim.vsan.VsanFileAnalyticsReport;
import com.vmware.vim.vsan.binding.vim.vsan.VsanFileAnalyticsReportData;
import com.vmware.vsan.client.services.fileanalytics.FileAnalyticsUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@TsModel
public class FileAnalyticsFilesByAgeData {
   private static final Log logger = LogFactory.getLog(FileAnalyticsFilesByAgeData.class);
   public Map ageDistributionByCreatedDate;
   public Map ageDistributionByModifiedDate;
   public Map ageDistributionByLastAccessedDate;

   public static Map fromVmodl(VsanFileAnalyticsReport report) {
      Map result = new HashMap();
      if (report != null && !ArrayUtils.isEmpty(report.reportData) && !ArrayUtils.isEmpty(report.categories)) {
         VsanFileAnalyticsReportData[] var2 = report.reportData;
         int var3 = var2.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            VsanFileAnalyticsReportData rowData = var2[var4];
            if (rowData != null && rowData.values != null) {
               List filesCountData = new ArrayList();
               switch(FileAnalyticsCommonDashboardLabels.valueOf(rowData.label)) {
               case file_count:
                  filesCountData.addAll(FileAnalyticsUtil.parseStringsToLongs(rowData.values));
                  break;
               default:
                  logger.warn("Unexpected label '" + rowData.label + "' when processing file count by age distribution");
               }

               for(int i = 0; i < report.categories.length; ++i) {
                  result.put(FileAnalyticsAgeDistribution.valueOf(report.categories[i]), filesCountData.get(i));
               }
            }
         }

         return result;
      } else {
         logger.error("Invalid report data received for files by age.");
         return result;
      }
   }

   public String toString() {
      return "FileAnalyticsFilesByAgeData(ageDistributionByCreatedDate=" + this.ageDistributionByCreatedDate + ", ageDistributionByModifiedDate=" + this.ageDistributionByModifiedDate + ", ageDistributionByLastAccessedDate=" + this.ageDistributionByLastAccessedDate + ")";
   }
}
