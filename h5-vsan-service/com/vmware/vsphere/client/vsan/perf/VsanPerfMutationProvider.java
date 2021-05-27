package com.vmware.vsphere.client.vsan.perf;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vim.vm.DefinedProfileSpec;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanPerfTimeRange;
import com.vmware.vim.vsan.binding.vim.cluster.VsanPerformanceManager;
import com.vmware.vim.vsan.binding.vim.cluster.VsanPerfsvcConfig;
import com.vmware.vim.vsan.binding.vim.cluster.VsanVcClusterConfigSystem;
import com.vmware.vim.vsan.binding.vim.vsan.ReconfigSpec;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.services.config.VsanConfigService;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.VmodlHelper;
import com.vmware.vsphere.client.vsan.base.data.VsanCapabilityData;
import com.vmware.vsphere.client.vsan.base.util.BaseUtils;
import com.vmware.vsphere.client.vsan.base.util.VsanProfiler;
import com.vmware.vsphere.client.vsan.impl.ConfigureVsanClusterMutationProvider;
import com.vmware.vsphere.client.vsan.perf.model.PerfStatesObjSpec;
import com.vmware.vsphere.client.vsan.perf.model.PerfTimeRangeData;
import java.util.Calendar;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;

public class VsanPerfMutationProvider {
   private static final VsanProfiler _profiler = new VsanProfiler(VsanPerfMutationProvider.class);
   @Autowired
   private VsanClient vsanClient;
   @Autowired
   private ConfigureVsanClusterMutationProvider configureClusterService;
   @Autowired
   private VsanConfigService vsanConfigService;

   @TsService
   public ManagedObjectReference disablePerfService(ManagedObjectReference clusterRef) {
      if (VsanCapabilityUtils.isPerfSvcAutoConfigSupportedOnVc(clusterRef)) {
         ReconfigSpec spec = this.getClusterReconfigSpecForPerfService(clusterRef, (String)null, false);
         VsanCapabilityData vcCapabilities = VsanCapabilityUtils.getVcCapabilities(clusterRef);
         if (vcCapabilities.isPerfDiagnosticModeSupported) {
            spec.perfsvcConfig.diagnosticMode = false;
         }

         if (vcCapabilities.isVerboseModeInClusterConfigurationSupported) {
            spec.perfsvcConfig.verboseMode = false;
         } else if (vcCapabilities.isPerfVerboseModeSupported) {
            this.toggleVerboseMode(clusterRef, false);
         }

         return this.configureClusterService.startReconfigureTask(clusterRef, spec);
      } else {
         VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
         Throwable var3 = null;

         ManagedObjectReference var8;
         try {
            VsanPerformanceManager perfMgr = conn.getVsanPerformanceManager();

            try {
               VsanProfiler.Point p = _profiler.point("perfMgr.deleteStatsObjectTask");
               Throwable var6 = null;

               try {
                  ManagedObjectReference taskRef = perfMgr.deleteStatsObjectTask(clusterRef);
                  var8 = VmodlHelper.assignServerGuid(taskRef, clusterRef.getServerGuid());
               } catch (Throwable var33) {
                  var6 = var33;
                  throw var33;
               } finally {
                  if (p != null) {
                     if (var6 != null) {
                        try {
                           p.close();
                        } catch (Throwable var32) {
                           var6.addSuppressed(var32);
                        }
                     } else {
                        p.close();
                     }
                  }

               }
            } catch (Exception var35) {
               throw new VsanUiLocalizableException("vsan.perf.query.task.error", var35);
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
      }
   }

   @TsService
   public ManagedObjectReference enablePerfService(PerfStatesObjSpec spec) throws Exception {
      ManagedObjectReference taskRef = null;
      boolean isPerfSvcAutoConfigSupported = VsanCapabilityUtils.isPerfSvcAutoConfigSupportedOnVc(spec.clusterRef);
      VsanConnection conn;
      Throwable var5;
      VsanProfiler.Point p;
      Throwable var8;
      if (isPerfSvcAutoConfigSupported) {
         conn = this.vsanClient.getConnection(spec.clusterRef.getServerGuid());
         var5 = null;

         try {
            VsanVcClusterConfigSystem vsanConfigSystem = conn.getVsanConfigSystem();
            p = _profiler.point("vsanClusterConfigSystem.reconfigureEx");
            var8 = null;

            try {
               taskRef = vsanConfigSystem.reconfigureEx(spec.clusterRef, this.getClusterReconfigSpecForPerfService(spec.clusterRef, spec.profileId, true));
            } catch (Throwable var81) {
               var8 = var81;
               throw var81;
            } finally {
               if (p != null) {
                  if (var8 != null) {
                     try {
                        p.close();
                     } catch (Throwable var79) {
                        var8.addSuppressed(var79);
                     }
                  } else {
                     p.close();
                  }
               }

            }
         } catch (Throwable var86) {
            var5 = var86;
            throw var86;
         } finally {
            if (conn != null) {
               if (var5 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var78) {
                     var5.addSuppressed(var78);
                  }
               } else {
                  conn.close();
               }
            }

         }
      } else {
         conn = this.vsanClient.getConnection(spec.clusterRef.getServerGuid());
         var5 = null;

         try {
            VsanPerformanceManager perfMgr = conn.getVsanPerformanceManager();
            p = _profiler.point("perfMgr.createStatsObjectTask");
            var8 = null;

            try {
               DefinedProfileSpec definedSpec = new DefinedProfileSpec();
               definedSpec.setProfileId(spec.profileId);
               taskRef = perfMgr.createStatsObjectTask(spec.clusterRef, definedSpec);
            } catch (Throwable var80) {
               var8 = var80;
               throw var80;
            } finally {
               if (p != null) {
                  if (var8 != null) {
                     try {
                        p.close();
                     } catch (Throwable var77) {
                        var8.addSuppressed(var77);
                     }
                  } else {
                     p.close();
                  }
               }

            }
         } catch (Throwable var83) {
            var5 = var83;
            throw var83;
         } finally {
            if (conn != null) {
               if (var5 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var76) {
                     var5.addSuppressed(var76);
                  }
               } else {
                  conn.close();
               }
            }

         }
      }

      return taskRef != null ? new ManagedObjectReference(taskRef.getType(), taskRef.getValue(), spec.clusterRef.getServerGuid()) : null;
   }

