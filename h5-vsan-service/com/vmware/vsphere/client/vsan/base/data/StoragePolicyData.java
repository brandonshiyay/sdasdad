package com.vmware.vsphere.client.vsan.base.data;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.pbm.capability.CapabilityInstance;
import com.vmware.vim.binding.pbm.capability.ConstraintInstance;
import com.vmware.vim.binding.pbm.capability.PropertyInstance;
import com.vmware.vim.binding.pbm.profile.CapabilityBasedProfile;
import com.vmware.vim.binding.pbm.profile.Profile;
import com.vmware.vim.binding.pbm.profile.SubProfileCapabilityConstraints;
import com.vmware.vim.binding.pbm.profile.SubProfileCapabilityConstraints.SubProfile;
import org.apache.commons.lang3.ArrayUtils;

@TsModel
public class StoragePolicyData {
   private static final String LOCALITY = "locality";
   private static final String HOST_FAILURES_TO_TOLERATE = "hostFailuresToTolerate";
   public String id;
   public String name;
   public boolean isDefault;
   public boolean hasVsanNamespace;
   public boolean isVmCrypt;
   public Integer ftt;
   public String locality;
   public boolean isCompatible;

   public StoragePolicyData() {
   }

   public StoragePolicyData(Profile profile) {
      this.id = profile.getProfileId().getUniqueId();
      this.name = profile.getName();
      if (profile instanceof CapabilityBasedProfile) {
         CapabilityBasedProfile capabilityBasedProfile = (CapabilityBasedProfile)profile;
         if (capabilityBasedProfile.constraints instanceof SubProfileCapabilityConstraints) {
            SubProfileCapabilityConstraints constraints = (SubProfileCapabilityConstraints)capabilityBasedProfile.constraints;
            if (ArrayUtils.isEmpty(constraints.subProfiles)) {
               return;
            }

            SubProfile[] var4 = constraints.subProfiles;
            int var5 = var4.length;

            for(int var6 = 0; var6 < var5; ++var6) {
               SubProfile subProfile = var4[var6];
               if (ArrayUtils.isEmpty(subProfile.capability)) {
                  return;
               }

               CapabilityInstance[] var8 = subProfile.capability;
               int var9 = var8.length;

               for(int var10 = 0; var10 < var9; ++var10) {
                  CapabilityInstance capability = var8[var10];
                  if ("VSAN".equals(capability.id.namespace)) {
                     this.hasVsanNamespace = true;
                  }

                  ConstraintInstance[] var12 = capability.constraint;
                  int var13 = var12.length;

                  for(int var14 = 0; var14 < var13; ++var14) {
                     ConstraintInstance constraintInstance = var12[var14];
                     if (ArrayUtils.isEmpty(constraintInstance.propertyInstance)) {
                        return;
                     }

                     PropertyInstance[] var16 = constraintInstance.propertyInstance;
                     int var17 = var16.length;

                     for(int var18 = 0; var18 < var17; ++var18) {
                        PropertyInstance propertyInstance = var16[var18];
                        if (propertyInstance.id.equals("hostFailuresToTolerate")) {
                           this.ftt = (Integer)propertyInstance.value;
                        }

                        if (propertyInstance.id.equals("locality")) {
                           this.locality = (String)propertyInstance.value;
                        }
                     }
                  }
               }
            }
         }
      }

   }
}
