package com.vmware.vsan.client.services.capacity;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanWhatifCapacityHealthThreshold;
import com.vmware.vim.vsan.binding.vim.vsan.ConfigInfoEx;
import com.vmware.vim.vsan.binding.vim.vsan.VsanHealthConfigSpec;
import com.vmware.vim.vsan.binding.vim.vsan.VsanHealthThreshold;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.advancedoptions.AdvancedOptionsInfo;
import com.vmware.vsan.client.services.advancedoptions.AdvancedOptionsService;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.services.capacity.model.AlertThreshold;
import com.vmware.vsan.client.services.capacity.model.CapacityData;
import com.vmware.vsan.client.services.capacity.model.CapacityManagementData;
import com.vmware.vsan.client.services.capacity.model.CapacityReservationOption;
import com.vmware.vsan.client.services.capacity.model.DatastoreType;
import com.vmware.vsan.client.services.config.CapacityReservationConfig;
import com.vmware.vsan.client.services.config.ConfigInfoService;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsphere.client.vsan.base.util.BaseUtils;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CapacityManagementService {
   @Autowired
   CapacityDataService capacityDataService;
   @Autowired
   ConfigInfoService configInfoService;
   @Autowired
   CapacityHealthThresholdService thresholdService;
   @Autowired
   AdvancedOptionsService advancedOptionsService;
   private final Log logger = LogFactory.getLog(this.getClass());

   @TsService
   public CapacityManagementData getCapacityManagementData(ManagedObjectReference objectReference) {
      ManagedObjectReference clusterRef = BaseUtils.getCluster(objectReference);
      boolean isCapacityCustomizableThresholdsSupportedOnVc = VsanCapabilityUtils.isCapacityCustomizableThresholdsSupportedOnVc(clusterRef);
      if (!VsanCapabilityUtils.isSlackSpaceReservationSupported(clusterRef) && !isCapacityCustomizableThresholdsSupportedOnVc) {
         return null;
      } else {
         Map customizableAlertThresholds = null;

         Map allDatastoresCapacityData;
         AdvancedOptionsInfo advancedOptionsInfo;
         VsanWhatifCapacityHealthThreshold[] capacityThresholdsWhatifData;
         try {
            Measure measure = new Measure("Get Capacity reservation data");
            Throwable var9 = null;

            try {
               CompletableFuture configInfo = this.configInfoService.getVsanConfigInfoAsync(clusterRef);
               CompletableFuture capacityThresholds = this.thresholdService.getWhatIfCapacityThreshold(clusterRef);
               allDatastoresCapacityData = this.capacityDataService.getVsanSpaceUsage(clusterRef, !isCapacityCustomizableThresholdsSupportedOnVc);
               ConfigInfoEx configInfoEx = (ConfigInfoEx)configInfo.get();
               advancedOptionsInfo = AdvancedOptionsInfo.fromVmodl(configInfoEx, clusterRef);
               if (isCapacityCustomizableThresholdsSupportedOnVc) {
                  customizableAlertThresholds = this.getCustomizableAlertThresholds(configInfoEx, allDatastoresCapacityData);
               }

               capacityThresholdsWhatifData = (VsanWhatifCapacityHealthThreshold[])capacityThresholds.get();
            } catch (Throwable var21) {
               var9 = var21;
               throw var21;
            } finally {
               if (measure != null) {
                  if (var9 != null) {
                     try {
                        measure.close();
                     } catch (Throwable var20) {
                        var9.addSuppressed(var20);
                     }
                  } else {
                     measure.close();
                  }
               }

            }
         } catch (Exception var23) {
            this.logger.error("Unable to get capacity reservation data. Failed to extract the vSAN cluster's configuration.");
            throw new VsanUiLocalizableException(var23);
         }

         Map whatIfVSANThresholds = aggregateVsanWhatIfThresholdData((CapacityData)allDatastoresCapacityData.get(DatastoreType.VSAN), capacityThresholdsWhatifData);
         return new CapacityManagementData(advancedOptionsInfo.capacityReservationConfig, allDatastoresCapacityData, customizableAlertThresholds, whatIfVSANThresholds);
      }
   }

   private Map getCustomizableAlertThresholds(ConfigInfoEx configInfoEx, Map spaceUsageByDatastore) {
      Map alertThresholds = new HashMap();
      if (configInfoEx.vsanHealthConfig == null) {
         configInfoEx.vsanHealthConfig = new VsanHealthConfigSpec();
      }

      Supplier healthThresholds = () -> {
         return configInfoEx.vsanHealthConfig.healthCheckThresholdSpec == null ? Arrays.stream(new VsanHealthThreshold[0]) : Arrays.stream(configInfoEx.vsanHealthConfig.healthCheckThresholdSpec);
      };
      Iterator var5 = spaceUsageByDatastore.keySet().iterator();

      while(var5.hasNext()) {
         DatastoreType datastoreType = (DatastoreType)var5.next();
         Optional customizedThreshold = ((Stream)healthThresholds.get()).filter((threshold) -> {
            return AlertThreshold.toDatastoreEnum(threshold.target) == datastoreType;
         }).findFirst();
         alertThresholds.put(datastoreType, this.getCustomizableAlertThreshold(datastoreType, customizedThreshold, spaceUsageByDatastore));
      }

      return alertThresholds;
   }

   @TsService
   public AlertThreshold getCustomizableAlertThreshold(DatastoreType datastoreType, Optional storedThreshold, Map spaceUsageByDatastore) {
      CapacityData capacityData = (CapacityData)spaceUsageByDatastore.get(datastoreType);
      if (capacityData.totalSpace == 0L) {
         return new AlertThreshold(0.0D, 0.0D, datastoreType, false);
      } else {
         AlertThreshold alertThreshold = (AlertThreshold)storedThreshold.map((healthThreshold) -> {
            AlertThreshold customizedThreshold = AlertThreshold.fromVmodlInPercentage(healthThreshold);
            customizedThreshold.isDefault = false;
            return customizedThreshold;
         }).orElseGet(() -> {
            return new AlertThreshold(capacityData.thresholds, true);
         });
         return alertThreshold;
      }
   }

   private static Map aggregateVsanWhatIfThresholdData(CapacityData vsanCapacityData, VsanWhatifCapacityHealthThreshold[] capacityThresholdsWhatifData) {
      if (ArrayUtils.isEmpty(capacityThresholdsWhatifData)) {
         return null;
      } else {
         Map result = new HashMap();
         VsanWhatifCapacityHealthThreshold[] var3 = capacityThresholdsWhatifData;
         int var4 = capacityThresholdsWhatifData.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            VsanWhatifCapacityHealthThreshold capacityThresholdsData = var3[var5];
            long availableSpace = VsanCapacityBreakdownCalculator.getAvailableSpace(vsanCapacityData, CapacityReservationConfig.fromVmodl(capacityThresholdsData.capacityReservationInfo));
            result.put(CapacityReservationOption.fromVmodl(capacityThresholdsData.capacityReservationInfo), AlertThreshold.fromVmodlInBytes(capacityThresholdsData.whatifCapacityHealthThreshold, availableSpace));
         }

         return result;
      }
   }

   @TsService
   public ManagedObjectReference configureCapacityManagement(ManagedObjectReference moRef, CapacityReservationConfig reservationConfig, AlertThreshold[] alertThresholds) {
      ManagedObjectReference clusterRef = BaseUtils.getCluster(moRef);
      AdvancedOptionsInfo existingOptions = null;
      if (reservationConfig != null) {
         existingOptions = this.advancedOptionsService.getAdvancedOptionsInfo(clusterRef);
         existingOptions.capacityReservationConfig = reservationConfig;
      }

      return VsanCapabilityUtils.isCapacityCustomizableThresholdsSupportedOnVc(clusterRef) ? this.advancedOptionsService.configureAdvancedOptionsExtended(clusterRef, existingOptions, Arrays.asList(alertThresholds)) : this.advancedOptionsService.configureAdvancedOptions(clusterRef, existingOptions);
   }
}
