package com.vmware.vsan.client.sessionmanager.vlsi.client.explorer;

import com.vmware.vim.binding.lookup.ServiceRegistration;
import com.vmware.vim.binding.lookup.ServiceRegistration.EndpointType;
import com.vmware.vim.binding.lookup.ServiceRegistration.Filter;
import com.vmware.vim.binding.lookup.ServiceRegistration.Info;
import com.vmware.vim.binding.lookup.ServiceRegistration.ServiceType;
import com.vmware.vsan.client.sessionmanager.vlsi.util.ServiceRegistrationUtils;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VcLsExplorer extends AbstractLsExplorer {
   private static final ServiceType VC_SERVICE_TYPE = new ServiceType("com.vmware.cis", "vcenterserver");
   private static final EndpointType VC_ENDPOINT_TYPE = new EndpointType("vmomi", "com.vmware.vim");
   private static final Filter ALL_VCS;

   public VcLsExplorer(ServiceRegistration lookupService) {
      super(lookupService);
   }

   protected VcRegistration createRegistration(Info registrationInfo) {
      return new VcRegistration(registrationInfo);
   }

   protected void mapRegistration(VcRegistration registration, Map map) {
      map.put(registration.getUuid(), registration);
   }

   protected Filter getFilter() {
      return ALL_VCS;
   }

   public Map getServerGuidToDomainIdMap() {
      List vcInfos = this.getServiceInfos();
      return (Map)vcInfos.stream().collect(Collectors.toMap(this::getServiceId, ServiceRegistrationUtils::getDomainId));
   }

   public List getVcRegistrations() {
      List vcInfos = this.getServiceInfos();
      return (List)vcInfos.stream().map(VcRegistration::new).collect(Collectors.toList());
   }

   private String getServiceId(Info info) {
      return info.getServiceId();
   }

   static {
      ALL_VCS = new Filter((String)null, (String)null, VC_SERVICE_TYPE, VC_ENDPOINT_TYPE, (String)null, true);
   }
}
