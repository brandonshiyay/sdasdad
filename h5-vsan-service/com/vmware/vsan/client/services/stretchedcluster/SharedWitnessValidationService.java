package com.vmware.vsan.client.services.stretchedcluster;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vmomi.core.Future;
import com.vmware.vim.vsan.binding.vim.cluster.VsanVcStretchedClusterSystem;
import com.vmware.vim.vsan.binding.vim.vsan.ClusterRuntimeInfo;
import com.vmware.vim.vsan.binding.vim.vsan.SharedWitnessCompatibilityResult;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.stretchedcluster.model.SharedWitnessLimits;
import com.vmware.vsan.client.services.stretchedcluster.model.SharedWitnessValidationData;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsan.client.util.VmodlHelper;
import com.vmware.vsan.client.util.VsanInventoryHelper;
import com.vmware.vsphere.client.vsan.util.Utils;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SharedWitnessValidationService {
   private static final Log logger = LogFactory.getLog(SharedWitnessValidationService.class);
   @Autowired
   VsanClient vsanClient;
   @Autowired
   VsanInventoryHelper inventoryHelper;
   @Autowired
   SharedWitnessHelper sharedWitnessHelper;

   public SharedWitnessValidationData querySharedWitnessCompatibility(ManagedObjectReference hostRef, ManagedObjectReference[] clustersRefs) {
      Validate.notNull(hostRef);
      Validate.notEmpty(clustersRefs);

      try {
         VsanConnection conn = this.vsanClient.getConnection(hostRef.getServerGuid());
         Throwable var4 = null;

         SharedWitnessValidationData var9;
         try {
            VsanVcStretchedClusterSystem stretchedClusterSystem = conn.getVcStretchedClusterSystem();
            Measure measure = new Measure("stretchedClusterSystem.querySharedWitnessCompatibility");
            Throwable var7 = null;

            try {
               SharedWitnessCompatibilityResult sharedWitnessCompatibilityResult = stretchedClusterSystem.querySharedWitnessCompatibility(hostRef, clustersRefs);
               var9 = SharedWitnessValidationData.fromVmodl(sharedWitnessCompatibilityResult);
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
         } catch (Throwable var36) {
            var4 = var36;
            throw var36;
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
      } catch (Exception var38) {
         logger.error("Failed to retrieve shared witness compatibility result: " + var38);
         throw new VsanUiLocalizableException("vsan.sharedWitness.validation.general");
      }
   }

   @TsService
   public Map querySharedWitnessPrecheckCompatibility(ManagedObjectReference hostRef) {
      Future assignedClustersInfoFuture = this.sharedWitnessHelper.getWitnessHostClustersInfoFuture(hostRef);
      VsanInventoryHelper var10000 = this.inventoryHelper;
      String witnessName = VsanInventoryHelper.getInventoryNode(hostRef).name;
      SharedWitnessLimits sharedWitnessLimits = this.sharedWitnessHelper.getSharedWitnessLimits(hostRef);
      Map hostNameToValidation = new HashMap();
      if (sharedWitnessLimits == null) {
         hostNameToValidation.put(witnessName, (Object)null);
         return hostNameToValidation;
      } else {
         ClusterRuntimeInfo[] assignedClustersInfo = this.sharedWitnessHelper.getWitnessHostClustersInfo(assignedClustersInfoFuture, hostRef);
         if (ArrayUtils.isEmpty(assignedClustersInfo)) {
            hostNameToValidation.put(witnessName, (Object)null);
            return hostNameToValidation;
         } else {
            String validationError = null;
            if (assignedClustersInfo.length >= sharedWitnessLimits.maxWitnessClusters) {
               validationError = Utils.getLocalizedString("vsan.sharedWitness.addClustersPrecheck.dialog.validation.tooManyClusters", witnessName, sharedWitnessLimits.maxWitnessClusters.toString());
            } else {
               ClusterRuntimeInfo clusterWithMaxComponents = (ClusterRuntimeInfo)Arrays.stream(assignedClustersInfo).max(Comparator.comparing(ClusterRuntimeInfo::getTotalComponentsCount)).orElseGet((Supplier)null);
               if (clusterWithMaxComponents != null && clusterWithMaxComponents.totalComponentsCount >= sharedWitnessLimits.maxComponentsPerCluster) {
                  var10000 = this.inventoryHelper;
                  String clusterName = VsanInventoryHelper.getInventoryNode(VmodlHelper.assignServerGuid(clusterWithMaxComponents.cluster, hostRef.getServerGuid())).name;
                  validationError = Utils.getLocalizedString("vsan.sharedWitness.addClustersPrecheck.dialog.validation.notEnoughComponents", clusterName, Integer.toString(clusterWithMaxComponents.totalComponentsCount));
               }
            }

            hostNameToValidation.put(witnessName, validationError);
            return hostNameToValidation;
         }
      }
   }
}
