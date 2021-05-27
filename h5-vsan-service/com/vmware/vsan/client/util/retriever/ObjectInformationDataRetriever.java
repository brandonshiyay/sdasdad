package com.vmware.vsan.client.util.retriever;

import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vmomi.core.Future;
import com.vmware.vim.vsan.binding.vim.cluster.VsanObjectInformation;
import com.vmware.vim.vsan.binding.vim.cluster.VsanObjectQuerySpec;
import com.vmware.vim.vsan.binding.vim.cluster.VsanObjectSystem;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class ObjectInformationDataRetriever extends AbstractAsyncDataRetriever {
   private static final Log logger = LogFactory.getLog(ObjectInformationDataRetriever.class);
   private static final int UUID_BATCH_SIZE = 500;
   private List futures;
   private Set uuids;

   public ObjectInformationDataRetriever(ManagedObjectReference clusterRef, Measure measure, Set uuids, VsanClient vsanClient) {
      super(clusterRef, measure, vsanClient);
      this.uuids = uuids;
   }

   public void start() {
      List objectInfoFutures = new ArrayList();
      VsanConnection conn = this.vsanClient.getConnection(this.clusterRef.getServerGuid());
      Throwable var3 = null;

      try {
         VsanObjectSystem objectSystem = conn.getVsanObjectSystem();
         if (!this.uuids.isEmpty()) {
            List allUuids = new ArrayList(this.uuids);
            int hop = 0;
            int from = false;

            for(int to = 0; to < allUuids.size(); ++hop) {
               int from = hop * 500;
               to = Math.min((hop + 1) * 500, allUuids.size());
               Set batch = new HashSet(allUuids.subList(from, to));
               Future future = this.measure.newFuture("ObjectSystem.queryVsanObjectInformation");
               objectSystem.queryVsanObjectInformation(this.clusterRef, this.buildQuerySpecs(batch), future);
               objectInfoFutures.add(future);
            }

            logger.info("Requesting " + allUuids.size() + " UUIDs split into " + objectInfoFutures.size() + " separate calls.");
         }
      } catch (Throwable var18) {
         var3 = var18;
         throw var18;
      } finally {
         if (conn != null) {
            if (var3 != null) {
               try {
                  conn.close();
               } catch (Throwable var17) {
                  var3.addSuppressed(var17);
               }
            } else {
               conn.close();
            }
         }

      }

      this.futures = objectInfoFutures;
   }

   public VsanObjectInformation[] prepareResult() throws ExecutionException, InterruptedException {
      List objInfosList = new ArrayList();
      Iterator var2 = this.futures.iterator();

      while(var2.hasNext()) {
         Future objectInfoFuture = (Future)var2.next();
         VsanObjectInformation[] result = (VsanObjectInformation[])objectInfoFuture.get();
         if (ArrayUtils.isEmpty(result)) {
            logger.warn("Found an empty VsanObjectInformation result. Probably something is wrong with the server.");
         } else {
            objInfosList.addAll(Arrays.asList(result));
         }
      }

      return (VsanObjectInformation[])objInfosList.toArray(new VsanObjectInformation[0]);
   }

   private VsanObjectQuerySpec[] buildQuerySpecs(Set vsanObjectIds) {
      List vsanQuerySpecs = new ArrayList();
      Iterator var3 = vsanObjectIds.iterator();

      while(var3.hasNext()) {
         String objectId = (String)var3.next();
         vsanQuerySpecs.add(new VsanObjectQuerySpec(objectId, ""));
      }

      return (VsanObjectQuerySpec[])vsanQuerySpecs.toArray(new VsanObjectQuerySpec[0]);
   }
}
