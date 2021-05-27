package com.vmware.vsan.client.util.retriever;

import com.vmware.vim.binding.vim.option.OptionManager;
import com.vmware.vim.binding.vim.option.OptionValue;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vmomi.core.Future;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcConnection;
import com.vmware.vsan.client.util.Measure;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class HostIsWitnessApplianceDataRetriever extends AbstractAsyncDataRetriever {
   private static final Log logger = LogFactory.getLog(HostIsWitnessApplianceDataRetriever.class);
   private static final String HOST_VIRTUAL_APPLIANCE = "Misc.vsanWitnessVirtualAppliance";
   private static final long HOST_VIRTUAL_APPLIANCE_ENABLED = 1L;
   private static final long HOST_VIRTUAL_APPLIANCE_DISABLED = 0L;
   private final VcClient vcClient;
   private final List hosts;
   private Map hostFutures = new HashMap();

   public HostIsWitnessApplianceDataRetriever(ManagedObjectReference clusterRef, Measure measure, VcClient vcClient, List hosts) {
      super(clusterRef, measure);
      this.vcClient = vcClient;
      this.hosts = hosts;
   }

   public void start() {
      this.hosts.forEach((hostRef) -> {
         VcConnection vcConnection = this.vcClient.getConnection(hostRef.getServerGuid());
         Throwable var3 = null;

         try {
            OptionManager optionManager = vcConnection.getHostAdvancedSettingsManager(hostRef);
            Future future = this.measure.newFuture("EsxHostAdvSettings[" + hostRef.toString() + "]");
            optionManager.queryView("Misc.vsanWitnessVirtualAppliance", future);
            this.hostFutures.put(hostRef, future);
         } catch (Throwable var13) {
            var3 = var13;
            throw var13;
         } finally {
            if (vcConnection != null) {
               if (var3 != null) {
                  try {
                     vcConnection.close();
                  } catch (Throwable var12) {
                     var3.addSuppressed(var12);
                  }
               } else {
                  vcConnection.close();
               }
            }

         }

      });
   }

   public Map prepareResult() {
      Map result = new HashMap();
      Iterator var2 = this.hostFutures.entrySet().iterator();

      while(var2.hasNext()) {
         Entry hostFutureEntry = (Entry)var2.next();
         ManagedObjectReference hostRef = (ManagedObjectReference)hostFutureEntry.getKey();

         try {
            OptionValue[] optionValues = (OptionValue[])((Future)hostFutureEntry.getValue()).get();
            Long witnessApplianceOptionValue = (Long)((Long)((OptionValue)Arrays.stream(optionValues).findFirst().orElse(new OptionValue("Misc.vsanWitnessVirtualAppliance", 0L))).getValue());
            result.put(hostRef, 1L == witnessApplianceOptionValue);
         } catch (Exception var7) {
            logger.warn("Failed to get Misc.vsanWitnessVirtualAppliance advance setting for host: " + hostRef);
         }
      }

      return result;
   }
}
