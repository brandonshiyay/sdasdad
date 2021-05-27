package com.vmware.vsan.client.services.capacity;

import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanSpaceReportSystem;
import com.vmware.vim.vsan.binding.vim.cluster.VsanWhatifCapacityHealthThreshold;
import com.vmware.vim.vsan.binding.vim.vsan.CapacityReservationInfo;
import com.vmware.vsan.client.services.config.ReservationStatus;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class CapacityHealthThresholdService {
   @Autowired
   private VsanClient vsanClient;
   private static final CapacityReservationInfo[] reservationConfigurations;

   @Async
   public CompletableFuture getWhatIfCapacityThreshold(ManagedObjectReference clusterRef) {
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var4 = null;

      VsanWhatifCapacityHealthThreshold[] thresholds;
      try {
         VsanSpaceReportSystem vsanSpaceReportSystem = conn.getVsanSpaceReportSystem();
         Measure measure = new Measure("VsanSpaceReportSystem.queryVsanCapacityHealthThreshold");
         Throwable var7 = null;

         try {
            thresholds = vsanSpaceReportSystem.queryVsanCapacityHealthThreshold(clusterRef, reservationConfigurations);
         } catch (Throwable var30) {
            var7 = var30;
            throw var30;
         } finally {
            if (measure != null) {
               if (var7 != null) {
                  try {
                     measure.close();
                  } catch (Throwable var29) {
                     var7.addSuppressed(var29);
                  }
               } else {
                  measure.close();
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

      return CompletableFuture.completedFuture(thresholds);
   }

   static {
      reservationConfigurations = new CapacityReservationInfo[]{new CapacityReservationInfo(ReservationStatus.REPORTED.toString(), ReservationStatus.REPORTED.toString()), new CapacityReservationInfo(ReservationStatus.REPORTED.toString(), ReservationStatus.ENFORCED.toString()), new CapacityReservationInfo(ReservationStatus.ENFORCED.toString(), ReservationStatus.ENFORCED.toString())};
   }
}
