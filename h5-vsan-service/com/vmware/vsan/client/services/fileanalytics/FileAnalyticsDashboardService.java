package com.vmware.vsan.client.services.fileanalytics;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.vsan.FileAnalyticsCommonDashboardLabels;
import com.vmware.vim.vsan.binding.vim.vsan.VsanFileAnalyticsReport;
import com.vmware.vim.vsan.binding.vim.vsan.VsanFileAnalyticsReportType;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.services.fileanalytics.model.FileAnalyticsDateGroupBy;
import com.vmware.vsan.client.services.fileanalytics.model.FileAnalyticsFastestGrowingShareData;
import com.vmware.vsan.client.services.fileanalytics.model.FileAnalyticsFileShareAccessData;
import com.vmware.vsan.client.services.fileanalytics.model.FileAnalyticsFileShareQuotaOccupancyData;
import com.vmware.vsan.client.services.fileanalytics.model.FileAnalyticsFileShareUsageData;
import com.vmware.vsan.client.services.fileanalytics.model.FileAnalyticsFileSharesListType;
import com.vmware.vsan.client.services.fileanalytics.model.FileAnalyticsFileTypeUsage;
import com.vmware.vsan.client.services.fileanalytics.model.FileAnalyticsFilesByAgeData;
import com.vmware.vsan.client.services.fileanalytics.model.FileAnalyticsShareCapacityTrending;
import com.vmware.vsan.client.services.fileanalytics.model.FileAnalyticsSizeDistribution;
import com.vmware.vsphere.client.vsan.base.util.BaseUtils;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FileAnalyticsDashboardService {
   private final Log logger = LogFactory.getLog(this.getClass());
   @Autowired
   private FileServiceAnalyticsService fileAnalyticsService;

   @TsService
   public FileAnalyticsFilesByAgeData queryFilesByAge(ManagedObjectReference clusterRef, String[] fileShareUuids) {
      if (!VsanCapabilityUtils.isFileAnalyticsSupported(clusterRef)) {
         return null;
      } else {
         VsanFileAnalyticsReport[] reports = this.fileAnalyticsService.queryReports(clusterRef, FileAnalyticsUtil.buildSpec(VsanFileAnalyticsReportType.by_file_age, fileShareUuids));
         FileAnalyticsFilesByAgeData result = new FileAnalyticsFilesByAgeData();
         if (ArrayUtils.isEmpty(reports)) {
            this.logger.warn("The query for file age usage didn't return any report data!");
            result.ageDistributionByCreatedDate = new HashMap();
            result.ageDistributionByLastAccessedDate = new HashMap();
            result.ageDistributionByModifiedDate = new HashMap();
            return result;
         } else {
            VsanFileAnalyticsReport[] var5 = reports;
            int var6 = reports.length;

            for(int var7 = 0; var7 < var6; ++var7) {
               VsanFileAnalyticsReport report = var5[var7];
               switch(FileAnalyticsDateGroupBy.fromString(report.reportGroupedBy)) {
               case CREATED_DATE:
                  result.ageDistributionByCreatedDate = FileAnalyticsFilesByAgeData.fromVmodl(report);
                  break;
               case ACCESSED_DATE:
                  result.ageDistributionByLastAccessedDate = FileAnalyticsFilesByAgeData.fromVmodl(report);
                  break;
               case MODIFIED_DATE:
                  result.ageDistributionByModifiedDate = FileAnalyticsFilesByAgeData.fromVmodl(report);
               }
            }

            return result;
         }
      }
   }

   @TsService
   public Map queryFilesBySize(ManagedObjectReference clusterRef, String[] fileShareUuids) {
      if (!VsanCapabilityUtils.isFileAnalyticsSupported(clusterRef)) {
         return null;
      } else {
         VsanFileAnalyticsReport[] reports = this.fileAnalyticsService.queryReports(clusterRef, FileAnalyticsUtil.buildSpec(VsanFileAnalyticsReportType.by_file_size, fileShareUuids));
         if (ArrayUtils.isEmpty(reports)) {
            this.logger.warn("The query for files by size didn't return any report data!");
            return Collections.emptyMap();
         } else {
            if (reports.length > 1) {
               this.logger.warn("The query for files by size contains more than one report!");
            }

            return this.getFileSizeReport(reports[0]);
         }
      }
   }

   private Map getFileSizeReport(VsanFileAnalyticsReport report) {
      if (report != null && !ArrayUtils.isEmpty(report.reportData) && !ArrayUtils.isEmpty(report.reportData[0].values) && FileAnalyticsCommonDashboardLabels.valueOf(report.reportData[0].label) == FileAnalyticsCommonDashboardLabels.file_count) {
         List categories = Arrays.asList(report.categories);
         List filesCount = FileAnalyticsUtil.parseStringsToLongs(report.reportData[0].values);
         Map result = new HashMap();

         for(int i = 0; i < categories.size(); ++i) {
            result.put(FileAnalyticsSizeDistribution.valueOf((String)categories.get(i)), filesCount.get(i));
         }

         return result;
      } else {
         this.logger.error("The query for files by size contains invalid report data: " + report.reportData[0]);
         return new HashMap();
      }
   }

   @TsService
   public List queryFileTypeUsage(ManagedObjectReference clusterRef, String[] fileShareUuids) {
      if (!VsanCapabilityUtils.isFileAnalyticsSupported(clusterRef)) {
         return null;
      } else {
         VsanFileAnalyticsReport[] reports = this.fileAnalyticsService.queryReports(clusterRef, FileAnalyticsUtil.buildSpec(VsanFileAnalyticsReportType.by_file_type, fileShareUuids));
         if (ArrayUtils.isEmpty(reports)) {
            this.logger.warn("The query for file type usage didn't return any report data!");
            return Collections.emptyList();
         } else {
            if (reports.length > 1) {
               this.logger.warn("The query for file type usage contains more than one report!");
            }

            return FileAnalyticsFileTypeUsage.fromVmodl(reports[0]);
         }
      }
   }

   @TsService
   public List queryFileShareCapacityTrending(ManagedObjectReference clusterRef, long startDate, long endDate) {
      if (!VsanCapabilityUtils.isFileAnalyticsSupported(clusterRef)) {
         return null;
      } else {
         VsanFileAnalyticsReport[] reports = this.fileAnalyticsService.queryReports(clusterRef, FileAnalyticsUtil.buildSpec(VsanFileAnalyticsReportType.by_fileshare_capacity, startDate, endDate));
         if (ArrayUtils.isEmpty(reports)) {
            this.logger.warn("The query for file shares capacity trending didn't return any report data!");
            return Collections.emptyList();
         } else {
            if (reports.length > 1) {
               this.logger.warn("The query for top accessed file shares contains more than one report!");
            }

            return FileAnalyticsShareCapacityTrending.fromVmodl(reports[0]);
         }
      }
   }

   @TsService
   public List queryFastestGrowingShares(ManagedObjectReference clusterRef, long startDate, long endDate) {
      if (!VsanCapabilityUtils.isFileAnalyticsSupported(clusterRef)) {
         return null;
      } else {
         VsanFileAnalyticsReport[] reports = this.fileAnalyticsService.queryReports(clusterRef, FileAnalyticsUtil.buildSpec(VsanFileAnalyticsReportType.by_fileshare_growth, startDate, endDate));
         if (ArrayUtils.isEmpty(reports)) {
            this.logger.warn("The query for fastest growing file shares didn't return any report data!");
            return Collections.emptyList();
         } else {
            List result = (List)Arrays.stream(reports).map((report) -> {
               return FileAnalyticsFastestGrowingShareData.fromVmodl(report);
            }).collect(Collectors.toList());
            return result;
         }
      }
   }

   @TsService
   public List queryTopAccessedFileShares(ManagedObjectReference clusterRef, long startDate, long endDate) {
      if (!VsanCapabilityUtils.isFileAnalyticsSupported(clusterRef)) {
         return null;
      } else {
         VsanFileAnalyticsReport[] reports = this.fileAnalyticsService.queryReports(clusterRef, FileAnalyticsUtil.buildSpec(VsanFileAnalyticsReportType.by_fileshare_access, startDate, endDate));
         if (ArrayUtils.isEmpty(reports)) {
            this.logger.warn("The query for top accessed file shares didn't return any report data!");
            return Collections.emptyList();
         } else {
            if (reports.length > 1) {
               this.logger.warn("The query for top accessed file shares contains more than one report!");
            }

            List topAccessedFileShares = FileAnalyticsFileShareAccessData.fromVmodl(reports[0]);
            long topAccessedFileShareCount = topAccessedFileShares.stream().mapToLong((share) -> {
               return share.accessCount;
            }).max().orElse(0L);

            FileAnalyticsFileShareAccessData fileShareData;
            for(Iterator var10 = topAccessedFileShares.iterator(); var10.hasNext(); fileShareData.accessCountPercentage = topAccessedFileShareCount == 0L ? 0.0D : BaseUtils.toPercentage((double)fileShareData.accessCount, (double)topAccessedFileShareCount)) {
               fileShareData = (FileAnalyticsFileShareAccessData)var10.next();
            }

            topAccessedFileShares.sort((o1, o2) -> {
               return (int)Math.round(o2.accessCountPercentage - o1.accessCountPercentage);
            });
            return topAccessedFileShares;
         }
      }
   }

   @TsService
   public List queryLargestFileShares(ManagedObjectReference clusterRef, long startDate, long endDate) {
      if (!VsanCapabilityUtils.isFileAnalyticsSupported(clusterRef)) {
         return null;
      } else {
         VsanFileAnalyticsReport[] reports = this.fileAnalyticsService.queryReports(clusterRef, FileAnalyticsUtil.buildSpec(VsanFileAnalyticsReportType.by_fileshare_largest, startDate, endDate));
         if (ArrayUtils.isEmpty(reports)) {
            this.logger.warn("The query for largest file shares didn't return any report data!");
            return Collections.emptyList();
         } else {
            if (reports.length > 1) {
               this.logger.warn("The query for largest file shares contains more than one report!");
            }

            List largestFileShares = FileAnalyticsFileShareUsageData.fromVmodl(reports[0]);
            long largestFileShareUsage = largestFileShares.stream().mapToLong((share) -> {
               return share.usage;
            }).max().orElse(0L);

            FileAnalyticsFileShareUsageData fileShareData;
            for(Iterator var10 = largestFileShares.iterator(); var10.hasNext(); fileShareData.usagePercentage = BaseUtils.toPercentage((double)fileShareData.usage, (double)largestFileShareUsage)) {
               fileShareData = (FileAnalyticsFileShareUsageData)var10.next();
            }

            largestFileShares.sort((o1, o2) -> {
               return (int)Math.round(o2.usagePercentage - o1.usagePercentage);
            });
            return largestFileShares;
         }
      }
   }

   @TsService
   public List queryTopQuotaOccupancyFileShares(ManagedObjectReference clusterRef, long startDate, long endDate) {
      if (!VsanCapabilityUtils.isFileAnalyticsSupported(clusterRef)) {
         return null;
      } else {
         VsanFileAnalyticsReport[] reports = this.fileAnalyticsService.queryReports(clusterRef, FileAnalyticsUtil.buildSpec(VsanFileAnalyticsReportType.by_fileshare_quota, startDate, endDate));
         if (ArrayUtils.isEmpty(reports)) {
            this.logger.warn("The query for top quota occupancy file shares didn't return any report data!");
            return Collections.emptyList();
         } else {
            if (reports.length > 1) {
               this.logger.warn("The query for top quota occupancy file shares contains more than one report!");
            }

            List topQuotaOccupancyFileShares = FileAnalyticsFileShareQuotaOccupancyData.fromVmodl(reports[0]);
            topQuotaOccupancyFileShares.sort((o1, o2) -> {
               if (o2.usageOverHardQuotaPercentage > o1.usageOverHardQuotaPercentage) {
                  return 1;
               } else {
                  return o2.usageOverHardQuotaPercentage < o1.usageOverHardQuotaPercentage ? -1 : 0;
               }
            });
            return topQuotaOccupancyFileShares;
         }
      }
   }

   @TsService
   public List queryFileShares(ManagedObjectReference clusterRef, FileAnalyticsFileSharesListType reportType, long startDate, long endDate) {
      return !VsanCapabilityUtils.isFileAnalyticsSupported(clusterRef) ? null : this.fileAnalyticsService.queryFileShares(clusterRef, FileAnalyticsUtil.buildSpec(VsanFileAnalyticsReportType.by_fileshare_details, startDate, endDate));
   }

   @TsService
   public List queryFileShareFiles(ManagedObjectReference clusterRef, String shareUuid) {
      return !VsanCapabilityUtils.isFileAnalyticsSupported(clusterRef) ? null : this.fileAnalyticsService.queryFileShareFiles(clusterRef, FileAnalyticsUtil.buildSpec(VsanFileAnalyticsReportType.by_recent_activity, new String[]{shareUuid}));
   }
}
