package com.vmware.vsan.client.sessionmanager.vlsi.client.vsan;

import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanCapability;
import com.vmware.vim.vsan.binding.vim.cluster.VsanCapabilitySystem;
import com.vmware.vim.vsan.binding.vsan.version.versions;
import com.vmware.vsan.client.sessionmanager.common.VersionService;
import com.vmware.vsan.client.sessionmanager.common.util.RequestUtil;
import com.vmware.vsan.client.sessionmanager.vlsi.client.AbstractConnectionFactory;
import com.vmware.vsan.client.sessionmanager.vlsi.client.VlsiSettings;
import com.vmware.vsan.client.sessionmanager.vlsi.client.http.HttpSettings;
import com.vmware.vsan.client.util.Measure;
import java.util.Arrays;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class VsanExploratoryFactory extends AbstractConnectionFactory {
   private static final Log logger = LogFactory.getLog(VsanExploratoryFactory.class);
   private static final String VSAN_HEALTH_SERVICE_SUBDIR = "/vsanHealth";
   @Autowired
   private VersionService versionService;

   public VsanConnection acquire(VsanVlsiSettings settings) {
      Class releasedVmodlVersion = this.getReleasedVmodlVersion(settings);
      VsanConnection releasedVersionConnection = (VsanConnection)super.acquire((VlsiSettings)this.getVsanVlsiSettings(settings, releasedVmodlVersion));
      if (this.isDevelopmentVmodlVersionSupported(releasedVersionConnection)) {
         logger.debug("The VC supports dev VMODL versioning.");
         releasedVersionConnection.close();
         return (VsanConnection)super.acquire((VlsiSettings)this.getVsanVlsiSettings(settings, versions.VSAN_VERSION_NEWEST));
      } else {
         logger.debug("The VC does NOT support dev VMODL versioning.");
         return releasedVersionConnection;
      }
   }

   protected VsanConnection buildConnection(VsanVlsiSettings id) {
      return new VsanConnection();
   }

   private Class getReleasedVmodlVersion(VlsiSettings settings) {
      String serverUri = settings.getHttpSettings().getServiceUri().toString();
      return this.versionService.getVsanVmodlVersion(serverUri);
   }

   private VsanVlsiSettings getVsanVlsiSettings(VlsiSettings settings, Class vmodlVersion) {
      HttpSettings vcHttpSettings = settings.getHttpSettings();
      HttpSettings vsanHttpSettings = vcHttpSettings.setPath("/vsanHealth").setVersion(vmodlVersion);
      VsanVlsiSettings vsanSettings = new VsanVlsiSettings(settings.setHttpSettings(vsanHttpSettings), RequestUtil.getVsanRequestIdKey());
      logger.info("Creating vSAN connection using VMODL version: " + vmodlVersion);
      logger.debug("Using settings: " + vsanSettings);
      return vsanSettings;
   }

   private boolean isDevelopmentVmodlVersionSupported(VsanConnection conn) {
      VsanCapabilitySystem vsanCapabilitySystem = conn.getVsanCapabilitySystem();
      VsanCapability[] capabilities = null;

      try {
         Measure measure = new Measure("VsanVcCapabilitySystem.GetCapabilities");
         Throwable var5 = null;

         try {
            capabilities = vsanCapabilitySystem.getCapabilities((ManagedObjectReference[])null);
         } catch (Throwable var15) {
            var5 = var15;
            throw var15;
         } finally {
            if (measure != null) {
               if (var5 != null) {
                  try {
                     measure.close();
                  } catch (Throwable var14) {
                     var5.addSuppressed(var14);
                  }
               } else {
                  measure.close();
               }
            }

         }
      } catch (Exception var17) {
         logger.warn("Failed to get capabilities.", var17);
         return false;
      }

      if (ArrayUtils.isEmpty(capabilities)) {
         logger.warn("No capabilities found...");
         return false;
      } else {
         VsanCapability vcCapabilities = capabilities[0];
         if (vcCapabilities != null && !ArrayUtils.isEmpty(vcCapabilities.capabilities)) {
            return Arrays.stream(vcCapabilities.capabilities).anyMatch((cap) -> {
               return "apidevversionenabled".equals(cap);
            });
         } else {
            logger.warn("No VC capabilities found...");
            return false;
         }
      }
   }
}
