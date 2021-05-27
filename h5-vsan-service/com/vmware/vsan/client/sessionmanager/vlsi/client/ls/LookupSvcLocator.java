package com.vmware.vsan.client.sessionmanager.vlsi.client.ls;

import java.security.KeyStore;
import java.security.PrivateKey;

public interface LookupSvcLocator {
   String H5_ALIAS = "vsphere-webclient";

   LookupSvcInfo getInfo();

   KeyStore getH5Keystore();

   PrivateKey getPrivateKey();

   String getNodeId();
}
