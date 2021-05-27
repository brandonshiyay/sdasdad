package com.vmware.vsan.client.services.fileanalytics.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.vsan.binding.vim.vsan.FileAnalyticsCommonDashboardLabels;
import com.vmware.vim.vsan.binding.vim.vsan.FileAnalyticsFileShareDashboardLabels;
import com.vmware.vim.vsan.binding.vim.vsan.VsanFileAnalyticsReport;
import com.vmware.vim.vsan.binding.vim.vsan.VsanFileAnalyticsReportData;
import com.vmware.vsan.client.services.fileanalytics.FileAnalyticsUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@TsModel
public class FileAnalyticsFileShareAccessData {
   private static final Log logger = LogFactory.getLog(FileAnalyticsFileShareAccessData.class);
   public String name;
   public long accessCount;
   public double accessCountPercentage;
   public Date lastAccessedTime;

   public FileAnalyticsFileShareAccessData() {
   }

   public FileAnalyticsFileShareAccessData(String name, long accessCount, Date lastAccessedTime) {
      this.name = name;
      this.accessCount = accessCount;
      this.lastAccessedTime = lastAccessedTime;
   }

   public static List fromVmodl(VsanFileAnalyticsReport report) {
      if (report != null && report.reportData != null && report.categories != null) {
         List fileShareNames = Arrays.asList(report.categories);
         List fileAccessCounts = new ArrayList();
         List lastAccessedDates = new ArrayList();
         VsanFileAnalyticsReportData[] var4 = report.reportData;
         int i = var4.length;

         for(int var6 = 0; var6 < i; ++var6) {
            VsanFileAnalyticsReportData rowData = var4[var6];
            if (rowData != null && rowData.values != null) {
               if (EnumUtils.isValidEnum(FileAnalyticsCommonDashboardLabels.class, rowData.label)) {
                  switch(FileAnalyticsCommonDashboardLabels.valueOf(rowData.label)) {
                  case access_count:
                     fileAccessCounts.addAll(FileAnalyticsUtil.parseStringsToLongs(rowData.values));
                     break;
                  default:
                     logger.warn("Unknown value '" + rowData.label + "' of FileAnalyticsCommonDashboardLabels");
                  }
               }

               if (EnumUtils.isValidEnum(FileAnalyticsFileShareDashboardLabels.class, rowData.label)) {
                  switch(FileAnalyticsFileShareDashboardLabels.valueOf(rowData.label)) {
                  case last_accessed_time:
                     lastAccessedDates.addAll(FileAnalyticsUtil.parseTimestampsToDates(rowData.values));
                     break;
                  default:
                     logger.warn("Unknown value '" + rowData.label + "' of FileAnalyticsFieShareDashboardLabels");
                  }
               }
            }
         }

         if (fileShareNames.size() == lastAccessedDates.size() && lastAccessedDates.size() == fileAccessCounts.size()) {
            List models = new ArrayList();

            for(i = 0; i < fileShareNames.size(); ++i) {
               models.add(new FileAnalyticsFileShareAccessData((String)fileShareNames.get(i), (Long)fileAccessCounts.get(i), (Date)lastAccessedDates.get(i)));
            }

            return models;
         } else {
            logger.warn("Cannot fetch all shares properties.");
            return Collections.emptyList();
         }
      } else {
         return Collections.emptyList();
      }
   }

   public String toString() {
      return "FileAnalyticsFileShareAccessData(name=" + this.name + ", accessCount=" + this.accessCount + ", accessCountPercentage=" + this.accessCountPercentage + ", lastAccessedTime=" + this.lastAccessedTime + ")";
   }
}
