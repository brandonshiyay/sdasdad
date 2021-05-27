package com.vmware.vsan.client.sessionmanager.common;

import com.vmware.vsan.client.sessionmanager.common.util.RequestUtil;
import com.vmware.vsan.client.sessionmanager.resource.CachedResourceFactory;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

public class RequestConnectionCleanupInterceptor implements HandlerInterceptor {
   private Log logger = LogFactory.getLog(this.getClass());
   @Autowired
   @Qualifier("vsanFactory")
   private CachedResourceFactory vsanFactory;

   public boolean preHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o) throws Exception {
      String requestId = RequestUtil.assignVsanRequestId();
      this.logger.trace("Inside RequestConnectionCleanupInterceptor preHandle method for request: " + requestId);
      return true;
   }

   public void postHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, ModelAndView modelAndView) throws Exception {
      List removedConnections = this.vsanFactory.removeRequestEntries();
      this.logger.debug("RequestConnectionCleanupInterceptor: Removed cached vSAN connections: " + removedConnections.size());
   }

   public void afterCompletion(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, Exception e) throws Exception {
      this.logger.trace("Inside RequestConnectionCleanupInterceptor afterCompletion method" + RequestUtil.getVsanRequestIdKey());
   }
}
