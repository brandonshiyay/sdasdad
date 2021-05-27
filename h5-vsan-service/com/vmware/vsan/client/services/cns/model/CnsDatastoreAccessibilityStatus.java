package com.vmware.vsan.client.services.cns.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsphere.client.vsan.util.EnumUtils;

@TsModel
public enum CnsDatastoreAccessibilityStatus {
   accessible,
   notAccessible,
   notApplicable,
   partiallyAccessible;

   public static CnsDatastoreAccessibilityStatus fromName(String value) {
      return (CnsDatastoreAccessibilityStatus)EnumUtils.fromString(CnsDatastoreAccessibilityStatus.class, value);
   }

   public static CnsDatastoreAccessibilityStatus fromHealthStatus(CnsHealthStatus status) {
      switch(status) {
      case red:
         return notAccessible;
      case green:
         return accessible;
      case yellow:
         return partiallyAccessible;
      case unknown:
      default:
         return null;
      }
   }
}
