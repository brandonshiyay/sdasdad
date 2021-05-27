package com.vmware.vsan.client.sessionmanager.vlsi.util;

import com.vmware.vsphere.client.vsan.util.EnumWithKey;

enum Protocol implements EnumWithKey {
   HTTP("http", 80),
   HTTPS("https", 443);

   public final String value;
   public final int defaultPort;

   private Protocol(String value, int defaultPort) {
      this.value = value;
      this.defaultPort = defaultPort;
   }

   public String getKey() {
      return this.value;
   }
}
