package com.vmware.vsphere.client.vsan.stretched;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vim.host.ScsiDisk;
import com.vmware.vim.binding.vim.vsan.host.DiskMapping;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VSANStretchedClusterFaultDomainConfig;
import com.vmware.vim.vsan.binding.vim.cluster.VsanVcStretchedClusterSystem;
import com.vmware.vsan.client.services.stretchedcluster.WitnessHostValidationService;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsan.client.util.VmodlHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class VsanStretchedClusterMutationProvider {
   private static final Log logger = LogFactory.getLog(VsanStretchedClusterMutationProvider.class);
   @Autowired
   VsanClient vsanClient;
   @Autowired
   WitnessHostValidationService witnessHostValidationService;

   @TsService
   public ManagedObjectReference configureStretchedCluster(ManagedObjectReference clusterRef, VsanStretchedClusterConfig spec) throws Exception {
      logger.info("Invoke configure stretched cluster mutation operation for cluster: " + clusterRef.getServerGuid());
      logger.info("Configuring stretched cluster with witness `" + spec.witnessHost.getServerGuid() + "` and preffered fault domain `" + spec.preferredSiteName + "`");
      ManagedObjectReference stretchedClusterTask = null;
      DiskMapping diskMapping = null;
      if (spec.witnessHostDiskMapping != null) {
         diskMapping = this.copyProperties(spec.witnessHostDiskMapping);
      }

      if (spec.isFaultDomainConfigurationChanged) {
         VSANStretchedClusterFaultDomainConfig fdConfig = new VSANStretchedClusterFaultDomainConfig();
         fdConfig.firstFdName = spec.preferredSiteName;
         fdConfig.firstFdHosts = (ManagedObjectReference[])spec.preferredSiteHosts.toArray(new ManagedObjectReference[spec.preferredSiteHosts.size()]);
         fdConfig.secondFdName = spec.secondarySiteName;
         fdConfig.secondFdHosts = (ManagedObjectReference[])spec.secondarySiteHosts.toArray(new ManagedObjectReference[spec.secondarySiteHosts.size()]);
         VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
         Throwable var7 = null;

         try {
            VsanVcStretchedClusterSystem stretchedClusterSystem = conn.getVcStretchedClusterSystem();
            Measure measure = new Measure("stretchedClusterSystem.convertToStretchedCluster");
            Throwable var10 = null;

            try {
               stretchedClusterTask = stretchedClusterSystem.convertToStretchedCluster(clusterRef, fdConfig, spec.witnessHost, spec.preferredSiteName, diskMapping);
            } catch (Throwable var83) {
               var10 = var83;
               throw var83;
            } finally {
               if (measure != null) {
                  if (var10 != null) {
                     try {
                        measure.close();
                     } catch (Throwable var80) {
                        var10.addSuppressed(var80);
                     }
                  } else {
                     measure.close();
                  }
               }

            }
         } catch (Throwable var85) {
            var7 = var85;
            throw var85;
         } finally {
            if (conn != null) {
               if (var7 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var79) {
                     var7.addSuppressed(var79);
                  }
               } else {
                  conn.close();
               }
            }

         }
      } else {
         VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
         Throwable var91 = null;

         try {
            VsanVcStretchedClusterSystem stretchedClusterSystem = conn.getVcStretchedClusterSystem();
            Measure measure = new Measure("stretchedClusterSystem.addWitnessHost");
            Throwable var94 = null;

            try {
               stretchedClusterTask = stretchedClusterSystem.addWitnessHost(clusterRef, spec.witnessHost, spec.preferredSiteName, diskMapping, (Boolean)null);
            } catch (Throwable var82) {
               var94 = var82;
               throw var82;
            } finally {
               if (measure != null) {
                  if (var94 != null) {
                     try {
                        measure.close();
                     } catch (Throwable var81) {
                        var94.addSuppressed(var81);
                     }
                  } else {
                     measure.close();
                  }
               }

            }
         } catch (Throwable var88) {
            var91 = var88;
            throw var88;
         } finally {
            if (conn != null) {
               if (var91 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var78) {
                     var91.addSuppressed(var78);
                  }
               } else {
                  conn.close();
               }
            }

         }
      }

      return VmodlHelper.assignServerGuid(stretchedClusterTask, clusterRef.getServerGuid());
   }

   @TsService
   public ManagedObjectReference setPreferredFaultDomain(ManagedObjectReference clusterRef, PreferredFaultDomainData preferredFaultDomainData) throws Exception {
      logger.info("Invoke set preferred fault domain mutation operation for cluster: " + clusterRef.getServerGuid());
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var4 = null;

      ManagedObjectReference var9;
      try {
         VsanVcStretchedClusterSystem stretchedClusterSystem = conn.getVcStretchedClusterSystem();
         Measure measure = new Measure("stretchedClusterSystem.setPreferredFaultDomain");
         Throwable var7 = null;

         try {
            logger.info("Setting preferred fault domain to: " + preferredFaultDomainData.preferredFaultDomainName);
            ManagedObjectReference setPreferredFdTask = stretchedClusterSystem.setPreferredFaultDomain(clusterRef, preferredFaultDomainData.preferredFaultDomainName, (ManagedObjectReference)null);
            var9 = VmodlHelper.assignServerGuid(setPreferredFdTask, clusterRef.getServerGuid());
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
   public ManagedObjectReference setWitnessHost(ManagedObjectReference clusterRef, VsanWitnessConfig witnessConfig) throws Exception {
      logger.info("Invoke change witness host mutation operation for cluster: " + clusterRef);
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var4 = null;

      ManagedObjectReference var9;
      try {
         VsanVcStretchedClusterSystem stretchedClusterSystem = conn.getVcStretchedClusterSystem();
         Measure measure = new Measure("stretchedClusterSystem.addWitnessHost");
         Throwable var7 = null;

         try {
            logger.info("Changing witness host: " + witnessConfig.host);
            ManagedObjectReference changeWitnessTask = stretchedClusterSystem.addWitnessHost(clusterRef, witnessConfig.host, witnessConfig.preferredFaultDomain, witnessConfig.diskMapping, (Boolean)null);
            var9 = VmodlHelper.assignServerGuid(changeWitnessTask, clusterRef.getServerGuid());
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
   public ManagedObjectReference removeWitnessHost(ManagedObjectReference clusterRef, ManagedObjectReference witnessHost) throws Exception {
      logger.info("Invoke remove witness host mutation operation for cluster: " + clusterRef.getServerGuid());
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var4 = null;

      ManagedObjectReference var9;
      try {
         VsanVcStretchedClusterSystem stretchedClusterSystem = conn.getVcStretchedClusterSystem();
         Measure measure = new Measure("stretchedClusterSystem.removeWitnessHost");
         Throwable var7 = null;

         try {
            logger.info("Removing witness host: " + witnessHost.getServerGuid());
            ManagedObjectReference removeWitnessTask = stretchedClusterSystem.removeWitnessHost(clusterRef, witnessHost, (String)null);
            var9 = VmodlHelper.assignServerGuid(removeWitnessTask, clusterRef.getServerGuid());
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

   private DiskMapping copyProperties(DiskMapping original) {
      DiskMapping result = new DiskMapping();
      result.ssd = original.ssd;
      result.nonSsd = new ScsiDisk[original.nonSsd.length];

      for(int i = 0; i < result.nonSsd.length; ++i) {
         result.nonSsd[i] = original.nonSsd[i];
      }

      return result;
   }
}
