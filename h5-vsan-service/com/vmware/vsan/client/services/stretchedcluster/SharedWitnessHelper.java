package com.vmware.vsan.client.services.stretchedcluster;

import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vmomi.core.Future;
import com.vmware.vim.vsan.binding.vim.cluster.VSANWitnessHostInfo;
import com.vmware.vim.vsan.binding.vim.cluster.VsanVcStretchedClusterSystem;
import com.vmware.vim.vsan.binding.vim.host.VsanSystemEx;
import com.vmware.vim.vsan.binding.vim.vsan.ClusterRuntimeInfo;
import com.vmware.vim.vsan.binding.vim.vsan.host.RuntimeStats;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.services.stretchedcluster.model.SharedWitnessLimits;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsan.client.util.VmodlHelper;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SharedWitnessHelper {
   private static final String RUNTIME_STAT_COMPONENT_LIMIT_PER_CLUSTER = "componentLimitPerCluster";
   private static final String RUNTIME_STAT_MAX_WITNESS_CLUSTERS = "maxWitnessClusters";
   private static final String[] RUNTIME_STATS = new String[]{"maxWitnessClusters", "componentLimitPerCluster"};
   private static final Log logger = LogFactory.getLog(SharedWitnessHelper.class);
   @Autowired
   private VcClient vcClient;
   @Autowired
   private VsanClient vsanClient;

   public Map getWitnessHostsFutures(List clustersRefs, Measure measure) {
      Map futures = new HashMap();
      VsanConnection connection = this.vsanClient.getConnection(((ManagedObjectReference)clustersRefs.get(0)).getServerGuid());
      Throwable var5 = null;

      try {
         VsanVcStretchedClusterSystem stretchedClusterSystem = connection.getVcStretchedClusterSystem();
         Iterator var7 = clustersRefs.iterator();

         while(var7.hasNext()) {
            ManagedObjectReference clusterRef = (ManagedObjectReference)var7.next();
            Future witnessHostsFuture = measure.newFuture("VsanVcStretchedClusterSystem.getWitnessHosts");
            stretchedClusterSystem.getWitnessHosts(clusterRef, witnessHostsFuture);
            futures.put(clusterRef, witnessHostsFuture);
         }
      } catch (Throwable var17) {
         var5 = var17;
         throw var17;
      } finally {
         if (connection != null) {
            if (var5 != null) {
               try {
                  connection.close();
               } catch (Throwable var16) {
                  var5.addSuppressed(var16);
               }
            } else {
               connection.close();
            }
         }

      }

      return futures;
   }

   public ManagedObjectReference findWitnessHost(Map witnessHostsFutures, ManagedObjectReference clusterRef) throws Exception {
      VSANWitnessHostInfo[] witnessHostInfos = (VSANWitnessHostInfo[])((Future)witnessHostsFutures.get(clusterRef)).get();
      return (ManagedObjectReference)((Stream)Optional.ofNullable(witnessHostInfos).map(Arrays::stream).orElseGet(Stream::empty)).filter((hostInfo) -> {
         return BooleanUtils.isNotTrue(hostInfo.metadataMode);
      }).findFirst().map((hostInfo) -> {
         return VmodlHelper.assignServerGuid(hostInfo.host, clusterRef.getServerGuid());
      }).orElse((Object)null);
   }

   public ClusterRuntimeInfo[] getWitnessHostClustersInfo(ManagedObjectReference hostRef) {
      Future assignedClustersInfoFuture = this.getWitnessHostClustersInfoFuture(hostRef);
      return this.getWitnessHostClustersInfo(assignedClustersInfoFuture, hostRef);
   }

   public ClusterRuntimeInfo[] getWitnessHostClustersInfo(Future assignedClustersInfoFuture, ManagedObjectReference moRef) {
      try {
         if (assignedClustersInfoFuture == null) {
            return null;
         } else {
            ClusterRuntimeInfo[] assignedClustersInfo = (ClusterRuntimeInfo[])assignedClustersInfoFuture.get();
            if (assignedClustersInfo == null) {
               return new ClusterRuntimeInfo[0];
            } else {
               Arrays.stream(assignedClustersInfo).forEach((clsInfo) -> {
                  VmodlHelper.assignServerGuid(clsInfo.cluster, moRef.getServerGuid());
               });
               return assignedClustersInfo;
            }
         }
      } catch (Exception var4) {
         logger.error("Failed to retrieve stretched cluster info: " + var4);
         throw new VsanUiLocalizableException();
      }
   }

   public Future getWitnessHostClustersInfoFuture(ManagedObjectReference hostRef) {
      Validate.notNull(hostRef);
      if (VsanCapabilityUtils.isSharedWitnessSupported(hostRef) && VsanCapabilityUtils.isSharedWitnessSupportedOnVc(hostRef)) {
         VsanConnection conn = this.vsanClient.getConnection(hostRef.getServerGuid());
         Throwable var3 = null;

         Future var8;
         try {
            VsanVcStretchedClusterSystem stretchedClusterSystem = conn.getVcStretchedClusterSystem();

            try {
               Measure measure = new Measure("Query witnesshost ClusterRuntimeInfo[]");
               Throwable var6 = null;

               try {
                  Future assignedClustersInfoFuture = measure.newFuture("stretchedClusterSystem.queryWitnessHostClusterInfo");
                  stretchedClusterSystem.queryWitnessHostClusterInfo(hostRef, (Boolean)null, assignedClustersInfoFuture);
                  var8 = assignedClustersInfoFuture;
               } catch (Throwable var33) {
                  var6 = var33;
                  throw var33;
               } finally {
                  if (measure != null) {
                     if (var6 != null) {
                        try {
                           measure.close();
                        } catch (Throwable var32) {
                           var6.addSuppressed(var32);
                        }
                     } else {
                        measure.close();
                     }
                  }

               }
            } catch (Exception var35) {
               logger.error("Failed to retrieve stretched cluster info: " + var35);
               throw new VsanUiLocalizableException();
            }
         } catch (Throwable var36) {
            var3 = var36;
            throw var36;
         } finally {
            if (conn != null) {
               if (var3 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var31) {
                     var3.addSuppressed(var31);
                  }
               } else {
                  conn.close();
               }
            }

         }

         return var8;
      } else {
         return null;
      }
   }

   public SharedWitnessLimits getSharedWitnessLimits(ManagedObjectReference hostRef) {
      Integer maxWitnessClusters = null;
      Integer maxComponentsPerCluster = null;
      VsanConnection conn = this.vsanClient.getConnection(hostRef.getServerGuid());
      Throwable var5 = null;

      try {
         VsanSystemEx vsanSystemEx = conn.getVsanSystemEx(hostRef);

         Throwable var8;
         try {
            Measure measure = new Measure("vsanSystemEx.getRuntimeStats");
            var8 = null;

            try {
               RuntimeStats runtimeStats = vsanSystemEx.getRuntimeStats(RUNTIME_STATS, (String)null);
               if (runtimeStats == null) {
                  Object var10 = null;
                  return (SharedWitnessLimits)var10;
               }

               maxComponentsPerCluster = runtimeStats.componentLimitPerCluster;
               maxWitnessClusters = runtimeStats.maxWitnessClusters;
            } catch (Throwable var39) {
               var8 = var39;
               throw var39;
            } finally {
               if (measure != null) {
                  if (var8 != null) {
                     try {
                        measure.close();
                     } catch (Throwable var38) {
                        var8.addSuppressed(var38);
                     }
                  } else {
                     measure.close();
                  }
               }

            }
         } catch (Exception var41) {
            logger.error("Failed to get runtime stats for host: " + hostRef, var41);
            var8 = null;
            return var8;
         }
      } catch (Throwable var42) {
         var5 = var42;
         throw var42;
      } finally {
         if (conn != null) {
            if (var5 != null) {
               try {
                  conn.close();
               } catch (Throwable var37) {
                  var5.addSuppressed(var37);
               }
            } else {
               conn.close();
            }
         }

      }

      return new SharedWitnessLimits(hostRef, maxComponentsPerCluster, maxWitnessClusters);
   }
}
