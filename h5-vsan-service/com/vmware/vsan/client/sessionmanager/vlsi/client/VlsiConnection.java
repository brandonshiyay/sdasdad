package com.vmware.vsan.client.sessionmanager.vlsi.client;

import com.vmware.vim.binding.vmodl.ManagedObject;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vmomi.client.Client;
import com.vmware.vsan.client.sessionmanager.resource.Resource;
import com.vmware.vsan.client.sessionmanager.vlsi.client.http.ClientCfg;
import com.vmware.vsan.client.sessionmanager.vlsi.util.MoRef;
import com.vmware.vsan.client.sessionmanager.vlsi.util.RequestContextUtil;

public class VlsiConnection extends Resource {
   protected Client client;
   protected ClientCfg clientCfg;
   protected VlsiSettings settings;

   public Client getClient() {
      return this.client;
   }

   protected void setClient(Client client) {
      this.client = client;
   }

   public ClientCfg getClientConfig() {
      return this.clientCfg;
   }

   public void setClientConfig(ClientCfg clientCfg) {
      this.clientCfg = clientCfg;
   }

   public VlsiSettings getSettings() {
      return this.settings;
   }

   public ManagedObject createStub(Class clazz, String moId) {
      ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();

      ManagedObject var4;
      try {
         Thread.currentThread().setContextClassLoader(VlsiConnection.class.getClassLoader());
         var4 = RequestContextUtil.setDiagnosticOperationId(this.client.createStub(clazz, new MoRef(clazz, moId)));
      } finally {
         Thread.currentThread().setContextClassLoader(oldClassLoader);
      }

      return var4;
   }

   public ManagedObject createStub(Class clazz, ManagedObjectReference moRef) {
      ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();

      ManagedObject var4;
      try {
         Thread.currentThread().setContextClassLoader(VlsiConnection.class.getClassLoader());
         var4 = RequestContextUtil.setDiagnosticOperationId(this.client.createStub(clazz, moRef));
      } finally {
         Thread.currentThread().setContextClassLoader(oldClassLoader);
      }

      return var4;
   }

   public String toString() {
      String connectionType = this.getClass().getSimpleName();
      String host = this.settings != null ? this.settings.getHttpSettings().getHost() : "initializing";
      return String.format("%s(%s)", connectionType, host);
   }
}
