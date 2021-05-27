package com.vmware.vsan.client.sessionmanager.vlsi.util;

import com.vmware.vim.binding.vmodl.ManagedObject;
import com.vmware.vim.sso.client.SamlToken;
import com.vmware.vim.vmomi.core.RequestContext;
import com.vmware.vim.vmomi.core.Stub;
import com.vmware.vim.vmomi.core.impl.RequestContextImpl;
import com.vmware.vim.vmomi.core.security.impl.SignInfoImpl;
import java.security.PrivateKey;
import org.slf4j.MDC;

public class RequestContextUtil {
   private static final String DIAGNOSTIC_CONTEXT_OPERATION_ID = "operationID";
   private static final String VC_SESSION_COOKIE = "Cookie";

   public static ManagedObject setDiagnosticOperationId(ManagedObject t) {
      String opId = MDC.get("operationID");
      if (opId == null) {
         return t;
      } else {
         RequestContext requestContext = ((Stub)t)._getRequestContext();
         if (requestContext == null) {
            requestContext = new RequestContextImpl();
         }

         ((RequestContext)requestContext).put("operationID", opId);
         ((Stub)t)._setRequestContext((RequestContext)requestContext);
         return t;
      }
   }

   public static ManagedObject setSignInfo(ManagedObject t, PrivateKey privateKey, SamlToken token) {
      RequestContext requestContext = ((Stub)t)._getRequestContext();
      if (requestContext == null) {
         requestContext = new RequestContextImpl();
      }

      ((RequestContextImpl)requestContext).setSignInfo(new SignInfoImpl(privateKey, token));
      ((Stub)t)._setRequestContext((RequestContext)requestContext);
      return t;
   }

   public static ManagedObject setVcSessionCookie(ManagedObject t, String cookie) {
      RequestContext requestContext = ((Stub)t)._getRequestContext();
      if (requestContext == null) {
         requestContext = new RequestContextImpl();
      }

      ((RequestContext)requestContext).put("Cookie", cookie);
      ((Stub)t)._setRequestContext((RequestContext)requestContext);
      return t;
   }
}
