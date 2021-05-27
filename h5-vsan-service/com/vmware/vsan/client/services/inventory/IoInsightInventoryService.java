package com.vmware.vsan.client.services.inventory;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vim.HostSystem;
import com.vmware.vim.binding.vim.VirtualMachine;
import com.vmware.vim.binding.vim.HostSystem.ConnectionState;
import com.vmware.vim.binding.vim.VirtualMachine.PowerState;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vise.data.query.ResultItem;
import com.vmware.vsan.client.services.ProxygenSerializer;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.services.dataprotection.model.PscConnectionDetails;
import com.vmware.vsan.client.services.ioinsight.IoInsightService;
import com.vmware.vsan.client.services.ioinsight.model.IoInsightInstance;
import com.vmware.vsan.client.services.ioinsight.model.IoInsightRunningState;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcConnection;
import com.vmware.vsan.client.util.VmodlHelper;
import com.vmware.vsan.client.util.VsanInventoryHelper;
import com.vmware.vsan.client.util.dataservice.query.DataServiceHelper;
import com.vmware.vsphere.client.vsan.base.util.BaseUtils;
import com.vmware.vsphere.client.vsan.util.DataServiceResponse;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import com.vmware.vsphere.client.vsan.util.Utils;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class IoInsightInventoryService extends InventoryBrowserService {
   private static final Log logger = LogFactory.getLog(IoInsightInventoryService.class);
   @Autowired
   private VcClient vcClient;
   @Autowired
   private VmodlHelper vmodlHelper;
   @Autowired
   private VsanInventoryHelper vsanInventoryHelper;
   @Autowired
   private IoInsightService ioInsightService;
   @Autowired
   private DataServiceHelper dataServiceHelper;

   @TsService
   public InventoryEntryData[] getNodeInfo(ManagedObjectReference[] nodeRefs, PscConnectionDetails pscDetails) throws Exception {
      if (ArrayUtils.isEmpty(nodeRefs)) {
         return new InventoryEntryData[0];
      } else {
         ManagedObjectReference clusterRef = nodeRefs[0];
         if (nodeRefs.length > 1 && this.vmodlHelper.isOfType(clusterRef, ClusterComputeResource.class)) {
            logger.error("IOInsight Inventory Service does not support multiple clusters.");
            throw new VsanUiLocalizableException("vsan.ioInsight.inventoryservice.notsupported.error");
         } else if (nodeRefs.length == 1 && this.vmodlHelper.isOfType(clusterRef, ClusterComputeResource.class)) {
            VcConnection vcConnection = this.vcClient.getConnection(clusterRef.getServerGuid());
            Throwable var5 = null;

            InventoryEntryData[] var8;
            try {
               ClusterComputeResource cluster = (ClusterComputeResource)vcConnection.createStub(ClusterComputeResource.class, clusterRef);
               ManagedObjectReference[] hostRefs = cluster.getHost();
               var8 = super.getNodeInfo(VmodlHelper.assignServerGuid(hostRefs, clusterRef.getServerGuid()), pscDetails);
            } catch (Throwable var17) {
               var5 = var17;
               throw var17;
            } finally {
               if (vcConnection != null) {
                  if (var5 != null) {
                     try {
                        vcConnection.close();
                     } catch (Throwable var16) {
                        var5.addSuppressed(var16);
                     }
                  } else {
                     vcConnection.close();
                  }
               }

            }

            return var8;
         } else {
            return super.getNodeInfo(nodeRefs, pscDetails);
         }
      }
   }

   protected Object collectData(List nodeRefs) {
      ManagedObjectReference clusterRef = BaseUtils.getCluster((ManagedObjectReference)nodeRefs.get(0));
      VcConnection vcConnection = this.vcClient.getConnection(clusterRef.getServerGuid());
      Throwable var4 = null;

      List var6;
      try {
         ClusterComputeResource cluster = (ClusterComputeResource)vcConnection.createStub(ClusterComputeResource.class, clusterRef);
         var6 = this.ioInsightService.getIoInsightInstances(clusterRef);
      } catch (Throwable var15) {
         var4 = var15;
         throw var15;
      } finally {
         if (vcConnection != null) {
            if (var4 != null) {
               try {
                  vcConnection.close();
               } catch (Throwable var14) {
                  var4.addSuppressed(var14);
               }
            } else {
               vcConnection.close();
            }
         }

      }

      return var6;
   }

   @TsService
   public InventoryEntryData[] getNodeChildren(ManagedObjectReference parentRef, PscConnectionDetails pscDetails, @ProxygenSerializer.ElementType(key = FilterContextKey.class,value = ManagedObjectReference.class) Map filterContext) throws Exception {
      return super.getNodeChildren(parentRef, pscDetails, filterContext);
   }

   protected List listChildrenRefs(ManagedObjectReference parent, PscConnectionDetails pscDetails, Map filterContext) {
      if (!this.vmodlHelper.isOfType(parent, HostSystem.class)) {
         return Collections.emptyList();
      } else {
         try {
            return this.dataServiceHelper.getVmsOnlyWithoutTemplates(parent);
         } catch (Exception var5) {
            logger.error("Failed to retrieve host's VMs without templates", var5);
            return Collections.emptyList();
         }
      }
   }

   protected InventoryEntryData createDSModel(ResultItem item, Object customData) {
      InventoryEntryData model = super.createDSModel(item);
      if (this.vmodlHelper.isOfType(model.nodeRef, HostSystem.class)) {
         this.updateHostDSModel(model, customData);
      } else {
         this.updateVmDSModel(model);
      }

      return model;
   }

   private void updateHostDSModel(InventoryEntryData model, Object customData) {
      boolean isHostConnected;
      boolean isHostInMmode;
      try {
         DataServiceResponse hostProperties = QueryUtil.getProperties(model.nodeRef, new String[]{"runtime.connectionState", "runtime.inMaintenanceMode"});
         ConnectionState hostConnectionState = (ConnectionState)hostProperties.getProperty(model.nodeRef, "runtime.connectionState");
         isHostConnected = ConnectionState.connected.equals(hostConnectionState);
         isHostInMmode = (Boolean)hostProperties.getProperty(model.nodeRef, "runtime.inMaintenanceMode");
      } catch (Exception var11) {
         isHostConnected = false;
         isHostInMmode = false;
      }

      if (isHostConnected && !isHostInMmode) {
         boolean isIoInsightSupported = VsanCapabilityUtils.isIoInsightSupported(model.nodeRef);
         List allInstances = (List)customData;
         boolean isIoInsightRunning = this.hasRunningIoInsightInstance(allInstances, model.nodeRef);
         boolean allHostVMsPoweredOff = true;
         if (isIoInsightSupported && !isIoInsightRunning) {
            try {
               allHostVMsPoweredOff = this.dataServiceHelper.isAllHostVMsPoweredOff(model.nodeRef);
            } catch (Exception var10) {
               logger.warn("Failed to retrieve power state of host's VMs. Falling back to powered off state.", var10);
            }
         }

         model.isDisabled = !isIoInsightSupported || isIoInsightRunning || allHostVMsPoweredOff;
         model.isLeafNode = model.isDisabled;
         if (!isIoInsightSupported) {
            model.name = Utils.getLocalizedString("vsan.ioInsight.model.name.notSupported", model.name);
         } else if (isIoInsightRunning) {
            model.name = Utils.getLocalizedString("vsan.ioInsight.model.name.running", model.name);
         } else if (allHostVMsPoweredOff) {
            model.name = Utils.getLocalizedString("vsan.ioInsight.model.name.poweredOffVms", model.name);
         }

      } else {
         model.name = !isHostConnected ? Utils.getLocalizedString("vsan.ioInsight.model.name.disconnectedOrNotResponding", model.name) : Utils.getLocalizedString("vsan.ioInsight.model.name.mmodeHost", model.name);
         model.isDisabled = true;
         model.isLeafNode = true;
      }
   }

   private boolean hasRunningIoInsightInstance(List allInstances, ManagedObjectReference hostRef) {
      return allInstances.stream().filter((instance) -> {
         return this.isHostInstance(instance, hostRef);
      }).anyMatch((instance) -> {
         return instance.state == IoInsightRunningState.RUNNING;
      });
   }

   private boolean isHostInstance(IoInsightInstance instance, ManagedObjectReference hostRef) {
      return instance.hostIoInsightInfos.stream().anyMatch((hostIoInsightInfo) -> {
         return hostIoInsightInfo.host.moRef.equals(hostRef);
      });
   }

   private void updateVmDSModel(InventoryEntryData model) {
      boolean isVmPoweredOn = this.isVmPoweredOn(model.nodeRef);
      boolean isVmOnVsanDatastore = false;

      try {
         isVmOnVsanDatastore = this.vsanInventoryHelper.getVsanDatastore(model.nodeRef) != null;
      } catch (Exception var5) {
         logger.error(var5);
      }

      if (!isVmOnVsanDatastore) {
         model.name = Utils.getLocalizedString("vsan.ioInsight.model.name.nonvSanVM", model.name);
      } else if (!isVmPoweredOn) {
         model.name = Utils.getLocalizedString("vsan.ioInsight.model.name.poweredOffVm", model.name);
      }

      model.isDisabled = !isVmPoweredOn || !isVmOnVsanDatastore;
   }

   private boolean isVmPoweredOn(ManagedObjectReference vmRef) {
      try {
         return QueryUtil.getProperty(vmRef, "powerState") == PowerState.poweredOn;
      } catch (Exception var3) {
         logger.warn("Failed to retrieve virtual machine power state. Falling back to powered off state.", var3);
         return false;
      }
   }

   protected boolean isLeafNode(ManagedObjectReference item) {
      return this.vmodlHelper.isOfType(item, VirtualMachine.class);
   }

   protected List createRemoteNodeModel(List nodeRefs, PscConnectionDetails pscDetails) {
      throw new NotImplementedException("IOInsight inventory does not support remote VC!");
   }
}
