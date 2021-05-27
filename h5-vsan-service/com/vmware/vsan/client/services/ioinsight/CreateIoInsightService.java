package com.vmware.vsan.client.services.ioinsight;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vim.HostSystem;
import com.vmware.vim.binding.vim.VirtualMachine;
import com.vmware.vim.binding.vim.HostSystem.ConnectionState;
import com.vmware.vim.binding.vim.VirtualMachine.PowerState;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanIoInsightManager;
import com.vmware.vise.data.query.RequestSpec;
import com.vmware.vise.data.query.ResultSet;
import com.vmware.vsan.client.services.BackendLocalizedException;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.common.data.BasicVmData;
import com.vmware.vsan.client.services.inventory.InventoryNode;
import com.vmware.vsan.client.services.ioinsight.model.HostIoInsightInfo;
import com.vmware.vsan.client.services.ioinsight.model.IoInsightInstance;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsan.client.util.VmodlHelper;
import com.vmware.vsan.client.util.dataservice.query.QueryBuilder;
import com.vmware.vsphere.client.vsan.util.DataServiceResponse;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import com.vmware.vsphere.client.vsan.util.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CreateIoInsightService {
   private static final Logger logger = LoggerFactory.getLogger(CreateIoInsightService.class);
   private static final String HOST_CLUSTER_NAME_PROPERTY = "hostClusterName";
   @Autowired
   private VsanClient vsanClient;
   @Autowired
   private IoInsightService ioInsightService;
   @Autowired
   private VmodlHelper vmodlHelper;

   @TsService
   public List getHostVms(ManagedObjectReference[] hostRefs) {
      return ArrayUtils.isEmpty(hostRefs) ? Collections.emptyList() : getPoweredOnVmsOnlyWithoutTemplates(Arrays.asList(hostRefs));
   }

   public static List getPoweredOnVmsOnlyWithoutTemplates(List hostRefs) {
      String[] properties = new String[]{"name", "primaryIconId", "summary.runtime.host", "config.template", "powerState"};
      RequestSpec requestSpec = (new QueryBuilder()).newQuery().select(properties).from((Collection)hostRefs).join(VirtualMachine.class).on("vm").where().propertyEquals("config.template", false).end().build();
      ArrayList result = new ArrayList();

      try {
         ResultSet[] resultSet = QueryUtil.getDataMultiSpec(requestSpec.querySpec);
         DataServiceResponse response = QueryUtil.getDataServiceResponse(resultSet[0], properties);
         Iterator var6 = response.getResourceObjects().iterator();

         while(var6.hasNext()) {
            ManagedObjectReference resourceObject = (ManagedObjectReference)var6.next();
            if (response.getProperty(resourceObject, "powerState") == PowerState.poweredOn) {
               BasicVmData vmData = new BasicVmData(resourceObject);
               vmData.name = (String)response.getProperty(resourceObject, "name");
               vmData.primaryIconId = (String)response.getProperty(resourceObject, "primaryIconId");
               vmData.hostRef = (ManagedObjectReference)response.getProperty(resourceObject, "summary.runtime.host");
               result.add(vmData);
            }
         }

         return result;
      } catch (Exception var9) {
         logger.error("Failed to retrieve hosts' vms: ", var9);
         throw new VsanUiLocalizableException();
      }
   }

   @TsService
   public InventoryNode getClusterObject(ManagedObjectReference objectRef) {
      try {
         return this.vmodlHelper.isOfType(objectRef, VirtualMachine.class) ? this.getClusterObjectForVM(objectRef) : this.getClusterObjectForHostAndCluster(objectRef);
      } catch (Exception var3) {
         logger.error("Failed to retrieve cluster's managed object ref and cluster's name: ", var3);
         throw new VsanUiLocalizableException();
      }
   }

   private InventoryNode getClusterObjectForHostAndCluster(ManagedObjectReference objectRef) throws Exception {
      String[] properties = this.vmodlHelper.isOfType(objectRef, HostSystem.class) ? new String[]{"hostClusterName", "cluster", "primaryIconId"} : new String[]{"name", "primaryIconId"};
      DataServiceResponse response = QueryUtil.getProperties(objectRef, properties);
      InventoryNode result = null;
      if (!response.getResourceObjects().isEmpty()) {
         ManagedObjectReference resourceObject = (ManagedObjectReference)response.getResourceObjects().iterator().next();
         String clusterName = (String)response.getProperty(resourceObject, properties[0]);
         ManagedObjectReference clusterObject = this.vmodlHelper.isOfType(objectRef, HostSystem.class) ? (ManagedObjectReference)response.getProperty(resourceObject, properties[1]) : objectRef;
         String primaryIcon = (String)response.getProperty(resourceObject, "primaryIconId");
         result = new InventoryNode(clusterObject, clusterName, primaryIcon);
      }

      return result;
   }

   private InventoryNode getClusterObjectForVM(ManagedObjectReference vmRef) throws Exception {
      DataServiceResponse response = QueryUtil.getPropertiesForRelatedObjects(vmRef, "cluster", ClusterComputeResource.class.getSimpleName(), new String[]{"name", "primaryIconId"});
      InventoryNode result = null;
      if (!response.getResourceObjects().isEmpty()) {
         ManagedObjectReference clusterObject = (ManagedObjectReference)response.getResourceObjects().iterator().next();
         String clusterName = (String)response.getProperty(clusterObject, "name");
         String primaryIcon = (String)response.getProperty(clusterObject, "primaryIconId");
         result = new InventoryNode(clusterObject, clusterName, primaryIcon);
      }

      return result;
   }

   @TsService
   public ManagedObjectReference getVmHostRef(ManagedObjectReference objectRef) {
      if (!this.vmodlHelper.isOfType(objectRef, VirtualMachine.class)) {
         return null;
      } else {
         try {
            return (ManagedObjectReference)QueryUtil.getProperty(objectRef, "summary.runtime.host");
         } catch (Exception var3) {
            logger.error("Failed to retrieve vm's parent host object ref: ", var3);
            throw new VsanUiLocalizableException();
         }
      }
   }

   @TsService
   public List getIsInstanceNameValid(ManagedObjectReference clusterRef, String instanceName) {
      List errorMessages = new ArrayList();
      if (StringUtils.isBlank(instanceName)) {
         errorMessages.add(Utils.getLocalizedString("vsan.ioInsight.create.noInstanceName.error"));
      }

      boolean isDuplicated = this.isInstanceNameDuplicated(clusterRef, instanceName);
      if (isDuplicated) {
         errorMessages.add(Utils.getLocalizedString("vsan.ioInsight.create.duplicateInstanceName.error"));
      }

      return errorMessages.size() > 0 ? errorMessages : null;
   }

   private boolean isInstanceNameDuplicated(ManagedObjectReference clusterRef, String instanceName) {
      List instances = this.ioInsightService.getIoInsightInstances(clusterRef);
      return instances.stream().anyMatch((instance) -> {
         return instance.name.equals(instanceName);
      });
   }

   @TsService
   public ManagedObjectReference createIoInsightInstance(ManagedObjectReference clusterRef, ManagedObjectReference[] hosts, ManagedObjectReference[] vms, String instanceName, long durationSeconds) {
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var8 = null;

      ManagedObjectReference var13;
      try {
         VsanIoInsightManager vsanIoInsightManager = conn.getVsanIoInsightManager();

         try {
            Measure measure = new Measure("VsanIoInsightManager.startIoInsight");
            Throwable var11 = null;

            try {
               ManagedObjectReference createIoInsightTask = vsanIoInsightManager.startIoInsight(clusterRef, instanceName, durationSeconds, hosts, vms);
               var13 = VmodlHelper.assignServerGuid(createIoInsightTask, clusterRef.getServerGuid());
            } catch (Throwable var38) {
               var11 = var38;
               throw var38;
            } finally {
               if (measure != null) {
                  if (var11 != null) {
                     try {
                        measure.close();
                     } catch (Throwable var37) {
                        var11.addSuppressed(var37);
                     }
                  } else {
                     measure.close();
                  }
               }

            }
         } catch (Exception var40) {
            throw new BackendLocalizedException(var40);
         }
      } catch (Throwable var41) {
         var8 = var41;
         throw var41;
      } finally {
         if (conn != null) {
            if (var8 != null) {
               try {
                  conn.close();
               } catch (Throwable var36) {
                  var8.addSuppressed(var36);
               }
            } else {
               conn.close();
            }
         }

      }

      return var13;
   }

   @TsService
   public boolean areTargetsValid(IoInsightInstance instance, ManagedObjectReference moRef) {
      if (instance.hostIoInsightInfos == null) {
         return true;
      } else {
         IoInsightInstance fullInstance = this.ioInsightService.getIoInsightInstanceInfo(instance, moRef);
         List targetHosts = new ArrayList();
         List targetVms = new ArrayList();
         Iterator var6 = fullInstance.hostIoInsightInfos.iterator();

         while(var6.hasNext()) {
            HostIoInsightInfo hostIoInsightInfo = (HostIoInsightInfo)var6.next();
            targetHosts.add(hostIoInsightInfo.host.moRef);
            if (hostIoInsightInfo.monitoredVms != null) {
               targetVms.addAll(hostIoInsightInfo.monitoredVms);
            }
         }

         DataServiceResponse hostsPropResponse;
         try {
            hostsPropResponse = QueryUtil.getProperties((ManagedObjectReference[])targetHosts.toArray(new ManagedObjectReference[0]), new String[]{"name", "runtime.connectionState", "runtime.inMaintenanceMode"});
         } catch (Exception var11) {
            logger.warn("Failed to retrieve name property for all instance's targets", var11);
            return false;
         }

         long countNameProperties = Arrays.stream(hostsPropResponse.getPropertyValues()).filter((propertyValue) -> {
            return propertyValue.propertyName == "name";
         }).count();
         if (countNameProperties != (long)targetHosts.size()) {
            return false;
         } else {
            boolean hasDisconnectedHosts = Arrays.stream(hostsPropResponse.getPropertyValues()).filter((propertyValue) -> {
               return propertyValue.propertyName.equals("runtime.connectionState");
            }).anyMatch((propertyValue) -> {
               return propertyValue.value != ConnectionState.connected;
            });
            boolean hasMmodeHosts = Arrays.stream(hostsPropResponse.getPropertyValues()).filter((propertyValue) -> {
               return propertyValue.propertyName.equals("runtime.inMaintenanceMode");
            }).anyMatch((propertyValue) -> {
               return (Boolean)propertyValue.value;
            });
            if (!hasDisconnectedHosts && !hasMmodeHosts) {
               if (fullInstance.vmUuids == null) {
                  return true;
               } else if (fullInstance.vmUuids.size() != targetVms.size()) {
                  return false;
               } else {
                  return this.areAllVmsPoweredOn(targetVms);
               }
            } else {
               return false;
            }
         }
      }
   }

   private boolean areAllVmsPoweredOn(List vms) {
      DataServiceResponse vmsPropResponse;
      try {
         vmsPropResponse = QueryUtil.getProperties((ManagedObjectReference[])vms.toArray(new ManagedObjectReference[0]), new String[]{"powerState"});
      } catch (Exception var4) {
         logger.warn("Failed to retrieve power state property for all instance's vms targets", var4);
         return false;
      }

      return Arrays.stream(vmsPropResponse.getPropertyValues()).filter((propertyValue) -> {
         return propertyValue.propertyName == "powerState";
      }).allMatch((propertyValue) -> {
         return propertyValue.value == PowerState.poweredOn;
      });
   }

   @TsService
   public ManagedObjectReference rerunInstance(ManagedObjectReference clusterRef, IoInsightInstance instance, String newName, Integer durationInSeconds) {
      boolean isInstanceNameDuplicated = this.isInstanceNameDuplicated(clusterRef, newName);
      if (isInstanceNameDuplicated) {
         throw new VsanUiLocalizableException("vsan.ioInsight.create.duplicateInstanceName.error");
      } else {
         List hosts = new ArrayList();
         List vms = new ArrayList();
         if (instance.hostIoInsightInfos != null) {
            Iterator var8 = instance.hostIoInsightInfos.iterator();

            while(true) {
               HostIoInsightInfo hostIoInsightInfo;
               do {
                  if (!var8.hasNext()) {
                     return this.createIoInsightInstance(clusterRef, (ManagedObjectReference[])hosts.toArray(new ManagedObjectReference[0]), (ManagedObjectReference[])vms.toArray(new ManagedObjectReference[0]), newName, (long)durationInSeconds);
                  }

                  hostIoInsightInfo = (HostIoInsightInfo)var8.next();
                  hosts.add(hostIoInsightInfo.host.moRef);
               } while(hostIoInsightInfo.monitoredVms == null);

               Iterator var10 = hostIoInsightInfo.monitoredVms.iterator();

               while(var10.hasNext()) {
                  ManagedObjectReference vm = (ManagedObjectReference)var10.next();
                  vms.add(vm);
               }
            }
         } else {
            return this.createIoInsightInstance(clusterRef, (ManagedObjectReference[])hosts.toArray(new ManagedObjectReference[0]), (ManagedObjectReference[])vms.toArray(new ManagedObjectReference[0]), newName, (long)durationInSeconds);
         }
      }
   }
}
