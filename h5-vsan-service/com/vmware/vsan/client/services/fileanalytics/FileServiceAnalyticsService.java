package com.vmware.vsan.client.services.fileanalytics;

import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.VsanVcFileAnalyticsSystem;
import com.vmware.vim.vsan.binding.vim.vsan.FileAnalyticsScanStatus;
import com.vmware.vim.vsan.binding.vim.vsan.VsanFileAnalyticsFileShareDetails;
import com.vmware.vim.vsan.binding.vim.vsan.VsanFileAnalyticsRecentActivity;
import com.vmware.vim.vsan.binding.vim.vsan.VsanFileAnalyticsReport;
import com.vmware.vim.vsan.binding.vim.vsan.VsanFileAnalyticsReportQuerySpec;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.fileanalytics.model.FileAnalyticsScanStatusInfo;
import com.vmware.vsan.client.services.fileanalytics.model.FileAnalyticsShareData;
import com.vmware.vsan.client.services.fileanalytics.model.FileAnalyticsShareFileData;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class FileServiceAnalyticsService {
   private final Log logger = LogFactory.getLog(this.getClass());
   @Autowired
   VsanClient vsanClient;

   @Async
   public CompletableFuture getLastScanResultAsync(ManagedObjectReference clusterRef) {
      FileAnalyticsScanStatusInfo scanResult = this.getLastScanResult(clusterRef);
      return CompletableFuture.completedFuture(scanResult);
   }

   public FileAnalyticsScanStatusInfo getLastScanResult(ManagedObjectReference clusterRef) {
      try {
         VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
         Throwable var3 = null;

         FileAnalyticsScanStatusInfo scanResult;
         try {
            VsanVcFileAnalyticsSystem fileAnalyticsSystem = conn.getVsanFileAnalyticsSystem();
            Measure measure = new Measure("VsanVcFileAnalyticsSystem.getScanStatus");
            Throwable var6 = null;

            try {
               FileAnalyticsScanStatus scanStatus = fileAnalyticsSystem.getScanStatus(clusterRef);
               if (scanStatus != null) {
                  scanResult = new FileAnalyticsScanStatusInfo(scanStatus.scanDuration != null ? (long)Integer.parseInt(scanStatus.scanDuration) : 0L, scanStatus.lastScanTime.getTime());
                  FileAnalyticsScanStatusInfo var9 = scanResult;
                  return var9;
               }

               this.logger.debug("The file service analytics hasn't done any file share scanning yet.");
               scanResult = null;
            } catch (Throwable var37) {
               var6 = var37;
               throw var37;
            } finally {
               if (measure != null) {
                  if (var6 != null) {
                     try {
                        measure.close();
                     } catch (Throwable var36) {
                        var6.addSuppressed(var36);
                     }
                  } else {
                     measure.close();
                  }
               }

            }
         } catch (Throwable var39) {
            var3 = var39;
            throw var39;
         } finally {
            if (conn != null) {
               if (var3 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var35) {
                     var3.addSuppressed(var35);
                  }
               } else {
                  conn.close();
               }
            }

         }

         return scanResult;
      } catch (Exception var41) {
         this.logger.error("Unable to fetch the file service analytics last scan result.", var41);
         throw new VsanUiLocalizableException("vsan.fileanalytics.scan.error", var41);
      }
   }

   public VsanFileAnalyticsReport[] queryReports(ManagedObjectReference clusterRef, VsanFileAnalyticsReportQuerySpec spec) {
      try {
         VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
         Throwable var4 = null;

         Object var8;
         try {
            VsanVcFileAnalyticsSystem fileAnalyticsSystem = conn.getVsanFileAnalyticsSystem();
            Measure m = new Measure("fileAnalyticsSystem.queryVsanVcFileAnalyticsReports");
            Throwable var7 = null;

            try {
               var8 = fileAnalyticsSystem.queryVsanVcFileAnalyticsReports(clusterRef, spec);
            } catch (Throwable var33) {
               var8 = var33;
               var7 = var33;
               throw var33;
            } finally {
               if (m != null) {
                  if (var7 != null) {
                     try {
                        m.close();
                     } catch (Throwable var32) {
                        var7.addSuppressed(var32);
                     }
                  } else {
                     m.close();
                  }
               }

            }
         } catch (Throwable var35) {
            var4 = var35;
            throw var35;
         } finally {
            if (conn != null) {
               if (var4 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var31) {
                     var4.addSuppressed(var31);
                  }
               } else {
                  conn.close();
               }
            }

         }

         return (VsanFileAnalyticsReport[])var8;
      } catch (Exception var37) {
         this.logger.error("Unable to fetch file analytics report", var37);
         throw new VsanUiLocalizableException("vsan.fileanalytics.report.fetch.error", var37);
      }
   }

   public List queryFileShares(ManagedObjectReference clusterRef, VsanFileAnalyticsReportQuerySpec spec) {
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var5 = null;

      VsanFileAnalyticsFileShareDetails[] shares;
      try {
         VsanVcFileAnalyticsSystem fileAnalyticsSystem = conn.getVsanFileAnalyticsSystem();
         Measure m = new Measure("fileAnalyticsSystem.queryVsanVcFileAnalyticsReports");
         Throwable var8 = null;

         try {
            shares = fileAnalyticsSystem.queryVsanVcFileShareDetails(clusterRef, spec);
         } catch (Throwable var31) {
            var8 = var31;
            throw var31;
         } finally {
            if (m != null) {
               if (var8 != null) {
                  try {
                     m.close();
                  } catch (Throwable var30) {
                     var8.addSuppressed(var30);
                  }
               } else {
                  m.close();
               }
            }

         }
      } catch (Throwable var33) {
         var5 = var33;
         throw var33;
      } finally {
         if (conn != null) {
            if (var5 != null) {
               try {
                  conn.close();
               } catch (Throwable var29) {
                  var5.addSuppressed(var29);
               }
            } else {
               conn.close();
            }
         }

      }

      List result = new ArrayList();
      if (ArrayUtils.isEmpty(shares)) {
         return result;
      } else {
         VsanFileAnalyticsFileShareDetails[] var36 = shares;
         int var37 = shares.length;

         for(int var38 = 0; var38 < var37; ++var38) {
            VsanFileAnalyticsFileShareDetails share = var36[var38];
            result.add(FileAnalyticsShareData.fromVmodl(share));
         }

         return result;
      }
   }

   public List queryFileShareFiles(ManagedObjectReference clusterRef, VsanFileAnalyticsReportQuerySpec spec) {
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var5 = null;

      VsanFileAnalyticsRecentActivity[] shareFiles;
      try {
         VsanVcFileAnalyticsSystem fileAnalyticsSystem = conn.getVsanFileAnalyticsSystem();
         Measure m = new Measure("fileAnalyticsSystem.queryVsanVcFileAnalyticsReports");
         Throwable var8 = null;

         try {
            shareFiles = fileAnalyticsSystem.queryVsanVcFileShareRecentActivity(clusterRef, spec);
         } catch (Throwable var31) {
            var8 = var31;
            throw var31;
         } finally {
            if (m != null) {
               if (var8 != null) {
                  try {
                     m.close();
                  } catch (Throwable var30) {
                     var8.addSuppressed(var30);
                  }
               } else {
                  m.close();
               }
            }

         }
      } catch (Throwable var33) {
         var5 = var33;
         throw var33;
      } finally {
         if (conn != null) {
            if (var5 != null) {
               try {
                  conn.close();
               } catch (Throwable var29) {
                  var5.addSuppressed(var29);
               }
            } else {
               conn.close();
            }
         }

      }

      List result = new ArrayList();
      if (ArrayUtils.isEmpty(shareFiles)) {
         return result;
      } else {
         VsanFileAnalyticsRecentActivity[] var36 = shareFiles;
         int var37 = shareFiles.length;

         for(int var38 = 0; var38 < var37; ++var38) {
            VsanFileAnalyticsRecentActivity file = var36[var38];
            result.add(FileAnalyticsShareFileData.fromVmodl(file));
         }

         return result;
      }
   }
}
