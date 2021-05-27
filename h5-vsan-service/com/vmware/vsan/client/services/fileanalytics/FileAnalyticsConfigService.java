package com.vmware.vsan.client.services.fileanalytics;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.VsanVcFileAnalyticsSystem;
import com.vmware.vim.vsan.binding.vim.vsan.ConfigInfoEx;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.config.ConfigInfoService;
import com.vmware.vsan.client.services.fileanalytics.model.FileAnalyticsConfigData;
import com.vmware.vsan.client.services.fileanalytics.model.FileAnalyticsScanStatusInfo;
import com.vmware.vsan.client.services.fileservice.model.VsanFileServiceCommonConfig;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsan.client.util.VmodlHelper;
import java.util.concurrent.CompletableFuture;
import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FileAnalyticsConfigService {
   private final Log logger = LogFactory.getLog(this.getClass());
   @Autowired
   VsanClient vsanClient;
   @Autowired
   ConfigInfoService configInfoService;
   @Autowired
   private FileServiceAnalyticsService fileAnalyticsService;

   @TsService
   public FileAnalyticsConfigData getFileAnalyticsConfigData(ManagedObjectReference clusterRef) {
      Validate.notNull(clusterRef);

      try {
         Measure measure = new Measure("Retrieving file analytics config data");
         Throwable var3 = null;

         FileAnalyticsConfigData var8;
         try {
            CompletableFuture configInfo = this.configInfoService.getVsanConfigInfoAsync(clusterRef);
            CompletableFuture scanResult = this.fileAnalyticsService.getLastScanResultAsync(clusterRef);
            VsanFileServiceCommonConfig fileServiceCommonConfig = VsanFileServiceCommonConfig.fromVmodl((ConfigInfoEx)configInfo.get(), clusterRef);
            FileAnalyticsScanStatusInfo scanStatus = null;
            if (fileServiceCommonConfig.isFileAnalyticsEnabled && fileServiceCommonConfig.isPerformanceServiceEnabled) {
               try {
                  scanStatus = (FileAnalyticsScanStatusInfo)scanResult.get();
               } catch (Exception var19) {
                  this.logger.warn("Unable to get the last scan result.", var19);
               }
            }

            var8 = new FileAnalyticsConfigData(fileServiceCommonConfig, scanStatus);
         } catch (Throwable var20) {
            var3 = var20;
            throw var20;
         } finally {
            if (measure != null) {
               if (var3 != null) {
                  try {
                     measure.close();
                  } catch (Throwable var18) {
                     var3.addSuppressed(var18);
                  }
               } else {
                  measure.close();
               }
            }

         }

         return var8;
      } catch (Exception var22) {
         this.logger.error("Unable to fetch file analytics enablement data.");
         throw new VsanUiLocalizableException("vsan.fileanalytics.config.error", var22);
      }
   }

   @TsService
   public FileAnalyticsScanStatusInfo getFileAnalyticsLastScan(ManagedObjectReference clusterRef) {
      return this.fileAnalyticsService.getLastScanResult(clusterRef);
   }

   @TsService
   public ManagedObjectReference startFileAnalyticsIndexing(ManagedObjectReference clusterRef) {
      try {
         VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
         Throwable var3 = null;

         Object var7;
         try {
            VsanVcFileAnalyticsSystem fileAnalyticsSystem = conn.getVsanFileAnalyticsSystem();
            Measure measure = new Measure("VsanVcFileAnalyticsSystem.scanCluster");
            Throwable var6 = null;

            try {
               var7 = VmodlHelper.assignServerGuid(fileAnalyticsSystem.scanCluster(clusterRef), clusterRef.getServerGuid());
            } catch (Throwable var32) {
               var7 = var32;
               var6 = var32;
               throw var32;
            } finally {
               if (measure != null) {
                  if (var6 != null) {
                     try {
                        measure.close();
                     } catch (Throwable var31) {
                        var6.addSuppressed(var31);
                     }
                  } else {
                     measure.close();
                  }
               }

            }
         } catch (Throwable var34) {
            var3 = var34;
            throw var34;
         } finally {
            if (conn != null) {
               if (var3 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var30) {
                     var3.addSuppressed(var30);
                  }
               } else {
                  conn.close();
               }
            }

         }

         return (ManagedObjectReference)var7;
      } catch (Exception var36) {
         this.logger.error("Unable to start file analytics indexing operation");
         throw new VsanUiLocalizableException("vsan.fileanalytics.scan.start.error", var36);
      }
   }
}
