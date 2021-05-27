package com.vmware.vsphere.client.vsan.iscsi.providers;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanIscsiInitiatorGroup;
import com.vmware.vim.vsan.binding.vim.cluster.VsanIscsiTargetSystem;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsphere.client.vsan.base.util.VsanProfiler;
import com.vmware.vsphere.client.vsan.iscsi.models.initiatorgroup.InitiatorGroupAdditionSpec;
import com.vmware.vsphere.client.vsan.iscsi.models.initiatorgroup.initiator.InitiatorGroupInitiatorAdditionSpec;
import com.vmware.vsphere.client.vsan.iscsi.models.initiatorgroup.initiator.InitiatorGroupInitiatorRemoveSpec;
import com.vmware.vsphere.client.vsan.iscsi.models.initiatorgroup.target.InitiatorGroupTargetAdditionSpec;
import com.vmware.vsphere.client.vsan.iscsi.models.initiatorgroup.target.InitiatorGroupTargetRemoveSpec;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class VsanIscsiInitiatorGroupMutationProvider {
   private static final Log _logger = LogFactory.getLog(VsanIscsiInitiatorGroupMutationProvider.class);
   private static final VsanProfiler _profiler = new VsanProfiler(VsanIscsiInitiatorGroupMutationProvider.class);
   @Autowired
   private VsanClient vsanClient;

   @TsService
   public void createInitiatorGroup(ManagedObjectReference clusterRef, InitiatorGroupAdditionSpec spec) throws Exception {
      Validate.notNull(spec);
      Validate.notEmpty(spec.initiatorGroupName);
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var4 = null;

      try {
         VsanIscsiTargetSystem vsanIscsiSystem = conn.getVsanIscsiSystem();
         VsanProfiler.Point p = _profiler.point("vsanIscsiSystem.addIscsiInitiatorGroup");
         Throwable var7 = null;

         try {
            vsanIscsiSystem.addIscsiInitiatorGroup(clusterRef, spec.initiatorGroupName);
         } catch (Throwable var52) {
            var7 = var52;
            throw var52;
         } finally {
            if (p != null) {
               if (var7 != null) {
                  try {
                     p.close();
                  } catch (Throwable var50) {
                     var7.addSuppressed(var50);
                  }
               } else {
                  p.close();
               }
            }

         }

         if (ArrayUtils.isNotEmpty(spec.initiatorNames)) {
            p = _profiler.point("vsanIscsiSystem.addIscsiInitiatorsToGroup");
            var7 = null;

            try {
               vsanIscsiSystem.addIscsiInitiatorsToGroup(clusterRef, spec.initiatorGroupName, spec.initiatorNames);
            } catch (Throwable var51) {
               var7 = var51;
               throw var51;
            } finally {
               if (p != null) {
                  if (var7 != null) {
                     try {
                        p.close();
                     } catch (Throwable var49) {
                        var7.addSuppressed(var49);
                     }
                  } else {
                     p.close();
                  }
               }

            }
         }
      } catch (Throwable var55) {
         var4 = var55;
         throw var55;
      } finally {
         if (conn != null) {
            if (var4 != null) {
               try {
                  conn.close();
               } catch (Throwable var48) {
                  var4.addSuppressed(var48);
               }
            } else {
               conn.close();
            }
         }

      }

   }

   @TsService
   public void removeInitiatorGroup(ManagedObjectReference clusterRef, String name) throws Exception {
      Validate.notEmpty(name);
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var4 = null;

      try {
         VsanIscsiTargetSystem vsanIscsiSystem = conn.getVsanIscsiSystem();
         VsanIscsiInitiatorGroup vsanIscsiInitiatorGroup = null;
         VsanProfiler.Point p = _profiler.point("vsanIscsiSystem.getIscsiInitiatorGroup");
         Throwable var8 = null;

         try {
            vsanIscsiInitiatorGroup = vsanIscsiSystem.getIscsiInitiatorGroup(clusterRef, name);
         } catch (Throwable var82) {
            var8 = var82;
            throw var82;
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

         if (vsanIscsiInitiatorGroup != null) {
            String[] initiatorIqns = vsanIscsiInitiatorGroup.getInitiators();
            if (ArrayUtils.isNotEmpty(initiatorIqns)) {
               VsanProfiler.Point p = _profiler.point("vsanIscsiSystem.removeIscsiInitiatorsFromGroup");
               Throwable var9 = null;

               try {
                  vsanIscsiSystem.removeIscsiInitiatorsFromGroup(clusterRef, name, initiatorIqns);
               } catch (Throwable var81) {
                  var9 = var81;
                  throw var81;
               } finally {
                  if (p != null) {
                     if (var9 != null) {
                        try {
                           p.close();
                        } catch (Throwable var78) {
                           var9.addSuppressed(var78);
                        }
                     } else {
                        p.close();
                     }
                  }

               }
            }
         }

         p = _profiler.point("vsanIscsiSystem.removeIscsiInitiatorGroup");
         var8 = null;

         try {
            vsanIscsiSystem.removeIscsiInitiatorGroup(clusterRef, name);
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
      } catch (Throwable var86) {
         var4 = var86;
         throw var86;
      } finally {
         if (conn != null) {
            if (var4 != null) {
               try {
                  conn.close();
               } catch (Throwable var76) {
                  var4.addSuppressed(var76);
               }
            } else {
               conn.close();
            }
         }

      }

   }

   @TsService
   public void addInitiators(ManagedObjectReference clusterRef, InitiatorGroupInitiatorAdditionSpec spec) throws Exception {
      Validate.notNull(spec);
      Validate.notEmpty(spec.initiatorNames);
      Validate.notEmpty(spec.initiatorGroupName);
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var4 = null;

      try {
         VsanIscsiTargetSystem vsanIscsiSystem = conn.getVsanIscsiSystem();
         VsanProfiler.Point p = _profiler.point("vsanIscsiSystem.addIscsiInitiatorsToGroup");
         Throwable var7 = null;

         try {
            vsanIscsiSystem.addIscsiInitiatorsToGroup(clusterRef, spec.initiatorGroupName, spec.initiatorNames);
         } catch (Throwable var30) {
            var7 = var30;
            throw var30;
         } finally {
            if (p != null) {
               if (var7 != null) {
                  try {
                     p.close();
                  } catch (Throwable var29) {
                     var7.addSuppressed(var29);
                  }
               } else {
                  p.close();
               }
            }

         }
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

   }

   @TsService
   public void removeInitiator(ManagedObjectReference clusterRef, InitiatorGroupInitiatorRemoveSpec spec) throws Exception {
      if (VsanCapabilityUtils.isIscsiTargetsSupportedOnVc(clusterRef)) {
         Validate.notNull(spec);
         Validate.notEmpty(spec.initiatorGroupName);
         Validate.notEmpty(spec.initiatorName);
         VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
         Throwable var4 = null;

         try {
            VsanIscsiTargetSystem vsanIscsiSystem = conn.getVsanIscsiSystem();
            VsanProfiler.Point p = _profiler.point("vsanIscsiSystem.removeIscsiInitiatorsFromGroup");
            Throwable var7 = null;

            try {
               vsanIscsiSystem.removeIscsiInitiatorsFromGroup(clusterRef, spec.initiatorGroupName, new String[]{spec.initiatorName});
            } catch (Throwable var30) {
               var7 = var30;
               throw var30;
            } finally {
               if (p != null) {
                  if (var7 != null) {
                     try {
                        p.close();
                     } catch (Throwable var29) {
                        var7.addSuppressed(var29);
                     }
                  } else {
                     p.close();
                  }
               }

            }
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

      }
   }

   @TsService
   public void addTarget(ManagedObjectReference clusterRef, InitiatorGroupTargetAdditionSpec spec) throws Exception {
      if (VsanCapabilityUtils.isIscsiTargetsSupportedOnVc(clusterRef)) {
         Validate.notNull(spec);
         Validate.notNull(spec.targetAliases);
         Validate.notEmpty(spec.initiatorGroupName);
         VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
         Throwable var4 = null;

         try {
            VsanIscsiTargetSystem vsanIscsiSystem = conn.getVsanIscsiSystem();
            VsanProfiler.Point p = _profiler.point("vsanIscsiSystem.addIscsiTargetToGroup");
            Throwable var7 = null;

            try {
               String initiatorGroupIqn = spec.initiatorGroupName;
               String[] var9 = spec.targetAliases;
               int var10 = var9.length;

               for(int var11 = 0; var11 < var10; ++var11) {
                  String targetAlias = var9[var11];
                  vsanIscsiSystem.addIscsiTargetToGroup(clusterRef, initiatorGroupIqn, targetAlias);
               }
            } catch (Throwable var34) {
               var7 = var34;
               throw var34;
            } finally {
               if (p != null) {
                  if (var7 != null) {
                     try {
                        p.close();
                     } catch (Throwable var33) {
                        var7.addSuppressed(var33);
                     }
                  } else {
                     p.close();
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

      }
   }

   @TsService
   public void removeTarget(ManagedObjectReference clusterRef, InitiatorGroupTargetRemoveSpec spec) throws Exception {
      if (VsanCapabilityUtils.isIscsiTargetsSupportedOnVc(clusterRef)) {
         Validate.notNull(spec);
         Validate.notEmpty(spec.initiatorGroupName);
         Validate.notEmpty(spec.targetAlias);
         VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
         Throwable var4 = null;

         try {
            VsanIscsiTargetSystem vsanIscsiSystem = conn.getVsanIscsiSystem();
            VsanProfiler.Point p = _profiler.point("vsanIscsiSystem.removeIscsiTargetFromGroup");
            Throwable var7 = null;

            try {
               vsanIscsiSystem.removeIscsiTargetFromGroup(clusterRef, spec.initiatorGroupName, spec.targetAlias);
            } catch (Throwable var30) {
               var7 = var30;
               throw var30;
            } finally {
               if (p != null) {
                  if (var7 != null) {
                     try {
                        p.close();
                     } catch (Throwable var29) {
                        var7.addSuppressed(var29);
                     }
                  } else {
                     p.close();
                  }
               }

            }
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

      }
   }
}
