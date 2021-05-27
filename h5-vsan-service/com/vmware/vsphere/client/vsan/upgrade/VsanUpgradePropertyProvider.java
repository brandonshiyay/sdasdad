package com.vmware.vsphere.client.vsan.upgrade;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vim.VsanUpgradeSystem;
import com.vmware.vim.binding.vim.VsanUpgradeSystem.NotEnoughFreeCapacityIssue;
import com.vmware.vim.binding.vim.VsanUpgradeSystem.PreflightCheckIssue;
import com.vmware.vim.binding.vim.VsanUpgradeSystem.UpgradeStatus;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.VsanUpgradeSystemEx;
import com.vmware.vim.vsan.binding.vim.cluster.VsanClusterHealthQuerySpec;
import com.vmware.vim.vsan.binding.vim.cluster.VsanDiskFormatConversionSpec;
import com.vmware.vim.vsan.binding.vim.cluster.VsanUpgradeStatusEx;
import com.vmware.vim.vsan.binding.vim.cluster.VsanVcClusterHealthSystem;
import com.vmware.vim.vsan.binding.vim.host.VsanObjectOverallHealth;
import com.vmware.vise.data.query.PropertyValue;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsphere.client.vsan.base.util.BaseUtils;
import com.vmware.vsphere.client.vsan.base.util.VsanProfiler;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class VsanUpgradePropertyProvider {
   private static final Log logger = LogFactory.getLog(VsanUpgradePropertyProvider.class);
   private static final VsanProfiler _profiler = new VsanProfiler(VsanUpgradePropertyProvider.class);
   @Autowired
   private VsanClient vsanClient;

   @TsService
   public VsanUpgradeStatusData getVsanUpgradeStatus(ManagedObjectReference objRef) {
      VsanUpgradeStatusData result = null;
      ManagedObjectReference clusterRef = BaseUtils.getCluster(objRef);
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var5 = null;

      try {
         VsanUpgradeSystemEx upgradeSystemEx = conn.getVsanUpgradeSystemEx();

         try {
            VsanProfiler.Point p = _profiler.point("upgradeSystemEx.queryUpgradeStatus");
            Throwable var67 = null;

            try {
               VsanUpgradeStatusEx vsanUpgradeStatusEx = upgradeSystemEx.queryUpgradeStatus(clusterRef);
               if (!vsanUpgradeStatusEx.inProgress && vsanUpgradeStatusEx.aborted == null && vsanUpgradeStatusEx.completed == null) {
                  result = new VsanUpgradeStatusData(true);
               } else {
                  result = new VsanUpgradeStatusData(vsanUpgradeStatusEx);
               }
            } catch (Throwable var62) {
               var67 = var62;
               throw var62;
            } finally {
               if (p != null) {
                  if (var67 != null) {
                     try {
                        p.close();
                     } catch (Throwable var58) {
                        var67.addSuppressed(var58);
                     }
                  } else {
                     p.close();
                  }
               }

            }
         } catch (Exception var64) {
            boolean isUpgradeSystem2Supported = VsanCapabilityUtils.isUpgradeSystem2SupportedOnVc(clusterRef);
            VsanUpgradeSystem upgradeSystem = isUpgradeSystem2Supported ? conn.getVsanUpgradeSystem() : conn.getVsanLegacyUpgradeSystem();

            try {
               VsanProfiler.Point p = _profiler.point("upgradeSystem.queryUpgradeStatus");
               Throwable var11 = null;

               try {
                  UpgradeStatus upgradeStatus = upgradeSystem.queryUpgradeStatus(clusterRef);
                  if (!upgradeStatus.inProgress && upgradeStatus.aborted == null && upgradeStatus.completed == null) {
                     result = new VsanUpgradeStatusData(false);
                  } else {
                     result = new VsanUpgradeStatusData(upgradeStatus);
                  }
               } catch (Throwable var59) {
                  var11 = var59;
                  throw var59;
               } finally {
                  if (p != null) {
                     if (var11 != null) {
                        try {
                           p.close();
                        } catch (Throwable var57) {
                           var11.addSuppressed(var57);
                        }
                     } else {
                        p.close();
                     }
                  }

               }
            } catch (Exception var61) {
               result = new VsanUpgradeStatusData(false);
            }
         }
      } catch (Throwable var65) {
         var5 = var65;
         throw var65;
      } finally {
         if (conn != null) {
            if (var5 != null) {
               try {
                  conn.close();
               } catch (Throwable var56) {
                  var5.addSuppressed(var56);
               }
            } else {
               conn.close();
            }
         }

      }

      return result;
   }

   @TsService
   public VsanVersionInfoPerHost[] getVsanHostVersions(ManagedObjectReference objRef) throws Exception {
      ManagedObjectReference clusterRef = BaseUtils.getCluster(objRef);
      List result = new ArrayList();
      PropertyValue[] hostsVersionValues = QueryUtil.getPropertyForRelatedObjects(clusterRef, "allVsanHosts", ClusterComputeResource.class.getSimpleName(), "vsanDiskVersionsData").getPropertyValues();
      PropertyValue[] var5 = hostsVersionValues;
      int var6 = hostsVersionValues.length;

      for(int var7 = 0; var7 < var6; ++var7) {
         PropertyValue value = var5[var7];
         VsanDiskVersionData[] vsanDiskVersionData = (VsanDiskVersionData[])((VsanDiskVersionData[])value.value);
         result.add(new VsanVersionInfoPerHost(vsanDiskVersionData));
      }

      return (VsanVersionInfoPerHost[])result.toArray(new VsanVersionInfoPerHost[result.size()]);
   }

   @TsService
   public VsanUpgradePreflightCheckIssue[] getVsanUpgradePreflightCheckResult(ManagedObjectReference objRef) throws Exception {
      ManagedObjectReference clusterRef = BaseUtils.getCluster(objRef);
      boolean isUpgradeSystemExSupported = VsanCapabilityUtils.isUpgradeSystemExSupportedOnVc(clusterRef);
      PreflightCheckIssue[] result = null;
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var6 = null;

      try {
         VsanProfiler.Point p;
         Throwable var9;
         if (isUpgradeSystemExSupported) {
            VsanUpgradeSystemEx upgradeSystem = conn.getVsanUpgradeSystemEx();
            p = _profiler.point("upgradeSystemEx.performUpgradePreflightCheck");
            var9 = null;

            try {
               result = upgradeSystem.performUpgradePreflightCheck(clusterRef, (Boolean)null, (VsanDiskFormatConversionSpec)null).issues;
            } catch (Throwable var54) {
               var9 = var54;
               throw var54;
            } finally {
               if (p != null) {
                  if (var9 != null) {
                     try {
                        p.close();
                     } catch (Throwable var52) {
                        var9.addSuppressed(var52);
                     }
                  } else {
                     p.close();
                  }
               }

            }
         } else {
            VsanUpgradeSystem upgradeSystem = conn.getVsanLegacyUpgradeSystem();
            p = _profiler.point("upgradeSystem.performUpgradePreflightCheck");
            var9 = null;

            try {
               result = upgradeSystem.performUpgradePreflightCheck(clusterRef, false).issues;
            } catch (Throwable var53) {
               var9 = var53;
               throw var53;
            } finally {
               if (p != null) {
                  if (var9 != null) {
                     try {
                        p.close();
                     } catch (Throwable var51) {
                        var9.addSuppressed(var51);
                     }
                  } else {
                     p.close();
                  }
               }

            }
         }
      } catch (Throwable var57) {
         var6 = var57;
         throw var57;
      } finally {
         if (conn != null) {
            if (var6 != null) {
               try {
                  conn.close();
               } catch (Throwable var50) {
                  var6.addSuppressed(var50);
               }
            } else {
               conn.close();
            }
         }

      }

      return convertIssues(result);
   }

   private static VsanUpgradePreflightCheckIssue[] convertIssues(PreflightCheckIssue[] originalIssues) {
      if (ArrayUtils.isEmpty(originalIssues)) {
         return new VsanUpgradePreflightCheckIssue[0];
      } else {
         List issues = new ArrayList(originalIssues.length);
         PreflightCheckIssue[] var2 = originalIssues;
         int var3 = originalIssues.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            PreflightCheckIssue originalIssue = var2[var4];
            VsanUpgradePreflightCheckIssue issue = new VsanUpgradePreflightCheckIssue();
            issue.message = originalIssue.msg;
            if (originalIssue instanceof NotEnoughFreeCapacityIssue) {
               NotEnoughFreeCapacityIssue nefcIssue = (NotEnoughFreeCapacityIssue)originalIssue;
               if (nefcIssue.reducedRedundancyUpgradePossible) {
                  issue.type = VsanUpgradePreflightCheckIssue.IssueType.WARNING;
               } else {
                  issue.type = VsanUpgradePreflightCheckIssue.IssueType.ERROR;
               }
            } else {
               issue.type = VsanUpgradePreflightCheckIssue.IssueType.ERROR;
            }

            issues.add(issue);
         }

         return (VsanUpgradePreflightCheckIssue[])issues.toArray(new VsanUpgradePreflightCheckIssue[issues.size()]);
      }
   }

   @TsService
   public int getLatestVsanVersion(ManagedObjectReference objRef) {
      int latestVersion = 2;
      ManagedObjectReference clusterRef = BaseUtils.getCluster(objRef);
      boolean isUpgradeSystemExSupported = VsanCapabilityUtils.isUpgradeSystemExSupportedOnVc(clusterRef);
      if (!isUpgradeSystemExSupported) {
         return latestVersion;
      } else {
         VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
         Throwable var6 = null;

         try {
            VsanUpgradeSystemEx upgradeSystemEx = conn.getVsanUpgradeSystemEx();

            try {
               VsanProfiler.Point p = _profiler.point("upgradeSystemEx.retrieveSupportedFormatVersion");
               Throwable var9 = null;

               try {
                  latestVersion = upgradeSystemEx.retrieveSupportedFormatVersion(clusterRef);
               } catch (Throwable var34) {
                  var9 = var34;
                  throw var34;
               } finally {
                  if (p != null) {
                     if (var9 != null) {
                        try {
                           p.close();
                        } catch (Throwable var33) {
                           var9.addSuppressed(var33);
                        }
                     } else {
                        p.close();
                     }
                  }

               }
            } catch (Exception var36) {
               logger.error("Could not retrieve latest available disk format version", var36);
            }
         } catch (Throwable var37) {
            var6 = var37;
            throw var37;
         } finally {
            if (conn != null) {
               if (var6 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var32) {
                     var6.addSuppressed(var32);
                  }
               } else {
                  conn.close();
               }
            }

         }

         return latestVersion;
      }
   }

   @TsService
   public boolean getHasOldVsanObject(ManagedObjectReference objRef) {
      Boolean hasOldVsanObject = null;
      ManagedObjectReference clusterRef = BaseUtils.getCluster(objRef);
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var5 = null;

      try {
         VsanVcClusterHealthSystem healthSystem = conn.getVsanVcClusterHealthSystem();

         try {
            VsanProfiler.Point p = _profiler.point("upgradeSystemEx.queryObjectHealthSummary");
            Throwable var8 = null;

            try {
               VsanObjectOverallHealth summary = healthSystem.queryObjectHealthSummary(clusterRef, (String[])null, (Boolean)null, (VsanClusterHealthQuerySpec)null, (Boolean)null);
               if (summary != null && summary.objectVersionCompliance != null) {
                  hasOldVsanObject = !summary.objectVersionCompliance;
               }
            } catch (Throwable var33) {
               var8 = var33;
               throw var33;
            } finally {
               if (p != null) {
                  if (var8 != null) {
                     try {
                        p.close();
                     } catch (Throwable var32) {
                        var8.addSuppressed(var32);
                     }
                  } else {
                     p.close();
                  }
               }

            }
         } catch (Exception var35) {
            logger.warn("Cannot retrieve object version compliance data from the health system.", var35);
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

      return hasOldVsanObject;
   }
}
