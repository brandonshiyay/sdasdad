package com.vmware.vsan.client.services.virtualobjects.data;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vsphere.client.vsan.base.data.VsanObjectCompositeHealth;
import com.vmware.vsphere.client.vsan.base.data.VsanObjectHealthState;
import com.vmware.vsphere.client.vsan.base.data.VsanObjectType;
import com.vmware.vsphere.client.vsan.whatif.VsanWhatIfComplianceStatus;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;

@TsModel
public class VirtualObjectModel {
   public String uid;
   public String diskUuid;
   public ManagedObjectReference vmRef;
   public String iconId;
   public String name;
   public VirtualObjectType type;
   public String applicationInstanceId;
   public VsanObjectHealthState healthState;
   public VsanObjectCompositeHealth compositeHealth;
   public DurabilityState durabilityState;
   public VsanWhatIfComplianceStatus whatIfComplianceStatus;
   public String storagePolicy;
   public VirtualObjectModel[] children;
   public static final Comparator COMPARATOR = new Comparator() {
      public int compare(VirtualObjectModel o1, VirtualObjectModel o2) {
         return o1.name.compareTo(o2.name);
      }
   };

   public VirtualObjectModel cloneWithoutChildren() {
      VirtualObjectModel o = new VirtualObjectModel();
      o.uid = this.uid;
      o.diskUuid = this.diskUuid;
      o.vmRef = this.vmRef;
      o.name = this.name;
      o.type = this.type;
      o.iconId = this.iconId;
      o.healthState = this.healthState;
      o.whatIfComplianceStatus = this.whatIfComplianceStatus;
      o.storagePolicy = this.storagePolicy;
      o.children = new VirtualObjectModel[0];
      return o;
   }

   public VirtualObjectModel cloneWithChildren() {
      VirtualObjectModel o = this.cloneWithoutChildren();
      List children = new ArrayList(this.children.length);
      VirtualObjectModel[] var3 = this.children;
      int var4 = var3.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         VirtualObjectModel child = var3[var5];
         children.add(child.cloneWithoutChildren());
      }

      o.children = (VirtualObjectModel[])children.toArray(new VirtualObjectModel[children.size()]);
      return o;
   }

   public void mergeChildren(VirtualObjectModel clone) {
      if (clone != null && !ArrayUtils.isEmpty(clone.children)) {
         Set children = new HashSet(Arrays.asList(this.children));
         VirtualObjectModel[] var3 = clone.children;
         int var4 = var3.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            VirtualObjectModel child = var3[var5];
            children.add(child);
         }

         this.children = (VirtualObjectModel[])children.toArray(new VirtualObjectModel[children.size()]);
      }
   }

   public boolean isOtherType() {
      if (this.vmRef != null) {
         return false;
      } else {
         return this.type.vmodlType != VsanObjectType.iscsiLun && this.type.vmodlType != VsanObjectType.iscsiTarget && this.type.vmodlType != VsanObjectType.improvedVirtualDisk && this.type.vmodlType != VsanObjectType.fileShare && this.type.vmodlType != VsanObjectType.improvedVirtualDisk && this.type.vmodlType != VsanObjectType.detachedCnsVolBlock && this.type.vmodlType != VsanObjectType.cnsVolFile && this.type.vmodlType != VsanObjectType.hbrCfg && this.type.vmodlType != VsanObjectType.hbrDisk;
      }
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (!(o instanceof VirtualObjectModel)) {
         return false;
      } else {
         VirtualObjectModel that = (VirtualObjectModel)o;
         if (!ObjectUtils.equals(this.vmRef, that.vmRef)) {
            return false;
         } else {
            return this.uid == null ? Objects.equals(this.name, that.name) : Objects.equals(this.uid, that.uid);
         }
      }
   }

   public int hashCode() {
      return this.uid == null ? Objects.hash(new Object[]{this.name}) : Objects.hash(new Object[]{this.uid});
   }

   public String toString() {
      return "VirtualObjectModel(uid=" + this.uid + ", diskUuid=" + this.diskUuid + ", vmRef=" + this.vmRef + ", iconId=" + this.iconId + ", name=" + this.name + ", type=" + this.type + ", applicationInstanceId=" + this.applicationInstanceId + ", healthState=" + this.healthState + ", compositeHealth=" + this.compositeHealth + ", durabilityState=" + this.durabilityState + ", whatIfComplianceStatus=" + this.whatIfComplianceStatus + ", storagePolicy=" + this.storagePolicy + ", children=" + Arrays.deepToString(this.children) + ")";
   }
}
