package com.vmware.vsan.client.sessionmanager.vlsi.util;

import com.vmware.vsphere.client.vsan.util.EnumUtils;
import java.net.URI;
import java.net.URISyntaxException;

class SidecarUrlFactory {
   private static final String EXTERNAL_VECS_HTTP_URL_FORMAMT = "/external-vecs/http1/%s/%d%s";
   public final String scheme;
   public final String hostname;
   public final int port;
   public final String path;
   public final String query;

   public SidecarUrlFactory(URI url) {
      Protocol protocol = (Protocol)EnumUtils.fromStringIgnoreCase(Protocol.class, url.getScheme());
      if (protocol == null) {
         throw new IllegalArgumentException("Unsupported protocol: " + url.toString());
      } else {
         this.scheme = protocol.value;
         this.hostname = url.getHost();
         this.port = this.extractPort(url, protocol);
         this.path = url.getPath();
         this.query = url.getRawQuery();
      }
   }

   private int extractPort(URI url, Protocol protocol) {
      int port = url.getPort();
      if (port == -1) {
         port = protocol.defaultPort;
      }

      return port;
   }

   public URI createLocalUrl() throws URISyntaxException {
      return new URI(SidecarConfig.SCHEME, (String)null, "localhost", 1080, this.path, this.query, (String)null);
   }

   public URI createRemoteUrl() throws URISyntaxException {
      String remotePath = String.format("/external-vecs/http1/%s/%d%s", this.hostname, this.port, this.path);
      return new URI(SidecarConfig.SCHEME, (String)null, "localhost", 1080, remotePath, this.query, (String)null);
   }
}
