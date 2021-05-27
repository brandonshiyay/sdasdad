package com.vmware.vsan.client.services.fileanalytics.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.vsan.binding.vim.vsan.FileAnalyticsFileShareDashboardLabels;
import com.vmware.vim.vsan.binding.vim.vsan.VsanFileAnalyticsReport;
import com.vmware.vim.vsan.binding.vim.vsan.VsanFileAnalyticsReportData;
import com.vmware.vsan.client.services.fileanalytics.FileAnalyticsUtil;
import com.vmware.vsphere.client.vsan.base.util.BaseUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@TsModel
public class FileAnalyticsFileShareQuotaOccupancyData {
   private static final Log logger = LogFactory.getLog(FileAnalyticsFileShareQuotaOccupancyData.class);
   public String name;
   public long currentUsage;
   public long hardQuota;
   public long usageOverHardQuotaPercentage;
   public int softQuotaPercentage;

   public FileAnalyticsFileShareQuotaOccupancyData() {
   }

   public FileAnalyticsFileShareQuotaOccupancyData(String name, long currentUsage, long softQuota, long hardQuota, long usageOverQuotaPercentage) {
      this.name = name;
      this.currentUsage = currentUsage;
      this.hardQuota = hardQuota;
      this.usageOverHardQuotaPercentage = usageOverQuotaPercentage;
      this.softQuotaPercentage = (int)Math.round(BaseUtils.toPercentage((double)softQuota, (double)hardQuota));
   }

   public static List fromVmodl(VsanFileAnalyticsReport report) {
      if (report != null && report.reportData != null && report.categories != null) {
         List fileShareNames = Arrays.asList(report.categories);
         List fileShareUsages = new ArrayList();
         List fileShareSoftQuota = new ArrayList();
         List fileShareHardQuota = new ArrayList();
         List fileShareUsagesOverQuota = new ArrayList();
         VsanFileAnalyticsReportData[] var6 = report.reportData;
         int i = var6.length;

         for(int var8 = 0; var8 < i; ++var8) {
            VsanFileAnalyticsReportData rowData = var6[var8];
            if (rowData != null && rowData.values != null) {
               switch(FileAnalyticsFileShareDashboardLabels.valueOf(rowData.label)) {
               case usage:
                  fileShareUsages.addAll(FileAnalyticsUtil.parseStringsToLongs(rowData.values));
                  break;
               case soft_quota:
                  fileShareSoftQuota.addAll(FileAnalyticsUtil.parseStringsToLongs(rowData.values));
                  break;
               case quota:
                  fileShareHardQuota.addAll(FileAnalyticsUtil.parseStringsToLongs(rowData.values));
                  break;
               case usage_by_quota:
                  fileShareUsagesOverQuota.addAll(FileAnalyticsUtil.parseStringsToLongs(rowData.values));
                  break;
               default:
                  logger.warn("Unknown value '" + rowData.label + "' of FileAnalyticsFieShareDashboardLabels");
               }
            }
         }

         if (fileShareNames.size() == fileShareUsages.size() && fileShareUsages.size() == fileShareSoftQuota.size() && fileShareSoftQuota.size() == fileShareHardQuota.size() && fileShareHardQuota.size() == fileShareUsagesOverQuota.size()) {
            List models = new ArrayList();

            for(i = 0; i < fileShareNames.size(); ++i) {
               models.add(new FileAnalyticsFileShareQuotaOccupancyData((String)fileShareNames.get(i), (Long)fileShareUsages.get(i), (Long)fileShareSoftQuota.get(i), (Long)fileShareHardQuota.get(i), (Long)fileShareUsagesOverQuota.get(i)));
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
      return "FileAnalyticsFileShareQuotaOccupancyData(name=" + this.name + ", currentUsage=" + this.currentUsage + ", hardQuota=" + this.hardQuota + ", usageOverHardQuotaPercentage=" + this.usageOverHardQuotaPercentage + ", softQuotaPercentage=" + this.softQuotaPercentage + ")";
   }
}
