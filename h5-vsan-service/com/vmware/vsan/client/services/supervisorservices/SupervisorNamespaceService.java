package com.vmware.vsan.client.services.supervisorservices;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vapi.bindings.client.InvocationConfig;
import com.vmware.vcenter.namespaces.SupervisorServices;
import com.vmware.vcenter.namespaces.SupervisorServicesTypes.SetSpec;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.services.supervisorservices.model.AdvancedSetting;
import com.vmware.vsan.client.services.supervisorservices.model.SupervisorService;
import com.vmware.vsan.client.services.vapi.VapiService;
import com.vmware.vsan.client.util.Measure;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SupervisorNamespaceService {
   @Autowired
   private VapiService vapiService;

   @TsService
   public void updateSupervisorService(ManagedObjectReference clusterRef, SupervisorService supervisorService) {
      Validate.notNull(clusterRef);
      Validate.notNull(supervisorService);
      SupervisorServices supervisorServiceStub = (SupervisorServices)this.vapiService.createStub(SupervisorServices.class);
      SetSpec setSpec = this.createSpec(clusterRef, supervisorService);
      InvocationConfig invocationConfig = this.vapiService.createConfig(clusterRef.getServerGuid());

      try {
         Measure measure = new Measure("SupervisorService.set");
         Throwable var7 = null;

         try {
            supervisorServiceStub.set(clusterRef.getValue(), supervisorService.serviceId, setSpec, invocationConfig);
         } catch (Throwable var17) {
            var7 = var17;
            throw var17;
         } finally {
            if (measure != null) {
               if (var7 != null) {
                  try {
                     measure.close();
                  } catch (Throwable var16) {
                     var7.addSuppressed(var16);
                  }
               } else {
                  measure.close();
               }
            }

         }

      } catch (Exception var19) {
         throw new VsanUiLocalizableException("vsan.supervisorservices.error", var19);
      }
   }

   private SetSpec createSpec(ManagedObjectReference clusterRef, SupervisorService supervisorService) {
      SetSpec setSpec = new SetSpec();
      setSpec.setEnabled(supervisorService.isEnabled);
      setSpec.setVersion(supervisorService.version);
      if (VsanCapabilityUtils.isPersistenceServiceAirGapSupportedOnVc(clusterRef)) {
         setSpec.setServiceConfig(this.createServiceConfigMap(supervisorService));
      }

      return setSpec;
   }

   private Map createServiceConfigMap(SupervisorService supervisorService) {
      Map serviceConfig = new HashMap();
      if (!CollectionUtils.isEmpty(supervisorService.advancedSettings)) {
         Iterator var3 = supervisorService.advancedSettings.iterator();

         while(var3.hasNext()) {
            AdvancedSetting setting = (AdvancedSetting)var3.next();
            serviceConfig.put(setting.key, setting.value);
         }
      }

      if (!StringUtils.isEmpty(supervisorService.repo)) {
         serviceConfig.put("registryName", supervisorService.repo);
      }

      if (!StringUtils.isEmpty(supervisorService.username)) {
         serviceConfig.put("registryUsername", supervisorService.username);
      }

      if (!StringUtils.isEmpty(supervisorService.password)) {
         serviceConfig.put("registryPasswd", supervisorService.password);
      }

      return serviceConfig;
   }
}
