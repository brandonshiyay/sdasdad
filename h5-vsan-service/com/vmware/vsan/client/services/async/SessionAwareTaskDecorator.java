package com.vmware.vsan.client.services.async;

import com.vmware.vise.usersession.UserSession;
import com.vmware.vise.usersession.UserSessionService;
import com.vmware.vsan.client.sessionmanager.common.util.RequestUtil;
import org.springframework.core.task.TaskDecorator;

public class SessionAwareTaskDecorator implements TaskDecorator {
   private UserSessionService userSessionService;

   public SessionAwareTaskDecorator(UserSessionService sessionService) {
      this.userSessionService = sessionService;
   }

   public Runnable decorate(Runnable runnable) {
      UserSession userSession = this.userSessionService.getUserSession();
      String requestId = RequestUtil.getVsanRequestIdKey();
      return () -> {
         Thread currentThread = Thread.currentThread();
         if (currentThread instanceof SessionAwareThread) {
            SessionAwareThread advancedThread = (SessionAwareThread)currentThread;

            try {
               advancedThread.userSession.set(userSession);
               advancedThread.requestId.set(requestId);
               runnable.run();
            } finally {
               advancedThread.userSession.remove();
               advancedThread.requestId.remove();
            }
         } else {
            runnable.run();
         }

      };
   }
}
