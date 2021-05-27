package com.vmware.vsan.client.services;

import com.vmware.vise.usersession.UserSessionService;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

public class AuthenticationFilter implements Filter {
   private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);
   @Autowired
   private UserSessionService userSessionService;

   public void init(FilterConfig filterConfig) {
      WebApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(filterConfig.getServletContext());
      AutowireCapableBeanFactory factory = context.getAutowireCapableBeanFactory();
      factory.autowireBean(this);
   }

   public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
      if (this.userSessionService.getUserSession() == null) {
         HttpServletRequest httpRequest = (HttpServletRequest)request;
         HttpServletResponse httpResponse = (HttpServletResponse)response;
         logger.warn(String.format("Null session detected for a %s request to %s", httpRequest.getMethod(), httpRequest.getRequestURL()));
         httpResponse.setStatus(401);
      } else {
         filterChain.doFilter(request, response);
      }
   }

   public void destroy() {
   }
}
