package com.vmware.vsphere.client.vsan.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Date;

public class FormatUtil {
   public static final long AUTO = -1L;
   public static final long B = 1L;
   public static final long KB = 1024L;
   public static final long MB = 1048576L;
   public static final long GB = 1073741824L;
   public static final long TB = 1099511627776L;

   public static String getStorageFormatted(Long value, long valueUnit, long targetUnit) {
      String formattedString = getDataSizeFormatted(value, 2, valueUnit, targetUnit, true);
      return formattedString;
   }

   public static String getDataSizeFormatted(Number value, int precision, long valueUnit, long targetUnit, boolean includeUnitLabel) {
      if (value != null && valueUnit != -1L) {
         if (targetUnit != -1L && targetUnit != 1L && targetUnit != 1024L && targetUnit != 1048576L && targetUnit != 1073741824L && targetUnit != 1099511627776L) {
            targetUnit = -1L;
         }

         BigDecimal inBytes = BigDecimal.valueOf(value.doubleValue()).multiply(BigDecimal.valueOf(valueUnit));
         BigDecimal inBytesAbs = inBytes.abs();
         if (targetUnit == -1L) {
            if (inBytesAbs.compareTo(BigDecimal.valueOf(1099511627776L)) >= 0) {
               targetUnit = 1099511627776L;
            } else if (inBytesAbs.compareTo(BigDecimal.valueOf(1073741824L)) >= 0) {
               targetUnit = 1073741824L;
            } else if (inBytesAbs.compareTo(BigDecimal.valueOf(1048576L)) >= 0) {
               targetUnit = 1048576L;
            } else if (inBytesAbs.compareTo(BigDecimal.valueOf(1024L)) >= 0) {
               targetUnit = 1024L;
            } else {
               targetUnit = 1L;
            }
         }

         BigDecimal targetUnitBD = BigDecimal.valueOf(targetUnit);
         BigDecimal val = inBytes.divide(targetUnitBD, precision, RoundingMode.HALF_UP);
         String formattedNumber = getLocalizedNumber(val);
         String formattedString = formattedNumber + (includeUnitLabel ? " " + getStorageUnit(targetUnit) : "");
         return formattedString;
      } else {
         return null;
      }
   }

   public static String getStorageUnit(long value) {
      if (value == 1024L) {
         return Utils.getLocalizedString("MEM_KB");
      } else if (value == 1048576L) {
         return Utils.getLocalizedString("MEM_MB");
      } else if (value == 1073741824L) {
         return Utils.getLocalizedString("MEM_GB");
      } else {
         return value == 1099511627776L ? Utils.getLocalizedString("MEM_TB") : Utils.getLocalizedString("MEM_B");
      }
   }

   public static String parseSecondsToLocalizedTimeUnit(long timeInSeconds) {
      if (timeInSeconds < 0L) {
         return "";
      } else {
         long seconds = timeInSeconds % 60L;
         long timeInMinutes = (timeInSeconds - seconds) / 60L;
         long minutes = timeInMinutes % 60L;
         long timeInHours = (timeInMinutes - minutes) / 60L;
         long hours = timeInHours % 24L;
         long days = (timeInHours - hours) / 24L;
         if (minutes == 0L && hours == 0L && days == 0L) {
            seconds += minutes * 60L;
            return seconds == 1L ? Utils.getLocalizedString("time.common.second") : Utils.getLocalizedString("time.common.seconds", String.valueOf(seconds));
         } else if (hours == 0L && days == 0L) {
            return minutes == 1L ? Utils.getLocalizedString("time.common.minute") : Utils.getLocalizedString("time.common.minutes", String.valueOf(minutes + hours * 60L));
         } else if (days == 0L) {
            return hours == 1L ? Utils.getLocalizedString("time.common.hour") : Utils.getLocalizedString("time.common.hours", String.valueOf(hours + days * 24L));
         } else {
            return days == 1L ? Utils.getLocalizedString("time.common.day") : Utils.getLocalizedString("time.common.days", String.valueOf(days));
         }
      }
   }

   public static long getMinutesFromNow(long dateInMilliseconds) {
      Date now = new Date();
      return (dateInMilliseconds - now.getTime()) / 60000L;
   }

   public static long getSecondsFromNow(long dateInMilliseconds) {
      Date now = new Date();
      return (dateInMilliseconds - now.getTime()) / 1000L;
   }

   private static String getLocalizedNumber(BigDecimal val) {
      return NumberFormat.getInstance(Utils.getCurrentLocale()).format(val.doubleValue());
   }
}
