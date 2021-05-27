package com.vmware.vsan.client.sessionmanager.vlsi.client.explorer;

import com.vmware.vim.binding.lookup.ServiceRegistration;
import com.vmware.vim.binding.lookup.ServiceRegistration.EndpointType;
import com.vmware.vim.binding.lookup.ServiceRegistration.Filter;
import com.vmware.vim.binding.lookup.ServiceRegistration.Info;
import com.vmware.vim.binding.lookup.ServiceRegistration.ServiceType;
import java.util.Map;

public class PbmLsExplorer extends AbstractLsExplorer {
   public PbmLsExplorer(ServiceRegistration lookupService) {
      super(lookupService);
   }

   protected PbmRegistration createRegistration(Info registrationInfo) {
      return new PbmRegistration(registrationInfo);
   }

   protected void mapRegistration(PbmRegistration registration, Map map) {
      map.put(registration.getUuid(), registration);
   }

   protected Filter getFilter() {
      ServiceType serviceType = new ServiceType("com.vmware.vim.sms", "sms");
      EndpointType endpointType = new EndpointType("https", "com.vmware.vim.pbm");
      return new Filter((String)null, (String)null, serviceType, endpointType, (String)null, true);
   }
}
