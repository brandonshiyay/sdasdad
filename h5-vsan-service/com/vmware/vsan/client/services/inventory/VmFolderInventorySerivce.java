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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class VmFolderInventorySerivce extends InventoryBrowserService {
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
      PscConnectionDetails remotePscDetails = pscDetails == null ? new PscConnectionDetails() : pscDetails;
      VcConnection vcConnection = this.vcClient.getConnection(parent.getServerGuid(), remotePscDetails.toLsInfo());
      Throwable var6 = null;

      try {
         if (Datacenter.class.isAssignableFrom(this.vmodlHelper.getTypeClass(parent))) {
            parent = VmodlHelper.assignServerGuid(((Datacenter)vcConnection.createStub(Datacenter.class, parent)).getVmFolder(), parent.getServerGuid());
         }

         if (Folder.class.isAssignableFrom(this.vmodlHelper.getTypeClass(parent))) {
            ManagedObjectReference[] children = ((Folder)vcConnection.createStub(Folder.class, parent)).getChildEntity();
            List var8 = this.filterChildren(VmodlHelper.assignServerGuid(children, parent.getServerGuid()));
            return var8;
         }
      } catch (Throwable var18) {
         var6 = var18;
         throw var18;
      } finally {
         if (vcConnection != null) {
            if (var6 != null) {
               try {
                  vcConnection.close();
               } catch (Throwable var17) {
                  var6.addSuppressed(var17);
               }
            } else {
               vcConnection.close();
            }
         }

      }

      return Collections.emptyList();
   }

   protected boolean isLeafNode(ManagedObjectReference item) {
      return false;
   }

   private List filterChildren(ManagedObjectReference[] allChildren) {
      List result = new ArrayList();
      ManagedObjectReference[] var3 = allChildren;
      int var4 = allChildren.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         ManagedObjectReference childRef = var3[var5];
         if (this.vmodlHelper.isVmFolder(childRef)) {
            result.add(childRef);
         }
      }

      return result;
   }
}
