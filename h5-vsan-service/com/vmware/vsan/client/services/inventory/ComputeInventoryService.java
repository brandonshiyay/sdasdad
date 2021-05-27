package com.vmware.vsan.client.services.inventory;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vim.ComputeResource;
import com.vmware.vim.binding.vim.Datacenter;
import com.vmware.vim.binding.vim.Folder;
import com.vmware.vim.binding.vim.HostSystem;
import com.vmware.vim.binding.vim.ResourcePool;
import com.vmware.vim.binding.vim.HostSystem.ConnectionState;
import com.vmware.vim.binding.vim.cluster.DrsConfigInfo;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vise.data.query.PropertyValue;
import com.vmware.vise.data.query.ResultItem;
import com.vmware.vsan.client.services.ProxygenSerializer;
import com.vmware.vsan.client.services.dataprotection.model.PscConnectionDetails;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcConnection;
import com.vmware.vsan.client.util.VmodlHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ComputeInventoryService extends InventoryBrowserService {
   @Autowired
   private VcClient vcClient;
   @Autowired
   protected VmodlHelper vmodlHelper;

   @TsService
   public InventoryEntryData[] getNodeInfo(ManagedObjectReference[] nodeRefs, PscConnectionDetails pscDetails) throws Exception {
      return super.getNodeInfo(nodeRefs, pscDetails);
   }

   @TsService
   public InventoryEntryData[] getNodeChildren(ManagedObjectReference parentRef, PscConnectionDetails pscDetails, @ProxygenSerializer.ElementType(key = FilterContextKey.class,value = ManagedObjectReference.class) Map filterContext) throws Exception {
      return super.getNodeChildren(parentRef, pscDetails, filterContext);
   }

   protected boolean isLeafNode(ManagedObjectReference item) {
      return this.vmodlHelper.getTypeClass(item) == HostSystem.class;
   }

   protected List createRemoteNodeModel(List nodeRefs, PscConnectionDetails pscDetails) {
      throw new NotImplementedException("Compute inventory does not support remote VC!");
   }

   protected Set getDataServiceProperties() {
      Set result = super.getDataServiceProperties();
      result.add("configuration.drsConfig");
      result.add("runtime.connectionState");
      result.add("runtime.inMaintenanceMode");
      result.add("runtime.inQuarantineMode");
      return result;
   }

   protected InventoryEntryData createDSModel(ResultItem item) {
      InventoryEntryData model = super.createDSModel(item);
      PropertyValue[] var3 = item.properties;
      int var4 = var3.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         PropertyValue prop = var3[var5];
         boolean isInQuarantine;
         if (prop.propertyName.equals("runtime.connectionState")) {
            isInQuarantine = prop.value.toString().equals(ConnectionState.connected.toString());
            if (!isInQuarantine) {
               model.connected = false;
            }
         } else if (prop.propertyName.equals("runtime.inMaintenanceMode")) {
            isInQuarantine = (Boolean)prop.value;
            if (isInQuarantine) {
               model.connected = false;
            }
         } else if (prop.propertyName.equals("runtime.inQuarantineMode")) {
            isInQuarantine = (Boolean)prop.value;
            if (isInQuarantine) {
               model.connected = false;
            }
         } else if (prop.propertyName.equals("configuration.drsConfig")) {
            DrsConfigInfo drsConfig = (DrsConfigInfo)prop.value;
            model.isDrsEnabled = drsConfig != null ? drsConfig.enabled : false;
         }
      }

      return model;
   }

   protected List listChildrenRefs(ManagedObjectReference parentRef, PscConnectionDetails pscDetails, Map filterContext) {
      PscConnectionDetails remotePscDetails = pscDetails == null ? new PscConnectionDetails() : pscDetails;
      VcConnection vcConnection = this.vcClient.getConnection(parentRef.getServerGuid(), remotePscDetails.toLsInfo());
      Throwable var6 = null;

      List var9;
      try {
         if (Datacenter.class.isAssignableFrom(this.vmodlHelper.getTypeClass(parentRef))) {
            ManagedObjectReference hostFolderRef = ((Datacenter)vcConnection.createStub(Datacenter.class, parentRef)).getHostFolder();
            Folder hostFolder = (Folder)vcConnection.createStub(Folder.class, hostFolderRef);
            var9 = this.filterChildren(VmodlHelper.assignServerGuid(hostFolder.getChildEntity(), parentRef.getServerGuid()), remotePscDetails);
            return var9;
         }

         if (!ClusterComputeResource.class.isAssignableFrom(this.vmodlHelper.getTypeClass(parentRef))) {
            if (ComputeResource.class.isAssignableFrom(this.vmodlHelper.getTypeClass(parentRef))) {
               ComputeResource host = (ComputeResource)vcConnection.createStub(ComputeResource.class, parentRef);
               ResourcePool pool = (ResourcePool)vcConnection.createStub(ResourcePool.class, host.getResourcePool());
               var9 = this.filterChildren(VmodlHelper.assignServerGuid(pool.getResourcePool(), parentRef.getServerGuid()), remotePscDetails);
               return var9;
            }

            if (!Folder.class.isAssignableFrom(this.vmodlHelper.getTypeClass(parentRef))) {
               return new ArrayList();
            }

            Folder folder = (Folder)vcConnection.createStub(Folder.class, parentRef);
            List var27 = this.filterChildren(VmodlHelper.assignServerGuid(folder.getChildEntity(), parentRef.getServerGuid()), remotePscDetails);
            return var27;
         }

         ClusterComputeResource cluster = (ClusterComputeResource)vcConnection.createStub(ClusterComputeResource.class, parentRef);
         ManagedObjectReference[] resourcePools = ((ResourcePool)vcConnection.createStub(ResourcePool.class, cluster.getResourcePool())).getResourcePool();
         var9 = this.filterChildren(VmodlHelper.assignServerGuid((ManagedObjectReference[])ArrayUtils.addAll(resourcePools, cluster.getHost()), parentRef.getServerGuid()), remotePscDetails);
      } catch (Throwable var22) {
         var6 = var22;
         throw var22;
      } finally {
         if (vcConnection != null) {
            if (var6 != null) {
               try {
                  vcConnection.close();
               } catch (Throwable var21) {
                  var6.addSuppressed(var21);
               }
            } else {
               vcConnection.close();
            }
         }

      }

      return var9;
   }

   protected boolean isTypeSupported(ManagedObjectReference ref) {
      return this.vmodlHelper.isOfType(ref, HostSystem.class) || this.vmodlHelper.isOfType(ref, ClusterComputeResource.class) || this.vmodlHelper.isOfType(ref, ResourcePool.class) || this.vmodlHelper.isHostFolder(ref);
   }

   private List filterChildren(ManagedObjectReference[] allChildren, PscConnectionDetails pscDetails) {
      List result = new ArrayList();
      ManagedObjectReference[] var4 = allChildren;
      int var5 = allChildren.length;

      for(int var6 = 0; var6 < var5; ++var6) {
         ManagedObjectReference childRef = var4[var6];
         if (this.isTypeSupported(childRef)) {
            result.add(childRef);
         } else if (this.vmodlHelper.isOfType(childRef, ComputeResource.class)) {
            VcConnection vcConnection = this.vcClient.getConnection(childRef.getServerGuid(), pscDetails.toLsInfo());
            Throwable var9 = null;

            try {
               ComputeResource computeResource = (ComputeResource)vcConnection.createStub(ComputeResource.class, childRef);
               ManagedObjectReference[] var11 = computeResource.getHost();
               int var12 = var11.length;

               for(int var13 = 0; var13 < var12; ++var13) {
                  ManagedObjectReference hostRef = var11[var13];
                  result.add(VmodlHelper.assignServerGuid(hostRef, childRef.getServerGuid()));
               }
            } catch (Throwable var22) {
               var9 = var22;
               throw var22;
            } finally {
               if (vcConnection != null) {
                  if (var9 != null) {
                     try {
                        vcConnection.close();
                     } catch (Throwable var21) {
                        var9.addSuppressed(var21);
                     }
                  } else {
                     vcConnection.close();
                  }
               }

            }
         }
      }

      return result;
   }
}
