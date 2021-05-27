package com.vmware.vsan.client.sessionmanager.vlsi.client.vc;

import com.vmware.vim.binding.vim.ExtensionManager;
import com.vmware.vim.binding.vim.ServiceInstanceContent;
import com.vmware.vim.binding.vim.SessionManager;
import com.vmware.vim.binding.vim.UserSession;
import com.vmware.vim.binding.vim.host.VsanInternalSystem;
import com.vmware.vim.binding.vim.host.VsanSystem;
import com.vmware.vim.binding.vim.option.OptionManager;
import com.vmware.vim.binding.vim.vslm.vcenter.VStorageObjectManager;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.binding.vmodl.query.PropertyCollector;
import com.vmware.vim.vmomi.client.common.ProtocolBinding;
import com.vmware.vim.vmomi.client.common.Session;
import com.vmware.vsan.client.sessionmanager.vlsi.client.VlsiConnection;
import java.util.concurrent.atomic.AtomicReference;

public class VcConnection extends VlsiConnection {
   protected UserSession session;
   protected AtomicReference serviceInstanceContentReference;

   public UserSession getSession() {
      return this.session;
   }

   public void setSession(UserSession session) {
      this.session = session;
   }

   public SessionManager getSessionManager() {
      return (SessionManager)this.createStub(SessionManager.class, this.getContent().getSessionManager());
   }

   public ExtensionManager getExtensionManager() {
      return (ExtensionManager)this.createStub(ExtensionManager.class, this.getContent().getExtensionManager());
   }

   public OptionManager getOptionManager() {
      return (OptionManager)this.createStub(OptionManager.class, this.getContent().getSetting());
   }

   public PropertyCollector getPropertyCollector() {
      return (PropertyCollector)this.createStub(PropertyCollector.class, this.getContent().getPropertyCollector());
   }

   public VStorageObjectManager getVStorageObjectManager() {
      return (VStorageObjectManager)this.createStub(VStorageObjectManager.class, this.getContent().getVStorageObjectManager());
   }

   public VsanSystem getHostVsanSystem(ManagedObjectReference hostRef) {
      ManagedObjectReference hostVsanSystemRef = new ManagedObjectReference("HostVsanSystem", hostRef.getValue().replace("host", "vsanSystem"), hostRef.getServerGuid());
      return (VsanSystem)this.createStub(VsanSystem.class, hostVsanSystemRef);
   }

   public OptionManager getHostAdvancedSettingsManager(ManagedObjectReference hostRef) {
      ManagedObjectReference optionManagerRef = new ManagedObjectReference("OptionManager", hostRef.getValue().replace("host", "EsxHostAdvSettings"), hostRef.getServerGuid());
      return (OptionManager)this.createStub(OptionManager.class, optionManagerRef);
   }

   public VsanInternalSystem getVsanInternalSystem(ManagedObjectReference hostRef) {
      ManagedObjectReference hostVsanSystemRef = new ManagedObjectReference("HostVsanInternalSystem", hostRef.getValue().replace("host", "ha-vsan-internal-system"), hostRef.getServerGuid());
      return (VsanInternalSystem)this.createStub(VsanInternalSystem.class, hostVsanSystemRef);
   }

   public ServiceInstanceContent getContent() {
      return (ServiceInstanceContent)this.serviceInstanceContentReference.get();
   }

   public String getSessionCookie() {
      if (this.client == null) {
         return null;
      } else {
         ProtocolBinding binding = this.client.getBinding();
         if (binding == null) {
            return null;
         } else {
            Session session = binding.getSession();
            if (session == null) {
               return null;
            } else {
               String sessionCookie = session.getId();
               return sessionCookie;
            }
         }
      }
   }

   public String toString() {
      ServiceInstanceContent content = this.getContent();
      return this.settings != null && content != null ? String.format("VcConnection(host=%s, uuid=%s)", this.settings.getHttpSettings().getHost(), content.getAbout().getInstanceUuid()) : "VcConnection(initializing)";
   }
}
