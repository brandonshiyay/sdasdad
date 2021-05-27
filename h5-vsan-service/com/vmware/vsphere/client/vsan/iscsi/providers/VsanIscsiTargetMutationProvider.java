package com.vmware.vsphere.client.vsan.iscsi.providers;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vim.fault.VsanFault;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanIscsiLUN;
import com.vmware.vim.vsan.binding.vim.cluster.VsanIscsiTarget;
import com.vmware.vim.vsan.binding.vim.cluster.VsanIscsiTargetSystem;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.VmodlHelper;
import com.vmware.vsphere.client.vsan.base.util.VsanProfiler;
import com.vmware.vsphere.client.vsan.iscsi.models.target.TargetOperatoinSpec;
import com.vmware.vsphere.client.vsan.iscsi.models.target.initiator.TargetInitiatorEditSpec;
import com.vmware.vsphere.client.vsan.iscsi.models.target.initiator.TargetInitiatorRemoveSpec;
import com.vmware.vsphere.client.vsan.iscsi.models.target.lun.LunOperationSpec;
import com.vmware.vsphere.client.vsan.iscsi.models.target.lun.TargetLunRemoveSpec;
import com.vmware.vsphere.client.vsan.util.Utils;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class VsanIscsiTargetMutationProvider {
   private static final Log _logger = LogFactory.getLog(VsanIscsiTargetMutationProvider.class);
   private static final VsanProfiler _profiler = new VsanProfiler(VsanIscsiTargetMutationProvider.class);
   @Autowired
   private VsanClient vsanClient;

   @TsService
   public ManagedObjectReference addTarget(ManagedObjectReference clusterRef, TargetOperatoinSpec spec) throws Exception {
      Validate.notNull(spec);
      this.validateTargetIQN(clusterRef, spec.iqn);
      this.validateTargetAlias(clusterRef, spec.alias);
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var4 = null;

      ManagedObjectReference var9;
      try {
         VsanIscsiTargetSystem vsanIscsiSystem = conn.getVsanIscsiSystem();
         VsanProfiler.Point p = _profiler.point("vsanIscsiSystem.addIscsiTarget");
         Throwable var7 = null;

         try {
            ManagedObjectReference taskRef = vsanIscsiSystem.addIscsiTarget(clusterRef, spec.toVmodlVsanIscsiTargetSpec(clusterRef));
            if (taskRef == null) {
               return null;
            }

            VmodlHelper.assignServerGuid(taskRef, clusterRef.getServerGuid());
            var9 = taskRef;
         } catch (Throwable var35) {
            var7 = var35;
            throw var35;
         } finally {
            if (p != null) {
               if (var7 != null) {
                  try {
                     p.close();
                  } catch (Throwable var34) {
                     var7.addSuppressed(var34);
                  }
               } else {
                  p.close();
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

      return var9;
   }

   private void validateTargetIQN(ManagedObjectReference clusterRef, String iqn) throws Exception {
      if (!StringUtils.isEmpty(StringUtils.trim(iqn))) {
         VsanIscsiTarget[] targets = null;
         VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
         Throwable var5 = null;

         try {
            VsanIscsiTargetSystem vsanIscsiSystem = conn.getVsanIscsiSystem();

            try {
               VsanProfiler.Point p = _profiler.point("vsanIscsiSystem.getIscsiTargets");
               Throwable var8 = null;

               try {
                  targets = vsanIscsiSystem.getIscsiTargets(clusterRef);
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
               _logger.error("Failed to get the vSAN iSCSI target list.", var35);
               throw new Exception(var35.getLocalizedMessage(), var35);
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

         if (targets != null) {
            VsanIscsiTarget[] var38 = targets;
            int var39 = targets.length;

            for(int var40 = 0; var40 < var39; ++var40) {
               VsanIscsiTarget existingTarget = var38[var40];
               if (existingTarget != null && iqn.equalsIgnoreCase(existingTarget.iqn)) {
                  throw new VsanUiLocalizableException("vsan.error.target.iqn.duplicated", new Object[]{iqn});
               }
            }
         }

      }
   }

   private void validateTargetAlias(ManagedObjectReference clusterRef, String newTargetAlias) throws Exception {
      VsanIscsiTarget target = null;
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var5 = null;

      try {
         VsanIscsiTargetSystem vsanIscsiSystem = conn.getVsanIscsiSystem();

         try {
            VsanProfiler.Point p = _profiler.point("vsanIscsiSystem.getIscsiTarget");
            Throwable var8 = null;

            try {
               target = vsanIscsiSystem.getIscsiTarget(clusterRef, newTargetAlias);
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

      if (target != null) {
         throw new VsanUiLocalizableException("vsan.error.target.alias.duplicated", new Object[]{newTargetAlias});
      }
   }

   @TsService
   public ManagedObjectReference editTarget(ManagedObjectReference clusterRef, TargetOperatoinSpec spec) throws Exception {
      Validate.notNull(spec);
      if (StringUtils.isNotEmpty(StringUtils.trim(spec.newAlias))) {
         this.validateTargetAlias(clusterRef, spec.newAlias);
      }

      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var4 = null;

      ManagedObjectReference var9;
      try {
         VsanIscsiTargetSystem vsanIscsiSystem = conn.getVsanIscsiSystem();
         VsanProfiler.Point p = _profiler.point("vsanIscsiSystem.editIscsiTarget");
         Throwable var7 = null;

         try {
            ManagedObjectReference taskRef = vsanIscsiSystem.editIscsiTarget(clusterRef, spec.toVmodlVsanIscsiTargetSpec(clusterRef));
            if (taskRef == null) {
               return null;
            }

            VmodlHelper.assignServerGuid(taskRef, clusterRef.getServerGuid());
            var9 = taskRef;
         } catch (Throwable var35) {
            var7 = var35;
            throw var35;
         } finally {
            if (p != null) {
               if (var7 != null) {
                  try {
                     p.close();
                  } catch (Throwable var34) {
                     var7.addSuppressed(var34);
                  }
               } else {
                  p.close();
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

      return var9;
   }

   @TsService
   public List batchPolicyReapply(ManagedObjectReference clusterRef, TargetOperatoinSpec[] specs) throws Exception {
      Validate.notEmpty(specs);
      List tasksList = new ArrayList();
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var5 = null;

      try {
         VsanIscsiTargetSystem vsanIscsiSystem = conn.getVsanIscsiSystem();
         VsanProfiler.Point p = _profiler.point("vsanIscsiSystem.editIscsiTarget");
         Throwable var8 = null;

         try {
            TargetOperatoinSpec[] var9 = specs;
            int var10 = specs.length;

            for(int var11 = 0; var11 < var10; ++var11) {
               TargetOperatoinSpec spec = var9[var11];

               try {
                  ManagedObjectReference taskRef = vsanIscsiSystem.editIscsiTarget(clusterRef, spec.toVmodlVsanIscsiTargetSpec(clusterRef));
                  if (taskRef != null) {
                     tasksList.add(VmodlHelper.assignServerGuid(taskRef, clusterRef.getServerGuid()));
                  }
               } catch (Exception var37) {
                  _logger.error(var37);
               }
            }
         } catch (Throwable var38) {
            var8 = var38;
            throw var38;
         } finally {
            if (p != null) {
               if (var8 != null) {
                  try {
                     p.close();
                  } catch (Throwable var36) {
                     var8.addSuppressed(var36);
                  }
               } else {
                  p.close();
               }
            }

         }
      } catch (Throwable var40) {
         var5 = var40;
         throw var40;
      } finally {
         if (conn != null) {
            if (var5 != null) {
               try {
                  conn.close();
               } catch (Throwable var35) {
                  var5.addSuppressed(var35);
               }
            } else {
               conn.close();
            }
         }

      }

      return tasksList;
   }

   @TsService
   public ManagedObjectReference removeTarget(ManagedObjectReference clusterRef, String targetAlias) throws Exception {
      Validate.notEmpty(targetAlias);
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var4 = null;

      Object ex;
      try {
         VsanIscsiTargetSystem vsanIscsiSystem = conn.getVsanIscsiSystem();
         VsanProfiler.Point p = _profiler.point("Remove an existing vSAN iSCSI target.");
         Throwable var7 = null;

         try {
            VsanIscsiLUN[] luns = null;

            try {
               VsanProfiler.Point sp = _profiler.point("vsanIscsiSystem.getIscsiLUNs");
               ex = null;

               try {
                  luns = vsanIscsiSystem.getIscsiLUNs(clusterRef, new String[]{targetAlias});
               } catch (Throwable var60) {
                  ex = var60;
                  throw var60;
               } finally {
                  if (sp != null) {
                     if (ex != null) {
                        try {
                           sp.close();
                        } catch (Throwable var59) {
                           ((Throwable)ex).addSuppressed(var59);
                        }
                     } else {
                        sp.close();
                     }
                  }

               }
            } catch (VsanFault var62) {
               ex = new Exception(var62.getLocalizedMessage(), var62.getCause());
               throw (Exception)ex;
            }

            if (luns != null) {
               throw new Exception(Utils.getLocalizedString("vsan.error.target.delete.fail"));
            }

            ManagedObjectReference taskRef = vsanIscsiSystem.removeIscsiTarget(clusterRef, targetAlias);
            if (taskRef == null) {
               return null;
            }

            VmodlHelper.assignServerGuid(taskRef, clusterRef.getServerGuid());
            ex = taskRef;
         } catch (Throwable var63) {
            var7 = var63;
            throw var63;
         } finally {
            if (p != null) {
               if (var7 != null) {
                  try {
                     p.close();
                  } catch (Throwable var58) {
                     var7.addSuppressed(var58);
                  }
               } else {
                  p.close();
               }
            }

         }
      } catch (Throwable var65) {
         var4 = var65;
         throw var65;
      } finally {
         if (conn != null) {
            if (var4 != null) {
               try {
                  conn.close();
               } catch (Throwable var57) {
                  var4.addSuppressed(var57);
               }
            } else {
               conn.close();
            }
         }

      }

      return (ManagedObjectReference)ex;
   }

   @TsService
   public ManagedObjectReference createLun(ManagedObjectReference clusterRef, LunOperationSpec spec) throws Exception {
      this.validate(clusterRef, spec, spec.targetAlias);
      this.validateLunId(clusterRef, spec.targetAlias, spec.lunId);
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var4 = null;

      try {
         VsanIscsiTargetSystem vsanIscsiSystem = conn.getVsanIscsiSystem();
         VsanProfiler.Point p = _profiler.point("vsanIscsiSystem.addIscsiLUN");
         Throwable var7 = null;

         try {
            ManagedObjectReference taskRef = vsanIscsiSystem.addIscsiLUN(clusterRef, spec.targetAlias, spec.toVmodlVsanIscsiLUNSpec());
            if (taskRef != null) {
               VmodlHelper.assignServerGuid(taskRef, clusterRef.getServerGuid());
               ManagedObjectReference var9 = taskRef;
               return var9;
            }
         } catch (Throwable var35) {
            var7 = var35;
            throw var35;
         } finally {
            if (p != null) {
               if (var7 != null) {
                  try {
                     p.close();
                  } catch (Throwable var34) {
                     var7.addSuppressed(var34);
                  }
               } else {
                  p.close();
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

      return null;
   }

   @TsService
   public ManagedObjectReference editLun(ManagedObjectReference clusterRef, LunOperationSpec spec) throws Exception {
      this.validate(clusterRef, spec, spec.targetAlias);
      this.validateLunId(clusterRef, spec.targetAlias, spec.newLunId);
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var4 = null;

      ManagedObjectReference var9;
      try {
         VsanIscsiTargetSystem vsanIscsiSystem = conn.getVsanIscsiSystem();
         VsanProfiler.Point p = _profiler.point("vsanIscsiSystem.editIscsiLUN");
         Throwable var7 = null;

         try {
            ManagedObjectReference taskRef = vsanIscsiSystem.editIscsiLUN(clusterRef, spec.targetAlias, spec.toVmodlVsanIscsiLUNSpec());
            if (taskRef == null) {
               return null;
            }

            VmodlHelper.assignServerGuid(taskRef, clusterRef.getServerGuid());
            var9 = taskRef;
         } catch (Throwable var35) {
            var7 = var35;
            throw var35;
         } finally {
            if (p != null) {
               if (var7 != null) {
                  try {
                     p.close();
                  } catch (Throwable var34) {
                     var7.addSuppressed(var34);
                  }
               } else {
                  p.close();
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

      return var9;
   }

   private void validateLunId(ManagedObjectReference clusterRef, String targetAlias, int newId) throws VsanUiLocalizableException {
      Validate.notEmpty(String.valueOf(newId));
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var5 = null;

      try {
         VsanIscsiTargetSystem vsanIscsiSystem = conn.getVsanIscsiSystem();
         VsanIscsiLUN lun = null;

         try {
            VsanProfiler.Point p = _profiler.point("vsanIscsiSystem.getIscsiLUN");
            Throwable var9 = null;

            try {
               lun = vsanIscsiSystem.getIscsiLUN(clusterRef, targetAlias, newId);
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
         }

         if (lun != null) {
            throw new VsanUiLocalizableException("vsan.error.lun.id.duplicated", new Object[]{newId});
         }
      } catch (Throwable var37) {
         var5 = var37;
         throw var37;
      } finally {
         if (conn != null) {
            if (var5 != null) {
               try {
                  conn.close();
               } catch (Throwable var32) {
                  var5.addSuppressed(var32);
               }
            } else {
               conn.close();
            }
         }

      }

   }

   @TsService
   public ManagedObjectReference[] removeLun(ManagedObjectReference clusterRef, TargetLunRemoveSpec spec) throws Exception {
      this.validate(clusterRef, spec, spec.targetAlias);
      Validate.notEmpty(spec.targetLunIds);
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var4 = null;

      Object var9;
      try {
         VsanIscsiTargetSystem vsanIscsiSystem = conn.getVsanIscsiSystem();
         VsanProfiler.Point p = _profiler.point("vsanIscsiSystem.removeIscsiLUN");
         Throwable var7 = null;

         try {
            List tasksList = new ArrayList();
            var9 = spec.targetLunIds;
            int var10 = ((Object[])var9).length;

            for(int var11 = 0; var11 < var10; ++var11) {
               int lunId = ((Object[])var9)[var11];
               ManagedObjectReference taskRef = vsanIscsiSystem.removeIscsiLUN(clusterRef, spec.targetAlias, lunId);
               if (taskRef != null) {
                  tasksList.add(VmodlHelper.assignServerGuid(taskRef, clusterRef.getServerGuid()));
               }
            }

            if (tasksList.size() <= 0) {
               return new ManagedObjectReference[0];
            }

            var9 = (ManagedObjectReference[])tasksList.toArray(new ManagedObjectReference[tasksList.size()]);
         } catch (Throwable var38) {
            var7 = var38;
            throw var38;
         } finally {
            if (p != null) {
               if (var7 != null) {
                  try {
                     p.close();
                  } catch (Throwable var37) {
                     var7.addSuppressed(var37);
                  }
               } else {
                  p.close();
               }
            }

         }
      } catch (Throwable var40) {
         var4 = var40;
         throw var40;
      } finally {
         if (conn != null) {
            if (var4 != null) {
               try {
                  conn.close();
               } catch (Throwable var36) {
                  var4.addSuppressed(var36);
               }
            } else {
               conn.close();
            }
         }

      }

      return (ManagedObjectReference[])var9;
   }

   @TsService
   public void allowIniatorAccess(ManagedObjectReference clusterRef, TargetInitiatorEditSpec spec) throws Exception {
      this.validate(clusterRef, spec, spec.targetAlias);
      Validate.notEmpty(spec.targetInitiatorNames);
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var4 = null;

      try {
         VsanIscsiTargetSystem vsanIscsiSystem = conn.getVsanIscsiSystem();
         VsanProfiler.Point p = _profiler.point("vsanIscsiSystem.addIscsiInitiatorsToTarget");
         Throwable var7 = null;

         try {
            vsanIscsiSystem.addIscsiInitiatorsToTarget(clusterRef, spec.targetAlias, spec.targetInitiatorNames);
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
   public void disallowInitiatorAccess(ManagedObjectReference clusterRef, TargetInitiatorRemoveSpec spec) throws Exception {
      this.validate(clusterRef, spec, spec.targetAlias);
      Validate.notEmpty(spec.targetInitiatorNames);
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var4 = null;

      try {
         VsanIscsiTargetSystem vsanIscsiSystem = conn.getVsanIscsiSystem();
         VsanProfiler.Point p = _profiler.point("vsanIscsiSystem.removeIscsiInitiatorsFromTarget");
         Throwable var7 = null;

         try {
            vsanIscsiSystem.removeIscsiInitiatorsFromTarget(clusterRef, spec.targetAlias, spec.targetInitiatorNames);
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

   private void validate(ManagedObjectReference clusterRef, Object spec, String targetAlias) throws Exception {
      Validate.notNull(spec);
      Validate.notEmpty(targetAlias);
   }
}
