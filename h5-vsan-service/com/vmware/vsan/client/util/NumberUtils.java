package com.vmware.vsan.client.util;

import com.vmware.vim.binding.vim.NumericRange;

public class NumberUtils {
   private static final String RANGE_DELIMITER = ", ";
   private static final String RANGE_DIVIDER = "-";

   public static int toInt(Number value) {
      return toInt(value, 0);
   }

   public static int toInt(Number value, int defaultValue) {
      return value != null ? value.intValue() : defaultValue;
   }

   public static long toLong(Long value) {
      return value != null ? value : 0L;
   }

   public static String parseNumericRange(NumericRange[] ranges) {
      if (ranges == null) {
         return null;
      } else {
         StringBuilder result = new StringBuilder();
         NumericRange[] var2 = ranges;
         int var3 = ranges.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            NumericRange range = var2[var4];
            if (range.start == range.end) {
               result.append(Integer.toString(range.start));
            } else {
               result.append(range.start + "-" + range.end);
            }

            result.append(", ");
         }

         if (result.length() >= ", ".length()) {
            result.setLength(result.length() - ", ".length());
         }

         return result.toString();
      }
   }
}
