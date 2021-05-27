package com.vmware.vsan.client.sessionmanager.vlsi.client.ls;

import com.vmware.vim.binding.lookup.ServiceContent;
import com.vmware.vim.binding.lookup.ServiceRegistration;
import com.vmware.vim.binding.lookup.ServiceEndpoint.EndpointProtocol;
import com.vmware.vim.binding.lookup.ServiceRegistration.Endpoint;
import com.vmware.vim.binding.lookup.ServiceRegistration.EndpointType;
import com.vmware.vim.binding.lookup.ServiceRegistration.Filter;
import com.vmware.vim.binding.lookup.ServiceRegistration.Info;
import com.vmware.vim.binding.lookup.ServiceRegistration.ServiceType;
import com.vmware.vim.sso.client.util.codec.Base64;
import com.vmware.vsan.client.sessionmanager.vlsi.client.VlsiConnection;
import com.vmware.vsan.client.sessionmanager.vlsi.client.sso.ServiceEndpoint;
import com.vmware.vsan.client.sessionmanager.vlsi.client.sso.SsoEndpoints;
import com.vmware.vsan.client.sessionmanager.vlsi.client.sso.SsoException;
import com.vmware.vsan.client.sessionmanager.vlsi.util.ServiceRegistrationUtils;
import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class LookupSvcConnection extends VlsiConnection {
   private static final String SSO_SERVICE_PRODUCT = "com.vmware.cis";
   private static final String SSO_SERVICE_TYPE = "cs.identity";
   private static final String SSO_ADMIN_ENDPOINT_TYPE = "com.vmware.cis.cs.identity.admin";
   private static final String SSO_STS_ENDPOINT_TYPE = "com.vmware.cis.cs.identity.sso";
   private final Log logger = LogFactory.getLog(this.getClass());
   protected volatile ServiceContent content;
   private static final Filter URN_SSO_STS;
   private static final Filter URN_SSO_ADMIN;
   protected static CertificateFactory cf;

   public ServiceRegistration getServiceRegistration() {
      return (ServiceRegistration)this.createStub(ServiceRegistration.class, this.content.getServiceRegistration());
   }

   public ServiceContent getContent() {
      return this.content;
   }

   protected static Filter mkSearchFilter(String serviceProduct, String serviceType, EndpointProtocol endpointProtocol, String endpointType) {
      Filter filter = new Filter();
      ServiceType service = new ServiceType(serviceProduct, serviceType);
      filter.setServiceType(service);
      EndpointType endpoint = new EndpointType(endpointProtocol.toString(), endpointType);
      filter.setEndpointType(endpoint);
      filter.searchAllSsoDomains = true;
      return filter;
   }

   protected static CertificateFactory getCertFactory() {
      try {
         return CertificateFactory.getInstance("X.509");
      } catch (CertificateException var1) {
         throw new SsoException(var1);
      }
   }

   public SsoEndpoints getSsoEndpoints() {
      return new SsoEndpoints(this.getSts(), this.getAdmin(), (ServiceEndpoint)null);
   }

   public ServiceEndpoint getSts() {
      return this.retrieveEndpoint(URN_SSO_STS);
   }

   public ServiceEndpoint getAdmin() {
      return this.retrieveEndpoint(URN_SSO_ADMIN);
   }

   public String getDomainId(String nodeId) {
      Info info = this.getServiceInfo(URN_SSO_STS, nodeId);
      if (info == null) {
         this.logger.warn("No service info found for: " + nodeId);
         return null;
      } else {
         return ServiceRegistrationUtils.getDomainId(info);
      }
   }

   public ServiceEndpoint retrieveEndpoint(Filter filter) {
      Info service = (Info)this.getServiceInfos(filter).get(0);
      return this.fromInfo(service, (String)null, (String)null);
   }

   private List getServiceInfos(Filter filter) {
      List services = this.findRawServices(filter);
      if (services.isEmpty()) {
         throw new RuntimeException("Service not found: " + filter);
      } else {
         return services;
      }
   }

   private Info getServiceInfo(Filter filter, String nodeId) {
      List services = this.getServiceInfos(filter);
      return (Info)services.stream().filter((info) -> {
         return nodeId.equalsIgnoreCase(info.getNodeId());
      }).findAny().orElse((Object)null);
   }

   public ServiceEndpoint fromInfo(Info service, String endpointProtocol, String endpointType) {
      Endpoint[] endpoints = service.getServiceEndpoints();
      if (endpoints != null && endpoints.length >= 1) {
         Endpoint point = null;
         if (endpointProtocol == null && endpointType == null) {
            point = endpoints[0];
         } else {
            Endpoint[] var6 = endpoints;
            int var7 = endpoints.length;

            for(int var8 = 0; var8 < var7; ++var8) {
               Endpoint endpoint = var6[var8];
               if ((endpointProtocol == null || endpointProtocol.equals(endpoint.getEndpointType().getProtocol())) && (endpointType == null || endpointType.equals(endpoint.getEndpointType().getType()))) {
                  point = endpoint;
                  break;
               }
            }
         }

         if (point == null) {
            throw new RuntimeException("Cannot find endpoint for protocol " + endpointProtocol + " and/or type " + endpointType + " in service " + service.getServiceId());
         } else {
            X509Certificate[] certs;
            try {
               certs = point.getSslTrust() == null ? new X509Certificate[0] : getCerts(point.getSslTrust());
            } catch (CertificateException var10) {
               throw new RuntimeException("Could not retrieve endpoint trust anchor", var10);
            }

            return new ServiceEndpoint(point.getUrl(), certs);
         }
      } else {
         throw new RuntimeException("Service has zero endpoints: " + service);
      }
   }

   public List findRawServices(Filter filter) {
      try {
         Info[] services = this.getServiceRegistration().list(filter);
         return services == null ? Collections.emptyList() : Arrays.asList(services);
      } catch (RuntimeException var4) {
         throw var4;
      } catch (Exception var5) {
         throw new RuntimeException("ServiceRegistration search failure", var5);
      }
   }

   public static X509Certificate fromBase64(String pem) throws CertificateException {
      return (X509Certificate)cf.generateCertificate(new ByteArrayInputStream(Base64.decodeBase64(pem.getBytes(Charset.forName("ASCII")))));
   }

   public static X509Certificate[] getCerts(String[] certs) throws CertificateException {
      List result = new ArrayList();
      String[] var2 = certs;
      int var3 = certs.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         String pem = var2[var4];
         result.add(fromBase64(pem));
      }

      return (X509Certificate[])result.toArray(new X509Certificate[result.size()]);
   }

   static {
      URN_SSO_STS = mkSearchFilter("com.vmware.cis", "cs.identity", EndpointProtocol.wsTrust, "com.vmware.cis.cs.identity.sso");
      URN_SSO_ADMIN = mkSearchFilter("com.vmware.cis", "cs.identity", EndpointProtocol.vmomi, "com.vmware.cis.cs.identity.admin");
      cf = getCertFactory();
   }
}
