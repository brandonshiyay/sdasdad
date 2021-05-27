package com.vmware.vsan.client.services.inventory;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vim.ManagedEntity;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vise.data.query.PropertyValue;
import com.vmware.vise.data.query.QuerySpec;
import com.vmware.vise.data.query.ResultItem;
import com.vmware.vise.data.query.ResultSet;
import com.vmware.vsan.client.services.ProxygenSerializer;
import com.vmware.vsan.client.services.dataprotection.model.PscConnectionDetails;
import com.vmware.vsan.client.sessionmanager.vlsi.client.explorer.VcLsExplorer;
import com.vmware.vsan.client.sessionmanager.vlsi.client.explorer.VcRegistration;
import com.vmware.vsan.client.sessionmanager.vlsi.client.ls.LookupSvcClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.ls.LookupSvcConnection;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcConnection;
import com.vmware.vsan.client.util.VmodlHelper;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class InventoryBrowserService {
   @Autowired
   private VcClient vcClient;
   @Autowired
   private VmodlHelper vmodlHelper;
   @Autowired
   private LookupSvcClient lsClient;

   @TsService
   public InventoryEntryData[] getNodeInfo(ManagedObjectReference[] nodeRefs, PscConnectionDetails pscDetails) throws Exception {
      if (ArrayUtils.isEmpty(nodeRefs)) {
         return new InventoryEntryData[0];
      } else {
         List result = null;
         if (pscDetails == null) {
            result = this.createLocalNodeModel(Arrays.asList(nodeRefs));
         } else if (this.vmodlHelper.isVcRootFolder(nodeRefs[0])) {
            result = this.createRemoteVcModel(Arrays.asList(nodeRefs), pscDetails);
         } else {
            result = this.createRemoteNodeModel(Arrays.asList(nodeRefs), pscDetails);
         }

         return (InventoryEntryData[])result.toArray(new InventoryEntryData[result.size()]);
      }
   }

   @TsService
   public InventoryEntryData[] getNodeChildren(ManagedObjectReference parentNode, PscConnectionDetails pscDetails, @ProxygenSerializer.ElementType(key = FilterContextKey.class,value = ManagedObjectReference.class) Map filterContext) throws Exception {
      List childrenRefs = this.listChildrenRefs(parentNode, pscDetails, filterContext);
      return CollectionUtils.isEmpty(childrenRefs) ? new InventoryEntryData[0] : this.getNodeInfo((ManagedObjectReference[])childrenRefs.toArray(new ManagedObjectReference[0]), pscDetails);
   }

   protected List createLocalNodeModel(List nodeRefs) throws Exception {
      Set dsProps = this.getDataServiceProperties();
      QuerySpec querySpec = QueryUtil.buildQuerySpec((ManagedObjectReference[])nodeRefs.toArray(new ManagedObjectReference[0]), (String[])dsProps.toArray(new String[0]));
      ResultSet response = QueryUtil.getData(querySpec);
      Object customData = this.collectData(nodeRefs);
      List result = new ArrayList();
      ResultItem[] var7 = response.items;
      int var8 = var7.length;

      for(int var9 = 0; var9 < var8; ++var9) {
         ResultItem item = var7[var9];
         InventoryEntryData model = this.createDSModel(item, customData);
         if (model != null) {
            result.add(model);
         }
      }

      return result;
   }

   protected Object collectData(List nodeRefs) {
      return null;
   }

   protected InventoryEntryData createDSModel(ResultItem item, Object customData) {
      return this.createDSModel(item);
   }

   protected InventoryEntryData createDSModel(ResultItem item) {
      InventoryEntryData model = new InventoryEntryData();
      model.nodeRef = (ManagedObjectReference)item.resourceObject;
      model.connected = true;
      PropertyValue[] var3 = item.properties;
      int var4 = var3.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         PropertyValue prop = var3[var5];
         if (prop.propertyName.equals("name")) {
            model.name = prop.value + "";
         } else if (prop.propertyName.equals("primaryIconId")) {
            model.iconShape = prop.value + "";
         }
      }

      model.isLeafNode = this.isLeafNode(model.nodeRef);
      return model;
   }

   protected List createRemoteNodeModel(List nodeRefs, PscConnectionDetails pscDetails) {
      List result = new ArrayList();
      Iterator var4 = nodeRefs.iterator();

      while(var4.hasNext()) {
         ManagedObjectReference nodeRef = (ManagedObjectReference)var4.next();
         VcConnection vcConnection = this.vcClient.getConnection(nodeRef.getServerGuid(), pscDetails.toLsInfo());
         Throwable var7 = null;

         try {
            ManagedEntity managedObject = (ManagedEntity)vcConnection.createStub(ManagedEntity.class, nodeRef);
            InventoryEntryData model = new InventoryEntryData();
            model.nodeRef = nodeRef;
            model.name = managedObject.getName();
            model.isLeafNode = this.isLeafNode(nodeRef);
            model.iconShape = this.getDefaultIcon(nodeRef);
            result.add(model);
         } catch (Throwable var17) {
            var7 = var17;
            throw var17;
         } finally {
            if (vcConnection != null) {
               if (var7 != null) {
                  try {
                     vcConnection.close();
                  } catch (Throwable var16) {
                     var7.addSuppressed(var16);
                  }
               } else {
                  vcConnection.close();
               }
            }

         }
      }

      return result;
   }

   private List createRemoteVcModel(List nodeRefs, PscConnectionDetails pscDetails) {
      List result = new ArrayList();
      LookupSvcConnection lsConn = this.lsClient.getConnection(pscDetails.toLsInfo());
      Throwable var5 = null;

      try {
         Map vcRegistrations = (new VcLsExplorer(lsConn.getServiceRegistration())).map();
         Iterator var7 = nodeRefs.iterator();

         while(var7.hasNext()) {
            ManagedObjectReference nodeRef = (ManagedObjectReference)var7.next();
            InventoryEntryData model = new InventoryEntryData();
            VcRegistration vcRegistration = (VcRegistration)vcRegistrations.get(UUID.fromString(nodeRef.getServerGuid()));
            model.name = vcRegistration.getVcName();
            model.nodeRef = nodeRef;
            model.isLeafNode = this.isLeafNode(nodeRef);
            model.iconShape = this.getDefaultIcon(nodeRef);
            result.add(model);
         }
      } catch (Throwable var18) {
         var5 = var18;
         throw var18;
      } finally {
         if (lsConn != null) {
            if (var5 != null) {
               try {
                  lsConn.close();
               } catch (Throwable var17) {
                  var5.addSuppressed(var17);
               }
            } else {
               lsConn.close();
            }
         }

      }

      return result;
   }

   protected abstract List listChildrenRefs(ManagedObjectReference var1, PscConnectionDetails var2, Map var3);

   protected Set getDataServiceProperties() {
      Set result = new HashSet();
      result.add("name");
      result.add("primaryIconId");
      return result;
   }

   protected abstract boolean isLeafNode(ManagedObjectReference var1);

   private String getDefaultIcon(ManagedObjectReference objRef) {
      if (this.vmodlHelper.isVcRootFolder(objRef)) {
         return "vsphere-icon-vcenter";
      } else {
         String var2 = objRef.getType();
         byte var3 = -1;
         switch(var2.hashCode()) {
         case -1450678229:
            if (var2.equals("ClusterComputeResource")) {
               var3 = 1;
            }
            break;
         case -786828786:
            if (var2.equals("Network")) {
               var3 = 6;
            }
            break;
         case -740059428:
            if (var2.equals("VirtualMachine")) {
               var3 = 4;
            }
            break;
         case -580407137:
            if (var2.equals("Datacenter")) {
               var3 = 0;
            }
            break;
         case 894390807:
            if (var2.equals("HostSystem")) {
               var3 = 2;
            }
            break;
         case 1659069271:
            if (var2.equals("Datastore")) {
               var3 = 3;
            }
            break;
         case 2109868174:
            if (var2.equals("Folder")) {
               var3 = 5;
            }
         }

         switch(var3) {
         case 0:
            return "vsphere-icon-datacenter";
         case 1:
            return "vsphere-icon-cluster";
         case 2:
            return "vsphere-icon-host";
         case 3:
            return "vsphere-icon-datastore";
         case 4:
            return "vsphere-icon-vm";
         case 5:
            return "vsphere-icon-folder";
         case 6:
            return "vsphere-icon-network";
         default:
            return "info";
         }
      }
   }
}
