package com.vmware.vsan.client.services.async;

import com.vmware.vise.usersession.UserSession;
import com.vmware.vise.usersession.UserSessionService;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AsyncUserSessionService {
   @Autowired
   UserSessionService userSessionService;

   public UserSession getUserSession() {
      UserSession userSession = this.userSessionService.getUserSession();
      if (userSession != null) {
         return userSession;
      } else {
         Thread currentThread = Thread.currentThread();
         if (currentThread instanceof SessionAwareThread) {
            return (UserSession)((SessionAwareThread)currentThread).userSession.get();
         } else {
            throw new VsanUiLocalizableException("vsan.common.missing.session.error");
         }
      }
   }
}
