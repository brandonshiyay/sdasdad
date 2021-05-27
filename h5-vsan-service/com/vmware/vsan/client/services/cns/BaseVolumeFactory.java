package com.vmware.vsan.client.services.cns;

import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cns.EntityMetadata;
import com.vmware.vim.vsan.binding.vim.cns.KubernetesEntityMetadata;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.services.cns.model.CnsVolumeType;
import com.vmware.vsan.client.services.cns.model.KubernetesEntity;
import com.vmware.vsan.client.services.cns.model.KubernetesEntityType;
import com.vmware.vsan.client.services.cns.model.Volume;
import com.vmware.vsan.client.services.common.data.LabelData;
import com.vmware.vsan.client.services.common.data.StorageCompliance;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;

public abstract class BaseVolumeFactory implements VolumeFactory {
   protected BaseVolumeFactory() {
   }

   public static BaseVolumeFactory getInstance(ManagedObjectReference objectRef) {
      return (BaseVolumeFactory)(VsanCapabilityUtils.isFileVolumesSupportedOnVc(objectRef) ? new VolumeFactoryImpl() : new LegacyVolumeFactory());
   }

   public Volume createVolume(ArrayList dsData, String dsType, com.vmware.vim.vsan.binding.vim.cns.Volume cnsVolume) {
      Volume volume = new Volume();
      volume.id = cnsVolume.volumeId.id;
      volume.name = cnsVolume.name;
      this.updateHealthStatus(cnsVolume, volume);
      volume.type = CnsVolumeType.fromName(cnsVolume.volumeType);
      volume.iconId = CnsVolumeType.FILE.equals(volume.type) ? "cns-file-volume" : "cns-volume";
      volume.datastoreData = dsData;
      volume.storagePolicyId = cnsVolume.storagePolicyId;
      if (cnsVolume.backingObjectDetails != null) {
         volume.capacity = cnsVolume.backingObjectDetails.capacityInMb * 1024L * 1024L;
      } else {
         volume.capacity = 0L;
      }

      volume.datastoreType = dsType;
      volume.compliance = StorageCompliance.fromName(cnsVolume.complianceStatus);
      this.updateVolume(cnsVolume, volume);
      return volume;
   }

   protected KubernetesEntity createClusterPersistentVolume(KubernetesEntityMetadata kMetadata, KubernetesEntityType type) {
      KubernetesEntity persistentVolume = new KubernetesEntity();
      persistentVolume.name = kMetadata.entityName;
      persistentVolume.type = type;
      persistentVolume.namespace = kMetadata.namespace;
      if (!ArrayUtils.isEmpty(kMetadata.labels)) {
         persistentVolume.labels = LabelData.fromKeyValue(Arrays.asList(kMetadata.labels));
      }

      return persistentVolume;
   }

   protected void distributeEntitiesByType(List pv, List pvc, List pod, List labels, EntityMetadata[] entityMetadata) {
      EntityMetadata[] var6 = entityMetadata;
      int var7 = entityMetadata.length;

      for(int var8 = 0; var8 < var7; ++var8) {
         EntityMetadata metadata = var6[var8];
         if (metadata instanceof KubernetesEntityMetadata) {
            KubernetesEntityMetadata kMetadata = (KubernetesEntityMetadata)metadata;
            switch(KubernetesEntityType.fromString(kMetadata.entityType)) {
            case PERSISTENT_VOLUME:
               pv.add(kMetadata);
               break;
            case POD:
               pod.add(kMetadata);
               break;
            case PERSISTENT_VOLUME_CLAIM:
               pvc.add(kMetadata);
            }
         }

         if (!ArrayUtils.isEmpty(metadata.labels)) {
            labels.addAll(Arrays.asList(metadata.labels));
         }
      }

   }

   protected abstract void updateVolume(com.vmware.vim.vsan.binding.vim.cns.Volume var1, Volume var2);

   protected abstract void updateHealthStatus(com.vmware.vim.vsan.binding.vim.cns.Volume var1, Volume var2);
}
