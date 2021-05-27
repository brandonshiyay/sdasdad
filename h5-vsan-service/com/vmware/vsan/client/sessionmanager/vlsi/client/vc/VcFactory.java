package com.vmware.vsan.client.sessionmanager.vlsi.client.vc;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.vmware.vim.binding.vim.ServiceInstance;
import com.vmware.vim.binding.vim.ServiceInstanceContent;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.sessionmanager.vlsi.client.AbstractConnectionFactory;
import com.vmware.vsan.client.sessionmanager.vlsi.client.VlsiSettings;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

public class VcFactory extends AbstractConnectionFactory {
   private static final Cache serviceInstanceContentCache = CacheBuilder.newBuilder().weakValues().build();
   private static final Callable serviceInstanceContentCacheLoader = () -> {
      return new AtomicReference();
   };

   protected VcConnection buildConnection(VlsiSettings id) {
      return new VcConnection();
   }

   public void onConnect(VlsiSettings id, VcConnection connection) {
      super.onConnect(id, connection);
      ServiceInstance vcSi = (ServiceInstance)connection.createStub(ServiceInstance.class, "ServiceInstance");
      ServiceInstanceContent content = vcSi.getContent();

      try {
         connection.serviceInstanceContentReference = (AtomicReference)serviceInstanceContentCache.get(id.getHttpSettings().getServiceUri().toString(), serviceInstanceContentCacheLoader);
         connection.serviceInstanceContentReference.set(content);
      } catch (ExecutionException var6) {
         throw new VsanUiLocalizableException("vsan.common.vlsi.establish.connection");
      }
   }
}
