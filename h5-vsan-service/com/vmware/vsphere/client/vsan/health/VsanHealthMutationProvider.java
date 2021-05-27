package com.vmware.vsphere.client.vsan.health;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.VsanUpgradeSystemEx;
import com.vmware.vim.vsan.binding.vim.cluster.VsanClusterHealthConfigs;
import com.vmware.vim.vsan.binding.vim.cluster.VsanClusterHealthResultKeyValuePair;
import com.vmware.vim.vsan.binding.vim.cluster.VsanClusterMgmtInternalSystem;
import com.vmware.vim.vsan.binding.vim.cluster.VsanClusterTelemetryProxyConfig;
import com.vmware.vim.vsan.binding.vim.cluster.VsanDiskFormatConversionSpec;
import com.vmware.vim.vsan.binding.vim.cluster.VsanVcClusterHealthSystem;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.VmodlHelper;
import com.vmware.vsphere.client.vsan.base.util.VsanProfiler;
import com.vmware.vsphere.client.vsan.health.util.VsanHealthUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class VsanHealthMutationProvider {
   private static final VsanProfiler _profiler = new VsanProfiler(VsanHealthMutationProvider.class);
   private static final Log logger = LogFactory.getLog(VsanHealthMutationProvider.class);
   @Autowired
   private VsanClient vsanClient;

   @TsService
   public ManagedObjectReference repairClusterObjectsImmediate(ManagedObjectReference clusterRef) throws Exception {
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var3 = null;

      ManagedObjectReference var8;
      try {
         VsanVcClusterHealthSystem healthSystem = conn.getVsanVcClusterHealthSystem();
         VsanProfiler.Point p = _profiler.point("healthSystem.repairClusterObjectsImmediate");
         Throwable var6 = null;

         try {
            ManagedObjectReference taskRef = healthSystem.repairClusterObjectsImmediate(clusterRef, (String[])null);
            taskRef.setServerGuid(clusterRef.getServerGuid());
            var8 = taskRef;
         } catch (Throwable var31) {
            var6 = var31;
            throw var31;
         } finally {
            if (p != null) {
               if (var6 != null) {
                  try {
                     p.close();
                  } catch (Throwable var30) {
                     var6.addSuppressed(var30);
                  }
               } else {
                  p.close();
               }
            }

         }
      } catch (Throwable var33) {
         var3 = var33;
         throw var33;
      } finally {
         if (conn != null) {
            if (var3 != null) {
               try {
                  conn.close();
               } catch (Throwable var29) {
                  var3.addSuppressed(var29);
               }
            } else {
               conn.close();
            }
         }

      }

      return var8;
   }

   @TsService
   public void setTelementryConfig(ManagedObjectReference entity, ExternalProxySettingsConfig config) {
      Validate.notNull(entity);
      Validate.notNull(config);

      try {
         VsanClusterHealthConfigs newConfigs = new VsanClusterHealthConfigs();
         if (!StringUtils.isBlank(config.hostName) && !StringUtils.isBlank(String.valueOf(config.port))) {
            VsanClusterTelemetryProxyConfig vsanTelemetryProxy = new VsanClusterTelemetryProxyConfig();
            vsanTelemetryProxy.setHost(config.hostName);
            vsanTelemetryProxy.setPort(config.port);
            vsanTelemetryProxy.setUser(config.userName);
            vsanTelemetryProxy.setPassword(config.password);
            newConfigs.setVsanTelemetryProxy(vsanTelemetryProxy);
         }

         newConfigs.setConfigs(new VsanClusterHealthResultKeyValuePair[]{new VsanClusterHealthResultKeyValuePair("enableInternetAccess", String.valueOf(Boolean.TRUE.equals(config.enableInternetAccess)))});
         VsanConnection conn = this.vsanClient.getConnection(entity.getServerGuid());
         Throwable var5 = null;

         try {
            VsanVcClusterHealthSystem healthSystem = conn.getVsanVcClusterHealthSystem();
            VsanProfiler.Point p = _profiler.point("healthSystem.setVsanClusterTelemetryConfig");
            Throwable var8 = null;

            try {
               healthSystem.setVsanClusterTelemetryConfig(entity, newConfigs);
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
         } catch (Throwable var35) {
            var5 = var35;
            throw var35;
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

      } catch (Exception var37) {
         logger.error("Failed to set telemetry config", var37);
         throw new VsanUiLocalizableException("vsan.internet.error.configuration.error", var37);
      }
   }

   @TsService
   public ManagedObjectReference rebalanceCluster(ManagedObjectReference clusterRef) throws Exception {
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var3 = null;

      ManagedObjectReference var8;
      try {
         VsanVcClusterHealthSystem healthSystem = conn.getVsanVcClusterHealthSystem();
         VsanProfiler.Point p = _profiler.point("healthSystem.rebalanceCluster");
         Throwable var6 = null;

         try {
            ManagedObjectReference taskRef = healthSystem.rebalanceCluster(clusterRef, (ManagedObjectReference[])null);
            VmodlHelper.assignServerGuid(taskRef, clusterRef.getServerGuid());
            var8 = taskRef;
         } catch (Throwable var31) {
            var6 = var31;
            throw var31;
         } finally {
            if (p != null) {
               if (var6 != null) {
                  try {
                     p.close();
                  } catch (Throwable var30) {
                     var6.addSuppressed(var30);
                  }
               } else {
                  p.close();
               }
            }

         }
      } catch (Throwable var33) {
         var3 = var33;
         throw var33;
      } finally {
         if (conn != null) {
            if (var3 != null) {
               try {
                  conn.close();
               } catch (Throwable var29) {
                  var3.addSuppressed(var29);
               }
            } else {
               conn.close();
            }
         }

      }

      return var8;
   }

   @TsService
   public ManagedObjectReference stopRebalanceCluster(ManagedObjectReference clusterRef) throws Exception {
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var3 = null;

      ManagedObjectReference var8;
      try {
         VsanVcClusterHealthSystem healthSystem = conn.getVsanVcClusterHealthSystem();
         VsanProfiler.Point p = _profiler.point("healthSystem.stopRebalanceCluster");
         Throwable var6 = null;

         try {
            ManagedObjectReference taskRef = healthSystem.stopRebalanceCluster(clusterRef, (ManagedObjectReference[])null);
            VmodlHelper.assignServerGuid(taskRef, clusterRef.getServerGuid());
            var8 = taskRef;
         } catch (Throwable var31) {
            var6 = var31;
            throw var31;
         } finally {
            if (p != null) {
               if (var6 != null) {
                  try {
                     p.close();
                  } catch (Throwable var30) {
                     var6.addSuppressed(var30);
                  }
               } else {
                  p.close();
               }
            }

         }
      } catch (Throwable var33) {
         var3 = var33;
         throw var33;
      } finally {
         if (conn != null) {
            if (var3 != null) {
               try {
                  conn.close();
               } catch (Throwable var29) {
                  var3.addSuppressed(var29);
               }
            } else {
               conn.close();
            }
         }

      }

      return var8;
   }

   @TsService
   public void clearTelementryConfig(ManagedObjectReference entity, ExternalProxySettingsConfig spec) throws Exception {
      VsanProfiler.Point p = _profiler.point("healthSystem.setVsanClusterTelemetryConfig");
      Throwable var4 = null;

      try {
         Validate.notNull(entity);
         VsanConnection conn = this.vsanClient.getConnection(entity.getServerGuid());
         Throwable var6 = null;

         try {
            VsanVcClusterHealthSystem healthSystem = conn.getVsanVcClusterHealthSystem();
            VsanClusterHealthConfigs newConfigs = new VsanClusterHealthConfigs();
            newConfigs.setVsanTelemetryProxy(new VsanClusterTelemetryProxyConfig());
            healthSystem.setVsanClusterTelemetryConfig(entity, newConfigs);
         } catch (Throwable var30) {
            var6 = var30;
            throw var30;
         } finally {
            if (conn != null) {
               if (var6 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var29) {
                     var6.addSuppressed(var29);
                  }
               } else {
                  conn.close();
               }
            }

         }
      } catch (Throwable var32) {
         var4 = var32;
         throw var32;
      } finally {
         if (p != null) {
            if (var4 != null) {
               try {
                  p.close();
               } catch (Throwable var28) {
                  var4.addSuppressed(var28);
               }
            } else {
               p.close();
            }
         }

      }

   }

   @TsService
   public ManagedObjectReference performUpgrade(ManagedObjectReference entity, VsanConvertDiskFormatSpec spec) throws Exception {
      VsanConnection conn = this.vsanClient.getConnection(entity.getServerGuid());
      Throwable var4 = null;

      ManagedObjectReference var9;
      try {
         VsanUpgradeSystemEx upgradeSystem = conn.getVsanUpgradeSystemEx();
         VsanProfiler.Point p = _profiler.point("upgradeSystemEx.performUpgrade");
         Throwable var7 = null;

         try {
            ManagedObjectReference taskRef = upgradeSystem.performUpgrade(entity, (Boolean)null, (Boolean)null, spec.allowReducedRedundancy, (ManagedObjectReference[])null, (VsanDiskFormatConversionSpec)null);
            VmodlHelper.assignServerGuid(taskRef, entity.getServerGuid());
            var9 = taskRef;
         } catch (Throwable var32) {
            var7 = var32;
            throw var32;
         } finally {
            if (p != null) {
               if (var7 != null) {
                  try {
                     p.close();
                  } catch (Throwable var31) {
                     var7.addSuppressed(var31);
                  }
               } else {
                  p.close();
               }
            }

         }
      } catch (Throwable var34) {
         var4 = var34;
         throw var34;
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

      return var9;
   }

   @TsService
   public ManagedObjectReference remediateCluster(ManagedObjectReference entity) throws Exception {
      VsanConnection conn = this.vsanClient.getConnection(entity.getServerGuid());
      Throwable var3 = null;

      ManagedObjectReference var8;
      try {
         VsanClusterMgmtInternalSystem system = conn.getVsanClusterMgmtInternalSystem();
         VsanProfiler.Point p = _profiler.point("VsanClusterMgmtInternalSystem.remediateVsanCluster");
         Throwable var6 = null;

         try {
            ManagedObjectReference taskRef = system.remediateVsanCluster(entity);
            if (taskRef == null) {
               return null;
            }

            var8 = VsanHealthUtil.buildTaskMor(taskRef.getValue(), entity.getServerGuid());
         } catch (Throwable var34) {
            var6 = var34;
            throw var34;
         } finally {
            if (p != null) {
               if (var6 != null) {
                  try {
                     p.close();
                  } catch (Throwable var33) {
                     var6.addSuppressed(var33);
                  }
               } else {
                  p.close();
               }
            }

         }
      } catch (Throwable var36) {
         var3 = var36;
         throw var36;
      } finally {
         if (conn != null) {
            if (var3 != null) {
               try {
                  conn.close();
               } catch (Throwable var32) {
                  var3.addSuppressed(var32);
               }
            } else {
               conn.close();
            }
         }

      }

      return var8;
   }
}
