package com.vmware.vsan.client.services.inventory;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vim.Folder;
import com.vmware.vim.binding.vim.VirtualMachine;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vise.data.query.RequestSpec;
import com.vmware.vsan.client.services.ProxygenSerializer;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.dataprotection.model.PscConnectionDetails;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcConnection;
import com.vmware.vsan.client.util.VmodlHelper;
import com.vmware.vsan.client.util.VsanInventoryHelper;
import com.vmware.vsan.client.util.dataservice.query.QueryBuilder;
import com.vmware.vsan.client.util.dataservice.query.QueryExecutor;
import com.vmware.vsan.client.util.dataservice.query.QueryExecutorResult;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class VmPerformanceInventoryService extends InventoryBrowserService {
   @Autowired
   private VcClient vcClient;
   @Autowired
   private VmodlHelper vmodlHelper;
   @Autowired
   private VsanInventoryHelper inventoryHelper;
   @Autowired
   private QueryExecutor queryExecutor;

   @TsService
   public InventoryEntryData[] getNodeInfo(ManagedObjectReference[] nodeRefs, PscConnectionDetails pscDetails) throws Exception {
      if (nodeRefs.length == 1 && this.vmodlHelper.isOfType(nodeRefs[0], ClusterComputeResource.class)) {
         ManagedObjectReference clusterRef = nodeRefs[0];
         VcConnection vcConnection = this.vcClient.getConnection(clusterRef.getServerGuid());
         Throwable var5 = null;

         InventoryEntryData[] var9;
         try {
            ManagedObjectReference vmFolderRef = this.inventoryHelper.getVmFolderOfDataCenter(clusterRef);
            ManagedObjectReference[] vmFolderChildren = VmodlHelper.assignServerGuid(((Folder)vcConnection.createStub(Folder.class, vmFolderRef)).getChildEntity(), clusterRef.getServerGuid());
            ManagedObjectReference[] filteredChildren = (ManagedObjectReference[])this.filterChildren(vmFolderChildren, clusterRef).toArray(new ManagedObjectReference[0]);
            var9 = super.getNodeInfo(filteredChildren, pscDetails);
         } catch (Throwable var18) {
            var5 = var18;
            throw var18;
         } finally {
            if (vcConnection != null) {
               if (var5 != null) {
                  try {
                     vcConnection.close();
                  } catch (Throwable var17) {
                     var5.addSuppressed(var17);
                  }
               } else {
                  vcConnection.close();
               }
            }

         }

         return var9;
      } else {
         return super.getNodeInfo(nodeRefs, pscDetails);
      }
   }

   @TsService
   public InventoryEntryData[] getNodeChildren(ManagedObjectReference parentRef, PscConnectionDetails pscDetails, @ProxygenSerializer.ElementType(key = FilterContextKey.class,value = ManagedObjectReference.class) Map filterContext) throws Exception {
      return super.getNodeChildren(parentRef, pscDetails, filterContext);
   }

   protected List listChildrenRefs(ManagedObjectReference parent, PscConnectionDetails pscDetails, Map filterContext) {
      VcConnection vcConnection = this.vcClient.getConnection(parent.getServerGuid());
      Throwable var5 = null;

      try {
         if (this.vmodlHelper.isOfType(parent, Folder.class)) {
            ManagedObjectReference contextRef = (ManagedObjectReference)filterContext.get(FilterContextKey.CONTEXT_REF);
            ManagedObjectReference[] children = VmodlHelper.assignServerGuid(((Folder)vcConnection.createStub(Folder.class, parent)).getChildEntity(), contextRef.getServerGuid());
            List var8 = this.filterChildren(children, contextRef);
            return var8;
         }
      } catch (Throwable var18) {
         var5 = var18;
         throw var18;
      } finally {
         if (vcConnection != null) {
            if (var5 != null) {
               try {
                  vcConnection.close();
               } catch (Throwable var17) {
                  var5.addSuppressed(var17);
               }
            } else {
               vcConnection.close();
            }
         }

      }

      return Collections.emptyList();
   }

   private List filterChildren(ManagedObjectReference[] allChildren, ManagedObjectReference clusterRef) {
      if (ArrayUtils.isEmpty(allChildren)) {
         return Collections.emptyList();
      } else {
         try {
            RequestSpec requestSpec = (new QueryBuilder()).newQuery().select().from(clusterRef).join(VirtualMachine.class).on("vm").where().propertyEquals("config.template", false).end().build();
            QueryExecutorResult result = this.queryExecutor.execute(requestSpec);
            Set clusterVms = result.getQueryResult().getResourceObjects();
            return (List)Arrays.stream(allChildren).filter((moRef) -> {
               return this.isRunningOnCluster(moRef, clusterVms);
            }).collect(Collectors.toList());
         } catch (Exception var6) {
            throw new VsanUiLocalizableException("vsan.vmPerformanceInventoryService.error", var6);
         }
      }
   }

   private boolean isRunningOnCluster(ManagedObjectReference moRef, Set clusterVms) {
      return this.vmodlHelper.isOfType(moRef, VirtualMachine.class) ? clusterVms.contains(moRef) : true;
   }

   protected boolean isLeafNode(ManagedObjectReference item) {
      return this.vmodlHelper.isOfType(item, VirtualMachine.class);
   }
}
