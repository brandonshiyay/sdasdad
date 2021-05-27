package com.vmware.vsan.client.services.inventory;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vim.Datacenter;
import com.vmware.vim.binding.vim.Folder;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vsan.client.services.ProxygenSerializer;
import com.vmware.vsan.client.services.dataprotection.model.PscConnectionDetails;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcConnection;
import com.vmware.vsan.client.util.VmodlHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DatacenterInventoryService extends InventoryBrowserService {
   @Autowired
   private VcClient vcClient;
   @Autowired
   private VmodlHelper vmodlHelper;

   @TsService
   public InventoryEntryData[] getNodeInfo(ManagedObjectReference[] nodeRefs, PscConnectionDetails pscDetails) throws Exception {
      return super.getNodeInfo(nodeRefs, pscDetails);
   }

   @TsService
   public InventoryEntryData[] getNodeChildren(ManagedObjectReference parentRef, PscConnectionDetails pscDetails, @ProxygenSerializer.ElementType(key = FilterContextKey.class,value = ManagedObjectReference.class) Map filterContext) throws Exception {
      return super.getNodeChildren(parentRef, pscDetails, filterContext);
   }

   protected List listChildrenRefs(ManagedObjectReference parent, PscConnectionDetails pscDetails, Map filterContext) {
      List result = new ArrayList();
      PscConnectionDetails remotePscDetails = pscDetails == null ? new PscConnectionDetails() : pscDetails;
      VcConnection vcConn = this.vcClient.getConnection(parent.getServerGuid(), remotePscDetails.toLsInfo());
      Throwable var7 = null;

      try {
         ManagedObjectReference[] var8 = ((Folder)vcConn.createStub(Folder.class, parent)).getChildEntity();
         int var9 = var8.length;

         for(int var10 = 0; var10 < var9; ++var10) {
            ManagedObjectReference childRef = var8[var10];
            VmodlHelper.assignServerGuid(childRef, parent.getServerGuid());
            if (this.vmodlHelper.isOfType(childRef, Datacenter.class)) {
               result.add(childRef);
            } else if (this.vmodlHelper.isDatacenterFolder(childRef)) {
               result.addAll(this.listChildrenRefs(childRef, remotePscDetails, filterContext));
            }
         }
      } catch (Throwable var19) {
         var7 = var19;
         throw var19;
      } finally {
         if (vcConn != null) {
            if (var7 != null) {
               try {
                  vcConn.close();
               } catch (Throwable var18) {
                  var7.addSuppressed(var18);
               }
            } else {
               vcConn.close();
            }
         }

      }

      return result;
   }

   protected boolean isLeafNode(ManagedObjectReference item) {
      return false;
   }
}
