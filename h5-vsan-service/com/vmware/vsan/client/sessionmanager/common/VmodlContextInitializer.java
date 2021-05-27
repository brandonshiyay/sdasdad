package com.vmware.vsan.client.sessionmanager.common;

import com.vmware.vim.vmomi.core.types.VmodlContext;
import org.springframework.stereotype.Component;

@Component
public final class VmodlContextInitializer {
   private static String[] VMODL_CONTEXT = new String[]{"com.vmware.vim.binding.vim", "com.vmware.vim.binding.lookup", "com.vmware.vim.binding.sso", "com.vmware.vim.binding.pbm", "com.vmware.vim.vsan.binding.vim"};

   public static VmodlContext createContext() {
      ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
      VmodlContext vmodlContext = null;

      try {
         Thread.currentThread().setContextClassLoader(VmodlContextInitializer.class.getClassLoader());
         vmodlContext = VmodlContext.createContext(VMODL_CONTEXT);
      } finally {
         Thread.currentThread().setContextClassLoader(originalClassLoader);
      }

      return vmodlContext;
   }
}
