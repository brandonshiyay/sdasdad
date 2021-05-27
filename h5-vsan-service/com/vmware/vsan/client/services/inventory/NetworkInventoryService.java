package com.vmware.vsan.client.services.inventory;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vim.Datacenter;
import com.vmware.vim.binding.vim.Folder;
import com.vmware.vim.binding.vim.Network;
import com.vmware.vim.binding.vim.Tag;
import com.vmware.vim.binding.vim.dvs.DistributedVirtualPortgroup;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vise.data.query.PropertyValue;
import com.vmware.vise.data.query.ResultItem;
import com.vmware.vsan.client.services.ProxygenSerializer;
import com.vmware.vsan.client.services.dataprotection.model.PscConnectionDetails;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcConnection;
import com.vmware.vsan.client.util.VmodlHelper;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NetworkInventoryService extends InventoryBrowserService {
   private static final String UPLINK_KEY = "SYSTEM/DVS.UPLINKPG";
   private static final String DVS_PROPERTY = "config.distributedVirtualSwitch";
   private static final Log logger = LogFactory.getLog(NetworkInventoryService.class);
   @Autowired
   private VcClient vcClient;
   @Autowired
   private VmodlHelper vmodlHelper;

   @TsService
   public InventoryEntryData[] getNodeInfo(ManagedObjectReference[] nodeRefs, PscConnectionDetails pscDetails) throws Exception {
      return super.getNodeInfo(nodeRefs, pscDetails);
   }

   protected Set getDataServiceProperties() {
      Set result = super.getDataServiceProperties();
      result.add("config.distributedVirtualSwitch");
      return result;
   }

   protected InventoryEntryData createDSModel(ResultItem item) {
      InventoryEntryData model = super.createDSModel(item);
      PropertyValue[] var3 = item.properties;
      int var4 = var3.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         PropertyValue prop = var3[var5];
         if (prop.propertyName.equals("config.distributedVirtualSwitch") && prop.value != null && prop.value instanceof ManagedObjectReference) {
            try {
               String dvsName = (String)QueryUtil.getProperty((ManagedObjectReference)prop.value, "name");
               model.name = model.name + " (" + dvsName + ")";
            } catch (Exception var8) {
               logger.error("Unable to get Distributed port group's DVS name!", var8);
            }
         }
      }

      return model;
   }

   @TsService
   public InventoryEntryData[] getNodeChildren(ManagedObjectReference parentRef, PscConnectionDetails pscDetails, @ProxygenSerializer.ElementType(key = FilterContextKey.class,value = ManagedObjectReference.class) Map filterContext) throws Exception {
      return super.getNodeChildren(parentRef, pscDetails, filterContext);
   }

   protected List listChildrenRefs(ManagedObjectReference parent, PscConnectionDetails pscDetails, Map filterContext) {
      PscConnectionDetails remotePscDetails = pscDetails == null ? new PscConnectionDetails() : pscDetails;
      VcConnection vcConnection = this.vcClient.getConnection(parent.getServerGuid(), remotePscDetails.toLsInfo());
      Throwable var6 = null;

      List var8;
      try {
         if (!ClusterComputeResource.class.isAssignableFrom(this.vmodlHelper.getTypeClass(parent))) {
            List var22;
            if (Datacenter.class.isAssignableFrom(this.vmodlHelper.getTypeClass(parent))) {
               parent = VmodlHelper.assignServerGuid(((Datacenter)vcConnection.createStub(Datacenter.class, parent)).getNetworkFolder(), parent.getServerGuid());
               var22 = this.filterChildren(VmodlHelper.assignServerGuid(((Folder)vcConnection.createStub(Folder.class, parent)).getChildEntity(), parent.getServerGuid()), remotePscDetails);
               return var22;
            }

            if (!Folder.class.isAssignableFrom(this.vmodlHelper.getTypeClass(parent))) {
               return Collections.emptyList();
            }

            var22 = this.filterChildren(VmodlHelper.assignServerGuid(((Folder)vcConnection.createStub(Folder.class, parent)).getChildEntity(), parent.getServerGuid()), remotePscDetails);
            return var22;
         }

         ManagedObjectReference[] networks = ((ClusterComputeResource)vcConnection.createStub(ClusterComputeResource.class, parent)).getNetwork();
         VmodlHelper.assignServerGuid(networks, parent.getServerGuid());
         var8 = this.filterChildren(networks, remotePscDetails);
      } catch (Throwable var20) {
         var6 = var20;
         throw var20;
      } finally {
         if (vcConnection != null) {
            if (var6 != null) {
               try {
                  vcConnection.close();
               } catch (Throwable var19) {
                  var6.addSuppressed(var19);
               }
            } else {
               vcConnection.close();
            }
         }

      }

      return var8;
   }

   protected boolean isLeafNode(ManagedObjectReference item) {
      return this.vmodlHelper.isOfType(item, Network.class);
   }

   private List filterChildren(ManagedObjectReference[] allChildren, PscConnectionDetails pscDetails) {
      List result = new ArrayList();
      ManagedObjectReference[] var4 = allChildren;
      int var5 = allChildren.length;

      for(int var6 = 0; var6 < var5; ++var6) {
         ManagedObjectReference childRef = var4[var6];
         if (DistributedVirtualPortgroup.class.isAssignableFrom(this.vmodlHelper.getTypeClass(childRef))) {
            VcConnection vcConnection = this.vcClient.getConnection(childRef.getServerGuid(), pscDetails.toLsInfo());
            Throwable var9 = null;

            try {
               Tag[] tags = ((DistributedVirtualPortgroup)vcConnection.createStub(DistributedVirtualPortgroup.class, childRef)).getTag();
               if (!searchTagsForKey(tags, "SYSTEM/DVS.UPLINKPG")) {
                  result.add(childRef);
               }
            } catch (Throwable var18) {
               var9 = var18;
               throw var18;
            } finally {
               if (vcConnection != null) {
                  if (var9 != null) {
                     try {
                        vcConnection.close();
                     } catch (Throwable var17) {
                        var9.addSuppressed(var17);
                     }
                  } else {
                     vcConnection.close();
                  }
               }

            }
         } else if (this.vmodlHelper.isNetworkFolder(childRef) || Network.class.isAssignableFrom(this.vmodlHelper.getTypeClass(childRef))) {
            result.add(childRef);
         }
      }

      return result;
   }

   private static boolean searchTagsForKey(Tag[] tags, String key) {
      if (!ArrayUtils.isEmpty(tags) && !StringUtils.isEmpty(key)) {
         Tag[] var2 = tags;
         int var3 = tags.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            Tag tag = var2[var4];
            if (key.equals(tag.key)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }
}
