package com.vmware.vsan.client.sessionmanager.common.util;

import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.async.SessionAwareThread;
import java.util.UUID;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class RequestUtil {
   private static final String VSAN_REQUEST_ID_KEY = "VSAN_PLUGIN_REQUEST_ID";
   private static Log logger = LogFactory.getLog("com.vmware.vsan.client.sessionmanager.common.util.RequestUtil");
   private static ThreadLocal requestId = new ThreadLocal();

   public static String assignVsanRequestId() {
      ServletRequestAttributes requestAttributes = (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
      if (requestAttributes == null) {
         logger.debug("Missing HTTP request. Probably inside data service call.");
         return assignDataServiceRequestId();
      } else {
         String requestId = generateRequestId();
         logger.debug("Assigned requestID: " + requestId);
         requestAttributes.getRequest().setAttribute("VSAN_PLUGIN_REQUEST_ID", requestId);
         return requestId;
      }
   }

   private static String assignDataServiceRequestId() {
      String clientRequestUuid = (String)requestId.get();
      if (clientRequestUuid != null) {
         logger.error("There is already request context ID set for this thread: " + clientRequestUuid);
         throw new VsanUiLocalizableException();
      } else {
         clientRequestUuid = generateRequestId();
         logger.info("Assigned requestID: " + clientRequestUuid);
         requestId.set(clientRequestUuid);
         return clientRequestUuid;
      }
   }

   private static String generateRequestId() {
      return String.format("%s-%s", Thread.currentThread().getName(), UUID.randomUUID().toString());
   }

   public static void removeVsanRequestId() {
      String clientRequestUuid = (String)requestId.get();
      if (clientRequestUuid != null) {
         requestId.remove();
      } else {
         logger.warn("There is no request context ID set for this thread: " + Thread.currentThread().getName());
      }

   }

   public static String getVsanRequestIdKey() {
      ServletRequestAttributes requestAttributes = (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
      if (requestAttributes == null) {
         return getThreadRequestId();
      } else {
         Object requestId = requestAttributes.getRequest().getAttribute("VSAN_PLUGIN_REQUEST_ID");
         if (requestId == null) {
            logger.error("The HttpRequest has no vSAN request ID specified!");
            throw new VsanUiLocalizableException();
         } else {
            return requestId.toString();
         }
      }
   }

   private static String getThreadRequestId() {
      Thread currentThread = Thread.currentThread();
      String clientRequestUuid;
      if (currentThread instanceof SessionAwareThread) {
         clientRequestUuid = (String)((SessionAwareThread)currentThread).requestId.get();
      } else {
         clientRequestUuid = (String)requestId.get();
      }

      if (clientRequestUuid == null) {
         logger.error("getThreadRequestId(): missing request ID!");
         throw new VsanUiLocalizableException();
      } else {
         return clientRequestUuid;
      }
   }
}
