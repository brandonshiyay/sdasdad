package com.vmware.vsan.client.services.fileservice;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.vsan.VsanFileServiceSystem;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.fileservice.model.ShareSnapshotRemoveResult;
import com.vmware.vsan.client.services.fileservice.model.VsanFileShareSnapshotConfig;
import com.vmware.vsan.client.services.fileservice.model.VsanFileShareSnapshotQueryResult;
import com.vmware.vsan.client.services.fileservice.model.VsanFileShareSnapshotQuerySpec;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsan.client.util.VmodlHelper;
import com.vmware.vsphere.client.vsan.util.Utils;
import java.util.ArrayList;
import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class VsanFileServiceSnapshotService {
   private static final Log logger = LogFactory.getLog(VsanFileServiceSnapshotService.class);
   @Autowired
   private VsanClient vsanClient;

   @TsService
   public ManagedObjectReference createShareSnapshot(ManagedObjectReference clusterRef, VsanFileShareSnapshotConfig snapshotConfig) {
      Validate.notNull(clusterRef);
      Validate.notNull(snapshotConfig);

      try {
         VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
         Throwable var4 = null;

         ManagedObjectReference var9;
         try {
            VsanFileServiceSystem fileServiceSystem = conn.getVsanFileServiceSystem();
            Measure measure = new Measure("VsanFileServiceSystem.createFileShareSnapshot");
            Throwable var7 = null;

            try {
               ManagedObjectReference taskRef = fileServiceSystem.createFileShareSnapshot(snapshotConfig.toVmodl(), clusterRef);
               var9 = VmodlHelper.assignServerGuid(taskRef, clusterRef.getServerGuid());
            } catch (Throwable var34) {
               var7 = var34;
               throw var34;
            } finally {
               if (measure != null) {
                  if (var7 != null) {
                     try {
                        measure.close();
                     } catch (Throwable var33) {
                        var7.addSuppressed(var33);
                     }
                  } else {
                     measure.close();
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
         throw new VsanUiLocalizableException("vsan.fileservice.error.createShareSnapshot", "Cannot create a share snapshotConfig: " + snapshotConfig, var38, new Object[0]);
      }
   }

   @TsService
   public VsanFileShareSnapshotQueryResult queryShareSnapshots(ManagedObjectReference clusterRef, VsanFileShareSnapshotQuerySpec spec) {
      Validate.notNull(clusterRef);
      Validate.notNull(spec);

      try {
         VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
         Throwable var4 = null;

         Object var8;
         try {
            VsanFileServiceSystem fileServiceSystem = conn.getVsanFileServiceSystem();
            Measure measure = new Measure("VsanFileServiceSystem.queryFileShareSnapshots");
            Throwable var7 = null;

            try {
               var8 = VsanFileShareSnapshotQueryResult.fromVmodl(fileServiceSystem.queryFileShareSnapshots(spec.toVmodl(), clusterRef));
            } catch (Throwable var33) {
               var8 = var33;
               var7 = var33;
               throw var33;
            } finally {
               if (measure != null) {
                  if (var7 != null) {
                     try {
                        measure.close();
                     } catch (Throwable var32) {
                        var7.addSuppressed(var32);
                     }
                  } else {
                     measure.close();
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

         return (VsanFileShareSnapshotQueryResult)var8;
      } catch (Exception var37) {
         throw new VsanUiLocalizableException("vsan.fileservice.error.queryShareSnapshot", "Cannot load snapshots for share: " + spec.shareUuid, var37, new Object[0]);
      }
   }

   @TsService
   public ShareSnapshotRemoveResult removeShareSnapshot(ManagedObjectReference clusterRef, String shareUuid, String[] snapshotNames) {
      Validate.notNull(clusterRef);
      Validate.notNull(shareUuid);
      Validate.notNull(snapshotNames);
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var5 = null;

      try {
         ShareSnapshotRemoveResult result = new ShareSnapshotRemoveResult();
         result.errors = new ArrayList();
         result.taskRefs = new ArrayList();
         VsanFileServiceSystem fileServiceSystem = conn.getVsanFileServiceSystem();
         String[] var8 = snapshotNames;
         int var9 = snapshotNames.length;

         for(int var10 = 0; var10 < var9; ++var10) {
            String snapshot = var8[var10];

            try {
               Measure measure = new Measure("VsanFileServiceSystem.removeFileShareSnapshot");
               Throwable var13 = null;

               try {
                  ManagedObjectReference taskRef = fileServiceSystem.removeFileShareSnapshot(shareUuid, snapshot, clusterRef);
                  result.taskRefs.add(VmodlHelper.assignServerGuid(taskRef, clusterRef.getServerGuid()));
               } catch (Throwable var38) {
                  var13 = var38;
                  throw var38;
               } finally {
                  if (measure != null) {
                     if (var13 != null) {
                        try {
                           measure.close();
                        } catch (Throwable var37) {
                           var13.addSuppressed(var37);
                        }
                     } else {
                        measure.close();
                     }
                  }

               }
            } catch (Exception var40) {
               result.errors.add(Utils.getLocalizedString("vsan.fileservice.error.removeShareSnapshot") + "Cannot remove the snapshot " + snapshot + " for share " + shareUuid);
               logger.error("Cannot remove the snapshot " + snapshot + " for share " + shareUuid, var40);
            }
         }

         ShareSnapshotRemoveResult var43 = result;
         return var43;
      } catch (Throwable var41) {
         var5 = var41;
         throw var41;
      } finally {
         if (conn != null) {
            if (var5 != null) {
               try {
                  conn.close();
               } catch (Throwable var36) {
                  var5.addSuppressed(var36);
               }
            } else {
               conn.close();
            }
         }

      }
   }
}
