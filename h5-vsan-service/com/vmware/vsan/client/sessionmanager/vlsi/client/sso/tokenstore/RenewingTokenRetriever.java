package com.vmware.vsan.client.sessionmanager.vlsi.client.sso.tokenstore;

import com.vmware.vim.sso.client.ConfirmationType;
import com.vmware.vim.sso.client.SamlToken;
import com.vmware.vim.sso.client.TokenSpec;
import com.vmware.vim.sso.client.exception.AuthenticationFailedException;
import com.vmware.vim.sso.client.exception.CertificateValidationException;
import com.vmware.vim.sso.client.exception.InternalError;
import com.vmware.vim.sso.client.exception.InvalidTokenException;
import com.vmware.vim.sso.client.exception.RequestExpiredException;
import com.vmware.vim.sso.client.exception.ServerCommunicationException;
import com.vmware.vim.sso.client.exception.ServerSecurityException;
import com.vmware.vim.sso.client.exception.TimeSynchronizationException;
import com.vmware.vim.sso.client.exception.TokenRequestRejectedException;
import com.vmware.vsan.client.sessionmanager.resource.ResourceFactory;
import com.vmware.vsan.client.sessionmanager.vlsi.client.VlsiSettings;
import com.vmware.vsan.client.sessionmanager.vlsi.client.ls.LookupSvcConnection;
import com.vmware.vsan.client.sessionmanager.vlsi.client.sso.StsService;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RenewingTokenRetriever extends AbstractTokenRetriever {
   private static Logger logger = LoggerFactory.getLogger(RenewingTokenRetriever.class);
   protected static final long ABOUT_TO_EXPIRE_MSEC = 3600000L;
   protected static final long EXTEND_LIFETIME_SEC = 86400L;
   protected static final long RENEW_RETRY_MSEC = 30000L;
   protected volatile SamlToken cachedToken;
   protected volatile boolean running;
   protected final ScheduledExecutorService scheduler;

   public RenewingTokenRetriever(PrivateKey privateKey, X509Certificate cert, VlsiSettings lsSettings, ResourceFactory lsFactory, ResourceFactory adminFactory, ScheduledExecutorService scheduler, SamlToken initialToken) {
      super(privateKey, cert, lsSettings, lsFactory, adminFactory);
      this.running = true;
      this.scheduler = scheduler;
      this.cachedToken = initialToken;
      this.scheduleRenewal(this.renewIn());
   }

   public RenewingTokenRetriever(PrivateKey privateKey, X509Certificate cert, VlsiSettings lsSettings, ResourceFactory lsFactory, ResourceFactory adminFactory, ScheduledExecutorService scheduler, String username, String password, TokenSpec tokenSpec) {
      this(privateKey, cert, lsSettings, lsFactory, adminFactory, scheduler, acquireToken(privateKey, cert, lsSettings, lsFactory, adminFactory, username, password, tokenSpec));
   }

   protected static SamlToken acquireToken(PrivateKey privateKey, X509Certificate cert, VlsiSettings lsSettings, ResourceFactory lsFactory, ResourceFactory adminFactory, String username, String password, TokenSpec tokenSpec) {
      ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();

      SamlToken var12;
      try {
         LookupSvcConnection conn = (LookupSvcConnection)lsFactory.acquire(lsSettings);
         Throwable var10 = null;

         try {
            Thread.currentThread().setContextClassLoader(RenewingTokenRetriever.class.getClassLoader());
            SamlToken samlToken = getSts(privateKey, cert, conn, adminFactory, lsSettings).getStsClient().acquireToken(username, password, tokenSpec);
            logger.debug("Successfully acquired token for: {} @ {}", samlToken.getSubject(), conn);
            var12 = samlToken;
         } catch (Throwable var32) {
            var10 = var32;
            throw var32;
         } finally {
            if (conn != null) {
               if (var10 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var31) {
                     var10.addSuppressed(var31);
                  }
               } else {
                  conn.close();
               }
            }

         }
      } catch (AuthenticationFailedException var34) {
         throw new IllegalAuthInfoException("Invalid authentication details.", var34);
      } catch (ServerCommunicationException | ServerSecurityException | RequestExpiredException | TimeSynchronizationException | InvalidTokenException | TokenRequestRejectedException | CertificateValidationException | InternalError var35) {
         throw new TokenAcquisitionException("Failed to acquire token for: " + username, var35);
      } finally {
         Thread.currentThread().setContextClassLoader(oldClassLoader);
      }

      return var12;
   }

   protected void scheduleRenewal(long renewIn) {
      if (!this.cachedToken.isRenewable()) {
         logger.warn("Not a renewable token: " + this.cachedToken.getSubject());
      } else if (this.cachedToken.getConfirmationType() != ConfirmationType.HOLDER_OF_KEY) {
         logger.warn("Non-HoK token cannot be renewed: " + this.cachedToken.getSubject());
      } else if (!this.running) {
         logger.debug("Retriever has been shut down, stopping renewal of token: {}", this.cachedToken.getSubject());
      } else {
         logger.debug("Scheduling token renewal for {} at {}.", this.cachedToken.getSubject(), new Date(System.currentTimeMillis() + renewIn));
         this.scheduler.schedule(new Runnable() {
            public void run() {
               if (RenewingTokenRetriever.this.running) {
                  try {
                     LookupSvcConnection conn = (LookupSvcConnection)RenewingTokenRetriever.this.lsFactory.acquire(RenewingTokenRetriever.this.lsSettings);
                     Throwable var2 = null;

                     try {
                        StsService sts = AbstractTokenRetriever.getSts(RenewingTokenRetriever.this.privateKey, RenewingTokenRetriever.this.certificate, conn, RenewingTokenRetriever.this.adminFactory, RenewingTokenRetriever.this.lsSettings);
                        RenewingTokenRetriever.this.cachedToken = sts.getStsClient().renewToken(RenewingTokenRetriever.this.cachedToken, 86400L);
                     } catch (Throwable var12) {
                        var2 = var12;
                        throw var12;
                     } finally {
                        if (conn != null) {
                           if (var2 != null) {
                              try {
                                 conn.close();
                              } catch (Throwable var11) {
                                 var2.addSuppressed(var11);
                              }
                           } else {
                              conn.close();
                           }
                        }

                     }

                     if (RenewingTokenRetriever.logger.isInfoEnabled()) {
                        RenewingTokenRetriever.logger.debug("Successfully renewed token for {}.", RenewingTokenRetriever.this.cachedToken.getSubject());
                     }

                     RenewingTokenRetriever.this.scheduleRenewal(RenewingTokenRetriever.this.renewIn());
                  } catch (Exception var14) {
                     RenewingTokenRetriever.logger.warn("SAML Token Renewal Failure for {}", RenewingTokenRetriever.this.cachedToken.getSubject(), var14);
                     RenewingTokenRetriever.this.scheduleRenewal(30000L);
                  }

               }
            }
         }, renewIn, TimeUnit.MILLISECONDS);
      }
   }

   protected long renewIn() {
      long renewIn = this.cachedToken.getExpirationTime().getTime() - System.currentTimeMillis() - 3600000L;
      if (renewIn < 0L) {
         renewIn = 0L;
      }

      return renewIn;
   }

   public TokenInfo retrieveToken() {
      Date expiration = this.cachedToken.getExpirationTime();
      if (expiration.before(new Date())) {
         throw new TokenExpiredException("The token has expired: " + expiration);
      } else {
         return new TokenInfo(this.privateKey, this.cachedToken);
      }
   }

   public void shutdown() {
      logger.debug("Shutting down token retriever: {}", this);
      this.running = false;
   }

   public String toString() {
      return "RenewingTokenRetriever{cachedToken=" + this.cachedToken.getSubject() + "}";
   }
}
