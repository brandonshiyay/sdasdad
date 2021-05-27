package com.vmware.vsan.client.services;

import com.vmware.vim.binding.vim.fault.VsanFault;
import com.vmware.vsphere.client.vsan.util.Utils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public class BackendLocalizedException extends RuntimeException {
   public BackendLocalizedException(Throwable exception) {
      super(!StringUtils.isEmpty(exception.getLocalizedMessage()) ? exception.getLocalizedMessage() : Utils.getLocalizedString("vsan.common.generic.error"));
   }

   public BackendLocalizedException(VsanFault exception) {
      super(getVsanFaultMessage(exception));
   }

   private static String getVsanFaultMessage(VsanFault exception) {
      if (ArrayUtils.isNotEmpty(exception.getFaultMessage())) {
         return exception.getFaultMessage()[0].getMessage();
      } else {
         return !StringUtils.isEmpty(exception.getLocalizedMessage()) ? exception.getLocalizedMessage() : Utils.getLocalizedString("vsan.common.generic.error");
      }
   }
}
