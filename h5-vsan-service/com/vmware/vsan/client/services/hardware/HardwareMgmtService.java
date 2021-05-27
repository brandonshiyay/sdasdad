package com.vmware.vsan.client.services.hardware;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vim.fault.VsanFault;
import com.vmware.vim.binding.vim.vsan.host.DiskResult;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanVcHardwareManagementSystem;
import com.vmware.vim.vsan.binding.vim.host.VsanHostHardwareInfo;
import com.vmware.vsan.client.services.hardware.model.HardwareMgmtData;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsan.client.util.retriever.VsanAsyncDataRetriever;
import com.vmware.vsan.client.util.retriever.VsanDataRetrieverFactory;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HardwareMgmtService {
   private static final Logger logger = LoggerFactory.getLogger(HardwareMgmtService.class);
   @Autowired
   private VsanDataRetrieverFactory dataRetrieverFactory;
   @Autowired
   private VsanClient vsanClient;

   @TsService
   public HardwareMgmtData getHardwareMgmtData(ManagedObjectReference hostRef) throws VsanFault {
      VsanHostHardwareInfo hardwareMgmtInfo = this.getHardwareMgmtInfo(hostRef);
      if (hardwareMgmtInfo == null) {
         return null;
      } else {
         Measure measure = new Measure("Collect Disk data");
         Throwable var4 = null;

         HardwareMgmtData var7;
         try {
            VsanAsyncDataRetriever dataRetriever = this.dataRetrieverFactory.createVsanAsyncDataRetriever(measure, (ManagedObjectReference)null).loadDisks(Arrays.asList(hostRef));
            HardwareMgmtData result = new HardwareMgmtData();
            result.overviewData = HardwareMgmtUtil.createOverviewData(hardwareMgmtInfo.getChassis(), hardwareMgmtInfo.getBootDevice());
            result.nics = HardwareMgmtUtil.createNics(hardwareMgmtInfo.getPnics());
            result.processors = HardwareMgmtUtil.createProcessors(hardwareMgmtInfo.getProcessors());
            result.memoryBoards = HardwareMgmtUtil.createMemoryBoards(hardwareMgmtInfo.getMemory());
            result.diskBoxes = HardwareMgmtUtil.createDiskBoxes(hardwareMgmtInfo.getChassis(), hardwareMgmtInfo.getStorageControllers(), getDiskResults(dataRetriever, hostRef));
            var7 = result;
         } catch (Throwable var16) {
            var4 = var16;
            throw var16;
         } finally {
            if (measure != null) {
               if (var4 != null) {
                  try {
                     measure.close();
                  } catch (Throwable var15) {
                     var4.addSuppressed(var15);
                  }
               } else {
                  measure.close();
               }
            }

         }

         return var7;
      }
   }

   private VsanHostHardwareInfo getHardwareMgmtInfo(ManagedObjectReference hostRef) throws VsanFault {
      VsanConnection conn = this.vsanClient.getConnection(hostRef.getServerGuid());
      Throwable var3 = null;

      Object var7;
      try {
         VsanVcHardwareManagementSystem vcHardwareManagementSystem = conn.getVcHardwareManagementSystem();
         Measure measure = new Measure("Query host hardware management summary");
         Throwable var6 = null;

         try {
            var7 = vcHardwareManagementSystem.queryHostHardwareSummary(hostRef);
         } catch (Throwable var30) {
            var7 = var30;
            var6 = var30;
            throw var30;
         } finally {
            if (measure != null) {
               if (var6 != null) {
                  try {
                     measure.close();
                  } catch (Throwable var29) {
                     var6.addSuppressed(var29);
                  }
               } else {
                  measure.close();
               }
            }

         }
      } catch (Throwable var32) {
         var3 = var32;
         throw var32;
      } finally {
         if (conn != null) {
            if (var3 != null) {
               try {
                  conn.close();
               } catch (Throwable var28) {
                  var3.addSuppressed(var28);
               }
            } else {
               conn.close();
            }
         }

      }

      return (VsanHostHardwareInfo)var7;
   }

   private static DiskResult[] getDiskResults(VsanAsyncDataRetriever dataRetriever, ManagedObjectReference hostRef) {
      try {
         return (DiskResult[])dataRetriever.getDisks().get(hostRef);
      } catch (Exception var3) {
         logger.error("Error while getting additional disks data. Some of disk properties will be not set", var3);
         return new DiskResult[0];
      }
   }
}
