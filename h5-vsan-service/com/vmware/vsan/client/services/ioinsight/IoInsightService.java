package com.vmware.vsan.client.services.ioinsight;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vim.HostSystem;
import com.vmware.vim.binding.vim.VirtualMachine;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanIoInsightInstance;
import com.vmware.vim.vsan.binding.vim.cluster.VsanIoInsightInstanceQuerySpec;
import com.vmware.vim.vsan.binding.vim.cluster.VsanIoInsightManager;
import com.vmware.vim.vsan.binding.vim.host.VsanHostIoInsightInfo;
import com.vmware.vise.data.query.RequestSpec;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.ioinsight.model.HostIoInsightInfo;
import com.vmware.vsan.client.services.ioinsight.model.IoInsightInstance;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsan.client.util.VmodlHelper;
import com.vmware.vsan.client.util.dataservice.query.QueryBuilder;
import com.vmware.vsan.client.util.dataservice.query.QueryExecutor;
import com.vmware.vsphere.client.vsan.perf.VsanPerfPropertyProvider;
import com.vmware.vsphere.client.vsan.perf.model.PerfVirtualMachineDiskData;
import com.vmware.vsphere.client.vsan.util.DataServiceResponse;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import com.vmware.vsphere.client.vsan.util.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class IoInsightService {
   private static final Logger logger = LoggerFactory.getLogger(IoInsightService.class);
   private static final String HOST_NAME_PROPERTY = "hostName";
   private static final String HOSTREF_UID_PREFIX = "host:";
   private static final String VM_UID_PREFIX = "vm:";
   private static final String RUN_NAME_PREFIX = "runName:";
   @Autowired
   private VsanClient vsanClient;
   @Autowired
   private VmodlHelper vmodlHelper;
   @Autowired
   private QueryExecutor queryExecutor;
   @Autowired
   private VsanPerfPropertyProvider perfPropertyProvider;

   @TsService
   public List getIoInsightInstances(ManagedObjectReference moRef) {
      List result = new ArrayList();
      VsanIoInsightInstance[] vsanIoInsightInstances = this.getVsanIoInsightInstances(moRef);
      if (vsanIoInsightInstances == null) {
         return result;
      } else {
         Collection hostUuids = this.getUniqueHostUuids(vsanIoInsightInstances);

         DataServiceResponse hostProperties;
         try {
            hostProperties = this.getHostProperties(moRef, hostUuids);
         } catch (Exception var11) {
            logger.error("Failed to retrieve host properties", var11);
            throw new VsanUiLocalizableException(var11);
         }

         VsanIoInsightInstance[] var6 = vsanIoInsightInstances;
         int var7 = vsanIoInsightInstances.length;

         for(int var8 = 0; var8 < var7; ++var8) {
            VsanIoInsightInstance instance = var6[var8];
            IoInsightInstance ioInsightInstance = IoInsightInstance.create(instance, hostProperties);
            result.add(ioInsightInstance);
         }

         return result;
      }
   }

   @TsService
   public List getIoInsightInstancesByTime(ManagedObjectReference moRef, Date from, Date to) {
      List result = new ArrayList();
      List instances = this.getIoInsightInstances(moRef);
      Iterator var6 = instances.iterator();

      while(var6.hasNext()) {
         IoInsightInstance instance = (IoInsightInstance)var6.next();
         if (instance.startTime.before(to) && instance.endTime.after(from)) {
            result.add(instance);
         }
      }

      return result;
   }

   private Collection getUniqueHostUuids(VsanIoInsightInstance[] vsanIoInsightInstances) {
      Set hostUuids = new HashSet();
      VsanIoInsightInstance[] var3 = vsanIoInsightInstances;
      int var4 = vsanIoInsightInstances.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         VsanIoInsightInstance instance = var3[var5];
         String[] var7 = instance.hostUuids;
         int var8 = var7.length;

         for(int var9 = 0; var9 < var8; ++var9) {
            String hostUuid = var7[var9];
            hostUuids.add(hostUuid);
         }
      }

      return hostUuids;
   }

   private DataServiceResponse getHostProperties(ManagedObjectReference moRef, Collection hostUuids) throws Exception {
      if (this.vmodlHelper.isOfType(moRef, ClusterComputeResource.class)) {
         RequestSpec requestSpec = (new QueryBuilder()).newQuery().select("name", "primaryIconId", "config.vsanHostConfig.clusterInfo.nodeUuid").from(moRef).join(HostSystem.class).on("host").where().propertyEqualsAnyOf("config.vsanHostConfig.clusterInfo.nodeUuid", hostUuids).end().build();
         return (DataServiceResponse)this.queryExecutor.execute(requestSpec).getDataServiceResponses().values().stream().findFirst().get();
      } else {
         return QueryUtil.getProperties(moRef, new String[]{"name", "primaryIconId", "config.vsanHostConfig.clusterInfo.nodeUuid"});
      }
   }

   @TsService
   public IoInsightInstance getIoInsightInstanceInfo(IoInsightInstance instance, ManagedObjectReference moRef) {
      VsanIoInsightInstance[] vsanIoInsightInstances = this.getVsanIoInsightInstances(instance, moRef);
      if (vsanIoInsightInstances == null) {
         return null;
      } else {
         DataServiceResponse hostProperties;
         try {
            hostProperties = this.getHostProperties(moRef, Arrays.asList(vsanIoInsightInstances[0].hostUuids));
         } catch (Exception var6) {
            logger.error("Failed to retrieve host properties: ", var6);
            throw new VsanUiLocalizableException(var6);
         }

         return IoInsightInstance.create(vsanIoInsightInstances[0], hostProperties);
      }
   }

   private VsanIoInsightInstance[] getVsanIoInsightInstances(ManagedObjectReference moRef) {
      return this.getVsanIoInsightInstances((IoInsightInstance)null, moRef);
   }

   private VsanIoInsightInstance[] getVsanIoInsightInstances(IoInsightInstance instance, ManagedObjectReference moRef) {
      VsanIoInsightInstance[] vsanIoInsightInstances = null;
      VsanIoInsightInstanceQuerySpec spec = new VsanIoInsightInstanceQuerySpec();
      ManagedObjectReference targetObject = moRef;
      String childNodeUuidProperty = "";
      DataServiceResponse response;
      ManagedObjectReference parentCluster;
      if (this.vmodlHelper.isOfType(moRef, HostSystem.class)) {
         try {
            response = QueryUtil.getProperties(moRef, new String[]{"config.vsanHostConfig.clusterInfo.nodeUuid", "parent"});
            childNodeUuidProperty = "host:" + response.getProperty(moRef, "config.vsanHostConfig.clusterInfo.nodeUuid");
            parentCluster = (ManagedObjectReference)response.getProperty(moRef, "parent");
            targetObject = parentCluster;
         } catch (Exception var42) {
            logger.error("Failed to retrieve IOInsight instances: ", var42);
            throw new VsanUiLocalizableException("vsan.common.generic.error");
         }
      }

      if (this.vmodlHelper.isOfType(moRef, VirtualMachine.class)) {
         try {
            response = QueryUtil.getProperties(moRef, new String[]{"cluster", "config.instanceUuid"});
            childNodeUuidProperty = "vm:" + response.getProperty(moRef, "config.instanceUuid");
            parentCluster = (ManagedObjectReference)response.getProperty(moRef, "cluster");
            targetObject = parentCluster;
         } catch (Exception var41) {
            logger.error("Failed to retrieve IOInsight instances: ", var41);
            throw new VsanUiLocalizableException("vsan.common.generic.error");
         }
      }

      if (instance != null || this.vmodlHelper.isOfType(moRef, HostSystem.class) || this.vmodlHelper.isOfType(moRef, VirtualMachine.class)) {
         spec.entityRefId = instance != null ? "runName:" + instance.name : childNodeUuidProperty;
      }

      VsanConnection conn = this.vsanClient.getConnection(moRef.getServerGuid());
      Throwable var48 = null;

      try {
         VsanIoInsightManager vsanIoInsightManager = conn.getVsanIoInsightManager();

         try {
            Measure measure = new Measure("VsanIoInsightManager.getIoInsightInstances");
            Throwable var11 = null;

            try {
               vsanIoInsightInstances = vsanIoInsightManager.queryIoInsightInstances(spec, targetObject);
            } catch (Throwable var40) {
               var11 = var40;
               throw var40;
            } finally {
               if (measure != null) {
                  if (var11 != null) {
                     try {
                        measure.close();
                     } catch (Throwable var39) {
                        var11.addSuppressed(var39);
                     }
                  } else {
                     measure.close();
                  }
               }

            }
         } catch (Exception var44) {
            logger.error("Failed to retrieve IOInsight instances: ", var44);
            throw new VsanUiLocalizableException("vsan.common.generic.error");
         }
      } catch (Throwable var45) {
         var48 = var45;
         throw var45;
      } finally {
         if (conn != null) {
            if (var48 != null) {
               try {
                  conn.close();
               } catch (Throwable var38) {
                  var48.addSuppressed(var38);
               }
            } else {
               conn.close();
            }
         }

      }

      return vsanIoInsightInstances;
   }

   @TsService
   public void deleteInstance(ManagedObjectReference clusterRef, IoInsightInstance instance) {
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var4 = null;

      try {
         VsanIoInsightManager vsanIoInsightManager = conn.getVsanIoInsightManager();

         try {
            Measure measure = new Measure("VsanIoInsightManager.deleteIoInsightInstance");
            Throwable var7 = null;

            try {
               vsanIoInsightManager.deleteIoInsightInstance(instance.name, clusterRef);
            } catch (Throwable var32) {
               var7 = var32;
               throw var32;
            } finally {
               if (measure != null) {
                  if (var7 != null) {
                     try {
                        measure.close();
                     } catch (Throwable var31) {
                        var7.addSuppressed(var31);
                     }
                  } else {
                     measure.close();
                  }
               }

            }
         } catch (Exception var34) {
            logger.error("Failed to delete IO Insight instance: ", var34);
            throw new VsanUiLocalizableException("vsan.ioInsight.delete.error");
         }
      } catch (Throwable var35) {
         var4 = var35;
         throw var35;
      } finally {
         if (conn != null) {
            if (var4 != null) {
               try {
                  conn.close();
               } catch (Throwable var30) {
                  var4.addSuppressed(var30);
               }
            } else {
               conn.close();
            }
         }

      }

   }

   @TsService
   public ManagedObjectReference stopInstance(ManagedObjectReference clusterRef, IoInsightInstance instance) {
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var4 = null;

      ManagedObjectReference var9;
      try {
         VsanIoInsightManager vsanIoInsightManager = conn.getVsanIoInsightManager();

         try {
            Measure measure = new Measure("VsanIoInsightManager.stopIoInsightInstance");
            Throwable var7 = null;

            try {
               ManagedObjectReference stopIoInsightTask = vsanIoInsightManager.stopIoInsight(clusterRef, instance.name, (VsanHostIoInsightInfo[])null);
               var9 = VmodlHelper.assignServerGuid(stopIoInsightTask, clusterRef.getServerGuid());
            } catch (Throwable var34) {
               var7 = var34;
               throw var34;
            } finally {
               if (measure != null) {
                  if (var7 != null) {
                     try {
                        measure.close();
                     } catch (Throwable var33) {
                        var7.addSuppressed(var33);
                     }
                  } else {
                     measure.close();
                  }
               }

            }
         } catch (Exception var36) {
            logger.error("Failed to stop IO Insight instance: ", var36);
            throw new VsanUiLocalizableException("vsan.ioInsight.stop.error");
         }
      } catch (Throwable var37) {
         var4 = var37;
         throw var37;
      } finally {
         if (conn != null) {
            if (var4 != null) {
               try {
                  conn.close();
               } catch (Throwable var32) {
                  var4.addSuppressed(var32);
               }
            } else {
               conn.close();
            }
         }

      }

      return var9;
   }

   @TsService
   public void renameInstance(ManagedObjectReference clusterRef, IoInsightInstance instance, String newName) {
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var5 = null;

      try {
         VsanIoInsightManager vsanIoInsightManager = conn.getVsanIoInsightManager();

         try {
            Measure measure = new Measure("VsanIoInsightManager.renameIoInsightInstance");
            Throwable var8 = null;

            try {
               vsanIoInsightManager.renameIoInsightInstance(instance.name, newName, clusterRef);
            } catch (Throwable var33) {
               var8 = var33;
               throw var33;
            } finally {
               if (measure != null) {
                  if (var8 != null) {
                     try {
                        measure.close();
                     } catch (Throwable var32) {
                        var8.addSuppressed(var32);
                     }
                  } else {
                     measure.close();
                  }
               }

            }
         } catch (Exception var35) {
            logger.error("Failed to rename IO Insight instance: ", var35);
            throw new VsanUiLocalizableException("vsan.ioInsight.rename.error");
         }
      } catch (Throwable var36) {
         var5 = var36;
         throw var36;
      } finally {
         if (conn != null) {
            if (var5 != null) {
               try {
                  conn.close();
               } catch (Throwable var31) {
                  var5.addSuppressed(var31);
               }
            } else {
               conn.close();
            }
         }

      }

   }

   @TsService
   public List getVirtualMachinesDiskData(IoInsightInstance instance, ManagedObjectReference moRef) {
      List result = new ArrayList();
      List vmRefs = this.getMonitoredVms(instance, moRef);
      if (vmRefs.isEmpty()) {
         logger.warn("No monitoredVMs found for IOinsight instance:" + instance.name);
         return result;
      } else {
         DataServiceResponse response;
         try {
            ManagedObjectReference[] vmRefsArray = new ManagedObjectReference[vmRefs.size()];
            vmRefsArray = (ManagedObjectReference[])vmRefs.toArray(vmRefsArray);
            response = QueryUtil.getProperties(vmRefsArray, new String[]{"name", "hostName"});
         } catch (Exception var13) {
            logger.error("Failed to retrieve virtual machines: ", var13);
            throw new VsanUiLocalizableException("vsan.common.generic.error");
         }

         Iterator var14 = response.getResourceObjects().iterator();

         while(var14.hasNext()) {
            Object resourceObject = var14.next();
            ManagedObjectReference vmRef = (ManagedObjectReference)resourceObject;
            String vmName = (String)response.getProperty(vmRef, "name");
            String hostName = (String)response.getProperty(vmRef, "hostName");

            try {
               PerfVirtualMachineDiskData diskData = this.perfPropertyProvider.getVirtualMachineDiskData(vmRef, false);
               diskData.entityLabelName = Utils.getLocalizedString("vsan.ioInsight.performance.vm.labael.name", hostName, vmName);
               result.add(diskData);
            } catch (Exception var12) {
               logger.error("Failed to retrieve virtual machine disks data: ", var12);
               throw new VsanUiLocalizableException("vsan.common.generic.error");
            }
         }

         return result;
      }
   }

   private List getMonitoredVms(IoInsightInstance instance, ManagedObjectReference moRef) {
      List vmRefs = new ArrayList();
      IoInsightInstance ioInsightInstanceInfo = this.getIoInsightInstanceInfo(instance, moRef);
      Iterator var5 = ioInsightInstanceInfo.hostIoInsightInfos.iterator();

      while(true) {
         HostIoInsightInfo hostIoInsightInfos;
         do {
            if (!var5.hasNext()) {
               return vmRefs;
            }

            hostIoInsightInfos = (HostIoInsightInfo)var5.next();
         } while(hostIoInsightInfos.monitoredVms == null);

         Iterator var7 = hostIoInsightInfos.monitoredVms.iterator();

         while(var7.hasNext()) {
            ManagedObjectReference monitoredVM = (ManagedObjectReference)var7.next();
            vmRefs.add(monitoredVM);
         }
      }
   }
}