   private ReconfigSpec getClusterReconfigSpecForPerfService(ManagedObjectReference clusterRef, String profileId, boolean enabled) {
      DefinedProfileSpec definedSpec = new DefinedProfileSpec();
      definedSpec.setProfileId(profileId);
      DefinedProfileSpec profileSpec = StringUtils.isBlank(profileId) ? null : definedSpec;
      VsanPerfsvcConfig perfConfig = this.vsanConfigService.getConfigInfoEx(clusterRef).perfsvcConfig;
      if (perfConfig == null) {
         perfConfig = new VsanPerfsvcConfig();
      }

      perfConfig.profile = profileSpec;
      perfConfig.enabled = enabled;
      ReconfigSpec reconfigSpec = new ReconfigSpec();
      reconfigSpec.perfsvcConfig = perfConfig;
      return reconfigSpec;
   }

   @TsService
   public ManagedObjectReference editPerfConfiguration(PerfStatesObjSpec spec) {
      Validate.notNull(spec.clusterRef);
      boolean isPerfSvcAutoConfigSupported = VsanCapabilityUtils.isPerfSvcAutoConfigSupportedOnVc(spec.clusterRef);
      if (!isPerfSvcAutoConfigSupported) {
         DefinedProfileSpec definedSpec = new DefinedProfileSpec();
         definedSpec.setProfileId(spec.profileId);
         DefinedProfileSpec profileSpec = spec.profileId == null ? null : definedSpec;
         VsanConnection conn = this.vsanClient.getConnection(spec.clusterRef.getServerGuid());
         Throwable var6 = null;

         Throwable var10;
         try {
            VsanPerformanceManager perfMgr = conn.getVsanPerformanceManager();

            try {
               VsanProfiler.Point p = _profiler.point("perfMgr.setStatsObjectPolicy");
               Throwable var9 = null;

               try {
                  perfMgr.setStatsObjectPolicy(spec.clusterRef, profileSpec);
                  var10 = null;
               } catch (Throwable var35) {
                  var10 = var35;
                  var9 = var35;
                  throw var35;
               } finally {
                  if (p != null) {
                     if (var9 != null) {
                        try {
                           p.close();
                        } catch (Throwable var34) {
                           var9.addSuppressed(var34);
                        }
                     } else {
                        p.close();
                     }
                  }

               }
            } catch (Exception var37) {
               throw new VsanUiLocalizableException("vsan.perf.query.task.error", var37);
            }
         } catch (Throwable var38) {
            var6 = var38;
            throw var38;
         } finally {
            if (conn != null) {
               if (var6 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var33) {
                     var6.addSuppressed(var33);
                  }
               } else {
                  conn.close();
               }
            }

         }

         return var10;
      } else {
         ReconfigSpec configSpec = this.getClusterReconfigSpecForPerfService(spec.clusterRef, spec.profileId, true);
         if (VsanCapabilityUtils.isPerfDiagnosticModeSupported(spec.clusterRef) || configSpec.perfsvcConfig.diagnosticMode != null) {
            configSpec.perfsvcConfig.diagnosticMode = spec.isNetworkDiagnosticModeEnabled;
         }

         if (VsanCapabilityUtils.isVerboseModeInClusterConfigurationSupported(spec.clusterRef)) {
            configSpec.perfsvcConfig.verboseMode = spec.isVerboseEnabled;
         } else {
            this.toggleVerboseMode(spec.clusterRef, spec.isVerboseEnabled);
         }

         return this.configureClusterService.startReconfigureTask(spec.clusterRef, configSpec);
      }
   }

   public void toggleVerboseMode(ManagedObjectReference clusterRef, boolean enableVerboseMode) {
      if (VsanCapabilityUtils.isPerfVerboseModeSupportedOnVc(clusterRef)) {
         VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
         Throwable var4 = null;

         try {
            VsanPerformanceManager perfMgr = conn.getVsanPerformanceManager();

            try {
               VsanProfiler.Point p = _profiler.point("perfMgr.toggleVerboseMode");
               Throwable var7 = null;

               try {
                  perfMgr.toggleVerboseMode(clusterRef, enableVerboseMode);
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
            } catch (Exception var34) {
               throw new VsanUiLocalizableException("vsan.perf.query.task.error", var34);
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
   }

   @TsService
   public void setTimeRanges(PerfTimeRangeData range) {
      VsanProfiler.Point p = _profiler.point("perfMgr.saveTimeRanges");
      Throwable var3 = null;

      try {
         VsanPerfTimeRange rangeObj = new VsanPerfTimeRange();
         rangeObj.name = range.name;
         rangeObj.startTime = Calendar.getInstance();
         BaseUtils.setUTCTimeZone(rangeObj.startTime);
         rangeObj.startTime.setTime(range.from);
         rangeObj.endTime = Calendar.getInstance();
         BaseUtils.setUTCTimeZone(rangeObj.endTime);
         rangeObj.endTime.setTime(range.to);

         try {
            VsanConnection conn = this.vsanClient.getConnection(range.clusterRef.getServerGuid());
            Throwable var6 = null;

            try {
               VsanPerformanceManager perfMgr = conn.getVsanPerformanceManager();
               perfMgr.saveTimeRanges(range.clusterRef, new VsanPerfTimeRange[]{rangeObj});
            } catch (Throwable var31) {
               var6 = var31;
               throw var31;
            } finally {
               if (conn != null) {
                  if (var6 != null) {
                     try {
                        conn.close();
                     } catch (Throwable var30) {
                        var6.addSuppressed(var30);
                     }
                  } else {
                     conn.close();
                  }
               }

            }
         } catch (Exception var33) {
            throw new VsanUiLocalizableException("vsan.perf.query.task.error", var33);
         }
      } catch (Throwable var34) {
         var3 = var34;
         throw var34;
      } finally {
         if (p != null) {
            if (var3 != null) {
               try {
                  p.close();
               } catch (Throwable var29) {
                  var3.addSuppressed(var29);
               }
            } else {
               p.close();
            }
         }

      }

   }

   @TsService
   public void deleteTimeRange(ManagedObjectReference clusterRef, PerfTimeRangeData range) {
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var4 = null;

      try {
         VsanPerformanceManager perfMgr = conn.getVsanPerformanceManager();

         try {
            VsanProfiler.Point p = _profiler.point("perfMgr.deleteTimeRange");
            Throwable var7 = null;

            try {
               perfMgr.deleteTimeRange(clusterRef, range.name);
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
         } catch (Exception var34) {
            throw new VsanUiLocalizableException("vsan.perf.query.task.error", var34);
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
}
