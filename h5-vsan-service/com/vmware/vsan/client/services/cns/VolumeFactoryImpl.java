package com.vmware.vsan.client.services.cns;

import com.vmware.vim.vsan.binding.vim.cns.BlockBackingDetails;
import com.vmware.vim.vsan.binding.vim.cns.ContainerCluster;
import com.vmware.vim.vsan.binding.vim.cns.EntityMetadata;
import com.vmware.vim.vsan.binding.vim.cns.KubernetesEntityMetadata;
import com.vmware.vim.vsan.binding.vim.cns.KubernetesEntityReference;
import com.vmware.vim.vsan.binding.vim.cns.Volume;
import com.vmware.vim.vsan.binding.vim.cns.VolumeType;
import com.vmware.vim.vsan.binding.vim.cns.VsanFileShareBackingDetails;
import com.vmware.vsan.client.services.cns.model.ClusterFlavor;
import com.vmware.vsan.client.services.cns.model.CnsHealthStatus;
import com.vmware.vsan.client.services.cns.model.KubernetesEntity;
import com.vmware.vsan.client.services.cns.model.KubernetesEntityType;
import com.vmware.vsan.client.services.cns.model.VolumeContainerCluster;
import com.vmware.vsan.client.services.common.data.LabelData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VolumeFactoryImpl extends BaseVolumeFactory {
   private static final Logger logger = LoggerFactory.getLogger(VolumeFactoryImpl.class);

   protected void updateVolume(Volume cnsVolume, com.vmware.vsan.client.services.cns.model.Volume volume) {
      if (cnsVolume.backingObjectDetails != null) {
         if (this.isFileVolume(cnsVolume)) {
            volume.fileshareName = ((VsanFileShareBackingDetails)cnsVolume.backingObjectDetails).name;
         }

         if (this.isBlockVolume(cnsVolume)) {
            BlockBackingDetails blockBackingDetails = (BlockBackingDetails)cnsVolume.backingObjectDetails;
            if (blockBackingDetails.backingDiskObjectId != null) {
               volume.backingObjectId = blockBackingDetails.backingDiskObjectId;
            }

            if (blockBackingDetails.backingDiskPath != null) {
               volume.path = blockBackingDetails.backingDiskPath;
            }
         }
      }

      if (cnsVolume.metadata != null && !ArrayUtils.isEmpty(cnsVolume.metadata.containerClusterArray)) {
         if (ArrayUtils.isNotEmpty(cnsVolume.metadata.entityMetadata)) {
            List pv = new ArrayList();
            List pvc = new ArrayList();
            List pod = new ArrayList();
            List labels = new ArrayList();
            this.distributeEntitiesByType(pv, pvc, pod, labels, cnsVolume.metadata.entityMetadata);
            volume.labels = LabelData.fromKeyValue((List)labels);
            List containerClusters = this.createNonEmptyClusters(cnsVolume, volume, pv, pvc, pod);
            this.addNonEmptyClustersToVolume(cnsVolume, volume, containerClusters);
         }

         this.addEmptyContainerClusters(cnsVolume, volume);
      }
   }

   private boolean isFileVolume(Volume volume) {
      return volume.volumeType.equals(VolumeType.FILE.toString());
   }

   protected void updateHealthStatus(Volume cnsVolume, com.vmware.vsan.client.services.cns.model.Volume volume) {
      volume.healthStatus = CnsHealthStatus.fromName(cnsVolume.healthStatus);
   }

   private List createNonEmptyClusters(Volume cnsVolume, com.vmware.vsan.client.services.cns.model.Volume volume, List pv, List pvc, List pod) {
      List containerClusters = new ArrayList();
      Iterator var7 = pv.iterator();

      KubernetesEntityMetadata metadata;
      VolumeContainerCluster cluster;
      while(var7.hasNext()) {
         metadata = (KubernetesEntityMetadata)var7.next();
         cluster = this.getContainerCluster(containerClusters, metadata, cnsVolume);
         if (cluster != null) {
            KubernetesEntity persistentVolume = this.createClusterPersistentVolume(metadata, KubernetesEntityType.PERSISTENT_VOLUME);
            cluster.persistentVolumes.add(persistentVolume);
         }
      }

      var7 = pvc.iterator();

      while(var7.hasNext()) {
         metadata = (KubernetesEntityMetadata)var7.next();
         this.createClusterPersistentVolumeClaim(containerClusters, metadata, cnsVolume);
      }

      var7 = pod.iterator();

      while(var7.hasNext()) {
         metadata = (KubernetesEntityMetadata)var7.next();
         cluster = this.getContainerCluster(containerClusters, metadata, cnsVolume);
         if (this.isBlockVolume(cnsVolume)) {
            this.createBlockVolumePOD(cluster, metadata, volume);
         } else {
            this.createFileVolumePOD(cluster, metadata);
         }
      }

      return containerClusters;
   }

   private VolumeContainerCluster getContainerCluster(List clusters, EntityMetadata metadata, Volume cnsVolume) {
      if (StringUtils.isEmpty(metadata.clusterId)) {
         logger.warn("A cns volume without container cluster is received: " + metadata.entityName);
         return null;
      } else {
         Iterator var4 = clusters.iterator();

         while(var4.hasNext()) {
            VolumeContainerCluster cluster = (VolumeContainerCluster)var4.next();
            if (cluster.name.equals(metadata.clusterId)) {
               return cluster;
            }
         }

         VolumeContainerCluster newCluster = new VolumeContainerCluster();
         newCluster.name = metadata.clusterId;
         if (ArrayUtils.isNotEmpty(cnsVolume.metadata.containerClusterArray)) {
            ContainerCluster[] var10 = cnsVolume.metadata.containerClusterArray;
            int var6 = var10.length;

            for(int var7 = 0; var7 < var6; ++var7) {
               ContainerCluster cluster = var10[var7];
               if (cluster.clusterId.equals(metadata.clusterId)) {
                  newCluster.type = cluster.clusterType;
                  newCluster.flavor = ClusterFlavor.fromString(cluster.clusterFlavor);
                  break;
               }
            }
         }

         clusters.add(newCluster);
         return newCluster;
      }
   }

   private void createClusterPersistentVolumeClaim(List containerClusters, KubernetesEntityMetadata pvc, Volume cnsVolume) {
      VolumeContainerCluster cluster = this.getContainerCluster(containerClusters, pvc, cnsVolume);
      if (cluster != null) {
         KubernetesEntity volumeClaim = this.createClusterPersistentVolume(pvc, KubernetesEntityType.PERSISTENT_VOLUME_CLAIM);
         if (this.isBlockVolume(cnsVolume) && cluster.persistentVolumes.size() > 0) {
            ((KubernetesEntity)cluster.persistentVolumes.get(0)).persistentVolumeClaim = volumeClaim;
         } else if (!ArrayUtils.isEmpty(pvc.referredEntity)) {
            Iterator var6 = cluster.persistentVolumes.iterator();

            while(var6.hasNext()) {
               KubernetesEntity persistentVolume = (KubernetesEntity)var6.next();
               if (pvc.referredEntity[0].entityName.equals(persistentVolume.name)) {
                  persistentVolume.persistentVolumeClaim = volumeClaim;
               }
            }

         }
      }
   }

   private boolean isBlockVolume(Volume volume) {
      return volume.volumeType.equals(VolumeType.BLOCK.toString());
   }

   private void createBlockVolumePOD(VolumeContainerCluster cluster, KubernetesEntityMetadata pod, com.vmware.vsan.client.services.cns.model.Volume volume) {
      if (cluster != null && !CollectionUtils.isEmpty(cluster.persistentVolumes) && ((KubernetesEntity)cluster.persistentVolumes.get(0)).persistentVolumeClaim != null) {
         ((KubernetesEntity)cluster.persistentVolumes.get(0)).persistentVolumeClaim.podNames.add(pod.entityName);
      } else {
         volume.podNames.add(pod.entityName);
      }

   }

   private void createFileVolumePOD(VolumeContainerCluster cluster, KubernetesEntityMetadata pod) {
      if (cluster != null && !ArrayUtils.isEmpty(pod.referredEntity)) {
         KubernetesEntityReference[] var3 = pod.referredEntity;
         int var4 = var3.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            KubernetesEntityReference entityReference = var3[var5];
            Iterator var7 = cluster.persistentVolumes.iterator();

            while(var7.hasNext()) {
               KubernetesEntity persistentVolume = (KubernetesEntity)var7.next();
               if (persistentVolume.persistentVolumeClaim != null && persistentVolume.persistentVolumeClaim.name.equals(entityReference.entityName) && persistentVolume.persistentVolumeClaim.namespace.equals(pod.namespace) && !persistentVolume.persistentVolumeClaim.podNames.contains(pod.entityName)) {
                  persistentVolume.persistentVolumeClaim.podNames.add(pod.entityName);
               }
            }
         }

      }
   }

   private void addNonEmptyClustersToVolume(Volume cnsVolume, com.vmware.vsan.client.services.cns.model.Volume volume, List containerClusters) {
      if (!CollectionUtils.isEmpty(containerClusters)) {
         List guestClusters = new ArrayList();
         List supervisorClusters = new ArrayList();
         Iterator var6 = containerClusters.iterator();

         while(var6.hasNext()) {
            VolumeContainerCluster cluster = (VolumeContainerCluster)var6.next();
            if (cluster.flavor == ClusterFlavor.GUEST) {
               guestClusters.add(cluster);
            } else {
               supervisorClusters.add(cluster);
            }
         }

         this.addGuestClusters(supervisorClusters, guestClusters, cnsVolume.metadata.entityMetadata);
         volume.containerClusters = supervisorClusters;
      }
   }

   private void addGuestClusters(List supervisorClusters, List guestClusters, EntityMetadata[] entityMetadata) {
      if (!CollectionUtils.isEmpty(supervisorClusters) && !CollectionUtils.isEmpty(guestClusters)) {
         Iterator var4 = guestClusters.iterator();

         while(var4.hasNext()) {
            VolumeContainerCluster guestCluster = (VolumeContainerCluster)var4.next();
            if (CollectionUtils.isEmpty(guestCluster.persistentVolumes)) {
               logger.warn("Guest cluster with no PV is received: " + guestCluster.name);
            } else {
               String guestClusterName = guestCluster.name;
               String pvNameFromGuestCluster = ((KubernetesEntity)guestCluster.persistentVolumes.get(0)).name;
               KubernetesEntityReference pvReferredEntity = (KubernetesEntityReference)Arrays.stream(entityMetadata).map((eMetadata) -> {
                  return (KubernetesEntityMetadata)eMetadata;
               }).filter((kMetadata) -> {
                  return KubernetesEntityType.fromString(kMetadata.entityType) == KubernetesEntityType.PERSISTENT_VOLUME && kMetadata.entityName.equals(pvNameFromGuestCluster) && kMetadata.clusterId.equals(guestClusterName);
               }).map((kMetadata) -> {
                  return kMetadata.referredEntity[0];
               }).findFirst().orElse((Object)null);
               if (pvReferredEntity != null) {
                  supervisorClusters.stream().filter((cls) -> {
                     return cls.flavor == ClusterFlavor.WORKLOAD && cls.name.equals(pvReferredEntity.clusterId);
                  }).flatMap((supCls) -> {
                     return supCls.persistentVolumes.stream();
                  }).map((pv) -> {
                     return pv.persistentVolumeClaim;
                  }).filter((pvc) -> {
                     return pvc != null;
                  }).forEach((pvc) -> {
                     if (pvReferredEntity.entityName.equals(pvc.name) && pvReferredEntity.namespace.equals(pvc.namespace)) {
                        pvc.guestCluster = guestCluster;
                     }

                  });
               }
            }
         }

      }
   }

   private void addEmptyContainerClusters(Volume cnsVolume, com.vmware.vsan.client.services.cns.model.Volume volume) {
      ContainerCluster[] var3 = cnsVolume.metadata.containerClusterArray;
      int var4 = var3.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         ContainerCluster cluster = var3[var5];
         if (ClusterFlavor.fromString(cluster.clusterFlavor) != ClusterFlavor.GUEST && !volume.containerClusters.stream().anyMatch((addedCluster) -> {
            return addedCluster.name.equals(cluster.clusterId) && addedCluster.type.equals(cluster.clusterType) && addedCluster.flavor == ClusterFlavor.fromString(cluster.clusterFlavor);
         })) {
            VolumeContainerCluster newCluster = new VolumeContainerCluster();
            newCluster.name = cluster.clusterId;
            newCluster.type = cluster.clusterType;
            newCluster.flavor = ClusterFlavor.fromString(cluster.clusterFlavor);
            volume.containerClusters.add(newCluster);
         }
      }

   }
}
