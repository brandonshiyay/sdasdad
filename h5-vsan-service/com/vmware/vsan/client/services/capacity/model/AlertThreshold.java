package com.vmware.vsan.client.services.capacity.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.vsan.binding.vim.vsan.VsanHealthThreshold;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsphere.client.vsan.base.util.BaseUtils;
import org.apache.commons.lang3.BooleanUtils;

@TsModel
public class AlertThreshold {
   private static final String VSAN_DATASTORES_TARGET = "diskspace_vsan_datastore";
   private static final String VSAN_DIRECT_DATASTORES_TARGET = "diskspace_vsan_direct";
   private static final String PMEM_DATASTORES_TARGET = "diskspace_vsan_pmem";
   public double warningThreshold;
   public double errorThreshold;
   public DatastoreType datastoreType;
   public Boolean isEnabled;
   public Boolean isValid;
   public Boolean isDefault;

   public AlertThreshold() {
   }

   public AlertThreshold(double warningThreshold, double errorThreshold, DatastoreType datastoreType, Boolean isEnabled) {
      this.warningThreshold = BaseUtils.round(warningThreshold, 2);
      this.errorThreshold = BaseUtils.round(errorThreshold, 2);
      this.datastoreType = datastoreType;
      this.isEnabled = isEnabled;
   }

   public AlertThreshold(AlertThreshold copyThreshold, boolean roundValues) {
      this.warningThreshold = roundValues ? (double)Math.round(copyThreshold.warningThreshold) : copyThreshold.warningThreshold;
      this.errorThreshold = roundValues ? (double)Math.round(copyThreshold.errorThreshold) : copyThreshold.errorThreshold;
      this.datastoreType = copyThreshold.datastoreType;
      this.isEnabled = copyThreshold.isEnabled;
      this.isValid = copyThreshold.isValid;
      this.isDefault = copyThreshold.isDefault;
   }

   public static VsanHealthThreshold toVmodl(AlertThreshold alertThresholdModel) {
      return new VsanHealthThreshold((long)((int)alertThresholdModel.warningThreshold), (long)((int)alertThresholdModel.errorThreshold), fromDatastoreEnum(alertThresholdModel.datastoreType), BooleanUtils.isTrue(alertThresholdModel.isEnabled));
   }

   public static AlertThreshold fromVmodlInBytes(VsanHealthThreshold healthThreshold, long availableSpace) {
      double errorThreshold = BaseUtils.toPercentage((double)healthThreshold.redValue, (double)availableSpace);
      double warningThreshold = BaseUtils.toPercentage((double)healthThreshold.yellowValue, (double)availableSpace);
      return new AlertThreshold(warningThreshold, errorThreshold, toDatastoreEnum(healthThreshold.target), BooleanUtils.isTrue(healthThreshold.enabled));
   }

   public static AlertThreshold fromVmodlInPercentage(VsanHealthThreshold healthThreshold) {
      return new AlertThreshold((double)healthThreshold.yellowValue, (double)healthThreshold.redValue, toDatastoreEnum(healthThreshold.target), BooleanUtils.isTrue(healthThreshold.enabled));
   }

   public static String fromDatastoreEnum(DatastoreType datastoreType) {
      if (datastoreType == null) {
         return null;
      } else if (datastoreType == DatastoreType.VSAN) {
         return "diskspace_vsan_datastore";
      } else if (datastoreType == DatastoreType.VMFS) {
         return "diskspace_vsan_direct";
      } else if (datastoreType == DatastoreType.PMEM) {
         return "diskspace_vsan_pmem";
      } else {
         throw new VsanUiLocalizableException("vsan.customizable.threshold.target.error", new String[]{datastoreType.getKey()});
      }
   }

   public static DatastoreType toDatastoreEnum(String target) {
      if (target == null) {
         return null;
      } else if (target.equalsIgnoreCase("diskspace_vsan_datastore")) {
         return DatastoreType.VSAN;
      } else if (target.equalsIgnoreCase("diskspace_vsan_direct")) {
         return DatastoreType.VMFS;
      } else if (target.equalsIgnoreCase("diskspace_vsan_pmem")) {
         return DatastoreType.PMEM;
      } else {
         throw new VsanUiLocalizableException("vsan.customizable.threshold.target.error", new String[]{target});
      }
   }
}
