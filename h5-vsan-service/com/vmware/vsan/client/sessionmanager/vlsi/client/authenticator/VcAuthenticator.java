package com.vmware.vsan.client.sessionmanager.vlsi.client.authenticator;

import com.vmware.vsan.client.sessionmanager.vlsi.client.VlsiConnection;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcConnection;

public abstract class VcAuthenticator extends Authenticator {
   protected final String locale;

   public VcAuthenticator(String locale) {
      this.locale = locale;
   }

   public VcAuthenticator(String locale, int id) {
      super(id);
      this.locale = locale;
   }

   public void login(VlsiConnection connection) {
      this.loginVc((VcConnection)connection);
   }

   protected abstract void loginVc(VcConnection var1);

   public void logout(VlsiConnection connection) {
      this.logoutVc((VcConnection)connection);
   }

   protected void logoutVc(VcConnection connection) {
      connection.getSessionManager().logout();
   }

   public String getLocale() {
      return this.locale;
   }

   public int hashCode() {
      int prime = true;
      int result = super.hashCode();
      result = 31 * result + (this.locale == null ? 0 : this.locale.hashCode());
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
         VcAuthenticator other = (VcAuthenticator)obj;
         if (this.locale == null) {
            if (other.locale != null) {
               return false;
            }
         } else if (!this.locale.equals(other.locale)) {
            return false;
         }

         return true;
      }
   }
}
