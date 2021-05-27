package com.vmware.vsan.client.sessionmanager.vlsi.util;

import com.vmware.vsan.client.util.config.ConfigurationUtil;
import com.vmware.vsan.client.util.fss.FeatureStateUtil;
import java.net.URI;
import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class EnvoySidecarUtils {
   private static final Log logger = LogFactory.getLog(EnvoySidecarUtils.class);

   public static URI convertLocalEndpoint(URI url) {
      return convertEndpoint(url, true);
   }

   public static URI convertRemoteEndpoint(URI url) {
      return convertEndpoint(url, false);
   }

   private static URI convertEndpoint(URI url, boolean isLocal) {
      Validate.notNull(url);

      try {
         if (!isSidecarSupported()) {
            logger.debug("Envoy Sidecar is not supported.");
            return url;
         } else if (isSidecarUrl(url)) {
            logger.debug("The URL already points to Envoy: " + url);
            return url;
         } else {
            SidecarUrlFactory factory = new SidecarUrlFactory(url);
            URI sidecarUrl = isLocal ? factory.createLocalUrl() : factory.createRemoteUrl();
            logger.debug("Converted successfully to Envoy Sidecar url: " + url + " -> " + sidecarUrl);
            return sidecarUrl;
         }
      } catch (Exception var4) {
         logger.error("URL conversion failed! Fallback to original URL: " + url, var4);
         return url;
      }
   }

   private static boolean isSidecarSupported() {
      return !ConfigurationUtil.isFling() && !ConfigurationUtil.isLocalDevEnv() && FeatureStateUtil.isEnvoySidecarEnabled();
   }

   private static boolean isSidecarUrl(URI url) {
      return SidecarConfig.SCHEME.equalsIgnoreCase(url.getScheme()) && "localhost".equalsIgnoreCase(url.getHost()) && 1080 == url.getPort();
   }
}
