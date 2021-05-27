package com.vmware.vsan.client.sessionmanager.vlsi.client.vsan;

import com.vmware.vsan.client.sessionmanager.resource.ResourceFactory;
import com.vmware.vsan.client.sessionmanager.vlsi.client.VlsiSettings;
import com.vmware.vsan.client.sessionmanager.vlsi.client.authenticator.Authenticator;
import com.vmware.vsan.client.sessionmanager.vlsi.client.http.HttpSettings;

public class VsanVlsiSettings extends VlsiSettings {
   private final String clientRequestId;

   public VsanVlsiSettings(ResourceFactory httpFactory, HttpSettings httpSettings, Authenticator authenticator, String sessionCookie, String clientRequestId) {
      super(httpFactory, httpSettings, authenticator, sessionCookie);
      this.clientRequestId = clientRequestId;
   }

   public VsanVlsiSettings(VlsiSettings vlsiSettings, String clientRequestId) {
      super(vlsiSettings.getHttpFactory(), vlsiSettings.getHttpSettings(), vlsiSettings.getAuthenticator(), vlsiSettings.getSessionCookie());
      this.clientRequestId = clientRequestId;
   }

   public String getClientRequestId() {
      return this.clientRequestId;
   }

   public boolean equals(Object obj) {
      if (!(obj instanceof VsanVlsiSettings)) {
         return false;
      } else {
         VsanVlsiSettings other = (VsanVlsiSettings)obj;
         if (!super.equals(other)) {
            return false;
         } else {
            if (this.clientRequestId == null) {
               if (other.clientRequestId != null) {
                  return false;
               }
            } else if (!this.clientRequestId.equals(other.clientRequestId)) {
               return false;
            }

            return true;
         }
      }
   }
}
