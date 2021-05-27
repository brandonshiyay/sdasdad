package com.vmware.vsan.client.services.stretchedcluster.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.vsan.binding.vim.vsan.EntityCompatibilityResult;
import com.vmware.vim.vsan.binding.vim.vsan.SharedWitnessCompatibilityResult;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

@TsModel
public class SharedWitnessValidationData {
   private static final Logger logger = LoggerFactory.getLogger(SharedWitnessValidationData.class);
   public List witnessHostValidationErrors;
   public Map clustersValidation;

   public static SharedWitnessValidationData fromVmodl(SharedWitnessCompatibilityResult vmodl) {
      if (vmodl == null) {
         logger.error("No validation result is returned");
         throw new VsanUiLocalizableException("vsan.sharedWitness.validation.general");
      } else {
         SharedWitnessValidationData result = new SharedWitnessValidationData();
         result.witnessHostValidationErrors = getHostResult(vmodl.witnessHostCompatibility);
         if (CollectionUtils.isEmpty(result.witnessHostValidationErrors)) {
            result.clustersValidation = SharedWitnessClusterValidationData.getClustersResult(vmodl.roboClusterCompatibility);
         }

         return result;
      }
   }

   public boolean isValidationSuccessful() {
      if (!CollectionUtils.isEmpty(this.witnessHostValidationErrors)) {
         return false;
      } else if (!CollectionUtils.isEmpty(this.clustersValidation)) {
         return this.clustersValidation.values().stream().filter((validationData) -> {
            return !validationData.compatible;
         }).count() == 0L;
      } else {
         return true;
      }
   }

   private static List getHostResult(EntityCompatibilityResult vmodl) {
      if (vmodl != null && !vmodl.compatible && !ArrayUtils.isEmpty(vmodl.incompatibleReasons)) {
         List incompatibleReasons = (List)Arrays.asList(vmodl.incompatibleReasons).stream().map((reason) -> {
            return reason.getMessage();
         }).collect(Collectors.toList());
         return incompatibleReasons;
      } else {
         return null;
      }
   }

   public String toString() {
      return "SharedWitnessValidationData{witnessHostValidationErrors=" + this.witnessHostValidationErrors + ", clustersValidation=" + this.clustersValidation + '}';
   }
}
