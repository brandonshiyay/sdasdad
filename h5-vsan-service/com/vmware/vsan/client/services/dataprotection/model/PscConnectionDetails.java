package com.vmware.vsan.client.services.dataprotection.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsan.client.sessionmanager.vlsi.client.ls.LookupSvcClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.ls.LookupSvcInfo;
import java.net.URISyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TsModel
public class PscConnectionDetails {
   private static final Logger logger = LoggerFactory.getLogger(PscConnectionDetails.class);
   public String pscHost;
   public Integer pscPort;
   public String pscThumbprint;

   public String toString() {
      return "PscConnectionDetails{pscHost='" + this.pscHost + "', pscPort='" + this.pscPort + "', pscThumbprint='" + this.pscThumbprint + "' " + '}';
   }

   public LookupSvcInfo toLsInfo() {
      if (this.pscHost != null && this.pscThumbprint != null) {
         LookupSvcInfo lookupSvcInfo = null;

         try {
            lookupSvcInfo = new LookupSvcInfo(LookupSvcClient.createServiceUri(this.pscHost, this.pscPort), this.pscThumbprint);
         } catch (URISyntaxException var3) {
            logger.error("Unable to create service URL.", var3);
         }

         return lookupSvcInfo;
      } else {
         return null;
      }
   }
}
