package com.vmware.vsphere.client.vsan.iscsi.providers;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanIscsiInitiatorGroup;
import com.vmware.vim.vsan.binding.vim.cluster.VsanIscsiTargetBasicInfo;
import com.vmware.vim.vsan.binding.vim.cluster.VsanIscsiTargetSystem;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsphere.client.vsan.base.util.VsanProfiler;
import com.vmware.vsphere.client.vsan.iscsi.models.initiatorgroup.InitiatorGroup;
import com.vmware.vsphere.client.vsan.iscsi.models.initiatorgroup.initiator.InitiatorGroupInitiator;
import com.vmware.vsphere.client.vsan.util.Utils;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class VsanIscsiInitiatorGroupPropertyProvider {
   private static final Log _logger = LogFactory.getLog(VsanIscsiInitiatorGroupPropertyProvider.class);
   private static final VsanProfiler _profiler = new VsanProfiler(VsanIscsiInitiatorGroupPropertyProvider.class);
   @Autowired
   private VsanClient vsanClient;

   @TsService
   public InitiatorGroup[] getVsanIscsiInitiatorGroupList(ManagedObjectReference clusterRef) throws Exception {
      if (!VsanCapabilityUtils.isIscsiTargetsSupportedOnVc(clusterRef)) {
         return null;
      } else {
         VsanIscsiInitiatorGroup[] initiatorGroups = null;
         List groups = new ArrayList();
         VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
         Throwable var5 = null;

         try {
            VsanIscsiTargetSystem vsanIscsiSystem = conn.getVsanIscsiSystem();

            try {
               VsanProfiler.Point p = _profiler.point("vsanIscsiSystem.getIscsiInitiatorGroups");
               Throwable var42 = null;

               try {
                  initiatorGroups = vsanIscsiSystem.getIscsiInitiatorGroups(clusterRef);
               } catch (Throwable var33) {
                  var42 = var33;
                  throw var33;
               } finally {
                  if (p != null) {
                     if (var42 != null) {
                        try {
                           p.close();
                        } catch (Throwable var32) {
                           var42.addSuppressed(var32);
                        }
                     } else {
                        p.close();
                     }
                  }

               }
            } catch (Exception var35) {
               String errorMsg = Utils.getMethodFault(var35).getMessage();
               if (!StringUtils.isBlank(errorMsg) && errorMsg.indexOf("vSAN iSCSI Target Service is not enabled or the enable task is in progress.") == -1) {
                  Exception ex = new Exception(var35.getLocalizedMessage(), var35);
                  throw ex;
               }

               _logger.info("iscsi targets service enabling in progress, ignore the error");
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

         if (ArrayUtils.isNotEmpty(initiatorGroups)) {
            VsanIscsiInitiatorGroup[] var38 = initiatorGroups;
            int var39 = initiatorGroups.length;

            for(int var40 = 0; var40 < var39; ++var40) {
               VsanIscsiInitiatorGroup group = var38[var40];
               groups.add(new InitiatorGroup(group));
            }
         }

         return (InitiatorGroup[])groups.toArray(new InitiatorGroup[0]);
      }
   }

   @TsService
   public VsanIscsiInitiatorGroup getVsanIscsiInitiatorGroup(ManagedObjectReference clusterRef, String name) throws Exception {
      if (!VsanCapabilityUtils.isIscsiTargetsSupportedOnVc(clusterRef)) {
         return null;
      } else {
         VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
         Throwable var4 = null;

         VsanIscsiInitiatorGroup var35;
         try {
            VsanIscsiTargetSystem vsanIscsiSystem = conn.getVsanIscsiSystem();
            VsanIscsiInitiatorGroup initiatorGroup = null;
            VsanProfiler.Point p = _profiler.point("vsanIscsiSystem.getIscsiInitiatorGroup");
            Throwable var8 = null;

            try {
               initiatorGroup = vsanIscsiSystem.getIscsiInitiatorGroup(clusterRef, name);
            } catch (Throwable var31) {
               var8 = var31;
               throw var31;
            } finally {
               if (p != null) {
                  if (var8 != null) {
                     try {
                        p.close();
                     } catch (Throwable var30) {
                        var8.addSuppressed(var30);
                     }
                  } else {
                     p.close();
                  }
               }

            }

            var35 = initiatorGroup;
         } catch (Throwable var33) {
            var4 = var33;
            throw var33;
         } finally {
            if (conn != null) {
               if (var4 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var29) {
                     var4.addSuppressed(var29);
                  }
               } else {
                  conn.close();
               }
            }

         }

         return var35;
      }
   }

   @TsService
   public InitiatorGroupInitiator[] getInitiatorGroupInitiatorList(ManagedObjectReference clusterRef, String initiatorGroupIqn) throws Exception {
      if (!VsanCapabilityUtils.isIscsiTargetsSupportedOnVc(clusterRef)) {
         return null;
      } else {
         VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
         Throwable var4 = null;

         InitiatorGroupInitiator[] var37;
         try {
            VsanIscsiTargetSystem vsanIscsiSystem = conn.getVsanIscsiSystem();
            VsanIscsiInitiatorGroup vsanIscsiInitiatorGroup = null;
            VsanProfiler.Point p = _profiler.point("vsanIscsiSystem.getIscsiInitiatorGroup");
            Throwable var8 = null;

            try {
               vsanIscsiInitiatorGroup = vsanIscsiSystem.getIscsiInitiatorGroup(clusterRef, initiatorGroupIqn);
            } catch (Throwable var31) {
               var8 = var31;
               throw var31;
            } finally {
               if (p != null) {
                  if (var8 != null) {
                     try {
                        p.close();
                     } catch (Throwable var30) {
                        var8.addSuppressed(var30);
                     }
                  } else {
                     p.close();
                  }
               }

            }

            InitiatorGroupInitiator[] initiators = null;
            if (vsanIscsiInitiatorGroup != null) {
               String[] initiatorIqns = vsanIscsiInitiatorGroup.getInitiators();
               if (ArrayUtils.isNotEmpty(initiatorIqns)) {
                  initiators = new InitiatorGroupInitiator[initiatorIqns.length];

                  for(int i = 0; i < initiatorIqns.length; ++i) {
                     InitiatorGroupInitiator initiator = new InitiatorGroupInitiator();
                     initiator.name = initiatorIqns[i];
                     initiators[i] = initiator;
                  }
               }
            }

            var37 = initiators;
         } catch (Throwable var33) {
            var4 = var33;
            throw var33;
         } finally {
            if (conn != null) {
               if (var4 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var29) {
                     var4.addSuppressed(var29);
                  }
               } else {
                  conn.close();
               }
            }

         }

         return var37;
      }
   }

   @TsService
   public VsanIscsiTargetBasicInfo[] getVsanIscsiInitiatorGroupTargetList(ManagedObjectReference clusterRef, String initiatorGroupIqn) throws Exception {
      if (!VsanCapabilityUtils.isIscsiTargetsSupportedOnVc(clusterRef)) {
         return null;
      } else {
         VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
         Throwable var4 = null;

         VsanIscsiTargetBasicInfo[] var35;
         try {
            VsanIscsiTargetSystem vsanIscsiSystem = conn.getVsanIscsiSystem();
            VsanIscsiInitiatorGroup vsanIscsiInitiatorGroup = null;
            VsanProfiler.Point p = _profiler.point("vsanIscsiSystem.getIscsiInitiatorGroup");
            Throwable var8 = null;

            try {
               vsanIscsiInitiatorGroup = vsanIscsiSystem.getIscsiInitiatorGroup(clusterRef, initiatorGroupIqn);
            } catch (Throwable var31) {
               var8 = var31;
               throw var31;
            } finally {
               if (p != null) {
                  if (var8 != null) {
                     try {
                        p.close();
                     } catch (Throwable var30) {
                        var8.addSuppressed(var30);
                     }
                  } else {
                     p.close();
                  }
               }

            }

            var35 = vsanIscsiInitiatorGroup.getTargets();
         } catch (Throwable var33) {
            var4 = var33;
            throw var33;
         } finally {
            if (conn != null) {
               if (var4 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var29) {
                     var4.addSuppressed(var29);
                  }
               } else {
                  conn.close();
               }
            }

         }

         return var35;
      }
   }
}
