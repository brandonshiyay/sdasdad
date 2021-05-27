package com.vmware.vsan.client.services.stretchedcluster.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;

@TsModel
public class SharedWitnessLimits {
   public ManagedObjectReference hostRef;
   public Integer maxComponentsPerCluster;
   public Integer maxWitnessClusters;

   public SharedWitnessLimits() {
   }

   public SharedWitnessLimits(ManagedObjectReference hostRef, Integer maxComponentsPerCluster, Integer maxWitnessClusters) {
      this.hostRef = hostRef;
      this.maxComponentsPerCluster = maxComponentsPerCluster;
      this.maxWitnessClusters = maxWitnessClusters;
   }

   public String toString() {
      return "SharedWitnessLimits{hostRef=" + this.hostRef + ", maxComponentsPerCluster=" + this.maxComponentsPerCluster + ", maxWitnessClusters=" + this.maxWitnessClusters + '}';
   }
}
