package com.vmware.vsan.client.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PhysicalDiskJsonParser {
   private static final String LSOM_CONTENT = "content";
   private static final String COMPOSITE_UUID = "compositeUuid";

   public static long getUsedCapacity(JsonNode disksProperties, String diskVsanUuid) {
      JsonNode contentRoot = getContentRoot(disksProperties, diskVsanUuid);
      return containsKey(contentRoot, "capacityUsed") ? contentRoot.get("capacityUsed").asLong() : 0L;
   }

   public static long getReservedCapacity(JsonNode disksProperties, String diskVsanUuid) {
      JsonNode contentRoot = getContentRoot(disksProperties, diskVsanUuid);
      return containsKey(contentRoot, "capacityReserved") ? contentRoot.get("capacityReserved").asLong() : 0L;
   }

   public static List getObjectUuids(JsonNode disksProperties, String diskVsanUuid) {
      JsonNode contentRoot = getContentRoot(disksProperties, diskVsanUuid);
      return (List)(containsKey(contentRoot, "lsom_objects") ? extractVirtualDiskUuids(contentRoot.get("lsom_objects")) : new ArrayList());
   }

   private static JsonNode getContentRoot(JsonNode disksProperties, String diskVsanUuid) {
      return containsKey(disksProperties, diskVsanUuid) ? disksProperties.get(diskVsanUuid) : null;
   }

   private static boolean containsKey(JsonNode node, String key) {
      return node != null && node.has(key);
   }

   private static List extractVirtualDiskUuids(JsonNode lsomObjects) {
      if (!(lsomObjects instanceof ArrayNode)) {
         return new ArrayList();
      } else {
         ArrayNode lsomArray = (ArrayNode)lsomObjects;
         Iterator iterator = lsomArray.iterator();
         ArrayList objectUuids = new ArrayList();

         while(iterator.hasNext()) {
            JsonNode lsomObjectNode = (JsonNode)iterator.next();
            JsonNode contentNode = lsomObjectNode.get("content");
            if (contentNode != null) {
               JsonNode compositeUuidNode = contentNode.get("compositeUuid");
               if (compositeUuidNode != null) {
                  objectUuids.add(compositeUuidNode.asText());
               }
            }
         }

         return objectUuids;
      }
   }
}
