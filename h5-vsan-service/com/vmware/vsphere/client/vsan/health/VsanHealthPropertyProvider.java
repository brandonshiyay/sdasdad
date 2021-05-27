package com.vmware.vsphere.client.vsan.health;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vim.HostSystem;
import com.vmware.vim.binding.vim.HostSystem.ConnectionState;
import com.vmware.vim.binding.vim.ManagedEntity.Status;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.VsanPhoneHomeSystem;
import com.vmware.vim.vsan.binding.vim.VsanVcPrecheckerSystem;
import com.vmware.vim.vsan.binding.vim.cluster.VsanClusterHealthConfigs;
import com.vmware.vim.vsan.binding.vim.cluster.VsanClusterHealthQuerySpec;
import com.vmware.vim.vsan.binding.vim.cluster.VsanClusterHealthResultKeyValuePair;
import com.vmware.vim.vsan.binding.vim.cluster.VsanClusterHealthSummary;
import com.vmware.vim.vsan.binding.vim.cluster.VsanClusterTelemetryProxyConfig;
import com.vmware.vim.vsan.binding.vim.cluster.VsanClusterVMsHealthOverallResult;
import com.vmware.vim.vsan.binding.vim.cluster.VsanClusterVMsHealthSummaryResult;
import com.vmware.vim.vsan.binding.vim.cluster.VsanComplianceResourceCheckStatus;
import com.vmware.vim.vsan.binding.vim.cluster.VsanVcClusterHealthSystem;
import com.vmware.vim.vsan.binding.vim.host.VsanPhysicalDiskHealth;
import com.vmware.vim.vsan.binding.vim.host.VsanPhysicalDiskHealthSummary;
import com.vmware.vim.vsan.binding.vim.vsan.VsanDiskComplianceResourceCheck;
import com.vmware.vim.vsan.binding.vim.vsan.VsanDiskGroupComplianceResourceCheck;
import com.vmware.vim.vsan.binding.vim.vsan.VsanFaultDomainComplianceResourceCheck;
import com.vmware.vim.vsan.binding.vim.vsan.VsanHealthPerspective;
import com.vmware.vim.vsan.binding.vim.vsan.VsanHostComplianceResourceCheck;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.services.common.CeipService;
import com.vmware.vsan.client.services.csd.CsdService;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vchealth.VcHealthClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vchealth.VcHealthConnection;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsan.client.util.VmodlHelper;
import com.vmware.vsphere.client.vsan.health.model.HealthCapabilityData;
import com.vmware.vsphere.client.vsan.health.util.VsanHealthUtil;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class VsanHealthPropertyProvider {
   private static final Log logger = LogFactory.getLog(VsanHealthPropertyProvider.class);
   private static final String[] REQUIRED_FIELDS = new String[]{"groups", "timestamp"};
   @Autowired
   private VsanClient vsanClient;
   @Autowired
   private VcHealthClient vcHealthClient;
   @Autowired
   private CeipService ceipService;
   @Autowired
   private CsdService csdService;
   @Autowired
   private VmodlHelper vmodlHelper;

   @TsService
   public VsanHealthData getVsanHealthFromCache(ManagedObjectReference moRef, Boolean isDefaultPerspective, boolean isVsphereHealth) {
      return isVsphereHealth ? this.getVcHealthHealthSummary(moRef, true, true) : this.getVsanHealthSummary(moRef, true, true, isDefaultPerspective);
   }

   @TsService
   public VsanHealthData getVsanHealth(ManagedObjectReference moRef, Boolean isDefaultPerspective, boolean isVsphereHealth) {
      return isVsphereHealth ? this.getVcHealthHealthSummary(moRef, true, false) : this.getVsanHealthSummary(moRef, true, false, isDefaultPerspective);
   }

   @TsService
   public VsanHealthData getVsanHealthSummaryFromCache(ManagedObjectReference clusterRef) {
      return this.getVsanHealthSummary(clusterRef, false, true, true);
   }

   private VsanHealthData getVsanHealthSummary(ManagedObjectReference clusterRef, boolean includeObjUuids, boolean fetchFromCache, Boolean isDefaultPerspective) {
      String perspective = isDefaultPerspective ? VsanHealthPerspective.defaultView.toString() : VsanHealthPerspective.deployAssist.toString();

      try {
         VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
         Throwable var7 = null;

         VsanHealthData var10;
         try {
            VsanVcClusterHealthSystem healthSystem = conn.getVsanVcClusterHealthSystem();
            VsanClusterHealthSummary healthSummary = this.getClusterHealthSummary(healthSystem, clusterRef, includeObjUuids, fetchFromCache, perspective);
            var10 = VsanHealthUtil.getVsanHealthData(healthSummary, clusterRef, false, false);
         } catch (Throwable var20) {
            var7 = var20;
            throw var20;
         } finally {
            if (conn != null) {
               if (var7 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var19) {
                     var7.addSuppressed(var19);
                  }
               } else {
                  conn.close();
               }
            }

         }

         return var10;
      } catch (Exception var22) {
         throw new VsanUiLocalizableException("vsan.health.system.get.error", var22);
      }
   }

   private VsanHealthData getVcHealthHealthSummary(ManagedObjectReference moRef, boolean includeObjUuids, boolean fetchFromCache) {
      try {
         VcHealthConnection vcHealthConnection = this.vcHealthClient.getConnection(moRef.getServerGuid());
         Throwable var5 = null;

         VsanHealthData var8;
         try {
            VsanVcClusterHealthSystem healthSystem = vcHealthConnection.getVsphereHealthSystem();
            VsanClusterHealthSummary healthSummary = this.getClusterHealthSummary(healthSystem, moRef, includeObjUuids, fetchFromCache, (String)null);
            var8 = VsanHealthUtil.getVsanHealthData(healthSummary, moRef, false, true);
         } catch (Throwable var18) {
            var5 = var18;
            throw var18;
         } finally {
            if (vcHealthConnection != null) {
               if (var5 != null) {
                  try {
                     vcHealthConnection.close();
                  } catch (Throwable var17) {
                     var5.addSuppressed(var17);
                  }
               } else {
                  vcHealthConnection.close();
               }
            }

         }

         return var8;
      } catch (Exception var20) {
         throw new VsanUiLocalizableException("vsan.health.system.get.error", var20);
      }
   }

   private VsanClusterHealthSummary getClusterHealthSummary(VsanVcClusterHealthSystem healthSystem, ManagedObjectReference moRef, boolean includeObjUuids, boolean fetchFromCache, String perspective) {
      try {
         Measure m = new Measure("#VsanClusterHealthSummary.queryClusterHealthSummary");
         Throwable var7 = null;

         VsanClusterHealthSummary var8;
         try {
            var8 = healthSystem.queryClusterHealthSummary(moRef, (Integer)null, (String[])null, includeObjUuids, REQUIRED_FIELDS, fetchFromCache, perspective, (ManagedObjectReference[])null, (VsanClusterHealthQuerySpec)null, (Boolean)null);
         } catch (Throwable var18) {
            var7 = var18;
            throw var18;
         } finally {
            if (m != null) {
               if (var7 != null) {
                  try {
                     m.close();
                  } catch (Throwable var17) {
                     var7.addSuppressed(var17);
                  }
               } else {
                  m.close();
               }
            }

         }

         return var8;
      } catch (Exception var20) {
         throw new VsanUiLocalizableException("vsan.health.status.error", var20);
      }
   }

   /** @deprecated */
   @Deprecated
   @TsService
   public boolean isSilentCheckSupported(ManagedObjectReference clusterRef) {
      return VsanCapabilityUtils.isSilentCheckSupportedOnVc(clusterRef);
   }

   @TsService
   public HealthCapabilityData getHealthCapabilityData(ManagedObjectReference moRef, boolean isVsphereHealth) {
      HealthCapabilityData data = new HealthCapabilityData();

      try {
         data.isCeipServiceEnabled = this.ceipService.getCeipServiceEnabled(moRef);
      } catch (Exception var5) {
         data.isCeipServiceEnabled = false;
         logger.error("Unable to retrieve getCeipServiceEnabled for " + moRef, var5);
      }

      data.isSilentCheckSupported = !this.vmodlHelper.isOfType(moRef, HostSystem.class) && VsanCapabilityUtils.isSilentCheckSupportedOnVc(moRef);
      if (!isVsphereHealth) {
         data.isCloudHealthSupported = VsanCapabilityUtils.isCloudHealthSupportedOnVc(moRef);

         try {
            data.isHealthTaskSupported = VsanCapabilityUtils.isCsdSupportedOnVC(moRef) && !this.csdService.getRemoteDatastores(moRef).isEmpty();
         } catch (Exception var6) {
            logger.warn("Could not fetch remote datastores.", var6);
            data.isHealthTaskSupported = false;
         }

         data.isHistoricalHealthSupported = ClusterComputeResource.class.getSimpleName().equals(moRef.getType()) ? VsanCapabilityUtils.isHistoricalHealthSupported(moRef) : false;
      }

      return data;
   }

   @TsService
   public String[] getSilentChecks(ManagedObjectReference moRef, boolean isVsphereHealth) {
      return isVsphereHealth ? this.getVcHealthSilenceChecks(moRef) : this.getVsanSilenceChecks(moRef);
   }

   private String[] getVcHealthSilenceChecks(ManagedObjectReference moRef) {
      try {
         VcHealthConnection vcHealthConnection = this.vcHealthClient.getConnection(moRef.getServerGuid());
         Throwable var3 = null;

         String[] var5;
         try {
            VsanVcClusterHealthSystem healthSystem = vcHealthConnection.getVsphereHealthSystem();
            var5 = this.getSilentChecks(healthSystem, moRef);
         } catch (Throwable var15) {
            var3 = var15;
            throw var15;
         } finally {
            if (vcHealthConnection != null) {
               if (var3 != null) {
                  try {
                     vcHealthConnection.close();
                  } catch (Throwable var14) {
                     var3.addSuppressed(var14);
                  }
               } else {
                  vcHealthConnection.close();
               }
            }

         }

         return var5;
      } catch (Exception var17) {
         throw new VsanUiLocalizableException("vsan.health.system.get.error", var17);
      }
   }

   private String[] getVsanSilenceChecks(ManagedObjectReference moRef) {
      try {
         VsanConnection conn = this.vsanClient.getConnection(moRef.getServerGuid());
         Throwable var3 = null;

         String[] var5;
         try {
            VsanVcClusterHealthSystem healthSystem = conn.getVsanVcClusterHealthSystem();
            var5 = this.getSilentChecks(healthSystem, moRef);
         } catch (Throwable var15) {
            var3 = var15;
            throw var15;
         } finally {
            if (conn != null) {
               if (var3 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var14) {
                     var3.addSuppressed(var14);
                  }
               } else {
                  conn.close();
               }
            }

         }

         return var5;
      } catch (Exception var17) {
         throw new VsanUiLocalizableException("vsan.health.system.get.error", var17);
      }
   }

   private String[] getSilentChecks(VsanVcClusterHealthSystem healthSystem, ManagedObjectReference moRef) {
      try {
         Measure m = new Measure("VsanVcClusterHealthSystem.getVsanClusterSilentChecks");
         Throwable var4 = null;

         String[] var6;
         try {
            String[] checks = healthSystem.getVsanClusterSilentChecks(moRef);
            if (ArrayUtils.isEmpty(checks)) {
               return new String[0];
            }

            var6 = checks;
         } catch (Throwable var17) {
            var4 = var17;
            throw var17;
         } finally {
            if (m != null) {
               if (var4 != null) {
                  try {
                     m.close();
                  } catch (Throwable var16) {
                     var4.addSuppressed(var16);
                  }
               } else {
                  m.close();
               }
            }

         }

         return var6;
      } catch (Exception var19) {
         throw new VsanUiLocalizableException("vsan.health.check.silent.status.get.error", var19);
      }
   }

   @TsService
   public void setSilentChecks(ManagedObjectReference moRef, boolean isVsphereHealth, String[] addedSilenceChecks, String[] removedSilenceChecks) {
      if (isVsphereHealth) {
         this.setVcHealthSilenceChecks(moRef, addedSilenceChecks, removedSilenceChecks);
      } else {
         this.setVsanSilenceChecks(moRef, addedSilenceChecks, removedSilenceChecks);
      }

   }

   private void setVcHealthSilenceChecks(ManagedObjectReference moRef, String[] addedSilenceChecks, String[] removedSilenceChecks) {
      try {
         VcHealthConnection conn = this.vcHealthClient.getConnection(moRef.getServerGuid());
         Throwable var5 = null;

         try {
            VsanVcClusterHealthSystem healthSystem = conn.getVsphereHealthSystem();
            this.setSilentChecks(healthSystem, moRef, addedSilenceChecks, removedSilenceChecks);
         } catch (Throwable var15) {
            var5 = var15;
            throw var15;
         } finally {
            if (conn != null) {
               if (var5 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var14) {
                     var5.addSuppressed(var14);
                  }
               } else {
                  conn.close();
               }
            }

         }

      } catch (Exception var17) {
         throw new VsanUiLocalizableException("vsan.health.system.get.error", var17);
      }
   }

   private void setVsanSilenceChecks(ManagedObjectReference moRef, String[] addedSilenceChecks, String[] removedSilenceChecks) {
      try {
         VsanConnection conn = this.vsanClient.getConnection(moRef.getServerGuid());
         Throwable var5 = null;

         try {
            VsanVcClusterHealthSystem healthSystem = conn.getVsanVcClusterHealthSystem();
            this.setSilentChecks(healthSystem, moRef, addedSilenceChecks, removedSilenceChecks);
         } catch (Throwable var15) {
            var5 = var15;
            throw var15;
         } finally {
            if (conn != null) {
               if (var5 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var14) {
                     var5.addSuppressed(var14);
                  }
               } else {
                  conn.close();
               }
            }

         }

      } catch (Exception var17) {
         throw new VsanUiLocalizableException("vsan.health.system.get.error", var17);
      }
   }

   private void setSilentChecks(VsanVcClusterHealthSystem healthSystem, ManagedObjectReference moRef, String[] addedSilenceChecks, String[] removedSilenceChecks) {
      try {
         Measure m = new Measure("VsanVcClusterHealthSystem.setVsanClusterSilentChecks");
         Throwable var6 = null;

         try {
            healthSystem.setVsanClusterSilentChecks(moRef, addedSilenceChecks, removedSilenceChecks);
         } catch (Throwable var16) {
            var6 = var16;
            throw var16;
         } finally {
            if (m != null) {
               if (var6 != null) {
                  try {
                     m.close();
                  } catch (Throwable var15) {
                     var6.addSuppressed(var15);
                  }
               } else {
                  m.close();
               }
            }

         }

      } catch (Exception var18) {
         throw new VsanUiLocalizableException("vsan.health.check.silent.status.switch.error");
      }
   }

   @TsService
   public ManagedObjectReference startClusterHealthCheckTask(ManagedObjectReference clusterRef, Boolean includeOnlineHealth) throws Exception {
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var4 = null;

      ManagedObjectReference var9;
      try {
         VsanVcClusterHealthSystem healthSystem = conn.getVsanVcClusterHealthSystem();
         Measure m = new Measure("healthSystem.queryClusterHealthSummaryTask");
         Throwable var7 = null;

         try {
            ManagedObjectReference taskRef = healthSystem.queryClusterHealthSummaryTask(clusterRef, (ManagedObjectReference[])null, (Boolean)null, includeOnlineHealth);
            var9 = VsanHealthUtil.buildTaskMor(taskRef.getValue(), clusterRef.getServerGuid());
         } catch (Throwable var32) {
            var7 = var32;
            throw var32;
         } finally {
            if (m != null) {
               if (var7 != null) {
                  try {
                     m.close();
                  } catch (Throwable var31) {
                     var7.addSuppressed(var31);
                  }
               } else {
                  m.close();
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
   public VsanHealthData getClusterHealthCheckTaskResult(ManagedObjectReference clusterRef, ManagedObjectReference taskRef) throws Exception {
      VsanConnection conn = this.vsanClient.getConnection(taskRef.getServerGuid());
      Throwable var4 = null;

      VsanHealthData var10;
      try {
         VsanVcClusterHealthSystem healthSystem = conn.getVsanVcClusterHealthSystem();
         Measure m = new Measure("healthSystem.QueryClusterHealthSummary");
         Throwable var7 = null;

         try {
            VsanClusterHealthQuerySpec querySpec = new VsanClusterHealthQuerySpec();
            querySpec.setTask(taskRef);
            VsanClusterHealthSummary healthSummary = healthSystem.queryClusterHealthSummary(clusterRef, (Integer)null, (String[])null, (Boolean)null, (String[])null, true, VsanHealthPerspective.defaultView.toString(), (ManagedObjectReference[])null, querySpec, (Boolean)null);
            var10 = VsanHealthUtil.getVsanHealthData(healthSummary, clusterRef, false, false);
         } catch (Throwable var33) {
            var7 = var33;
            throw var33;
         } finally {
            if (m != null) {
               if (var7 != null) {
                  try {
                     m.close();
                  } catch (Throwable var32) {
                     var7.addSuppressed(var32);
                  }
               } else {
                  m.close();
               }
            }

         }
      } catch (Throwable var35) {
         var4 = var35;
         throw var35;
      } finally {
         if (conn != null) {
            if (var4 != null) {
               try {
                  conn.close();
               } catch (Throwable var31) {
                  var4.addSuppressed(var31);
               }
            } else {
               conn.close();
            }
         }

      }

      return var10;
   }

   @TsService
   public AggregatedVsanHealthSummary getCachedClusterHealthSummary(ManagedObjectReference clusterRef) throws Exception {
      VsanClusterHealthSummary summary = null;
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var4 = null;

      try {
         VsanVcClusterHealthSystem healthSystem = conn.getVsanVcClusterHealthSystem();
         Measure m = new Measure("healthSystem.queryClusterHealthSummary");
         Throwable var7 = null;

         try {
            summary = healthSystem.queryClusterHealthSummary(clusterRef, (Integer)null, (String[])null, true, (String[])null, true, VsanHealthPerspective.defaultView.toString(), (ManagedObjectReference[])null, (VsanClusterHealthQuerySpec)null, (Boolean)null);
         } catch (Throwable var34) {
            var7 = var34;
            throw var34;
         } finally {
            if (m != null) {
               if (var7 != null) {
                  try {
                     m.close();
                  } catch (Throwable var33) {
                     var7.addSuppressed(var33);
                  }
               } else {
                  m.close();
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

      HardwareOverallHealth physicalDisksHealth = new HardwareOverallHealth();
      int total = 0;
      int redCount = 0;
      int yellowCount = 0;
      VsanPhysicalDiskHealthSummary[] results = summary.physicalDisksHealth;
      ArrayList statusList = new ArrayList();
      VsanPhysicalDiskHealthSummary[] var9 = results;
      int var10 = results.length;

      int var14;
      int var15;
      for(int var11 = 0; var11 < var10; ++var11) {
         VsanPhysicalDiskHealthSummary pdSummary = var9[var11];
         statusList.add(pdSummary.overallHealth);
         total += pdSummary.disks == null ? 0 : pdSummary.disks.length;
         if (pdSummary.disks != null) {
            VsanPhysicalDiskHealth[] var13 = pdSummary.disks;
            var14 = var13.length;

            for(var15 = 0; var15 < var14; ++var15) {
               VsanPhysicalDiskHealth h = var13[var15];
               if (h.summaryHealth.equals(VsanHealthStatus.red.toString())) {
                  ++redCount;
               } else if (h.summaryHealth.equals(VsanHealthStatus.yellow.toString())) {
                  ++yellowCount;
               }
            }
         }
      }

      physicalDisksHealth.total = total;
      if (statusList.contains(VsanHealthStatus.red.toString())) {
         physicalDisksHealth.overallStatus = VsanHealthStatus.red.toString();
         physicalDisksHealth.issueCount = redCount;
      } else if (statusList.contains(VsanHealthStatus.yellow.toString())) {
         physicalDisksHealth.overallStatus = VsanHealthStatus.yellow.toString();
         physicalDisksHealth.issueCount = yellowCount;
      } else {
         physicalDisksHealth.overallStatus = VsanHealthStatus.green.toString();
         physicalDisksHealth.issueCount = 0;
      }

      HardwareOverallHealth hostsHealth = new HardwareOverallHealth();
      ManagedObjectReference[] hosts = (ManagedObjectReference[])QueryUtil.getProperty(clusterRef, "host", (Object)null);
      hostsHealth.total = hosts != null ? hosts.length : 0;
      yellowCount = 0;
      redCount = 0;
      if (hosts != null) {
         ManagedObjectReference[] var45 = hosts;
         int var47 = hosts.length;

         for(int var49 = 0; var49 < var47; ++var49) {
            ManagedObjectReference h = var45[var49];
            ConnectionState state = (ConnectionState)QueryUtil.getProperty(h, "runtime.connectionState", (Object)null);
            Status status = (Status)QueryUtil.getProperty(h, "overallStatus", (Object)null);
            if (ConnectionState.connected.equals(state)) {
               if (Status.red.equals(status)) {
                  ++redCount;
                  hostsHealth.overallStatus = VsanHealthStatus.red.toString();
               } else if (Status.yellow.equals(status)) {
                  ++yellowCount;
                  if (!VsanHealthStatus.red.toString().equals(hostsHealth.overallStatus)) {
                     hostsHealth.overallStatus = VsanHealthStatus.yellow.toString();
                  }
               }
            } else {
               ++redCount;
               hostsHealth.overallStatus = VsanHealthStatus.red.toString();
            }
         }
      }

      if (VsanHealthStatus.red.toString().equals(hostsHealth.overallStatus)) {
         hostsHealth.issueCount = redCount;
      } else if (VsanHealthStatus.yellow.toString().equals(hostsHealth.overallStatus)) {
         hostsHealth.issueCount = yellowCount;
      } else {
         hostsHealth.overallStatus = VsanHealthStatus.green.toString();
         hostsHealth.issueCount = 0;
      }

      total = 0;
      yellowCount = 0;
      redCount = 0;
      VsanClusterVMsHealthOverallResult vmResult = summary.getVmHealth();
      HardwareOverallHealth vmsHealth = new HardwareOverallHealth();
      vmsHealth.overallStatus = vmResult.overallHealthState;
      if (vmResult.healthStateList != null) {
         VsanClusterVMsHealthSummaryResult[] var50 = vmResult.healthStateList;
         var14 = var50.length;

         for(var15 = 0; var15 < var14; ++var15) {
            VsanClusterVMsHealthSummaryResult r = var50[var15];
            total += r.numVMs;
            if (r.state.equalsIgnoreCase(VsanHealthStatus.red.toString())) {
               redCount += r.numVMs;
            } else if (r.state.equalsIgnoreCase(VsanHealthStatus.yellow.toString())) {
               yellowCount += r.numVMs;
            }
         }
      }

      vmsHealth.total = total;
      if (VsanHealthStatus.red.toString().equalsIgnoreCase(vmsHealth.overallStatus)) {
         vmsHealth.issueCount = redCount;
      } else if (VsanHealthStatus.yellow.toString().equalsIgnoreCase(vmsHealth.overallStatus)) {
         vmsHealth.issueCount = yellowCount;
      }

      AggregatedVsanHealthSummary aggregatedSummary = new AggregatedVsanHealthSummary();
      aggregatedSummary.hostSummary = hostsHealth;
      aggregatedSummary.physicalDiskSummary = physicalDisksHealth;
      aggregatedSummary.networkIssueDetected = summary.networkHealth.issueFound;
      aggregatedSummary.vmSummary = vmsHealth;
      return aggregatedSummary;
   }

   /** @deprecated */
   @Deprecated
   @TsService
   public boolean getIsCloudHealthSupported(ManagedObjectReference clusterRef) {
      return VsanCapabilityUtils.isCloudHealthSupportedOnVc(clusterRef);
   }

   @TsService
   public ManagedObjectReference getCloudHealthCheckResult(ManagedObjectReference clusterRef) throws Exception {
      ManagedObjectReference taskMoRef = null;
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var4 = null;

      ManagedObjectReference var34;
      try {
         VsanPhoneHomeSystem phoneHomeSystem = conn.getPhoneHomeSystem();
         Measure m = new Measure("phoneHomeSystem.vsanPerformOnlineHealthCheck");
         Throwable var7 = null;

         try {
            taskMoRef = phoneHomeSystem.vsanPerformOnlineHealthCheck(clusterRef);
            taskMoRef = VsanHealthUtil.buildTaskMor(taskMoRef.getValue(), clusterRef.getServerGuid());
         } catch (Throwable var30) {
            var7 = var30;
            throw var30;
         } finally {
            if (m != null) {
               if (var7 != null) {
                  try {
                     m.close();
                  } catch (Throwable var29) {
                     var7.addSuppressed(var29);
                  }
               } else {
                  m.close();
               }
            }

         }

         var34 = taskMoRef;
      } catch (Throwable var32) {
         var4 = var32;
         throw var32;
      } finally {
         if (conn != null) {
            if (var4 != null) {
               try {
                  conn.close();
               } catch (Throwable var28) {
                  var4.addSuppressed(var28);
               }
            } else {
               conn.close();
            }
         }

      }

      return var34;
   }

   @TsService
   public ExternalProxySettingsConfig getExternalProxySettings(ManagedObjectReference clusterRef) {
      if (clusterRef == null) {
         throw new VsanUiLocalizableException("vsan.internet.error.nocluster");
      } else {
         VsanClusterHealthConfigs configs = null;
         VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
         Throwable var4 = null;

         try {
            VsanVcClusterHealthSystem healthSystem = conn.getVsanVcClusterHealthSystem();

            try {
               Measure m = new Measure("mgmtSystem.queryVsanClusterHealthConfig");
               Throwable var7 = null;

               try {
                  configs = healthSystem.queryVsanClusterHealthConfig(clusterRef);
               } catch (Throwable var32) {
                  var7 = var32;
                  throw var32;
               } finally {
                  if (m != null) {
                     if (var7 != null) {
                        try {
                           m.close();
                        } catch (Throwable var31) {
                           var7.addSuppressed(var31);
                        }
                     } else {
                        m.close();
                     }
                  }

               }
            } catch (Exception var34) {
               logger.error(var34);
               throw new VsanUiLocalizableException("vsan.internet.error.remotecall");
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

         conn = null;
         if (configs == null) {
            return null;
         } else {
            VsanClusterTelemetryProxyConfig proxy = configs.getVsanTelemetryProxy();
            ExternalProxySettingsConfig result = new ExternalProxySettingsConfig();
            VsanClusterHealthResultKeyValuePair[] pairs = configs.getConfigs();
            if (pairs != null && pairs.length > 0) {
               VsanClusterHealthResultKeyValuePair[] var40 = configs.getConfigs();
               int var41 = var40.length;

               for(int var8 = 0; var8 < var41; ++var8) {
                  VsanClusterHealthResultKeyValuePair pair = var40[var8];
                  if ("enableInternetAccess".equalsIgnoreCase(pair.getKey())) {
                     result.enableInternetAccess = "true".equalsIgnoreCase(pair.getValue());
                  }
               }
            }

            if (proxy != null && proxy.host != null && !proxy.host.isEmpty()) {
               result.isAutoDiscovered = proxy.autoDiscovered != null ? proxy.autoDiscovered : false;
               result.hostName = proxy.getHost();
               result.port = proxy.getPort();
               result.userName = proxy.getUser();
               result.password = proxy.getPassword();
            }

            return result;
         }
      }
   }

   @TsService
   public ManagedObjectReference getCompliancePrecheckTask(ManagedObjectReference clusterRef) throws Exception {
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var3 = null;

      ManagedObjectReference var34;
      try {
         VsanVcPrecheckerSystem precheckerSystem = conn.getVsanPreCheckerSystem();
         ManagedObjectReference taskMoRef = null;
         Measure m = new Measure("precheckerSystem.queryComplianceResourceCheckAsync");
         Throwable var7 = null;

         try {
            taskMoRef = precheckerSystem.queryComplianceResourceCheckAsync(clusterRef);
         } catch (Throwable var30) {
            var7 = var30;
            throw var30;
         } finally {
            if (m != null) {
               if (var7 != null) {
                  try {
                     m.close();
                  } catch (Throwable var29) {
                     var7.addSuppressed(var29);
                  }
               } else {
                  m.close();
               }
            }

         }

         if (taskMoRef != null) {
            taskMoRef.setServerGuid(clusterRef.getServerGuid());
         }

         var34 = taskMoRef;
      } catch (Throwable var32) {
         var3 = var32;
         throw var32;
      } finally {
         if (conn != null) {
            if (var3 != null) {
               try {
                  conn.close();
               } catch (Throwable var28) {
                  var3.addSuppressed(var28);
               }
            } else {
               conn.close();
            }
         }

      }

      return var34;
   }

   @TsService
   public ComplianceCheckResultData getCompliancePrecheckResult(ManagedObjectReference clusterRef) throws Exception {
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var3 = null;

      try {
         VsanVcPrecheckerSystem precheckerSystem = conn.getVsanPreCheckerSystem();

         try {
            Measure m = new Measure("precheckerSystem.getComplianceResourceCheckStatus");
            Throwable var6 = null;

            try {
               VsanComplianceResourceCheckStatus status = precheckerSystem.getComplianceResourceCheckStatus(clusterRef);
               if (status != null && status.result != null && status.result.faultDomains != null) {
                  ComplianceCheckResultData var8 = this.parseComplianceCheck(status.result.faultDomains);
                  return var8;
               }
            } catch (Throwable var36) {
               var6 = var36;
               throw var36;
            } finally {
               if (m != null) {
                  if (var6 != null) {
                     try {
                        m.close();
                     } catch (Throwable var35) {
                        var6.addSuppressed(var35);
                     }
                  } else {
                     m.close();
                  }
               }

            }
         } catch (Exception var38) {
            logger.error("error in parse the compliance result", var38);
         }
      } catch (Throwable var39) {
         var3 = var39;
         throw var39;
      } finally {
         if (conn != null) {
            if (var3 != null) {
               try {
                  conn.close();
               } catch (Throwable var34) {
                  var3.addSuppressed(var34);
               }
            } else {
               conn.close();
            }
         }

      }

      return null;
   }

   private ComplianceCheckResultObj transformHostData(VsanHostComplianceResourceCheck host) {
      ComplianceCheckResultObj hostDataObj = new ComplianceCheckResultObj();
      hostDataObj.objectType = "host";
      hostDataObj.uuid = host.uuid;
      hostDataObj.name = host.host == null ? null : host.host.getValue();
      hostDataObj.isNew = host.isNew;
      hostDataObj.hasChanged = hostDataObj.isNew;
      if (!ArrayUtils.isEmpty(host.diskGroups)) {
         List diskGroupList = new ArrayList();
         VsanDiskGroupComplianceResourceCheck[] var4 = host.diskGroups;
         int var5 = var4.length;

         for(int var6 = 0; var6 < var5; ++var6) {
            VsanDiskGroupComplianceResourceCheck diskgroup = var4[var6];
            ComplianceCheckResultObj diskGroupDataObj = this.transformDiskGroupData(diskgroup);
            diskGroupList.add(diskGroupDataObj);
            hostDataObj.finalCacheCapacity += diskGroupDataObj.finalCacheCapacity;
            hostDataObj.originalCacheCapacity += diskGroupDataObj.originalCacheCapacity;
            hostDataObj.initCacheCapacity += diskGroupDataObj.initCacheCapacity;
            hostDataObj.finalUsedCacheCapacity += diskGroupDataObj.finalUsedCacheCapacity;
            hostDataObj.finalUsedCapacity += diskGroupDataObj.finalUsedCapacity;
            hostDataObj.initCapacity += diskGroupDataObj.initCapacity;
            hostDataObj.originalCapacity += diskGroupDataObj.originalCapacity;
            hostDataObj.finalCapacity += diskGroupDataObj.finalCapacity;
            if (!hostDataObj.hasChanged && diskGroupDataObj.hasChanged) {
               hostDataObj.hasChanged = diskGroupDataObj.hasChanged;
            }
         }

         hostDataObj.childDevices = this.parseListToArray(diskGroupList);
      }

      return hostDataObj;
   }

   private ComplianceCheckResultObj transformDiskData(VsanDiskComplianceResourceCheck disk) {
      ComplianceCheckResultObj diskDataObj = new ComplianceCheckResultObj();
      diskDataObj.objectType = "Disk";
      diskDataObj.uuid = disk.uuid;
      diskDataObj.name = disk.deviceName;
      diskDataObj.initCapacity = disk.initCapacity;
      diskDataObj.finalUsedCapacity = disk.finalCapacity;
      diskDataObj.isNew = disk.isNew;
      diskDataObj.hasChanged = diskDataObj.isNew || diskDataObj.initCapacity != diskDataObj.finalUsedCapacity;
      if (!diskDataObj.isNew) {
         diskDataObj.originalCapacity = disk.capacity;
      }

      diskDataObj.finalCapacity = disk.capacity;
      return diskDataObj;
   }

   private ComplianceCheckResultObj transformDiskGroupData(VsanDiskGroupComplianceResourceCheck diskgroup) {
      ComplianceCheckResultObj diskGroupDataObj = new ComplianceCheckResultObj();
      diskGroupDataObj.objectType = "Diskgroup";
      List diskList = new ArrayList();
      if (diskgroup.ssd != null) {
         ComplianceCheckResultObj ssdObj = new ComplianceCheckResultObj();
         ssdObj.objectType = "SSD";
         ssdObj.uuid = diskgroup.ssd.uuid;
         ssdObj.name = diskgroup.ssd.deviceName;
         ssdObj.initCacheCapacity = diskgroup.ssd.initCapacity;
         ssdObj.finalUsedCacheCapacity = diskgroup.ssd.finalCapacity;
         ssdObj.isNew = diskgroup.ssd.isNew;
         ssdObj.hasChanged = ssdObj.isNew || ssdObj.initCacheCapacity != ssdObj.finalCacheCapacity;
         if (!ssdObj.isNew) {
            ssdObj.originalCacheCapacity = diskgroup.ssd.capacity;
         }

         ssdObj.finalCacheCapacity = diskgroup.ssd.capacity;
         diskList.add(ssdObj);
         diskGroupDataObj.finalUsedCacheCapacity = diskgroup.ssd.finalCapacity;
         diskGroupDataObj.initCacheCapacity = diskgroup.ssd.initCapacity;
         diskGroupDataObj.isNew = diskgroup.ssd.isNew;
         diskGroupDataObj.hasChanged = ssdObj.hasChanged;
         if (!diskGroupDataObj.isNew) {
            diskGroupDataObj.originalCacheCapacity = diskgroup.ssd.capacity;
         }

         diskGroupDataObj.finalCacheCapacity = diskgroup.ssd.capacity;
      }

      if (!ArrayUtils.isEmpty(diskgroup.capacityDevices)) {
         VsanDiskComplianceResourceCheck[] var9 = diskgroup.capacityDevices;
         int var5 = var9.length;

         for(int var6 = 0; var6 < var5; ++var6) {
            VsanDiskComplianceResourceCheck disk = var9[var6];
            ComplianceCheckResultObj diskDataObj = this.transformDiskData(disk);
            diskList.add(diskDataObj);
            diskGroupDataObj.originalCapacity += diskDataObj.originalCapacity;
            diskGroupDataObj.finalCapacity += diskDataObj.finalCapacity;
            diskGroupDataObj.initCapacity += diskDataObj.initCapacity;
            diskGroupDataObj.finalUsedCapacity += diskDataObj.finalUsedCapacity;
            if (!diskGroupDataObj.hasChanged && diskDataObj.hasChanged) {
               diskGroupDataObj.hasChanged = diskDataObj.hasChanged;
            }
         }
      }

      if (diskList.size() > 0) {
         diskGroupDataObj.childDevices = this.parseListToArray(diskList);
      }

      return diskGroupDataObj;
   }

   private ComplianceCheckSummary parseComplianceCheckSummary(List fdList) {
      ComplianceCheckSummary resultSummary = new ComplianceCheckSummary();
      if (!CollectionUtils.isEmpty(fdList)) {
         Iterator var3 = fdList.iterator();

         while(var3.hasNext()) {
            ComplianceCheckResultObj fd = (ComplianceCheckResultObj)var3.next();
            resultSummary.newFinalTotalCapacity += fd.finalCapacity;
            resultSummary.newFinalUsedCapacity += fd.finalUsedCapacity;
            resultSummary.originalTotalCapacity += fd.originalCapacity;
            resultSummary.originalUsedCapacity += fd.finalUsedCapacity;
            if (fd.isNew) {
               ++resultSummary.newFaultDomainCount;
            } else {
               ++resultSummary.originalFaultDomainCount;
            }

            if (ArrayUtils.isEmpty(fd.childDevices)) {
               return resultSummary;
            }

            ComplianceCheckResultObj[] var5 = fd.childDevices;
            int var6 = var5.length;

            for(int var7 = 0; var7 < var6; ++var7) {
               ComplianceCheckResultObj host = var5[var7];
               if (host.isNew) {
                  ++resultSummary.newHostCount;
               } else {
                  ++resultSummary.originalHostCount;
               }

               if (!ArrayUtils.isEmpty(host.childDevices)) {
                  ComplianceCheckResultObj[] var9 = host.childDevices;
                  int var10 = var9.length;

                  for(int var11 = 0; var11 < var10; ++var11) {
                     ComplianceCheckResultObj diskgroup = var9[var11];
                     if (diskgroup.isNew) {
                        ++resultSummary.newDiskGroupCount;
                        ++resultSummary.newSSDCount;
                     } else {
                        ++resultSummary.originalDiskGroupCount;
                        ++resultSummary.originalSSDCount;
                     }

                     if (!ArrayUtils.isEmpty(diskgroup.childDevices)) {
                        ComplianceCheckResultObj[] var13 = diskgroup.childDevices;
                        int var14 = var13.length;

                        for(int var15 = 0; var15 < var14; ++var15) {
                           ComplianceCheckResultObj disk = var13[var15];
                           if (!disk.objectType.equals("SSD")) {
                              if (disk.isNew) {
                                 ++resultSummary.newCapacityDeviceCount;
                              } else {
                                 ++resultSummary.originalCapacityDeviceCount;
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      return resultSummary;
   }

   private ComplianceCheckResultData parseComplianceCheck(VsanFaultDomainComplianceResourceCheck[] faultDomains) {
      if (ArrayUtils.isEmpty(faultDomains)) {
         return null;
      } else {
         List fdList = new ArrayList();
         VsanFaultDomainComplianceResourceCheck[] var3 = faultDomains;
         int var4 = faultDomains.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            VsanFaultDomainComplianceResourceCheck faultDomain = var3[var5];
            ComplianceCheckResultObj faultDomainDataObj = this.transformFaultDomainData(faultDomain);
            fdList.add(faultDomainDataObj);
         }

         ComplianceCheckResultData result = new ComplianceCheckResultData();
         result.summary = this.parseComplianceCheckSummary(fdList);
         result.details = this.parseListToArray(fdList);
         return result;
      }
   }

   private ComplianceCheckResultObj transformFaultDomainData(VsanFaultDomainComplianceResourceCheck faultDomain) {
      ComplianceCheckResultObj faultDomainDataObj = new ComplianceCheckResultObj();
      faultDomainDataObj.objectType = "FaultDomain";
      faultDomainDataObj.uuid = faultDomain.uuid;
      faultDomainDataObj.name = faultDomain.fdName;
      faultDomainDataObj.isNew = faultDomain.isNew;
      faultDomainDataObj.hasChanged = faultDomainDataObj.isNew;
      if (!ArrayUtils.isEmpty(faultDomain.hosts)) {
         List hostList = new ArrayList();
         VsanHostComplianceResourceCheck[] var4 = faultDomain.hosts;
         int var5 = var4.length;

         for(int var6 = 0; var6 < var5; ++var6) {
            VsanHostComplianceResourceCheck host = var4[var6];
            ComplianceCheckResultObj hostDataObj = this.transformHostData(host);
            hostList.add(hostDataObj);
            faultDomainDataObj.originalCacheCapacity += hostDataObj.originalCacheCapacity;
            faultDomainDataObj.finalCacheCapacity += hostDataObj.finalCacheCapacity;
            faultDomainDataObj.initCacheCapacity += hostDataObj.initCacheCapacity;
            faultDomainDataObj.finalUsedCacheCapacity += hostDataObj.finalUsedCacheCapacity;
            faultDomainDataObj.finalUsedCapacity += hostDataObj.finalUsedCapacity;
            faultDomainDataObj.initCapacity += hostDataObj.initCapacity;
            faultDomainDataObj.originalCapacity += hostDataObj.originalCapacity;
            faultDomainDataObj.finalCapacity += hostDataObj.finalCapacity;
            if (!faultDomainDataObj.hasChanged && hostDataObj.hasChanged) {
               faultDomainDataObj.hasChanged = hostDataObj.hasChanged;
            }
         }

         faultDomainDataObj.childDevices = this.parseListToArray(hostList);
      }

      return faultDomainDataObj;
   }

   private ComplianceCheckResultObj[] parseListToArray(List dataList) {
      if (CollectionUtils.isEmpty(dataList)) {
         return null;
      } else {
         ComplianceCheckResultObj[] arr = new ComplianceCheckResultObj[dataList.size()];
         return (ComplianceCheckResultObj[])dataList.toArray(arr);
      }
   }
}
