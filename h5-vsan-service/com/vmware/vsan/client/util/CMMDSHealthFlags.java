package com.vmware.vsan.client.util;

import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;

public enum CMMDSHealthFlags {
   OK(0),
   FAILED(16),
   OFFLINE(32),
   DECOMMISSIONED(64),
   UNKNOWN(128),
   EVACUATING(256),
   EVACUATE_FAILED(512),
   EVACUATE_INACCESSIBLE(1024),
   EVACUATED(2048),
   READONLY(4096);

   private static final String FLAG_SEPARATOR = ",";
   private static final Set validValues = ImmutableSet.copyOf((Collection)Arrays.stream(values()).map(Enum::toString).collect(Collectors.toSet()));
   public final int value;

   private CMMDSHealthFlags(int value) {
      this.value = value;
   }

   public boolean isEqualTo(Integer healthFlag) {
      return healthFlag != null && healthFlag == this.value;
   }

   public static CMMDSHealthFlags fromString(String flag) {
      if (StringUtils.isEmpty(flag)) {
         return UNKNOWN;
      } else {
         Stream var10000 = Arrays.stream(flag.split(",")).map((f) -> {
            return f.trim().toUpperCase();
         });
         Set var10001 = validValues;
         var10001.getClass();
         return (CMMDSHealthFlags)var10000.filter(var10001::contains).map(CMMDSHealthFlags::valueOf).findFirst().orElse(UNKNOWN);
      }
   }
}
