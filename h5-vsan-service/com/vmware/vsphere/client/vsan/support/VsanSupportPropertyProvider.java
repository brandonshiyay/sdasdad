package com.vmware.vsphere.client.vsan.support;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanAttachToSrOperation;
import com.vmware.vim.vsan.binding.vim.cluster.VsanVcClusterHealthSystem;
import com.vmware.vsan.client.services.common.TaskService;
import com.vmware.vsan.client.services.common.data.TaskInfoData;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsphere.client.vsan.base.util.VsanProfiler;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class VsanSupportPropertyProvider {
   private static final Log _logger = LogFactory.getLog(VsanSupportPropertyProvider.class);
   private static final VsanProfiler _profiler = new VsanProfiler(VsanSupportPropertyProvider.class);
   @Autowired
   private TaskService taskService;
   @Autowired
   private VsanClient vsanClient;

   @TsService
   public VsanAttachToSrOperation getVsanSRLastOperation(ManagedObjectReference clusterRef) throws Exception {
      VsanAttachToSrOperation[] history = null;
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var4 = null;

      try {
         VsanVcClusterHealthSystem healthSystem = conn.getVsanVcClusterHealthSystem();
         VsanProfiler.Point p = _profiler.point("healthSystem.queryAttachToSrHistory");
         Throwable var7 = null;

         try {
            history = healthSystem.queryAttachToSrHistory(clusterRef, 10, (String)null);
         } catch (Throwable var30) {
            var7 = var30;
            throw var30;
         } finally {
            if (p != null) {
               if (var7 != null) {
                  try {
                     p.close();
                  } catch (Throwable var29) {
                     var7.addSuppressed(var29);
                  }
               } else {
                  p.close();
               }
            }

         }
      } catch (Throwable var32) {
         var4 = var32;
         throw var32;
      } finally {
         if (conn != null) {
            if (var4 != null) {
               try {
                  conn.close();
               } catch (Throwable var28) {
                  var4.addSuppressed(var28);
               }
            } else {
               conn.close();
            }
         }

      }

      if (history != null && history.length > 0) {
         for(int itr = history.length - 1; itr >= 0; --itr) {
            VsanAttachToSrOperation sr = history[itr];
            String srNumber = sr.getSrNumber();
            if (!StringUtils.isBlank(srNumber) && !srNumber.toLowerCase().contains("pr")) {
               if (sr.task != null) {
                  sr.task.setServerGuid(clusterRef.getServerGuid());
               }

               TaskInfoData taskInfo = this.taskService.getInfo(sr.task);
               if (taskInfo != null && taskInfo.progress == 100) {
                  sr.task = null;
               }

               return sr;
            }
         }
      }

      _logger.info("There is no sr uploaded for this cluster");
      return null;
   }
}
