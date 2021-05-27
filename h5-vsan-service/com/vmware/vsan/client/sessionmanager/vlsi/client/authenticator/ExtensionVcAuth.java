package com.vmware.vsan.client.sessionmanager.vlsi.client.authenticator;

import com.vmware.vsan.client.sessionmanager.common.util.CheckedRunnable;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcConnection;

public class ExtensionVcAuth extends VcAuthenticator {
   protected String extKey;

   public ExtensionVcAuth(String extKey, String locale) {
      super(locale);
      this.extKey = extKey;
   }

   protected void loginVc(final VcConnection connection) {
      CheckedRunnable.withoutChecked(new CheckedRunnable() {
         public void run() throws Exception {
            connection.setSession(connection.getSessionManager().loginExtensionByCertificate(ExtensionVcAuth.this.extKey, ExtensionVcAuth.this.locale));
         }
      });
   }

   public String getExtKey() {
      return this.extKey;
   }

   public int hashCode() {
      int prime = true;
      int result = super.hashCode();
      result = 31 * result + (this.extKey == null ? 0 : this.extKey.hashCode());
      return result;
   }

   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      } else if (!super.equals(obj)) {
         return false;
      } else if (this.getClass() != obj.getClass()) {
         return false;
      } else {
         ExtensionVcAuth other = (ExtensionVcAuth)obj;
         if (this.extKey == null) {
            if (other.extKey != null) {
               return false;
            }
         } else if (!this.extKey.equals(other.extKey)) {
            return false;
         }

         return true;
      }
   }
}
