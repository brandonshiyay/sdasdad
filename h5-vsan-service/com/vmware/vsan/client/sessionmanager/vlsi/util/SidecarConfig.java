package com.vmware.vsan.client.sessionmanager.vlsi.util;

final class SidecarConfig {
   public static final String SCHEME;
   public static final String HOSTNAME = "localhost";
   public static final int PORT = 1080;

   private SidecarConfig() {
   }

   static {
      SCHEME = Protocol.HTTP.value;
   }
}
