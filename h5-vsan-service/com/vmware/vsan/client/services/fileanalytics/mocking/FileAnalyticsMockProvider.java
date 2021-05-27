package com.vmware.vsan.client.services.fileanalytics.mocking;

import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.VsanVcFileAnalyticsSystem;
import com.vmware.vim.vsan.binding.vim.vsan.FileAnalyticsFileShareOperations;
import com.vmware.vim.vsan.binding.vim.vsan.VsanFileAnalyticsFileAgeRanges;
import com.vmware.vim.vsan.binding.vim.vsan.VsanFileAnalyticsFileTypeLabels;
import com.vmware.vsan.client.services.fileanalytics.model.FileAnalyticsAgeDistribution;
import com.vmware.vsan.client.services.fileanalytics.model.FileAnalyticsDateGroupBy;
import com.vmware.vsan.client.services.fileanalytics.model.FileAnalyticsSizeDistribution;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class FileAnalyticsMockProvider {
   private final Logger logger = LoggerFactory.getLogger(this.getClass());
   @Autowired
   private VsanClient vsanClient;

   public String getAllMockedData(ManagedObjectReference clusterRef) {
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var3 = null;

      String var5;
      try {
         VsanVcFileAnalyticsSystem fileAnalyticsSystem = conn.getVsanFileAnalyticsSystem();
         var5 = this.insertQueries(clusterRef, fileAnalyticsSystem, new String[]{"SELECT * from FAFileShareDashboardDetail", "SELECT * from FAFileDistByType", "SELECT * from FAFileDistBySize", "SELECT * from FAFileDistByAge"});
      } catch (Throwable var14) {
         var3 = var14;
         throw var14;
      } finally {
         if (conn != null) {
            if (var3 != null) {
               try {
                  conn.close();
               } catch (Throwable var13) {
                  var3.addSuppressed(var13);
               }
            } else {
               conn.close();
            }
         }

      }

      return var5;
   }

   public String[] getMockedFileShareUuids() {
      return new String[]{"file:ebdee9a6-ef89-410c-a88c-4d80a4996efd", "file:b04edc32-ba1d-40fa-b125-d8ebb8dce46c", "file:5ba400fb-d413-4fa7-8c8b-9bf736992118", "file:ac8bec48-0822-4739-83b2-7ab20777fd96", "file:3c90ae9c-8bf5-4e2c-b819-6b5d6f342528"};
   }

   public void populateMockData(ManagedObjectReference clusterRef) {
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var3 = null;

      try {
         VsanVcFileAnalyticsSystem fileAnalyticsSystem = conn.getVsanFileAnalyticsSystem();
         this.mockFileShareDetails(clusterRef, fileAnalyticsSystem);
         this.mockFileDistByAge(clusterRef, fileAnalyticsSystem);
         this.mockFileDistBySize(clusterRef, fileAnalyticsSystem);
         this.mockFileDistByType(clusterRef, fileAnalyticsSystem);
      } catch (Throwable var12) {
         var3 = var12;
         throw var12;
      } finally {
         if (conn != null) {
            if (var3 != null) {
               try {
                  conn.close();
               } catch (Throwable var11) {
                  var3.addSuppressed(var11);
               }
            } else {
               conn.close();
            }
         }

      }

   }

   public void deleteMockData(ManagedObjectReference clusterRef, VsanVcFileAnalyticsSystem fileAnalyticsSystem) {
      this.removeMockedFileShareDetailsData(clusterRef, fileAnalyticsSystem);
      this.removeMockedDistByAgeData(clusterRef, fileAnalyticsSystem);
      this.removeMockedFileDistBySizeData(clusterRef, fileAnalyticsSystem);
      this.removeMockedFileDistByTypeData(clusterRef, fileAnalyticsSystem);
   }

   private void mockFileShareDetails(ManagedObjectReference clusterRef, VsanVcFileAnalyticsSystem fileAnalyticsSystem) {
      String[] fileSharesUuids = this.getMockedFileShareUuids();
      List queries = new ArrayList();
      queries.add(this.insertFileShareDetailEntry("Analytics", fileSharesUuids[0], 5368709120L, 10000000L, 10737418240L, 50L, 17, new Date(), 10, 10));
      queries.add(this.insertFileShareDetailEntry("UI", fileSharesUuids[1], 270582939648L, 600000L, 375809638400L, 72L, 45, new Date(), 40, 13));
      queries.add(this.insertFileShareDetailEntry("vSAN", fileSharesUuids[2], 5153960755L, 600000L, 5368709120L, 96L, 1000, new Date(), 102, 8));
      queries.add(this.insertFileShareDetailEntry("vRops", fileSharesUuids[3], 14130442404L, 600000L, 30064771072L, 47L, 180, new Date(), 18, 73));
      queries.add(this.insertFileShareDetailEntry("VCF", fileSharesUuids[4], 17716740096L, 600000L, 35433480192L, 50L, 57, new Date(), 115, 120));
      this.insertQueries(clusterRef, fileAnalyticsSystem, (String[])queries.toArray(new String[0]));
   }

   private void removeMockedFileShareDetailsData(ManagedObjectReference clusterRef, VsanVcFileAnalyticsSystem fileAnalyticsSystem) {
      List queries = new ArrayList();
      queries.add(this.deleteFileShareDetailEntry("Analytics"));
      queries.add(this.deleteFileShareDetailEntry("UI"));
      queries.add(this.deleteFileShareDetailEntry("vSAN"));
      queries.add(this.deleteFileShareDetailEntry("vRops"));
      queries.add(this.deleteFileShareDetailEntry("VCF"));
      this.insertQueries(clusterRef, fileAnalyticsSystem, (String[])queries.toArray(new String[0]));
   }

   private void mockFileDistByType(ManagedObjectReference clusterRef, VsanVcFileAnalyticsSystem fileAnalyticsSystem) {
      String[] fileSharesUuids = this.getMockedFileShareUuids();
      List queries = new ArrayList();
      queries.add(this.insertDistByTypeEntry(fileSharesUuids[0], VsanFileAnalyticsFileTypeLabels.documents.name(), 17, 5751005000L));
      queries.add(this.insertDistByTypeEntry(fileSharesUuids[1], VsanFileAnalyticsFileTypeLabels.system_files.name(), 56, 8141675111L));
      queries.add(this.insertDistByTypeEntry(fileSharesUuids[2], VsanFileAnalyticsFileTypeLabels.archives.name(), 83, 7751975009L));
      queries.add(this.insertDistByTypeEntry(fileSharesUuids[3], VsanFileAnalyticsFileTypeLabels.installers.name(), 1600, 2751505001L));
      queries.add(this.insertDistByTypeEntry(fileSharesUuids[4], VsanFileAnalyticsFileTypeLabels.image.name(), 150, 1951455002L));
      this.insertQueries(clusterRef, fileAnalyticsSystem, (String[])queries.toArray(new String[0]));
   }

   private void removeMockedFileDistByTypeData(ManagedObjectReference clusterRef, VsanVcFileAnalyticsSystem fileAnalyticsSystem) {
      List queries = new ArrayList();
      queries.add(this.deleteDistByTypeEntry(VsanFileAnalyticsFileTypeLabels.documents.name()));
      queries.add(this.deleteDistByTypeEntry(VsanFileAnalyticsFileTypeLabels.system_files.name()));
      queries.add(this.deleteDistByTypeEntry(VsanFileAnalyticsFileTypeLabels.archives.name()));
      queries.add(this.deleteDistByTypeEntry(VsanFileAnalyticsFileTypeLabels.installers.name()));
      queries.add(this.deleteDistByTypeEntry(VsanFileAnalyticsFileTypeLabels.image.name()));
      this.insertQueries(clusterRef, fileAnalyticsSystem, (String[])queries.toArray(new String[0]));
   }

   private void mockFileDistBySize(ManagedObjectReference clusterRef, VsanVcFileAnalyticsSystem fileAnalyticsSystem) {
      String[] fileSharesUuids = this.getMockedFileShareUuids();
      List queries = new ArrayList();
      queries.add(this.insertDistBySizeEntry(fileSharesUuids[0], FileAnalyticsSizeDistribution.less_than_a_mb, 20));
      queries.add(this.insertDistBySizeEntry(fileSharesUuids[1], FileAnalyticsSizeDistribution.one_mb_to_ten_mb, 30));
      queries.add(this.insertDistBySizeEntry(fileSharesUuids[2], FileAnalyticsSizeDistribution.ten_mb_to_hundred_mb, 100));
      queries.add(this.insertDistBySizeEntry(fileSharesUuids[3], FileAnalyticsSizeDistribution.hundred_mb_to_one_gb, 3));
      queries.add(this.insertDistBySizeEntry(fileSharesUuids[4], FileAnalyticsSizeDistribution.more_than_one_gb, 10));
      this.insertQueries(clusterRef, fileAnalyticsSystem, (String[])queries.toArray(new String[0]));
   }

   private void removeMockedFileDistBySizeData(ManagedObjectReference clusterRef, VsanVcFileAnalyticsSystem fileAnalyticsSystem) {
      List queries = new ArrayList();
      queries.add(this.deleteDistBySizeEntry(FileAnalyticsSizeDistribution.less_than_a_mb));
      queries.add(this.deleteDistBySizeEntry(FileAnalyticsSizeDistribution.one_mb_to_ten_mb));
      queries.add(this.deleteDistBySizeEntry(FileAnalyticsSizeDistribution.ten_mb_to_hundred_mb));
      queries.add(this.deleteDistBySizeEntry(FileAnalyticsSizeDistribution.hundred_mb_to_one_gb));
      queries.add(this.deleteDistBySizeEntry(FileAnalyticsSizeDistribution.more_than_one_gb));
      this.insertQueries(clusterRef, fileAnalyticsSystem, (String[])queries.toArray(new String[0]));
   }

   private void mockFileDistByAge(ManagedObjectReference clusterRef, VsanVcFileAnalyticsSystem fileAnalyticsSystem) {
      String[] fileSharesUuids = this.getMockedFileShareUuids();
      new ArrayList();
   }

   private void removeMockedDistByAgeData(ManagedObjectReference clusterRef, VsanVcFileAnalyticsSystem fileAnalyticsSystem) {
      List queries = new ArrayList();
      this.insertQueries(clusterRef, fileAnalyticsSystem, (String[])queries.toArray(new String[0]));
   }

   private String insertDistByAgeEntry(String fileShareUuid, VsanFileAnalyticsFileAgeRanges ageDistribution, int fileCount, FileAnalyticsDateGroupBy groupBy) {
      return String.format("INSERT INTO FAFileDistByAge (arrivalTs, fileShareUUID, fileAgeRange, fileCount, sortBy) VALUES ('%d', '%s', '%s', %d, '%s')", this.getCurrentTime(), fileShareUuid, ageDistribution.toString(), fileCount, groupBy.getKey());
   }

   private String deleteDistByAgeEntry(FileAnalyticsAgeDistribution ageDistribution) {
      return String.format("DELETE FROM FAFileDistByAge WHERE fileAgeRange='%s'", ageDistribution.toString());
   }

   private String insertDistBySizeEntry(String fileShareUuid, FileAnalyticsSizeDistribution sizeDistribution, int fileCount) {
      return String.format("INSERT INTO FAFileDistBySize (arrivalTs, fileShareUUID, fileSizeRange, fileCount) VALUES ('%d', '%s', '%s', %d)", this.getCurrentTime(), fileShareUuid, sizeDistribution.toString(), fileCount);
   }

   private String deleteDistBySizeEntry(FileAnalyticsSizeDistribution sizeDistribution) {
      return String.format("DELETE FROM FAFileDistBySize WHERE fileSizeRange='%s'", sizeDistribution.toString());
   }

   private String insertFileShareDetailEntry(String fileShareName, String fileShareUuid, long currentUsage, long softQuota, long hardQuota, long usagePercentage, int accessCount, Date lastUpdate, int filesAddedCount, int filesDeletedCount) {
      return String.format("INSERT INTO FAFileShareDashboardDetail (arrivalTs, fileShareUUID, sharename, protocols, currentUsage, quota, softQuota, pctUsed, growthInADay, accesscount, lastAccessedTime, lastOperation, filesAdded, filesDeleted) VALUES ('%d', '%s', '%s', 'NFSv3,NFSv4', %d, %d, %d, %d, 0, %d, '%d', '%s', %d, %d)", this.getCurrentTime(), fileShareUuid, fileShareName, currentUsage, hardQuota, softQuota, usagePercentage, accessCount, lastUpdate.getTime(), FileAnalyticsFileShareOperations.Read.name(), filesAddedCount, filesDeletedCount);
   }

   private String deleteFileShareDetailEntry(String fileShareName) {
      return String.format("DELETE FROM FAFileShareDashboardDetail WHERE sharename='%s'", fileShareName);
   }

   private String insertDistByTypeEntry(String fileShareUuid, String fileType, int fileCount, long totalSize) {
      return String.format("INSERT INTO FAFileDistByType (arrivalTs, fileShareUUID, fileType, fileCount, totalSize) VALUES ('%d', '%s', '%s', %d, %d)", this.getCurrentTime(), fileShareUuid, fileType, fileCount, totalSize);
   }

   private String deleteDistByTypeEntry(String fileType) {
      return String.format("DELETE FROM FAFileDistByType WHERE fileType='%s'", fileType);
   }

   private String insertQueries(ManagedObjectReference clusterRef, VsanVcFileAnalyticsSystem fileAnalyticsSystem, String[] queries) {
      try {
         return fileAnalyticsSystem.executeVcSqlStatements(clusterRef, queries);
      } catch (Exception var5) {
         this.logger.debug(var5.getLocalizedMessage());
         return null;
      }
   }

   private long getCurrentTime() {
      return (new Date()).getTime() / 1000L;
   }
}
