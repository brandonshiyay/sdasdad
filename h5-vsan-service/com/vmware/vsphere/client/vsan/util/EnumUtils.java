package com.vmware.vsphere.client.vsan.util;

import java.util.Arrays;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class EnumUtils {
   private static final Log logger = LogFactory.getLog(EnumUtils.class);
   private static final EnumUtils.StringEqualityChecker caseSensitiveChecker = (s1, s2) -> {
      return s1.equals(s2);
   };
   private static final EnumUtils.StringEqualityChecker caseInsensitiveChecker = (s1, s2) -> {
      return s1.equalsIgnoreCase(s2);
   };

   public static Enum fromString(Class clazz, String str) {
      return parseEnumFromString(clazz, str, (Enum)null, false);
   }

   public static Enum fromString(Class clazz, String str, Enum defaultValue) {
      return parseEnumFromString(clazz, str, defaultValue, false);
   }

   public static Enum fromStringIgnoreCase(Class clazz, String str) {
      return parseEnumFromString(clazz, str, (Enum)null, true);
   }

   public static Enum fromStringIgnoreCase(Class clazz, String str, Enum defaultValue) {
      return parseEnumFromString(clazz, str, defaultValue, true);
   }

   private static Enum parseEnumFromString(Class clazz, String str, Enum defaultValue, boolean ignoreCase) {
      Validate.notNull(clazz);
      if (StringUtils.isEmpty(str)) {
         logger.warn("The given string is empty. Returning the default value: " + defaultValue);
         return defaultValue;
      } else {
         logger.debug("Ignoring case: " + ignoreCase);
         EnumUtils.StringEqualityChecker checker = ignoreCase ? caseInsensitiveChecker : caseSensitiveChecker;
         Enum val = getEnum(clazz, StringUtils.trim(str), checker);
         if (val == null) {
            logger.warn("Cannot parse '" + str + "' to Enum '" + clazz.getCanonicalName() + "'. Returning default value '" + defaultValue + "'");
            return defaultValue;
         } else {
            logger.debug("String successfully parsed to enum: " + str + " -> " + val);
            return val;
         }
      }
   }

   private static Enum getEnum(Class clazz, String str, EnumUtils.StringEqualityChecker checker) {
      Enum[] var3 = (Enum[])clazz.getEnumConstants();
      int var4 = var3.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         Enum enumValue = var3[var5];
         String valToCompare = enumValue.toString();
         if (EnumWithKey.class.isAssignableFrom(clazz)) {
            Object keys = ((EnumWithKey)enumValue).getKey();
            if (keys instanceof String[]) {
               if (Arrays.stream((String[])((String[])keys)).anyMatch((key) -> {
                  return checker.checkEquality(key, str);
               })) {
                  return enumValue;
               }
            } else {
               valToCompare = keys.toString();
            }
         }

         if (checker.checkEquality(valToCompare, str)) {
            return enumValue;
         }
      }

      return null;
   }

   @FunctionalInterface
   private interface StringEqualityChecker {
      boolean checkEquality(String var1, String var2);
   }
}
