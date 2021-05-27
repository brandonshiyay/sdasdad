package com.vmware.vsan.client.sessionmanager.vlsi.client;

import com.vmware.vim.vmomi.client.Client;
import com.vmware.vim.vmomi.client.Client.Factory;
import com.vmware.vim.vmomi.client.common.ProtocolBinding;
import com.vmware.vim.vmomi.client.common.Session;
import com.vmware.vsan.client.sessionmanager.common.util.CheckedRunnable;
import com.vmware.vsan.client.sessionmanager.resource.ResourceFactory;
import com.vmware.vsan.client.sessionmanager.vlsi.client.http.ClientCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractConnectionFactory implements ResourceFactory {
   private static Logger logger = LoggerFactory.getLogger(AbstractConnectionFactory.class);

   public VlsiConnection acquire(VlsiSettings settings) {
      VlsiConnection result = this.buildConnection(settings);

      try {
         this.onPreConnect(settings, result);
         logger.trace("Opening HTTP connection.");
         result.setClientConfig((ClientCfg)settings.getHttpFactory().acquire(settings.getHttpSettings()));
         logger.trace("Initializing VLSI client.");
         result.setClient(this.makeClient(settings, result));
         this.onConnect(settings, result);
         logger.trace("Authenticating connection.");
         settings.getAuthenticator().login(result);
      } catch (Exception var4) {
         result.close();
         CheckedRunnable.handle(var4);
      }

      logger.debug("Created connection: {}", result);
      return result;
   }

   protected void release(VlsiSettings settings, VlsiConnection resource) {
      try {
         if (settings.getAuthenticator() != null) {
            settings.getAuthenticator().logout(resource);
         }
      } catch (Exception var6) {
         logger.warn("Ignoring unsuccessful logout", var6);
      }

      try {
         resource.getClient().shutdown();
      } catch (Exception var5) {
         logger.warn("Ignoring problem when releasing client", var5);
      }

      try {
         if (resource.getClientConfig() != null) {
            resource.getClientConfig().close();
         }
      } catch (Exception var4) {
         logger.warn("Ignoring problem when releasing HttpConfig", var4);
      }

      logger.debug("Closed connection: {}", resource);
   }

   protected abstract VlsiConnection buildConnection(VlsiSettings var1);

   protected void onPreConnect(final VlsiSettings settings, final VlsiConnection connection) {
      connection.setCloseHandler(new Runnable() {
         public void run() {
            AbstractConnectionFactory.this.release(settings, connection);
         }
      });
   }

   protected void onConnect(VlsiSettings settings, VlsiConnection connection) {
      connection.settings = settings;
   }

   protected Client makeClient(VlsiSettings settings, VlsiConnection connection) {
      Client client = Factory.createClient(settings.getHttpSettings().makeUri(), settings.getHttpSettings().getVersion(), settings.getHttpSettings().getVmodlContext(), connection.getClientConfig().getClientConfig());
      if (settings.getSessionCookie() != null) {
         ProtocolBinding binding = client.getBinding();
         Session session = binding.createSession(settings.getSessionCookie());
         binding.setSession(session);
      }

      return client;
   }
}
