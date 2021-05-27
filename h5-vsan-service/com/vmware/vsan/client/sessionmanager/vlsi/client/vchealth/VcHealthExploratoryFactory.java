package com.vmware.vsan.client.sessionmanager.vlsi.client.vchealth;

import com.vmware.vim.vsan.binding.vsan.version.version7;
import com.vmware.vsan.client.sessionmanager.resource.ResourceFactory;
import com.vmware.vsan.client.sessionmanager.vlsi.client.VlsiExploratorySettings;
import com.vmware.vsan.client.sessionmanager.vlsi.client.VlsiSettings;
import com.vmware.vsan.client.sessionmanager.vlsi.client.http.HttpSettings;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class VcHealthExploratoryFactory implements ResourceFactory {
   private static final Log logger = LogFactory.getLog(VcHealthExploratoryFactory.class);
   private static final String VSPHERE_HEALTH_SERVICE_SUBDIR = "/analytics/cloudhealth/sdk";
   private static final Class vcHealthVmodlVersion = version7.class;
   private final ResourceFactory factory;

   public VcHealthExploratoryFactory(ResourceFactory factory) {
      this.factory = factory;
   }

   public VcHealthConnection acquire(VlsiExploratorySettings settings) {
      HttpSettings vcHttpSettings = settings.getSettings().getHttpSettings();
      HttpSettings vsanHttpSettings = vcHttpSettings.setPath("/analytics/cloudhealth/sdk").setVersion(vcHealthVmodlVersion);
      VlsiSettings vlsiSettings = settings.getSettings().setHttpSettings(vsanHttpSettings);
      logger.info("Creating VC Health connection using VMODL version: " + vcHealthVmodlVersion);
      logger.debug("Using settings: " + vlsiSettings);
      return (VcHealthConnection)this.factory.acquire(vlsiSettings);
   }
}
