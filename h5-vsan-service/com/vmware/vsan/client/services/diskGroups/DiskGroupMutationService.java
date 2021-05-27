package com.vmware.vsan.client.services.diskGroups;

import com.google.common.collect.ImmutableMap;
import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vim.host.MaintenanceSpec;
import com.vmware.vim.binding.vim.host.ScsiDisk;
import com.vmware.vim.binding.vim.host.StorageSystem;
import com.vmware.vim.binding.vim.host.VsanSystem;
import com.vmware.vim.binding.vim.vsan.host.DiskMapping;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanVcDiskManagementSystem;
import com.vmware.vim.vsan.binding.vim.host.VsanSystemEx;
import com.vmware.vim.vsan.binding.vim.vsan.host.DiskMappingCreationSpec;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.services.diskGroups.data.DecommissionMode;
import com.vmware.vsan.client.services.diskGroups.data.DiskMappingSpec;
import com.vmware.vsan.client.services.diskGroups.data.RecreateDiskGroupSpec;
import com.vmware.vsan.client.services.diskGroups.data.RemoveDiskGroupSpec;
import com.vmware.vsan.client.services.diskGroups.data.RemoveDiskSpec;
import com.vmware.vsan.client.services.diskGroups.data.UnmountDiskGroupSpec;
import com.vmware.vsan.client.services.diskGroups.data.VsanDiskMapping;
import com.vmware.vsan.client.services.diskmanagement.claiming.HostDisksClaimer;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcConnection;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.VmodlHelper;
import com.vmware.vsphere.client.vsan.base.util.VsanProfiler;
import com.vmware.vsphere.client.vsan.spec.VsanDiskMappingSpec;
import com.vmware.vsphere.client.vsan.spec.VsanSemiAutoDiskMappingsSpec;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DiskGroupMutationService {
   @Autowired
   VsanClient vsanClient;
   @Autowired
   private VcClient vcClient;
   @Autowired
   private HostDisksClaimer hostDisksClaimer;
   private static final Log logger = LogFactory.getLog(DiskGroupMutationService.class);
   private static final VsanProfiler profiler = new VsanProfiler(DiskGroupMutationService.class);

   @TsService
   public Map createDiskGroup(ManagedObjectReference hostRef, DiskMappingSpec spec) {
      VsanDiskMappingSpec vsanSpec = new VsanDiskMappingSpec();
      vsanSpec.clusterRef = spec.clusterRef;
      ArrayList mappings = new ArrayList();
      VsanDiskMapping[] var5 = spec.mappings;
      int var6 = var5.length;

      for(int var7 = 0; var7 < var6; ++var7) {
         VsanDiskMapping diskMapping = var5[var7];
         DiskMapping vsanDiskMapping = new DiskMapping();
         vsanDiskMapping.ssd = diskMapping.ssd;
         vsanDiskMapping.nonSsd = diskMapping.nonSsd;
         mappings.add(vsanDiskMapping);
      }

      vsanSpec.mappings = mappings.toArray();
      ManagedObjectReference task = this.claimDisks(hostRef, vsanSpec);
      return ImmutableMap.of("task", task);
   }

   @TsService
   public Map addDiskToDiskGroup(ManagedObjectReference clusterRef, ManagedObjectReference hostRef, VsanDiskMapping vsanDiskMappings) {
      DiskMapping diskMapping = new DiskMapping();
      diskMapping.ssd = vsanDiskMappings.ssd;
      diskMapping.nonSsd = vsanDiskMappings.nonSsd;
      VsanDiskMappingSpec spec = new VsanDiskMappingSpec();
      spec.mappings = new Object[]{diskMapping};
      spec.clusterRef = clusterRef;
      ManagedObjectReference task = this.claimDisks(hostRef, spec);
      return ImmutableMap.of("task", task);
   }

   private ManagedObjectReference claimDisks(ManagedObjectReference hostRef, VsanDiskMappingSpec diskMappingSpec) {
      DiskMapping[] diskMappings = (DiskMapping[])Arrays.copyOf(diskMappingSpec.mappings, diskMappingSpec.mappings.length, DiskMapping[].class);
      ManagedObjectReference initializeDisksTask = null;
      DiskMapping[] var5 = diskMappings;
      int var6 = diskMappings.length;

      for(int var7 = 0; var7 < var6; ++var7) {
         DiskMapping mapping = var5[var7];
         if (ArrayUtils.isEmpty(mapping.nonSsd)) {
            logger.error("No capacity disks selected!");
            throw new IllegalArgumentException("No capacity disks selected!");
         }

         Throwable var10;
         VsanProfiler.Point point;
         Throwable var13;
         if (VsanCapabilityUtils.isAllFlashSupportedOnCluster(diskMappingSpec.clusterRef)) {
            try {
               VsanConnection conn = this.vsanClient.getConnection(hostRef.getServerGuid());
               var10 = null;

               try {
                  VsanVcDiskManagementSystem diskManagement = conn.getVsanDiskManagementSystem();
                  point = profiler.point("diskManagement.initializeDiskMappings");
                  var13 = null;

                  try {
                     DiskMappingCreationSpec createSpec = this.hostDisksClaimer.toDiskMappingCreationSpecVmodl(hostRef, mapping);
                     initializeDisksTask = diskManagement.initializeDiskMappings(createSpec);
                  } catch (Throwable var89) {
                     var13 = var89;
                     throw var89;
                  } finally {
                     if (point != null) {
                        if (var13 != null) {
                           try {
                              point.close();
                           } catch (Throwable var88) {
                              var13.addSuppressed(var88);
                           }
                        } else {
                           point.close();
                        }
                     }

                  }
               } catch (Throwable var95) {
                  var10 = var95;
                  throw var95;
               } finally {
                  if (conn != null) {
                     if (var10 != null) {
                        try {
                           conn.close();
                        } catch (Throwable var87) {
                           var10.addSuppressed(var87);
                        }
                     } else {
                        conn.close();
                     }
                  }

               }
            } catch (Exception var97) {
               logger.error("Unable to claim disks for disk spec: " + mapping);
               throw new VsanUiLocalizableException("vsan.manage.diskManagement.claimDisks.error", var97);
            }
         } else {
            VcConnection conn = this.vcClient.getConnection(hostRef.getServerGuid());
            var10 = null;

            try {
               VsanSystem vsanSystem = conn.getHostVsanSystem(hostRef);
               point = profiler.point("vsanSystem.initializeDisks");
               var13 = null;

               try {
                  initializeDisksTask = vsanSystem.initializeDisks(diskMappings);
               } catch (Throwable var90) {
                  var13 = var90;
                  throw var90;
               } finally {
                  if (point != null) {
                     if (var13 != null) {
                        try {
                           point.close();
                        } catch (Throwable var86) {
                           var13.addSuppressed(var86);
                        }
                     } else {
                        point.close();
                     }
                  }

               }
            } catch (Throwable var92) {
               var10 = var92;
               throw var92;
            } finally {
               if (conn != null) {
                  if (var10 != null) {
                     try {
                        conn.close();
                     } catch (Throwable var85) {
                        var10.addSuppressed(var85);
                     }
                  } else {
                     conn.close();
                  }
               }

            }
         }
      }

      return VmodlHelper.assignServerGuid(initializeDisksTask, diskMappingSpec.clusterRef.getServerGuid());
   }

   @TsService
   public List autoClaimDisks(ManagedObjectReference hostRef, VsanSemiAutoDiskMappingsSpec spec) {
      List claimDisksTasks = new ArrayList();
      if (spec.isAllFlashSupported) {
         try {
            VsanConnection conn = this.vsanClient.getConnection(hostRef.getServerGuid());
            Throwable var5 = null;

            try {
               VsanVcDiskManagementSystem diskManagement = conn.getVsanDiskManagementSystem();
               VsanProfiler.Point point = profiler.point("diskManagement.initializeDiskMappings");
               Throwable var8 = null;

               try {
                  List createSpecs = this.hostDisksClaimer.toDiskMappingCreationSpecVmodls(spec);
                  Iterator var10 = createSpecs.iterator();

                  while(var10.hasNext()) {
                     DiskMappingCreationSpec createSpec = (DiskMappingCreationSpec)var10.next();
                     claimDisksTasks.add(diskManagement.initializeDiskMappings(createSpec));
                  }
               } catch (Throwable var90) {
                  var8 = var90;
                  throw var90;
               } finally {
                  if (point != null) {
                     if (var8 != null) {
                        try {
                           point.close();
                        } catch (Throwable var85) {
                           var8.addSuppressed(var85);
                        }
                     } else {
                        point.close();
                     }
                  }

               }
            } catch (Throwable var92) {
               var5 = var92;
               throw var92;
            } finally {
               if (conn != null) {
                  if (var5 != null) {
                     try {
                        conn.close();
                     } catch (Throwable var84) {
                        var5.addSuppressed(var84);
                     }
                  } else {
                     conn.close();
                  }
               }

            }
         } catch (Exception var94) {
            throw new VsanUiLocalizableException("vsan.manage.diskManagement.initializeDiskMappings.error", var94);
         }
      } else {
         ScsiDisk[] allDisks = (ScsiDisk[])Arrays.stream(spec.disks).map((d) -> {
            return d.disk;
         }).toArray((x$0) -> {
            return new ScsiDisk[x$0];
         });
         VcConnection conn = this.vcClient.getConnection(hostRef.getServerGuid());
         Throwable var98 = null;

         try {
            VsanSystem vsanSystem = conn.getHostVsanSystem(hostRef);
            VsanProfiler.Point point = profiler.point("vsanSystem.addDisks");
            Throwable var102 = null;

            try {
               claimDisksTasks.add(vsanSystem.addDisks(allDisks));
            } catch (Throwable var86) {
               var102 = var86;
               throw var86;
            } finally {
               if (point != null) {
                  if (var102 != null) {
                     try {
                        point.close();
                     } catch (Throwable var83) {
                        var102.addSuppressed(var83);
                     }
                  } else {
                     point.close();
                  }
               }

            }
         } catch (Throwable var88) {
            var98 = var88;
            throw var88;
         } finally {
            if (conn != null) {
               if (var98 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var82) {
                     var98.addSuppressed(var82);
                  }
               } else {
                  conn.close();
               }
            }

         }
      }

      Iterator var96 = claimDisksTasks.iterator();

      while(var96.hasNext()) {
         ManagedObjectReference claimDisksTask = (ManagedObjectReference)var96.next();
         VmodlHelper.assignServerGuid(claimDisksTask, spec.hostRef.getServerGuid());
      }

      return claimDisksTasks;
   }

   @TsService
   public ManagedObjectReference removeVsanDirectDisks(ManagedObjectReference clusterRef, RemoveDiskSpec diskSpec) {
      return this.removeDisksUsingVcApi(clusterRef, diskSpec);
   }

   @TsService
   public List removeDisksAndMappings(ManagedObjectReference clusterRef, ManagedObjectReference hostRef, RemoveDiskGroupSpec diskGroupSpec, RemoveDiskSpec diskSpec) {
      List tasks = new ArrayList();
      ManagedObjectReference task;
      if (diskGroupSpec != null && ArrayUtils.isNotEmpty(diskGroupSpec.mappings)) {
         task = this.removeDiskGroups(clusterRef, hostRef, diskGroupSpec);
         tasks.add(task);
      }

      if (diskSpec != null && ArrayUtils.isNotEmpty(diskSpec.disks)) {
         task = this.removeDisks(clusterRef, hostRef, diskSpec);
         tasks.add(task);
      }

      return tasks;
   }

   @TsService
   public ManagedObjectReference removeDiskGroups(ManagedObjectReference clusterRef, ManagedObjectReference hostRef, RemoveDiskGroupSpec diskGroupSpec) {
      return VsanCapabilityUtils.isDiskResourcePrecheckSupported(clusterRef) ? this.removeDiskGroupsUsingVcApi(clusterRef, diskGroupSpec) : this.removeDiskGroupsUsingHostApi(hostRef, diskGroupSpec);
   }

   private ManagedObjectReference removeDiskGroupsUsingHostApi(ManagedObjectReference hostRef, RemoveDiskGroupSpec diskGroupSpec) {
      VcConnection conn = this.vcClient.getConnection(hostRef.getServerGuid());
      Throwable var4 = null;

      ManagedObjectReference var9;
      try {
         VsanSystem vsanSystem = conn.getHostVsanSystem(hostRef);
         VsanProfiler.Point point = profiler.point("vsanSystem.removeDiskMapping");
         Throwable var7 = null;

         try {
            ManagedObjectReference removeDiskMappingsTask = vsanSystem.removeDiskMapping(this.getDiskMapping(diskGroupSpec.mappings), this.createMaintenanceSpec(diskGroupSpec.decommissionMode), (Integer)null);
            var9 = VmodlHelper.assignServerGuid(removeDiskMappingsTask, hostRef.getServerGuid());
         } catch (Throwable var32) {
            var7 = var32;
            throw var32;
         } finally {
            if (point != null) {
               if (var7 != null) {
                  try {
                     point.close();
                  } catch (Throwable var31) {
                     var7.addSuppressed(var31);
                  }
               } else {
                  point.close();
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

   private ManagedObjectReference removeDiskGroupsUsingVcApi(ManagedObjectReference clusterRef, RemoveDiskGroupSpec spec) {
      try {
         VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
         Throwable var4 = null;

         ManagedObjectReference var9;
         try {
            VsanVcDiskManagementSystem diskManagement = conn.getVsanDiskManagementSystem();
            VsanProfiler.Point point = profiler.point("diskManagement.removeDiskMappingEx");
            Throwable var7 = null;

            try {
               ManagedObjectReference task = diskManagement.removeDiskMappingEx(clusterRef, this.getDiskMapping(spec.mappings), this.createMaintenanceSpec(spec.decommissionMode));
               var9 = VmodlHelper.assignServerGuid(task, clusterRef.getServerGuid());
            } catch (Throwable var34) {
               var7 = var34;
               throw var34;
            } finally {
               if (point != null) {
                  if (var7 != null) {
                     try {
                        point.close();
                     } catch (Throwable var33) {
                        var7.addSuppressed(var33);
                     }
                  } else {
                     point.close();
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
         logger.error("Failed to remove disk groups using VC API: ", var38);
         throw new VsanUiLocalizableException("vsan.manage.diskManagement.removeDiskGroups.error");
      }
   }

   private DiskMapping[] getDiskMapping(VsanDiskMapping[] mappings) {
      List diskMappings = new ArrayList();
      VsanDiskMapping[] var3 = mappings;
      int var4 = mappings.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         VsanDiskMapping diskMapping = var3[var5];
         diskMappings.add(diskMapping.toVmodl());
      }

      return (DiskMapping[])diskMappings.toArray(new DiskMapping[0]);
   }

   private ManagedObjectReference removeDisks(ManagedObjectReference clusterRef, ManagedObjectReference hostRef, RemoveDiskSpec diskSpec) {
      return VsanCapabilityUtils.isDiskResourcePrecheckSupported(clusterRef) ? this.removeDisksUsingVcApi(clusterRef, diskSpec) : this.removeDisksUsingHostApi(hostRef, diskSpec);
   }

   private ManagedObjectReference removeDisksUsingHostApi(ManagedObjectReference hostRef, RemoveDiskSpec diskSpec) {
      VcConnection conn = this.vcClient.getConnection(hostRef.getServerGuid());
      Throwable var4 = null;

      ManagedObjectReference var9;
      try {
         VsanSystem vsanSystem = conn.getHostVsanSystem(hostRef);
         VsanProfiler.Point point = profiler.point("vsanSystem.removeDisk");
         Throwable var7 = null;

         try {
            ManagedObjectReference removeDisksTask = vsanSystem.removeDisk(diskSpec.disks, this.createMaintenanceSpec(diskSpec.decommissionMode), (Integer)null);
            var9 = VmodlHelper.assignServerGuid(removeDisksTask, hostRef.getServerGuid());
         } catch (Throwable var32) {
            var7 = var32;
            throw var32;
         } finally {
            if (point != null) {
               if (var7 != null) {
                  try {
                     point.close();
                  } catch (Throwable var31) {
                     var7.addSuppressed(var31);
                  }
               } else {
                  point.close();
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

   private ManagedObjectReference removeDisksUsingVcApi(ManagedObjectReference clusterRef, RemoveDiskSpec diskSpec) {
      try {
         VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
         Throwable var4 = null;

         ManagedObjectReference var9;
         try {
            VsanVcDiskManagementSystem diskManagement = conn.getVsanDiskManagementSystem();
            VsanProfiler.Point point = profiler.point("diskManagement.removeDiskEx");
            Throwable var7 = null;

            try {
               ManagedObjectReference task = diskManagement.removeDiskEx(clusterRef, diskSpec.disks, this.createMaintenanceSpec(diskSpec.decommissionMode));
               var9 = VmodlHelper.assignServerGuid(task, clusterRef.getServerGuid());
            } catch (Throwable var34) {
               var7 = var34;
               throw var34;
            } finally {
               if (point != null) {
                  if (var7 != null) {
                     try {
                        point.close();
                     } catch (Throwable var33) {
                        var7.addSuppressed(var33);
                     }
                  } else {
                     point.close();
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
         logger.error("Failed to remove disks using VC API: ", var38);
         throw new VsanUiLocalizableException("vsan.manage.diskManagement.removeDisks.error");
      }
   }

   @TsService
   public ManagedObjectReference mountDiskGroup(ManagedObjectReference hostRef, VsanDiskMapping mapping) {
      try {
         VcConnection vcConnection = this.vcClient.getConnection(hostRef.getServerGuid());
         Throwable var4 = null;

         ManagedObjectReference var7;
         try {
            VsanSystem vsanSystem = vcConnection.getHostVsanSystem(hostRef);
            ManagedObjectReference mountTask = vsanSystem.initializeDisks(new DiskMapping[]{mapping.toVmodl()});
            VmodlHelper.assignServerGuid(mountTask, hostRef.getServerGuid());
            var7 = mountTask;
         } catch (Throwable var17) {
            var4 = var17;
            throw var17;
         } finally {
            if (vcConnection != null) {
               if (var4 != null) {
                  try {
                     vcConnection.close();
                  } catch (Throwable var16) {
                     var4.addSuppressed(var16);
                  }
               } else {
                  vcConnection.close();
               }
            }

         }

         return var7;
      } catch (Exception var19) {
         logger.error("Failed to mount disk group: ", var19);
         throw new VsanUiLocalizableException("vsan.manage.diskManagement.mountDiskGroup.error");
      }
   }

   @TsService
   public ManagedObjectReference unmountDiskGroup(ManagedObjectReference clusterRef, ManagedObjectReference hostRef, UnmountDiskGroupSpec spec) {
      return VsanCapabilityUtils.isDiskResourcePrecheckSupported(clusterRef) ? this.unmountDiskGroupUsingVcApi(clusterRef, spec) : this.unmountDiskGroupUsingHostApi(hostRef, spec);
   }

   private ManagedObjectReference unmountDiskGroupUsingHostApi(ManagedObjectReference hostRef, UnmountDiskGroupSpec spec) {
      ManagedObjectReference unmountTask = null;
      Throwable var5;
      if (VsanCapabilityUtils.isUnmountWithMaintenanceModeSupported(hostRef)) {
         try {
            VsanConnection vsanConnection = this.vsanClient.getConnection(hostRef.getServerGuid());
            var5 = null;

            try {
               VsanSystemEx vsanSystemEx = vsanConnection.getVsanSystemEx(hostRef);
               unmountTask = vsanSystemEx.unmountDiskMappingEx(new DiskMapping[]{spec.diskMapping.toVmodl()}, this.createMaintenanceSpec(spec.decommissionMode), 0);
            } catch (Throwable var33) {
               var5 = var33;
               throw var33;
            } finally {
               if (vsanConnection != null) {
                  if (var5 != null) {
                     try {
                        vsanConnection.close();
                     } catch (Throwable var30) {
                        var5.addSuppressed(var30);
                     }
                  } else {
                     vsanConnection.close();
                  }
               }

            }
         } catch (Exception var37) {
            logger.error("Failed to unmount disk group: ", var37);
            throw new VsanUiLocalizableException("vsan.manage.diskManagement.unmountDiskGroup.error");
         }
      } else {
         try {
            VcConnection vcConnection = this.vcClient.getConnection(hostRef.getServerGuid());
            var5 = null;

            try {
               VsanSystem vsanSystem = vcConnection.getHostVsanSystem(hostRef);
               unmountTask = vsanSystem.unmountDiskMapping(new DiskMapping[]{spec.diskMapping.toVmodl()});
            } catch (Throwable var32) {
               var5 = var32;
               throw var32;
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
         } catch (Exception var35) {
            logger.error("Failed to unmount disk group: ", var35);
            throw new VsanUiLocalizableException("vsan.manage.diskManagement.unmountDiskGroup.error");
         }
      }

      VmodlHelper.assignServerGuid(unmountTask, hostRef.getServerGuid());
      return unmountTask;
   }

   private ManagedObjectReference unmountDiskGroupUsingVcApi(ManagedObjectReference clusterRef, UnmountDiskGroupSpec spec) {
      if (!VsanCapabilityUtils.isDiskResourcePrecheckSupported(clusterRef)) {
         throw new UnsupportedOperationException("The current cluster doesn't support unmounting disk group using VC api.");
      } else {
         try {
            VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
            Throwable var4 = null;

            ManagedObjectReference var9;
            try {
               VsanVcDiskManagementSystem diskManagement = conn.getVsanDiskManagementSystem();
               VsanProfiler.Point point = profiler.point("diskManagement.unmountDiskMappingEx");
               Throwable var7 = null;

               try {
                  ManagedObjectReference task = diskManagement.unmountDiskMappingEx(clusterRef, new DiskMapping[]{spec.diskMapping.toVmodl()}, this.createMaintenanceSpec(spec.decommissionMode));
                  var9 = VmodlHelper.assignServerGuid(task, clusterRef.getServerGuid());
               } catch (Throwable var34) {
                  var7 = var34;
                  throw var34;
               } finally {
                  if (point != null) {
                     if (var7 != null) {
                        try {
                           point.close();
                        } catch (Throwable var33) {
                           var7.addSuppressed(var33);
                        }
                     } else {
                        point.close();
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
            logger.error("Failed to unmount disk group using VC API: ", var38);
            throw new VsanUiLocalizableException("vsan.manage.diskManagement.unmountDiskGroup.error");
         }
      }
   }

   @TsService
   public ManagedObjectReference recreateDiskGroup(ManagedObjectReference hostRef, RecreateDiskGroupSpec spec) {
      try {
         VsanConnection conn = this.vsanClient.getConnection(hostRef.getServerGuid());
         Throwable var4 = null;

         ManagedObjectReference var10;
         try {
            VsanVcDiskManagementSystem diskManagement = conn.getVsanDiskManagementSystem();
            VsanProfiler.Point point = profiler.point("diskManagement.rebuildDiskMapping");
            Throwable var7 = null;

            try {
               MaintenanceSpec maintenanceSpec = this.createMaintenanceSpec(spec.decommissionMode);
               ManagedObjectReference task = diskManagement.rebuildDiskMapping(hostRef, spec.mapping.toVmodl(), maintenanceSpec);
               var10 = VmodlHelper.assignServerGuid(task, hostRef.getServerGuid());
            } catch (Throwable var35) {
               var7 = var35;
               throw var35;
            } finally {
               if (point != null) {
                  if (var7 != null) {
                     try {
                        point.close();
                     } catch (Throwable var34) {
                        var7.addSuppressed(var34);
                     }
                  } else {
                     point.close();
                  }
               }

            }
         } catch (Throwable var37) {
            var4 = var37;
            throw var37;
         } finally {
            if (conn != null) {
               if (var4 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var33) {
                     var4.addSuppressed(var33);
                  }
               } else {
                  conn.close();
               }
            }

         }

         return var10;
      } catch (Exception var39) {
         logger.error("Failed to recreate disk group", var39);
         throw new VsanUiLocalizableException("vsan.manage.diskManagement.recreateDiskGroup.error");
      }
   }

   private MaintenanceSpec createMaintenanceSpec(DecommissionMode decommissionMode) {
      MaintenanceSpec maintenanceSpec = new MaintenanceSpec();
      if (decommissionMode == null) {
         return maintenanceSpec;
      } else {
         maintenanceSpec.setVsanMode(new com.vmware.vim.binding.vim.vsan.host.DecommissionMode(decommissionMode.toString()));
         return maintenanceSpec;
      }
   }

   @TsService
   public ManagedObjectReference setDiskLedState(ManagedObjectReference hostRef, String[] diskUuids, boolean on) {
      try {
         ManagedObjectReference storageSystemRef = (ManagedObjectReference)QueryUtil.getProperty(hostRef, "storageSystem", (Object)null);
         VcConnection vcConnection = this.vcClient.getConnection(hostRef.getServerGuid());
         Throwable var7 = null;

         ManagedObjectReference taskRef;
         try {
            StorageSystem storageSystem = (StorageSystem)vcConnection.createStub(StorageSystem.class, storageSystemRef);
            if (on) {
               taskRef = storageSystem.turnDiskLocatorLedOn(diskUuids);
            } else {
               taskRef = storageSystem.turnDiskLocatorLedOff(diskUuids);
            }
         } catch (Throwable var17) {
            var7 = var17;
            throw var17;
         } finally {
            if (vcConnection != null) {
               if (var7 != null) {
                  try {
                     vcConnection.close();
                  } catch (Throwable var16) {
                     var7.addSuppressed(var16);
                  }
               } else {
                  vcConnection.close();
               }
            }

         }

         return VmodlHelper.assignServerGuid(taskRef, hostRef.getServerGuid());
      } catch (Exception var19) {
         throw new VsanUiLocalizableException("vsan.manage.diskManagement.changeLed.error", var19);
      }
   }

   @TsService
   public List setDiskLocality(ManagedObjectReference hostRef, String[] diskUuids, boolean local) {
      try {
         ManagedObjectReference storageSystemRef = (ManagedObjectReference)QueryUtil.getProperty(hostRef, "storageSystem", (Object)null);
         List tasks = new ArrayList(diskUuids.length);
         VcConnection vcConnection = this.vcClient.getConnection(hostRef.getServerGuid());
         Throwable var7 = null;

         try {
            StorageSystem storageSystem = (StorageSystem)vcConnection.createStub(StorageSystem.class, storageSystemRef);
            String[] var9 = diskUuids;
            int var10 = diskUuids.length;

            for(int var11 = 0; var11 < var10; ++var11) {
               String uuid = var9[var11];
               ManagedObjectReference task = local ? storageSystem.markAsLocal(uuid) : storageSystem.markAsNonLocal(uuid);
               tasks.add(VmodlHelper.assignServerGuid(task, hostRef.getServerGuid()));
            }
         } catch (Throwable var22) {
            var7 = var22;
            throw var22;
         } finally {
            if (vcConnection != null) {
               if (var7 != null) {
                  try {
                     vcConnection.close();
                  } catch (Throwable var21) {
                     var7.addSuppressed(var21);
                  }
               } else {
                  vcConnection.close();
               }
            }

         }

         return tasks;
      } catch (Exception var24) {
         throw new VsanUiLocalizableException("vsan.manage.diskManagement.diskLocality.error", var24);
      }
   }

   @TsService
   public List setDiskType(ManagedObjectReference hostRef, String[] diskUuids, boolean ssd) {
      try {
         ManagedObjectReference storageSystemRef = (ManagedObjectReference)QueryUtil.getProperty(hostRef, "storageSystem", (Object)null);
         List tasks = new ArrayList(diskUuids.length);
         VcConnection vcConnection = this.vcClient.getConnection(hostRef.getServerGuid());
         Throwable var7 = null;

         try {
            StorageSystem storageSystem = (StorageSystem)vcConnection.createStub(StorageSystem.class, storageSystemRef);
            String[] var9 = diskUuids;
            int var10 = diskUuids.length;

            for(int var11 = 0; var11 < var10; ++var11) {
               String uuid = var9[var11];
               ManagedObjectReference task;
               if (ssd) {
                  task = storageSystem.markAsSsd(uuid);
               } else {
                  task = storageSystem.markAsNonSsd(uuid);
               }

               tasks.add(VmodlHelper.assignServerGuid(task, hostRef.getServerGuid()));
            }
         } catch (Throwable var22) {
            var7 = var22;
            throw var22;
         } finally {
            if (vcConnection != null) {
               if (var7 != null) {
                  try {
                     vcConnection.close();
                  } catch (Throwable var21) {
                     var7.addSuppressed(var21);
                  }
               } else {
                  vcConnection.close();
               }
            }

         }

         return tasks;
      } catch (Exception var24) {
         throw new VsanUiLocalizableException("vsan.manage.diskManagement.diskType.error", var24);
      }
   }
}
