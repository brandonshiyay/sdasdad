package com.vmware.vsan.client.services.update;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanClusterHclInfo;
import com.vmware.vim.vsan.binding.vim.cluster.VsanVcClusterHealthSystem;
import com.vmware.vim.vsan.binding.vim.cluster.VsanVumSystem;
import com.vmware.vim.vsan.binding.vim.cluster.VsanVumSystemConfig;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsphere.client.vsan.health.HclUpdateOfflineSpec;
import com.vmware.vsphere.client.vsan.impl.VsanPropertyProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.zip.DataFormatException;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;
import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sun.misc.BASE64Encoder;

@Component
public class UpdateDbService {
   private static final Log logger = LogFactory.getLog(UpdateDbService.class);
   @Autowired
   private VsanPropertyProvider vsanPropertyProvider;
   @Autowired
   VsanClient vsanClient;

   @TsService
   public Date getHclLastUpdatedDate(ManagedObjectReference vcRef) throws Exception {
      ManagedObjectReference clusterRef = null;
      if (!VsanCapabilityUtils.isGetHclLastUpdateOnVcSupported(vcRef)) {
         clusterRef = this.vsanPropertyProvider.getAnyVsanCluster(vcRef);
         if (clusterRef == null) {
            logger.warn("Cannot find a cluster on the VC: " + vcRef);
            return null;
         }
      }

      VsanConnection conn = this.vsanClient.getConnection(vcRef.getServerGuid());
      Throwable var4 = null;

      Date var9;
      try {
         VsanVcClusterHealthSystem healthSystem = conn.getVsanVcClusterHealthSystem();
         Measure measure;
         if (healthSystem == null) {
            measure = null;
            return measure;
         }

         measure = new Measure("UpdateDbService.getHclLastUpdate");
         Throwable var7 = null;

         try {
            VsanClusterHclInfo hclInfo = healthSystem.getClusterHclInfo(clusterRef, false, false, (String)null);
            if (hclInfo != null) {
               var9 = this.convertUtcDate(hclInfo.getHclDbLastUpdate());
               return var9;
            }

            var9 = null;
         } catch (Throwable var36) {
            var7 = var36;
            throw var36;
         } finally {
            if (measure != null) {
               if (var7 != null) {
                  try {
                     measure.close();
                  } catch (Throwable var35) {
                     var7.addSuppressed(var35);
                  }
               } else {
                  measure.close();
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
   }

   @TsService
   public Date getReleaseCatalogLastUpdatedDate(ManagedObjectReference moRef) {
      VsanConnection conn = this.vsanClient.getConnection(moRef.getServerGuid());
      Throwable var3 = null;

      Date var8;
      try {
         VsanVumSystem vumSystem = conn.getVsanVumSystem();
         Measure measure = new Measure("UpdateDbService.getVsanVumConfig");
         Throwable var6 = null;

         try {
            VsanVumSystemConfig vumSystemConfig = vumSystem.getVsanVumConfig();
            var8 = this.convertUtcDate(vumSystemConfig.releaseDbLastUpdate);
         } catch (Throwable var31) {
            var6 = var31;
            throw var31;
         } finally {
            if (measure != null) {
               if (var6 != null) {
                  try {
                     measure.close();
                  } catch (Throwable var30) {
                     var6.addSuppressed(var30);
                  }
               } else {
                  measure.close();
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
   public void updateHclDbFromWeb(ManagedObjectReference entity) throws Exception {
      VsanConnection conn = this.vsanClient.getConnection(entity.getServerGuid());
      Throwable var3 = null;

      try {
         VsanVcClusterHealthSystem healthSystem = conn.getVsanVcClusterHealthSystem();
         Measure measure = new Measure("UpdateDbService.updateHclDbFromWeb");
         Throwable var6 = null;

         try {
            healthSystem.updateHclDbFromWeb((String)null);
         } catch (Throwable var29) {
            var6 = var29;
            throw var29;
         } finally {
            if (measure != null) {
               if (var6 != null) {
                  try {
                     measure.close();
                  } catch (Throwable var28) {
                     var6.addSuppressed(var28);
                  }
               } else {
                  measure.close();
               }
            }

         }
      } catch (Throwable var31) {
         var3 = var31;
         throw var31;
      } finally {
         if (conn != null) {
            if (var3 != null) {
               try {
                  conn.close();
               } catch (Throwable var27) {
                  var3.addSuppressed(var27);
               }
            } else {
               conn.close();
            }
         }

      }

   }

   @TsService
   public boolean uploadHclDb(ManagedObjectReference entity, HclUpdateOfflineSpec spec) throws Exception {
      VsanConnection conn = this.vsanClient.getConnection(entity.getServerGuid());
      Throwable var4 = null;

      Throwable var8;
      try {
         VsanVcClusterHealthSystem healthSystem = conn.getVsanVcClusterHealthSystem();
         Measure measure = new Measure("UpdateDbService.uploadHclDb");
         Throwable var7 = null;

         try {
            var8 = healthSystem.uploadHclDb(this.generateGzipBase64EncodedString(this.decompressZlibByteArrayString(spec.hclDatabaseFileZlibCompressedContent)));
         } catch (Throwable var31) {
            var8 = var31;
            var7 = var31;
            throw var31;
         } finally {
            if (measure != null) {
               if (var7 != null) {
                  try {
                     measure.close();
                  } catch (Throwable var30) {
                     var7.addSuppressed(var30);
                  }
               } else {
                  measure.close();
               }
            }

         }
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

      return (boolean)var8;
   }

   @TsService
   public void uploadReleaseDb(ManagedObjectReference entity, byte[] data) throws Exception {
      if (!VsanCapabilityUtils.isUpdateVumReleaseCatalogOfflineSupported(entity)) {
         throw new VsanUiLocalizableException("vsan.common.error.notSupported");
      } else {
         VsanConnection conn = this.vsanClient.getConnection(entity.getServerGuid());
         Throwable var4 = null;

         try {
            VsanVumSystem vumSystem = conn.getVsanVumSystem();
            Measure measure = new Measure("UpdateDbService.uploadReleaseDb");
            Throwable var7 = null;

            try {
               String releaseDb = new String(this.decompressZlibByteArrayString(data));
               vumSystem.uploadReleaseDb(releaseDb);
            } catch (Throwable var30) {
               var7 = var30;
               throw var30;
            } finally {
               if (measure != null) {
                  if (var7 != null) {
                     try {
                        measure.close();
                     } catch (Throwable var29) {
                        var7.addSuppressed(var29);
                     }
                  } else {
                     measure.close();
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

   private Date convertUtcDate(Calendar date) {
      if (date != null) {
         date.add(14, date.getTimeZone().getRawOffset());
      }

      return date == null ? null : date.getTime();
   }

   private String generateGzipBase64EncodedString(byte[] fileByteArray) throws Exception {
      Validate.notNull(fileByteArray);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      GZIPOutputStream gzipos = new GZIPOutputStream(baos);

      try {
         gzipos.write(fileByteArray);
      } catch (Exception var8) {
         throw var8;
      } finally {
         gzipos.close();
         baos.close();
      }

      BASE64Encoder var4 = new BASE64Encoder();
      return var4.encode(baos.toByteArray());
   }

   private byte[] decompressZlibByteArrayString(byte[] zlibByteArray) throws VsanUiLocalizableException, DataFormatException, IOException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream(zlibByteArray.length);
      Inflater decompressor = new Inflater();

      try {
         decompressor.setInput(zlibByteArray);
         byte[] buf = new byte[1024];

         while(!decompressor.finished()) {
            int count = decompressor.inflate(buf);
            if (count == 0 && decompressor.needsInput()) {
               throw new VsanUiLocalizableException("vsan.update.catalog.content.error");
            }

            baos.write(buf, 0, count);
         }
      } finally {
         decompressor.end();
         baos.close();
      }

      return baos.toByteArray();
   }
}
