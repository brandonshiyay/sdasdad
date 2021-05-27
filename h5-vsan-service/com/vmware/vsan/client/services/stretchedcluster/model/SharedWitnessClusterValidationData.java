package com.vmware.vsan.client.services.stretchedcluster.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vmodl.KeyAnyValue;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.vsan.EntityCompatibilityResult;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TsModel
public class SharedWitnessClusterValidationData {
   private static final Logger logger = LoggerFactory.getLogger(SharedWitnessClusterValidationData.class);
   private static final String COMPONENTS_COUNT_KEY = "componentCount";
   private static final String COMPONENTS_LIMIT_KEY = "limit";
   public ManagedObjectReference clusterRef;
   public boolean compatible;
   public String validationMessage;
   public Integer componentsCount;
   public Integer componentsLimit;

   public SharedWitnessClusterValidationData() {
   }

   public SharedWitnessClusterValidationData(ManagedObjectReference clusterRef) {
      this.clusterRef = clusterRef;
      this.compatible = true;
   }

   public static Map getClustersResult(EntityCompatibilityResult[] compatibilityResults) {
      if (ArrayUtils.isEmpty(compatibilityResults)) {
         logger.error("No clusters validation result is returned.");
         throw new VsanUiLocalizableException("vsan.sharedWitness.validation.general");
      } else {
         Map result = (Map)Arrays.asList(compatibilityResults).stream().map((entity) -> {
            return getClusterResult(entity);
         }).collect(Collectors.toMap((c) -> {
            return c.clusterRef.getValue();
         }, (c) -> {
            return c;
         }));
         return result;
      }
   }

   private static SharedWitnessClusterValidationData getClusterResult(EntityCompatibilityResult vmodl) {
      if (vmodl == null) {
         logger.error("No clusters validation result is returned");
         throw new VsanUiLocalizableException("vsan.sharedWitness.validation.general");
      } else {
         SharedWitnessClusterValidationData result = new SharedWitnessClusterValidationData();
         result.clusterRef = vmodl.entity;
         result.compatible = vmodl.compatible;
         if (BooleanUtils.isTrue(vmodl.compatible)) {
            addComponetsInfo(result, vmodl.extendedAttributes);
         }

         if (ArrayUtils.isNotEmpty(vmodl.incompatibleReasons)) {
            result.validationMessage = vmodl.incompatibleReasons[0].getMessage();
         }

         return result;
      }
   }

   private static void addComponetsInfo(SharedWitnessClusterValidationData clusterValidationData, KeyAnyValue[] extendedAttributes) {
      if (ArrayUtils.isEmpty(extendedAttributes)) {
         logger.warn("Cluster passing shared witness compatibility has no values for components' count and limit", clusterValidationData.clusterRef);
      } else {
         Arrays.stream(extendedAttributes).forEach((attribute) -> {
            String var2 = attribute.getKey();
            byte var3 = -1;
            switch(var2.hashCode()) {
            case 102976443:
               if (var2.equals("limit")) {
                  var3 = 1;
               }
               break;
            case 1344330258:
               if (var2.equals("componentCount")) {
                  var3 = 0;
               }
            }

            switch(var3) {
            case 0:
               clusterValidationData.componentsCount = (Integer)attribute.getValue();
               break;
            case 1:
               clusterValidationData.componentsLimit = (Integer)attribute.getValue();
            }

         });
         if (clusterValidationData.componentsCount == null) {
            logger.warn("Cluster passing shared witness compatibility has no values for components' count", clusterValidationData.clusterRef);
            clusterValidationData.componentsCount = 0;
         }

         if (clusterValidationData.componentsLimit == null) {
            logger.warn("Cluster passing shared witness compatibility has no values for components' limit", clusterValidationData.clusterRef);
            clusterValidationData.componentsLimit = 0;
         }

      }
   }

   public String toString() {
      return "SharedWitnessClusterValidationData{clusterRef=" + this.clusterRef + ", compatible=" + this.compatible + ", validationMessage='" + this.validationMessage + '\'' + ", componentsCount=" + this.componentsCount + ", componentsLimit=" + this.componentsLimit + '}';
   }
}
