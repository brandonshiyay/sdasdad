package com.vmware.vsan.client.services.fileanalytics;

import com.vmware.vim.vsan.binding.vim.vsan.VsanFileAnalyticsReportQuerySpec;
import com.vmware.vim.vsan.binding.vim.vsan.VsanFileAnalyticsReportType;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsphere.client.vsan.base.util.BaseUtils;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

public class FileAnalyticsUtil {
   public static Double getUsagePercentage(long currentUsage, Long quota) {
      return quota != null && quota != 0L ? BaseUtils.toPercentage((double)currentUsage, (double)quota) : null;
   }

   public static double getGrowthPercentage(long currentUsage, long shareGrowth) {
      if (shareGrowth == 0L) {
         return 0.0D;
      } else {
         long previousUsage = currentUsage - shareGrowth;
         return previousUsage == 0L ? 100.0D : BaseUtils.toPercentage((double)shareGrowth, (double)previousUsage);
      }
   }

   public static Date parseReportDate(String category) {
      DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
      dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

      try {
         return dateFormat.parse(category);
      } catch (ParseException var3) {
         throw new VsanUiLocalizableException("vsan.common.generic.error", "Unable to parse category to date. Expected format: dd/MM/yyyy, received: " + category, var3, new Object[0]);
      }
   }

   public static List parseStringsToLongs(String[] values) {
      return (List)Arrays.stream(values).map((value) -> {
         return Math.round(Double.parseDouble(value));
      }).collect(Collectors.toCollection(ArrayList::new));
   }

   public static List parseTimestampsToDates(String[] values) {
      return (List)Arrays.stream(values).map((ts) -> {
         return new Date(Math.round(Double.parseDouble(ts) * 1000.0D));
      }).collect(Collectors.toCollection(ArrayList::new));
   }

   public static VsanFileAnalyticsReportQuerySpec buildSpec(VsanFileAnalyticsReportType reportType, long from, long to) {
      return new VsanFileAnalyticsReportQuerySpec(BaseUtils.getCalendarFromLong(from), BaseUtils.getCalendarFromLong(to), reportType.name(), (String[])null);
   }

   public static VsanFileAnalyticsReportQuerySpec buildSpec(VsanFileAnalyticsReportType reportType, String[] fileShareUuids) {
      return new VsanFileAnalyticsReportQuerySpec((Calendar)null, (Calendar)null, reportType.name(), fileShareUuids);
   }
}
