package com.vmware.vsan.client.services.stretchedcluster;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vim.vsan.host.DiskMapping;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vmomi.core.Future;
import com.vmware.vim.vsan.binding.vim.cluster.VSANStretchedClusterFaultDomainConfig;
import com.vmware.vim.vsan.binding.vim.cluster.VsanStretchedClusterConfig;
import com.vmware.vim.vsan.binding.vim.cluster.VsanVcStretchedClusterSystem;
import com.vmware.vim.vsan.binding.vim.vsan.ClusterRuntimeInfo;
import com.vmware.vim.vsan.binding.vim.vsan.VsanVcStretchedClusterConfigSpec;
import com.vmware.vsan.client.services.ProxygenSerializer;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.diskGroups.data.VsanDiskMapping;
import com.vmware.vsan.client.services.inventory.InventoryNode;
import com.vmware.vsan.client.services.stretchedcluster.model.SharedWitnessLimits;
import com.vmware.vsan.client.services.stretchedcluster.model.TwoHostClusterData;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsan.client.util.VmodlHelper;
import com.vmware.vsan.client.util.VsanInventoryHelper;
import com.vmware.vsphere.client.vsan.stretched.VsanStretchedClusterService;
import com.vmware.vsphere.client.vsan.stretched.WitnessHostValidationResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConfigureSharedWitnessWorkflowBacking {
   private static final Log logger = LogFactory.getLog(ConfigureSharedWitnessWorkflowBacking.class);
   @Autowired
   private WitnessHostValidationService witnessHostValidationService;
   @Autowired
   private VsanInventoryHelper inventoryHelper;
   @Autowired
   private SharedWitnessHelper sharedWitnessHelper;
   @Autowired
   private VsanClient vsanClient;
   @Autowired
   private VsanStretchedClusterService vsanStretchedClusterService;

   @TsService
   public WitnessHostValidationResult getValidateCurrentWitnessHost(ManagedObjectReference[] clustersRefs, ManagedObjectReference witnessHost) {
      return this.witnessHostValidationService.validateWitnessHost(clustersRefs, witnessHost, false);
   }

   @TsService
   public WitnessHostValidationResult getValidateNewWitnessHost(ManagedObjectReference[] clustersRefs, ManagedObjectReference witnessHost) {
      return this.witnessHostValidationService.validateWitnessHost(clustersRefs, witnessHost, true);
   }

   @TsService
   public ManagedObjectReference configureSharedWitnessHost(ManagedObjectReference[] clustersRefs, ManagedObjectReference witnessHost, VsanDiskMapping witnessHostDiskMapping) {
      Validate.notNull(witnessHost);
      Validate.notEmpty(clustersRefs);
      DiskMapping[] diskMapping = null;
      if (witnessHostDiskMapping != null) {
         diskMapping = new DiskMapping[]{witnessHostDiskMapping.toVmodl()};
      }

      VsanConnection connection = this.vsanClient.getConnection(witnessHost.getServerGuid());
      Throwable var6 = null;

      ManagedObjectReference var13;
      try {
         VsanVcStretchedClusterSystem stretchedClusterSystem = connection.getVcStretchedClusterSystem();
         VsanStretchedClusterConfig[] clustersConfig = (VsanStretchedClusterConfig[])Arrays.stream(clustersRefs).map((c) -> {
            return new VsanStretchedClusterConfig(c, (String)null, (VSANStretchedClusterFaultDomainConfig)null);
         }).toArray((x$0) -> {
            return new VsanStretchedClusterConfig[x$0];
         });
         VsanVcStretchedClusterConfigSpec configSpec = new VsanVcStretchedClusterConfigSpec(witnessHost, clustersConfig, diskMapping);

         try {
            Measure measure = new Measure("VsanVcStretchedClusterSystem.replaceWitnessHostForClusters");
            Throwable var11 = null;

            try {
               ManagedObjectReference createReplaceWitnessTask = stretchedClusterSystem.replaceWitnessHostForClusters(configSpec);
               var13 = VmodlHelper.assignServerGuid(createReplaceWitnessTask, witnessHost.getServerGuid());
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
            logger.error("Failed to reassign clusters' witness host: ", var40);
            throw new VsanUiLocalizableException("vsan.sharedWitness.reassignClusters.error");
         }
      } catch (Throwable var41) {
         var6 = var41;
         throw var41;
      } finally {
         if (connection != null) {
            if (var6 != null) {
               try {
                  connection.close();
               } catch (Throwable var36) {
                  var6.addSuppressed(var36);
               }
            } else {
               connection.close();
            }
         }

      }

      return var13;
   }

   @TsService
   public List getTwoNodeClusters(ManagedObjectReference hostRef) {
      List result = new ArrayList();
      List twoNodeClusters = this.inventoryHelper.listTwoHostVsanClusters(hostRef);
      if (CollectionUtils.isEmpty(twoNodeClusters)) {
         return result;
      } else {
         Map clusterToWitnessHostInfo = this.queryWitnessHosts(twoNodeClusters);
         if (clusterToWitnessHostInfo.isEmpty()) {
            return result;
         } else {
            List result = (List)twoNodeClusters.stream().filter((cluster) -> {
               return clusterToWitnessHostInfo.containsKey(cluster.moRef);
            }).map((cluster) -> {
               return new TwoHostClusterData(cluster, (InventoryNode)clusterToWitnessHostInfo.get(cluster.moRef));
            }).collect(Collectors.toList());
            return result;
         }
      }
   }

   private Map queryWitnessHosts(List twoNodeClusters) {
      HashMap result = new HashMap();

      try {
         Measure measure = new Measure("Retrieving witness hosts");
         Throwable var4 = null;

         try {
            List clustersRefs = (List)twoNodeClusters.stream().map((clusterData) -> {
               return clusterData.moRef;
            }).collect(Collectors.toList());
            Map witnessHostsFutures = this.sharedWitnessHelper.getWitnessHostsFutures(clustersRefs, measure);
            Iterator var7 = twoNodeClusters.iterator();

            while(var7.hasNext()) {
               InventoryNode twoNodeCluster = (InventoryNode)var7.next();
               ManagedObjectReference witnessHostRef = this.sharedWitnessHelper.findWitnessHost(witnessHostsFutures, twoNodeCluster.moRef);
               if (witnessHostRef != null) {
                  result.put(twoNodeCluster.moRef, new InventoryNode(witnessHostRef));
               }
            }
         } catch (Throwable var18) {
            var4 = var18;
            throw var18;
         } finally {
            if (measure != null) {
               if (var4 != null) {
                  try {
                     measure.close();
                  } catch (Throwable var17) {
                     var4.addSuppressed(var17);
                  }
               } else {
                  measure.close();
               }
            }

         }
      } catch (Exception var20) {
         logger.error("Failed to retrieve clusters' witness hosts", var20);
         throw new VsanUiLocalizableException();
      }

      this.updateHostsData(result);
      return result;
   }

   private void updateHostsData(Map clusterToWitnessHost) {
      if (!clusterToWitnessHost.isEmpty()) {
         List hostsRefs = (List)clusterToWitnessHost.values().stream().map((witnessHost) -> {
            return witnessHost.moRef;
         }).collect(Collectors.toList());
         VsanInventoryHelper var10000 = this.inventoryHelper;
         Map hostsNodes = VsanInventoryHelper.getInventoryNodes(hostsRefs);
         clusterToWitnessHost.values().stream().forEach((witnessHostData) -> {
            witnessHostData.name = ((InventoryNode)hostsNodes.get(witnessHostData.moRef)).name;
            witnessHostData.primaryIconId = ((InventoryNode)hostsNodes.get(witnessHostData.moRef)).primaryIconId;
         });
      }
   }

   @TsService
   public InventoryNode getWitnessHostNode(ManagedObjectReference hostRef) {
      VsanInventoryHelper var10000 = this.inventoryHelper;
      return VsanInventoryHelper.getInventoryNode(hostRef);
   }

   @TsService
   public Collection getClustersNodes(@ProxygenSerializer.ElementType(ManagedObjectReference.class) List clustersRefsForData) {
      VsanInventoryHelper var10000 = this.inventoryHelper;
      return VsanInventoryHelper.getInventoryNodes(clustersRefsForData).values();
   }

   @TsService
   public List getWitnessAssignedClusters(ManagedObjectReference witnessHost) {
      ClusterRuntimeInfo[] clustersInfo = this.sharedWitnessHelper.getWitnessHostClustersInfo(witnessHost);
      return this.clustersInfoToMoRefs(clustersInfo, witnessHost);
   }

   @TsService
   public List getAssignedAndTwoNodeClusters(ManagedObjectReference witnessHost) {
      Future assignedClustersInfoFuture = this.sharedWitnessHelper.getWitnessHostClustersInfoFuture(witnessHost);
      List twoNodeClusters = this.getTwoNodeClusters(witnessHost);
      ClusterRuntimeInfo[] assignedClustersInfo = this.sharedWitnessHelper.getWitnessHostClustersInfo(assignedClustersInfoFuture, witnessHost);
      if (ArrayUtils.isNotEmpty(assignedClustersInfo) && CollectionUtils.isNotEmpty(twoNodeClusters)) {
         assignedClustersInfo = (ClusterRuntimeInfo[])Arrays.stream(assignedClustersInfo).filter((clsInfo) -> {
            return twoNodeClusters.stream().noneMatch((twoNodeCls) -> {
               return twoNodeCls.cluster.moRef.equals(clsInfo.cluster);
            });
         }).toArray((x$0) -> {
            return new ClusterRuntimeInfo[x$0];
         });
      }

      List assignedClustersData = new ArrayList();
      if (ArrayUtils.isNotEmpty(assignedClustersInfo)) {
         VsanInventoryHelper var10000 = this.inventoryHelper;
         InventoryNode witnessData = VsanInventoryHelper.getInventoryNode(witnessHost);
         var10000 = this.inventoryHelper;
         Map clustersData = VsanInventoryHelper.getInventoryNodes(this.clustersInfoToMoRefs(assignedClustersInfo, witnessHost));
         assignedClustersData = (List)Arrays.stream(assignedClustersInfo).map((clsInfo) -> {
            InventoryNode clusterData = new InventoryNode(clsInfo.cluster, ((InventoryNode)clustersData.get(clsInfo.cluster)).name, ((InventoryNode)clustersData.get(clsInfo.cluster)).primaryIconId);
            return new TwoHostClusterData(clusterData, witnessData);
         }).collect(Collectors.toList());
      }

      twoNodeClusters.addAll((Collection)assignedClustersData);
      this.sortSharedWitnessClusters(twoNodeClusters, witnessHost);
      return twoNodeClusters;
   }

   private List clustersInfoToMoRefs(ClusterRuntimeInfo[] clustersInfo, ManagedObjectReference witnessHost) {
      return ArrayUtils.isEmpty(clustersInfo) ? null : (List)Arrays.stream(clustersInfo).map((clusterInfo) -> {
         return VmodlHelper.assignServerGuid(clusterInfo.cluster, witnessHost.getServerGuid());
      }).collect(Collectors.toList());
   }

   private void sortSharedWitnessClusters(List twoNodeClusters, ManagedObjectReference witnessHost) {
      twoNodeClusters.sort((cls1, cls2) -> {
         int namesComparison = cls1.cluster.name.compareTo(cls2.cluster.name);
         return cls1.witnessHost.moRef.equals(witnessHost) ? (cls2.witnessHost.moRef.equals(witnessHost) ? namesComparison : -1) : (cls2.witnessHost.moRef.equals(witnessHost) ? 1 : namesComparison);
      });
   }

   @TsService
   public Integer getMaxComponentsPerCluster(ManagedObjectReference witnessHost) {
      SharedWitnessLimits sharedWitnessLimits = this.getSharedWitnessLimits(witnessHost);
      return sharedWitnessLimits == null ? null : sharedWitnessLimits.maxComponentsPerCluster;
   }

   @TsService
   public SharedWitnessLimits getSharedWitnessLimits(ManagedObjectReference hostRef) {
      return this.sharedWitnessHelper.getSharedWitnessLimits(hostRef);
   }
}
