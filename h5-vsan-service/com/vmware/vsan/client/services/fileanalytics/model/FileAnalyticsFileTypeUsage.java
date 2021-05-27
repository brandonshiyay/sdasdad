package com.vmware.vsan.client.services.fileanalytics.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.vsan.binding.vim.vsan.FileAnalyticsCommonDashboardLabels;
import com.vmware.vim.vsan.binding.vim.vsan.VsanFileAnalyticsFileTypeLabels;
import com.vmware.vim.vsan.binding.vim.vsan.VsanFileAnalyticsReport;
import com.vmware.vim.vsan.binding.vim.vsan.VsanFileAnalyticsReportData;
import com.vmware.vsan.client.services.fileanalytics.FileAnalyticsUtil;
import com.vmware.vsphere.client.vsan.util.Utils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@TsModel
public class FileAnalyticsFileTypeUsage {
   private static final Log logger = LogFactory.getLog(FileAnalyticsFileTypeUsage.class);
   public String fileTypeLabel;
   public long usageInBytes;
   public long filesCount;

   public FileAnalyticsFileTypeUsage() {
   }

   public FileAnalyticsFileTypeUsage(String fileTypeLabel, long usageInBytes, long filesCount) {
      this.fileTypeLabel = fileTypeLabel;
      this.usageInBytes = usageInBytes;
      this.filesCount = filesCount;
   }

   public static List fromVmodl(VsanFileAnalyticsReport report) {
      if (report != null && report.reportData != null && report.categories != null) {
         List fileTypeNames = new ArrayList();
         List fileTypeCounts = new ArrayList();
         List fileTypeUsages = new ArrayList();
         String[] var4 = report.categories;
         int i = var4.length;

         int var6;
         for(var6 = 0; var6 < i; ++var6) {
            String category = var4[var6];
            fileTypeNames.add(getFileTypeName(category));
         }

         VsanFileAnalyticsReportData[] var8 = report.reportData;
         i = var8.length;

         for(var6 = 0; var6 < i; ++var6) {
            VsanFileAnalyticsReportData rowData = var8[var6];
            if (rowData != null && rowData.values != null) {
               switch(FileAnalyticsCommonDashboardLabels.valueOf(rowData.label)) {
               case file_size:
                  fileTypeUsages.addAll(FileAnalyticsUtil.parseStringsToLongs(rowData.values));
                  break;
               case file_count:
                  fileTypeCounts.addAll(FileAnalyticsUtil.parseStringsToLongs(rowData.values));
                  break;
               default:
                  logger.warn("Unknown value '" + rowData.label + "' of FileAnalyticsCommonDashboardLabels");
               }
            }
         }

         if (fileTypeNames.size() == fileTypeUsages.size() && fileTypeUsages.size() == fileTypeCounts.size()) {
            List models = new ArrayList();

            for(i = 0; i < fileTypeNames.size(); ++i) {
               models.add(new FileAnalyticsFileTypeUsage((String)fileTypeNames.get(i), (Long)fileTypeUsages.get(i), (Long)fileTypeCounts.get(i)));
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

   private static String getFileTypeName(String enumKey) {
      switch(VsanFileAnalyticsFileTypeLabels.valueOf(enumKey)) {
      case archives:
         return Utils.getLocalizedString("vsan.fileanalytics.filesByDistribution.types.archives");
      case audio:
         return Utils.getLocalizedString("vsan.fileanalytics.filesByDistribution.types.audio");
      case backup:
         return Utils.getLocalizedString("vsan.fileanalytics.filesByDistribution.types.backup");
      case documents:
         return Utils.getLocalizedString("vsan.fileanalytics.filesByDistribution.types.documents");
      case image:
         return Utils.getLocalizedString("vsan.fileanalytics.filesByDistribution.types.image");
      case installers:
         return Utils.getLocalizedString("vsan.fileanalytics.filesByDistribution.types.installers");
      case log:
         return Utils.getLocalizedString("vsan.fileanalytics.filesByDistribution.types.log");
      case others:
         return Utils.getLocalizedString("vsan.fileanalytics.filesByDistribution.types.others");
      case scripts:
         return Utils.getLocalizedString("vsan.fileanalytics.filesByDistribution.types.scripts");
      case system_files:
         return Utils.getLocalizedString("vsan.fileanalytics.filesByDistribution.types.systemFiles");
      case temporary:
         return Utils.getLocalizedString("vsan.fileanalytics.filesByDistribution.types.temporary");
      case video:
         return Utils.getLocalizedString("vsan.fileanalytics.filesByDistribution.types.video");
      default:
         logger.warn("Unknown value '" + enumKey + "' of VsanFileAnalyticsFileTypeLabels");
         return null;
      }
   }

   public String toString() {
      return "FileAnalyticsFileTypeUsage(fileTypeLabel=" + this.fileTypeLabel + ", usageInBytes=" + this.usageInBytes + ", filesCount=" + this.filesCount + ")";
   }
}
