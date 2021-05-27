package com.vmware.vsphere.client.vsan.base.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.vmware.vsphere.client.vsan.base.data.VsanComponent;
import com.vmware.vsphere.client.vsan.base.data.VsanComponentState;
import com.vmware.vsphere.client.vsan.base.data.VsanObject;
import com.vmware.vsphere.client.vsan.base.data.VsanRaidConfig;
import com.vmware.vsphere.client.vsan.base.data.VsanRootConfig;
import com.vmware.vsphere.client.vsan.util.Utils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class VsanJsonParser {
   private static final Log _logger = LogFactory.getLog(VsanJsonParser.class);
   private static final int DELTA_COMPONENT_FLAG = 512;

   public static List parseVsanObjects(String vsanJson, List objectUuids) {
      ArrayList result = new ArrayList();
      if (vsanJson != null && objectUuids != null && objectUuids.size() != 0) {
         JsonNode root = Utils.getJsonRootNode(vsanJson);
         if (root == null) {
            return result;
         } else {
            JsonNode domObjects = root.get("dom_objects");
            JsonNode lsomObjects = root.get("lsom_objects");
            JsonNode diskObjects = root.get("disk_objects");
            if (domObjects != null && lsomObjects != null && diskObjects != null) {
               Iterator var7 = objectUuids.iterator();

               while(true) {
                  VsanObject vsanObject;
                  JsonNode content;
                  do {
                     if (!var7.hasNext()) {
                        return result;
                     }

                     String uuid = (String)var7.next();
                     vsanObject = new VsanObject(uuid);
                     JsonNode objNode = domObjects.findPath(uuid);
                     JsonNode config = objNode.findPath("config");
                     content = config.findPath("content");
                  } while(content == null);

                  VsanRootConfig rootConfig = new VsanRootConfig();
                  Iterator elements = content.elements();

                  while(elements.hasNext()) {
                     JsonNode contentChild = (JsonNode)elements.next();
                     if (contentChild.has("type")) {
                        String type = contentChild.get("type").asText();
                        if ("Witness".equals(type)) {
                           VsanComponent witnessComponent = getVsanComponent(diskObjects, lsomObjects, contentChild, type);
                           rootConfig.children.add(witnessComponent);
                        } else {
                           VsanRaidConfig raidConfig;
                           if ("Component".equals(type)) {
                              raidConfig = new VsanRaidConfig();
                              raidConfig.type = Utils.getLocalizedString("vsan.monitor.virtualPhysicalMapping.raid0");
                              raidConfig.children = getVsanComponents(diskObjects, lsomObjects, content);
                              rootConfig.children.add(raidConfig);
                              break;
                           }

                           raidConfig = new VsanRaidConfig();
                           raidConfig.type = getLocalizedType(type);
                           raidConfig.children = getVsanComponents(diskObjects, lsomObjects, contentChild);
                           if (raidConfig.children.size() > 0) {
                              rootConfig.children.add(raidConfig);
                           }
                        }
                     }
                  }

                  vsanObject.rootConfig = rootConfig;
                  result.add(vsanObject);
               }
            } else {
               return result;
            }
         }
      } else {
         return result;
      }
   }

   private static VsanComponent getVsanComponent(JsonNode diskObjects, JsonNode lsomObjects, JsonNode contentChild, String type) {
      VsanComponent component = new VsanComponent(true);
      JsonNode componentAttribute = contentChild.get("attributes");
      if (componentAttribute == null) {
         return component;
      } else {
         int flagsProp = getValueIntByKey(componentAttribute, "flags");
         component.type = getLocalizedType(type, flagsProp);
         component.componentUuid = getValueStringByKey(contentChild, "componentUuid");
         int stateNumber = getValueIntByKey(componentAttribute, "componentState");
         long bytesToSyncProp = getValueLongByKey(componentAttribute, "bytesToSync");
         component.byteToSync = bytesToSyncProp;
         if (bytesToSyncProp > 0L) {
            long recoveryETA = getValueLongByKey(componentAttribute, "recoveryETA");
            component.recoveryEta = recoveryETA;
         }

         component.state = VsanComponentState.fromCmmdsData(stateNumber, bytesToSyncProp, flagsProp);
         String diskUuid = getValueStringByKey(contentChild, "diskUuid");
         JsonNode disk = diskObjects.get(diskUuid);
         if (diskUuid != null && diskUuid != "") {
            component.capacityDiskUuid = diskUuid;
         }

         JsonNode lsomComponent;
         if (disk != null) {
            lsomComponent = disk.get("content");
            if (lsomComponent != null) {
               String cacheDiskUuid = getValueStringByKey(lsomComponent, "ssdUuid");
               if (StringUtils.isNotEmpty(cacheDiskUuid)) {
                  component.cacheDiskUuid = cacheDiskUuid;
               }
            }
         }

         lsomComponent = lsomObjects.get(component.componentUuid);
         if (lsomComponent != null) {
            component.hostUuid = getValueStringByKey(lsomComponent, "owner");
         }

         return component;
      }
   }

   private static List getVsanComponents(JsonNode diskObjects, JsonNode lsomObjects, JsonNode content) {
      List children = new ArrayList();
      Iterator elements = content.elements();

      while(elements.hasNext()) {
         JsonNode contentChild = (JsonNode)elements.next();
         if (contentChild.has("type")) {
            String type = contentChild.get("type").asText();
            if ("Component".equals(type)) {
               VsanComponent item = getVsanComponent(diskObjects, lsomObjects, contentChild, type);
               children.add(item);
            } else {
               List childrenItems = getVsanComponents(diskObjects, lsomObjects, contentChild);
               if (childrenItems != null && childrenItems.size() > 0) {
                  VsanRaidConfig raidItem = new VsanRaidConfig();
                  raidItem.type = getLocalizedType(type);
                  raidItem.children = childrenItems;
                  children.add(raidItem);
               }
            }
         }
      }

      return children;
   }

   private static String getLocalizedType(String type) {
      return getLocalizedType(type, 0);
   }

   private static String getLocalizedType(String type, int flags) {
      byte var3 = -1;
      switch(type.hashCode()) {
      case -1885104965:
         if (type.equals("RAID_0")) {
            var3 = 2;
         }
         break;
      case -1885104964:
         if (type.equals("RAID_1")) {
            var3 = 3;
         }
         break;
      case -1885104960:
         if (type.equals("RAID_5")) {
            var3 = 4;
         }
         break;
      case -1885104959:
         if (type.equals("RAID_6")) {
            var3 = 5;
         }
         break;
      case -1885104945:
         if (type.equals("RAID_D")) {
            var3 = 6;
         }
         break;
      case -1274991335:
         if (type.equals("Witness")) {
            var3 = 0;
         }
         break;
      case 353045048:
         if (type.equals("Concatenation")) {
            var3 = 7;
         }
         break;
      case 604060893:
         if (type.equals("Component")) {
            var3 = 1;
         }
      }

      switch(var3) {
      case 0:
         return Utils.getLocalizedString("vsan.monitor.virtualPhysicalMapping.witness");
      case 1:
         return getComponentLocalizedType(flags);
      case 2:
         return Utils.getLocalizedString("vsan.monitor.virtualPhysicalMapping.raid0");
      case 3:
         return Utils.getLocalizedString("vsan.monitor.virtualPhysicalMapping.raid1");
      case 4:
         return Utils.getLocalizedString("vsan.monitor.virtualPhysicalMapping.raid5");
      case 5:
         return Utils.getLocalizedString("vsan.monitor.virtualPhysicalMapping.raid6");
      case 6:
         return Utils.getLocalizedString("vsan.monitor.virtualPhysicalMapping.durability");
      case 7:
         return Utils.getLocalizedString("vsan.monitor.virtualPhysicalMapping.concatenation");
      default:
         _logger.error(String.format("Unexpected type %s found while passing vSAN component types.", type));
         return type;
      }
   }

   private static String getComponentLocalizedType(int flags) {
      boolean isDeltaComponent = (flags & 512) != 0;
      return isDeltaComponent ? Utils.getLocalizedString("vsan.monitor.virtualPhysicalMapping.durabilityComponent") : Utils.getLocalizedString("vsan.monitor.virtualPhysicalMapping.component");
   }

   private static String getValueStringByKey(JsonNode node, String key) {
      return node != null && !node.isMissingNode() && node.has(key) ? node.get(key).asText() : "";
   }

   private static long getValueLongByKey(JsonNode node, String key) {
      return node != null && !node.isMissingNode() && node.has(key) ? node.get(key).asLong() : 0L;
   }

   private static int getValueIntByKey(JsonNode node, String key) {
      return node != null && !node.isMissingNode() && node.has(key) ? node.get(key).asInt() : 0;
   }
}
