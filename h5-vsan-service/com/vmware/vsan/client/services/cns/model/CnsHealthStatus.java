package com.vmware.vsan.client.services.cns.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsphere.client.vsan.util.EnumUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TsModel
public enum CnsHealthStatus {
   red,
   yellow,
   green,
   unknown;

   private static final Logger logger = LoggerFactory.getLogger(CnsHealthStatus.class);

   public static CnsHealthStatus fromName(String value) {
      return (CnsHealthStatus)EnumUtils.fromString(CnsHealthStatus.class, value);
   }

   public static CnsHealthStatus fromDatastoreAccessibility(CnsDatastoreAccessibilityStatus value) {
      switch(value) {
      case accessible:
         return green;
      case notAccessible:
         return red;
      case partiallyAccessible:
         return yellow;
      default:
         return unknown;
      }
   }
}
