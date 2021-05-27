package com.vmware.vsphere.client.vsan.impl;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vim.cluster.ConfigSpecEx;
import com.vmware.vim.binding.vim.fault.VsanFault;
import com.vmware.vim.binding.vim.host.VsanSystem;
import com.vmware.vim.binding.vim.vsan.cluster.ConfigInfo;
import com.vmware.vim.binding.vim.vsan.host.ConfigInfo.FaultDomainInfo;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanPerfsvcConfig;
import com.vmware.vim.vsan.binding.vim.cluster.VsanVcClusterConfigSystem;
import com.vmware.vim.vsan.binding.vim.vsan.ConfigInfoEx;
import com.vmware.vim.vsan.binding.vim.vsan.ReconfigSpec;
import com.vmware.vim.vsan.binding.vim.vsan.ResyncIopsInfo;
import com.vmware.vim.vsan.binding.vim.vsan.cluster.VsanPMemConfigSpec;
import com.vmware.vsan.client.services.BackendLocalizedException;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.services.config.VsanConfigService;
import com.vmware.vsan.client.services.config.model.ClusterMode;
import com.vmware.vsan.client.services.diskmanagement.claiming.HostDisksClaimer;
import com.vmware.vsan.client.services.encryption.EncryptionPropertyProvider;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcConnection;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.VmodlHelper;
import com.vmware.vsphere.client.vsan.base.util.VsanProfiler;
import com.vmware.vsphere.client.vsan.config.ResyncThrottlingSpec;
import com.vmware.vsphere.client.vsan.data.VsanConfigSpec;
import com.vmware.vsphere.client.vsan.spec.VsanFaultDomainSpec;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class ConfigureVsanClusterMutationProvider {
   private static final Log logger = LogFactory.getLog(ConfigureVsanClusterMutationProvider.class);
   private static final VsanProfiler profiler = new VsanProfiler(ConfigureVsanClusterMutationProvider.class);
   @Autowired
   private EncryptionPropertyProvider encryptionPropertyProvider;
   @Autowired
   private VsanClient vsanClient;
   @Autowired
   private VcClient vcClient;
   @Autowired
   private HostDisksClaimer hostDisksClaimer;
   @Autowired
   private VsanConfigService vsanConfigService;

   @TsService
   public ManagedObjectReference configure(ManagedObjectReference clusterRef, VsanConfigSpec spec) {
      Validate.notNull(clusterRef);
      Validate.notNull(spec);
      ReconfigSpec reconfigSpec = this.getReconfigSpec(clusterRef, spec);
      return VsanCapabilityUtils.isClusterConfigSystemSupportedOnVc(clusterRef) ? this.startReconfigureTask(clusterRef, reconfigSpec) : this.startLegacyApiReconfigureTask(clusterRef, reconfigSpec);
   }

   public ManagedObjectReference startReconfigureTask(ManagedObjectReference clusterRef, ReconfigSpec reconfigSpec) {
      logger.info("Invoke configure vSAN cluster mutation operation for cluster: " + clusterRef);

      try {
         VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
         Throwable var4 = null;

         ManagedObjectReference var9;
         try {
            VsanVcClusterConfigSystem vsanConfigSystem = conn.getVsanConfigSystem();
            VsanProfiler.Point point = profiler.point("vsanConfigSystem.reconfigureEx");
            Throwable var7 = null;

            try {
               ManagedObjectReference taskRef = vsanConfigSystem.reconfigureEx(clusterRef, reconfigSpec);
               var9 = VmodlHelper.assignServerGuid(taskRef, clusterRef.getServerGuid());
            } catch (Throwable var36) {
               var7 = var36;
               throw var36;
            } finally {
               if (point != null) {
                  if (var7 != null) {
                     try {
                        point.close();
                     } catch (Throwable var35) {
                        var7.addSuppressed(var35);
                     }
                  } else {
                     point.close();
                  }
               }

            }
         } catch (Throwable var38) {
            var4 = var38;
            throw var38;
         } finally {
            if (conn != null) {
               if (var4 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var34) {
                     var4.addSuppressed(var34);
                  }
               } else {
                  conn.close();
               }
            }

         }

         return var9;
      } catch (VsanFault var40) {
         if (!StringUtils.isEmpty(var40.getLocalizedMessage())) {
            throw new BackendLocalizedException(var40);
         } else {
            throw new VsanUiLocalizableException("vsan.common.cluster.reconfigure.error", var40);
         }
      } catch (Exception var41) {
         throw new VsanUiLocalizableException("vsan.common.cluster.reconfigure.error", var41);
      }
   }

   private ManagedObjectReference startLegacyApiReconfigureTask(ManagedObjectReference clusterRef, ReconfigSpec reconfigSpec) {
      ConfigSpecEx clusterSpecEx = new ConfigSpecEx();
      clusterSpecEx.vsanConfig = reconfigSpec.vsanClusterConfig;
      VcConnection vcConnection = this.vcClient.getConnection(clusterRef.getServerGuid());
      Throwable var5 = null;

      ManagedObjectReference var10;
      try {
         ClusterComputeResource cluster = (ClusterComputeResource)vcConnection.createStub(ClusterComputeResource.class, clusterRef);
         VsanProfiler.Point point = profiler.point("cluster.reconfigureEx");
         Throwable var8 = null;

         try {
            ManagedObjectReference configureClusterTask = cluster.reconfigureEx(clusterSpecEx, true);
            var10 = VmodlHelper.assignServerGuid(configureClusterTask, clusterRef.getServerGuid());
         } catch (Throwable var33) {
            var8 = var33;
            throw var33;
         } finally {
            if (point != null) {
               if (var8 != null) {
                  try {
                     point.close();
                  } catch (Throwable var32) {
                     var8.addSuppressed(var32);
                  }
               } else {
                  point.close();
               }
            }

         }
      } catch (Throwable var35) {
         var5 = var35;
         throw var35;
      } finally {
         if (vcConnection != null) {
            if (var5 != null) {
               try {
                  vcConnection.close();
               } catch (Throwable var31) {
                  var5.addSuppressed(var31);
               }
            } else {
               vcConnection.close();
            }
         }

      }

      return var10;
   }

   private ReconfigSpec getReconfigSpec(ManagedObjectReference clusterRef, VsanConfigSpec spec) {
      if (spec.clusterMode == ClusterMode.COMPUTE) {
         return spec.getComputeOnlyReconfigSpec();
      } else {
         boolean hasEncryptionPermissions = true;
         boolean hasRekeyPermission = true;

         try {
            hasEncryptionPermissions = this.encryptionPropertyProvider.getEncryptionPermissions(clusterRef);
            hasRekeyPermission = this.encryptionPropertyProvider.getReKeyPermissions(clusterRef);
         } catch (Exception var7) {
         }

         ReconfigSpec reconfigSpec = spec.getReconfigSpec(clusterRef, hasEncryptionPermissions, hasRekeyPermission, this.hostDisksClaimer);
         ConfigInfoEx vsanConfig = this.vsanConfigService.getConfigInfoEx(clusterRef);
         if (vsanConfig != null) {
            if (vsanConfig.perfsvcConfig == null) {
               reconfigSpec.perfsvcConfig = new VsanPerfsvcConfig();
               reconfigSpec.perfsvcConfig.enabled = true;
            } else {
               reconfigSpec.perfsvcConfig = vsanConfig.perfsvcConfig;
            }
         }

         return reconfigSpec;
      }
   }

   @TsService
   public ManagedObjectReference turnOffVsan(ManagedObjectReference clusterRef) {
      Validate.notNull(clusterRef);
      ManagedObjectReference configureClusterTask = null;
      ConfigInfo configInfo = new ConfigInfo();
      configInfo.enabled = false;
      Throwable var6;
      VsanProfiler.Point point;
      Throwable var9;
      if (VsanCapabilityUtils.isClusterConfigSystemSupportedOnVc(clusterRef)) {
         ReconfigSpec reconfigSpec = new ReconfigSpec();
         reconfigSpec.vsanClusterConfig = configInfo;
         reconfigSpec.modify = true;
         if (VsanCapabilityUtils.isComputeOnlySupported(clusterRef)) {
            reconfigSpec.mode = ClusterMode.NONE.getKey();
         }

         try {
            VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
            var6 = null;

            try {
               VsanVcClusterConfigSystem vsanConfigSystem = conn.getVsanConfigSystem();
               point = profiler.point("vsanConfigSystem.reconfigureEx");
               var9 = null;

               try {
                  configureClusterTask = vsanConfigSystem.reconfigureEx(clusterRef, reconfigSpec);
               } catch (Throwable var86) {
                  var9 = var86;
                  throw var86;
               } finally {
                  if (point != null) {
                     if (var9 != null) {
                        try {
                           point.close();
                        } catch (Throwable var82) {
                           var9.addSuppressed(var82);
                        }
                     } else {
                        point.close();
                     }
                  }

               }
            } catch (Throwable var88) {
               var6 = var88;
               throw var88;
            } finally {
               if (conn != null) {
                  if (var6 != null) {
                     try {
                        conn.close();
                     } catch (Throwable var81) {
                        var6.addSuppressed(var81);
                     }
                  } else {
                     conn.close();
                  }
               }

            }
         } catch (Exception var90) {
            throw new VsanUiLocalizableException("vsan.common.cluster.reconfigure.error", var90);
         }
      } else {
         ConfigSpecEx clusterSpecEx = new ConfigSpecEx();
         clusterSpecEx.vsanConfig = configInfo;
         VcConnection vcConnection = this.vcClient.getConnection(clusterRef.getServerGuid());
         var6 = null;

         try {
            ClusterComputeResource cluster = (ClusterComputeResource)vcConnection.createStub(ClusterComputeResource.class, clusterRef);
            point = profiler.point("cluster.reconfigureEx");
            var9 = null;

            try {
               configureClusterTask = cluster.reconfigureEx(clusterSpecEx, true);
            } catch (Throwable var85) {
               var9 = var85;
               throw var85;
            } finally {
               if (point != null) {
                  if (var9 != null) {
                     try {
                        point.close();
                     } catch (Throwable var84) {
                        var9.addSuppressed(var84);
                     }
                  } else {
                     point.close();
                  }
               }

            }
         } catch (Throwable var92) {
            var6 = var92;
            throw var92;
         } finally {
            if (vcConnection != null) {
               if (var6 != null) {
                  try {
                     vcConnection.close();
                  } catch (Throwable var83) {
                     var6.addSuppressed(var83);
                  }
               } else {
                  vcConnection.close();
               }
            }

         }
      }

      VmodlHelper.assignServerGuid(configureClusterTask, clusterRef.getServerGuid());
      return configureClusterTask;
   }

   @TsService
   public ManagedObjectReference updateFaultDomainInfo(ManagedObjectReference hostRef, VsanFaultDomainSpec spec) {
      com.vmware.vim.binding.vim.vsan.host.ConfigInfo vsanConfig = new com.vmware.vim.binding.vim.vsan.host.ConfigInfo();
      String faultDomain = spec.faultDomain;
      if (faultDomain == null) {
         faultDomain = "";
      }

      vsanConfig.faultDomainInfo = new FaultDomainInfo(faultDomain);
      VcConnection conn = this.vcClient.getConnection(hostRef.getServerGuid());
      Throwable var6 = null;

      ManagedObjectReference var11;
      try {
         VsanSystem vsanSystem = conn.getHostVsanSystem(hostRef);
         VsanProfiler.Point point = profiler.point("vsanSystem.update");
         Throwable var9 = null;

         try {
            ManagedObjectReference task = vsanSystem.update(vsanConfig);
            var11 = VmodlHelper.assignServerGuid(task, hostRef.getServerGuid());
         } catch (Throwable var34) {
            var9 = var34;
            throw var34;
         } finally {
            if (point != null) {
               if (var9 != null) {
                  try {
                     point.close();
                  } catch (Throwable var33) {
                     var9.addSuppressed(var33);
                  }
               } else {
                  point.close();
               }
            }

         }
      } catch (Throwable var36) {
         var6 = var36;
         throw var36;
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

      return var11;
   }

   @TsService
   public ManagedObjectReference resyncThrottling(ManagedObjectReference clusterRef, ResyncThrottlingSpec spec) {
      ReconfigSpec reconfigSpec = new ReconfigSpec();
      reconfigSpec.resyncIopsLimitConfig = new ResyncIopsInfo();
      reconfigSpec.resyncIopsLimitConfig.setResyncIops(spec.iopsLimit);
      reconfigSpec.setModify(true);
      return this.startReconfigureTask(clusterRef, reconfigSpec);
   }

   @TsService
   public ManagedObjectReference stopManagingPmem(ManagedObjectReference clusterRef) {
      ReconfigSpec reconfigSpec = new ReconfigSpec();
      reconfigSpec.vsanPMemConfigSpec = new VsanPMemConfigSpec();
      reconfigSpec.vsanPMemConfigSpec.setEnabled(false);
      reconfigSpec.setModify(true);
      return this.startReconfigureTask(clusterRef, reconfigSpec);
   }
}
