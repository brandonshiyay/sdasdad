package com.vmware.vsphere.client.vsan.impl;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vim.host.ScsiDisk;
import com.vmware.vim.binding.vim.host.VsanInternalSystem;
import com.vmware.vim.binding.vim.host.VsanSystem;
import com.vmware.vim.binding.vim.host.VsanInternalSystem.DecomParam;
import com.vmware.vim.binding.vim.host.VsanInternalSystem.DecommissioningBatch;
import com.vmware.vim.binding.vim.host.VsanInternalSystem.DecommissioningSatisfiability;
import com.vmware.vim.binding.vim.vsan.host.DecommissionMode;
import com.vmware.vim.binding.vim.vsan.host.DiskResult;
import com.vmware.vim.binding.vim.vsan.host.DecommissionMode.ObjectAction;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vise.data.query.PropertyValue;
import com.vmware.vsan.client.services.diskmanagement.DiskManagementUtil;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcConnection;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsphere.client.vsan.data.VsanDiskData;
import com.vmware.vsphere.client.vsan.spec.VsanQueryDataEvacuationInfoSpec;
import com.vmware.vsphere.client.vsan.util.DataServiceResponse;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import java.util.ArrayList;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class VsanPropertyProvider {
   @Autowired
   private VcClient vcClient;
   private static final String VC_CLUSTERS_PROPERTY = "allClusters";
   private static final Log logger = LogFactory.getLog(VsanPropertyProvider.class);

   @TsService
   public ManagedObjectReference getAnyVsanCluster(ManagedObjectReference vcRef) throws Exception {
      DataServiceResponse propertiesForRelatedObjects = QueryUtil.getPropertiesForRelatedObjects(vcRef, "allClusters", ClusterComputeResource.class.getSimpleName(), new String[]{"configurationEx[@type='ClusterConfigInfoEx'].vsanConfigInfo.enabled"});
      PropertyValue[] properties = propertiesForRelatedObjects.getPropertyValues();
      if (ArrayUtils.isEmpty(properties)) {
         return null;
      } else {
         PropertyValue[] var4 = properties;
         int var5 = properties.length;

         for(int var6 = 0; var6 < var5; ++var6) {
            PropertyValue propertyValue = var4[var6];
            Boolean vsanEnabled = (Boolean)propertyValue.value;
            if (vsanEnabled != null && vsanEnabled) {
               return (ManagedObjectReference)propertyValue.resourceObject;
            }
         }

         return null;
      }
   }

   @TsService
   public VsanDiskData[] getEligibleDisks(ManagedObjectReference hostRef) {
      DiskResult[] results = null;
      VcConnection conn = this.vcClient.getConnection(hostRef.getServerGuid());
      Throwable var4 = null;

      try {
         VsanSystem vsanSystem = conn.getHostVsanSystem(hostRef);
         results = this.getHostDisksForVsan(vsanSystem);
      } catch (Throwable var13) {
         var4 = var13;
         throw var13;
      } finally {
         if (conn != null) {
            if (var4 != null) {
               try {
                  conn.close();
               } catch (Throwable var12) {
                  var4.addSuppressed(var12);
               }
            } else {
               conn.close();
            }
         }

      }

      if (results == null) {
         return null;
      } else {
         ArrayList eligibleDisks = new ArrayList();
         DiskResult[] var16 = results;
         int var17 = results.length;

         for(int var6 = 0; var6 < var17; ++var6) {
            DiskResult result = var16[var6];
            if (DiskManagementUtil.isDiskEligible(result)) {
               eligibleDisks.add(new VsanDiskData(result));
            }
         }

         return (VsanDiskData[])eligibleDisks.toArray(new VsanDiskData[0]);
      }
   }

   private DiskResult[] getHostDisksForVsan(VsanSystem vsanSystem) {
      if (vsanSystem == null) {
         return null;
      } else {
         DiskResult[] disks = null;

         try {
            Measure measure = new Measure("vsanSystem.queryDisksForVsan(null)");
            Throwable var4 = null;

            try {
               disks = vsanSystem.queryDisksForVsan((String[])null);
            } catch (Throwable var14) {
               var4 = var14;
               throw var14;
            } finally {
               if (measure != null) {
                  if (var4 != null) {
                     try {
                        measure.close();
                     } catch (Throwable var13) {
                        var4.addSuppressed(var13);
                     }
                  } else {
                     measure.close();
                  }
               }

            }
         } catch (Exception var16) {
            logger.warn(var16.getMessage());
         }

         return disks;
      }
   }

   @TsService
   public long getVsanDataEvacuationInfo(ManagedObjectReference hostRef, VsanQueryDataEvacuationInfoSpec spec) throws Exception {
      if (spec.disks != null && spec.disks.length != 0) {
         String vsanHostUuid = (String)QueryUtil.getProperty(hostRef, "config.vsanHostConfig.clusterInfo.nodeUuid");
         if (vsanHostUuid == null) {
            logger.warn("Failed to retrieve vsanHostUuid.");
            return 0L;
         } else {
            DecommissioningBatch batch = createNewDecommissioningBatch(spec.disks, vsanHostUuid);
            VcConnection conn = this.vcClient.getConnection(hostRef.getServerGuid());
            Throwable var7 = null;

            DecommissioningSatisfiability[] decommissionCosts;
            try {
               VsanInternalSystem vsanInternalSystem = conn.getVsanInternalSystem(hostRef);

               try {
                  Measure measure = new Measure("vsanInternalSystem.canDecommission");
                  Throwable var10 = null;

                  try {
                     decommissionCosts = vsanInternalSystem.canDecommission(new DecommissioningBatch[]{batch});
                  } catch (Throwable var35) {
                     var10 = var35;
                     throw var35;
                  } finally {
                     if (measure != null) {
                        if (var10 != null) {
                           try {
                              measure.close();
                           } catch (Throwable var34) {
                              var10.addSuppressed(var34);
                           }
                        } else {
                           measure.close();
                        }
                     }

                  }
               } catch (Exception var37) {
                  logger.error("Failed to retrieve vsan evacuation data!", var37);
                  throw var37;
               }
            } catch (Throwable var38) {
               var7 = var38;
               throw var38;
            } finally {
               if (conn != null) {
                  if (var7 != null) {
                     try {
                        conn.close();
                     } catch (Throwable var33) {
                        var7.addSuppressed(var33);
                     }
                  } else {
                     conn.close();
                  }
               }

            }

            if (decommissionCosts == null) {
               logger.error("Failed to retrieve vsan evacuation data: invalid result!");
               return 0L;
            } else {
               long dataToEvacuate = 0L;
               DecommissioningSatisfiability[] var41 = decommissionCosts;
               int var42 = decommissionCosts.length;

               for(int var43 = 0; var43 < var42; ++var43) {
                  DecommissioningSatisfiability cost = var41[var43];
                  if (cost != null && cost.cost != null) {
                     dataToEvacuate += cost.cost.usedDataSize;
                  }
               }

               return dataToEvacuate;
            }
         }
      } else {
         return 0L;
      }
   }

   private static DecommissioningBatch createNewDecommissioningBatch(ScsiDisk[] disks, String vsanHostUuid) {
      DecommissioningBatch batch = new DecommissioningBatch();
      DecomParam[] decomParams = new DecomParam[disks.length];

      for(int i = 0; i < disks.length; ++i) {
         decomParams[i] = new DecomParam();
         decomParams[i].scsiDisk = disks[i];
         decomParams[i].nodeUUID = vsanHostUuid;
      }

      batch.dp = decomParams;
      batch.mode = new DecommissionMode();
      batch.mode.objectAction = ObjectAction.evacuateAllData.toString();
      return batch;
   }
}
