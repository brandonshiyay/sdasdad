package com.vmware.vsphere.client.vsan.health.util;

import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanClusterHealthGroup;
import com.vmware.vim.vsan.binding.vim.cluster.VsanClusterHealthResultBase;
import com.vmware.vim.vsan.binding.vim.cluster.VsanClusterHealthResultColumnInfo;
import com.vmware.vim.vsan.binding.vim.cluster.VsanClusterHealthResultRow;
import com.vmware.vim.vsan.binding.vim.cluster.VsanClusterHealthResultTable;
import com.vmware.vim.vsan.binding.vim.cluster.VsanClusterHealthSummary;
import com.vmware.vim.vsan.binding.vim.cluster.VsanClusterHealthTest;
import com.vmware.vim.vsan.binding.vim.cluster.VsanHistoricalHealthTest;
import com.vmware.vim.vsan.binding.vim.host.VsanObjectHealthStateV2;
import com.vmware.vise.data.query.PropertyValue;
import com.vmware.vsphere.client.vsan.base.data.PspObjectHealthState;
import com.vmware.vsphere.client.vsan.base.data.VsanObjectCompositeHealth;
import com.vmware.vsphere.client.vsan.base.data.VsanObjectHealthState;
import com.vmware.vsphere.client.vsan.base.util.BaseUtils;
import com.vmware.vsphere.client.vsan.health.ColumnType;
import com.vmware.vsphere.client.vsan.health.ObjectWithName;
import com.vmware.vsphere.client.vsan.health.VsanHealthData;
import com.vmware.vsphere.client.vsan.health.VsanHealthStatus;
import com.vmware.vsphere.client.vsan.health.VsanTestCell;
import com.vmware.vsphere.client.vsan.health.VsanTestColumn;
import com.vmware.vsphere.client.vsan.health.VsanTestData;
import com.vmware.vsphere.client.vsan.health.VsanTestInstanceDetails;
import com.vmware.vsphere.client.vsan.health.VsanTestRow;
import com.vmware.vsphere.client.vsan.health.VsanTestTable;
import com.vmware.vsphere.client.vsan.util.EnumUtils;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import com.vmware.vsphere.client.vsan.util.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class VsanHealthUtil {
   public static final String TASK_TYPE = "Task";
   public static final String VSAN_INTERNET_ACCESS_ENABLED = "enableInternetAccess";
   private static final Log _logger = LogFactory.getLog(VsanHealthUtil.class);
   private static final String LIST_VALUES_DELIMITER = ",";
   private static final String HEALTH_V2_STATE_DELIMITER = ",";
   private static final String HEALTH_V2_KEY_VALUE_DELIMITER = ":";
   private static final int HEALTH_V2_KEY_VALUE_STATE_INDEX = 0;
   private static final int HEALTH_V2_KEY_VALUE_VALUE_INDEX = 1;

   public static ManagedObjectReference buildTaskMor(String taskId, String vcGuid) {
      ManagedObjectReference task = new ManagedObjectReference("Task", taskId, vcGuid);
      return task;
   }

   public static void addToTestMoRefs(VsanClusterHealthGroup healthGroup, Set allMoRefs, String serverGuid) {
      addToTestMoRefsFromBaseResults(healthGroup.groupDetails, allMoRefs, serverGuid);
      if (healthGroup.groupTests != null) {
         VsanClusterHealthTest[] var3 = healthGroup.groupTests;
         int var4 = var3.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            VsanClusterHealthTest test = var3[var5];
            addToTestMoRefsFromBaseResults(test.testDetails, allMoRefs, serverGuid);
         }
      }

   }

   public static void addToTestMoRefsFromBaseResults(VsanClusterHealthResultBase[] baseResults, Set allMoRefs, String serverGuid) {
      if (baseResults != null) {
         VsanClusterHealthResultBase[] var3 = baseResults;
         int var4 = baseResults.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            VsanClusterHealthResultBase baseResult = var3[var5];
            if (baseResult instanceof VsanClusterHealthResultTable) {
               VsanClusterHealthResultTable table = (VsanClusterHealthResultTable)baseResult;
               if (table.columns != null && table.rows != null) {
                  addToTestMoRefs(table.columns, table.rows, allMoRefs, serverGuid);
               }
            }
         }

      }
   }

   private static void addToTestMoRefs(VsanClusterHealthResultColumnInfo[] columns, VsanClusterHealthResultRow[] rows, Set allMoRefs, String serverGuid) {
      for(int i = 0; i < columns.length; ++i) {
         ColumnType columnType = (ColumnType)EnumUtils.fromStringIgnoreCase(ColumnType.class, columns[i].type, ColumnType.unknown);
         if (containsMoRefValue(columnType)) {
            VsanClusterHealthResultRow[] var6 = rows;
            int var7 = rows.length;

            for(int var8 = 0; var8 < var7; ++var8) {
               VsanClusterHealthResultRow row = var6[var8];
               addToMoRefsForIndex(row.values[i], columnType, allMoRefs, serverGuid);
            }
         }
      }

   }

   private static boolean containsMoRefValue(ColumnType columnType) {
      return columnType.equals(ColumnType.mor) || columnType.equals(ColumnType.listMor) || columnType.equals(ColumnType.dynamic);
   }

   private static void addToMoRefsForIndex(String rowValue, ColumnType columnType, Set allMoRefs, String serverGuid) {
      if (!StringUtils.isEmpty(rowValue)) {
         ColumnType cellType = columnType;
         String cellValue = rowValue;
         if (columnType.equals(ColumnType.dynamic)) {
            cellType = (ColumnType)EnumUtils.fromStringIgnoreCase(ColumnType.class, rowValue.split(":")[0], ColumnType.unknown);
            cellValue = rowValue.substring(cellType.toString().length() + 1);
         }

         ManagedObjectReference moRef;
         switch(cellType) {
         case mor:
            moRef = BaseUtils.generateMor(cellValue, serverGuid);
            if (moRef != null) {
               allMoRefs.add(moRef);
            }
            break;
         case listMor:
            String[] var7 = cellValue.split(",");
            int var8 = var7.length;

            for(int var9 = 0; var9 < var8; ++var9) {
               String mofStr = var7[var9];
               moRef = BaseUtils.generateMor(mofStr, serverGuid);
               if (moRef != null) {
                  allMoRefs.add(moRef);
               }
            }
         }

      }
   }

   public static Map getNamesForMoRefs(Set objects) {
      if (objects.size() == 0) {
         return new HashMap();
      } else {
         Map moRefToNameMap = new HashMap();
         objects.forEach((moRef) -> {
            String var10000 = (String)moRefToNameMap.put(moRef, (Object)null);
         });
         PropertyValue[] propValues = null;

         try {
            propValues = QueryUtil.getProperties((ManagedObjectReference[])objects.toArray(new ManagedObjectReference[0]), new String[]{"name"}).getPropertyValues();
         } catch (Exception var7) {
            _logger.error("Invalid query parameters are passed." + var7);
         }

         if (propValues == null) {
            return moRefToNameMap;
         } else {
            PropertyValue[] var3 = propValues;
            int var4 = propValues.length;

            for(int var5 = 0; var5 < var4; ++var5) {
               PropertyValue propValue = var3[var5];
               moRefToNameMap.put((ManagedObjectReference)propValue.resourceObject, (String)propValue.value);
            }

            return moRefToNameMap;
         }
      }
   }

   public static VsanHealthData getVsanHealthData(VsanClusterHealthSummary healthSummary, ManagedObjectReference clusterRef, boolean isFlat, boolean isVsphereHealth) {
      if (healthSummary == null) {
         return null;
      } else {
         VsanHealthData healthData = getVsanHealthData(healthSummary.overallHealth, healthSummary.overallHealthDescription, healthSummary.timestamp, healthSummary.groups, clusterRef, isFlat, isVsphereHealth);
         healthData.timestamp = healthSummary.getTimestamp();
         return healthData;
      }
   }

   private static VsanHealthData getVsanHealthData(String overallHealth, String overallHealthDescription, Calendar timestamp, VsanClusterHealthGroup[] groups, ManagedObjectReference clusterRef, boolean isFlat, boolean isVsphereHealth) {
      Set allMoRefs = new HashSet();
      if (ArrayUtils.isNotEmpty(groups)) {
         VsanClusterHealthGroup[] var8 = groups;
         int var9 = groups.length;

         for(int var10 = 0; var10 < var9; ++var10) {
            VsanClusterHealthGroup healthGroup = var8[var10];
            addToTestMoRefs(healthGroup, allMoRefs, clusterRef.getServerGuid());
         }
      }

      Map moRefToNameMap = getNamesForMoRefs(allMoRefs);
      VsanHealthData healthData = new VsanHealthData();
      healthData.description = overallHealthDescription;
      healthData.status = VsanHealthStatus.parse(overallHealth);
      healthData.timestamp = timestamp;
      healthData.testsData = new ArrayList();
      int var12;
      if (isFlat) {
         VsanTestData singleGroup = new VsanTestData();
         singleGroup.subtests = new ArrayList();
         VsanClusterHealthGroup[] var23 = groups;
         var12 = groups.length;

         for(int var13 = 0; var13 < var12; ++var13) {
            VsanClusterHealthGroup healthGroup = var23[var13];
            VsanClusterHealthTest[] var15 = healthGroup.groupTests;
            int var16 = var15.length;

            for(int var17 = 0; var17 < var16; ++var17) {
               VsanClusterHealthTest healthTest = var15[var17];
               singleGroup.subtests.add(new VsanTestData(healthTest, timestamp, moRefToNameMap, true, isVsphereHealth));
            }
         }

         healthData.testsData.add(singleGroup);
      } else if (ArrayUtils.isNotEmpty(groups)) {
         VsanClusterHealthGroup[] var22 = groups;
         int var24 = groups.length;

         for(var12 = 0; var12 < var24; ++var12) {
            VsanClusterHealthGroup healthGroup = var22[var12];
            healthData.testsData.add(new VsanTestData(healthGroup, timestamp, moRefToNameMap, isVsphereHealth));
         }
      }

      return healthData;
   }

   public static List createTestInstancesDetails(VsanHistoricalHealthTest[] historicalTests, String serverGuid) {
      return (List)(ArrayUtils.isEmpty(historicalTests) ? new ArrayList() : (List)Arrays.stream(historicalTests).map((historicalTest) -> {
         return createTestInstanceDetails(historicalTest.timestamp, historicalTest.health, historicalTest.testDetails, serverGuid);
      }).collect(Collectors.toList()));
   }

   private static VsanTestInstanceDetails createTestInstanceDetails(Calendar timestamp, String health, VsanClusterHealthResultBase[] testDetails, String serverGuid) {
      VsanTestInstanceDetails result = new VsanTestInstanceDetails();
      result.timestamp = timestamp;
      result.status = VsanHealthStatus.valueOf(health);
      result.details = createTestTables(testDetails, serverGuid);
      return result;
   }

   public static List createTestTables(VsanClusterHealthResultBase[] tableResults, String serverGuid) {
      if (ArrayUtils.isEmpty(tableResults)) {
         return new ArrayList();
      } else {
         Set allMoRefs = new HashSet();
         addToTestMoRefsFromBaseResults(tableResults, allMoRefs, serverGuid);
         Map namesForMoRefs = getNamesForMoRefs(allMoRefs);
         return (List)Arrays.stream(tableResults).map((testDetail) -> {
            return createTestTable((VsanClusterHealthResultTable)testDetail, namesForMoRefs);
         }).collect(Collectors.toList());
      }
   }

   public static VsanTestTable createTestTable(VsanClusterHealthResultTable tableResult, Map moRefToNameMap) {
      return createTestTable(tableResult.label, tableResult.columns, tableResult.rows, moRefToNameMap);
   }

   private static VsanTestTable createTestTable(String title, VsanClusterHealthResultColumnInfo[] rowColumns, VsanClusterHealthResultRow[] rows, Map moRefToNameMap) {
      VsanTestTable testTable = new VsanTestTable();
      testTable.showHeader = true;
      testTable.title = title;
      VsanTestColumn[] columns = testTable.columns = createTestColumns(rowColumns);
      if (rows != null) {
         testTable.rows = (VsanTestRow[])Arrays.stream(rows).map((row) -> {
            return createVsanTestRow(row, columns, moRefToNameMap);
         }).toArray((x$0) -> {
            return new VsanTestRow[x$0];
         });
      }

      return testTable;
   }

   private static VsanTestColumn[] createTestColumns(VsanClusterHealthResultColumnInfo[] sourceColumns) {
      return ArrayUtils.isEmpty(sourceColumns) ? null : (VsanTestColumn[])Arrays.stream(sourceColumns).map((sourceColumn) -> {
         try {
            ColumnType columnType = (ColumnType)EnumUtils.fromStringIgnoreCase(ColumnType.class, sourceColumn.type, ColumnType.unknown);
            return new VsanTestColumn(sourceColumn.label, columnType);
         } catch (Exception var2) {
            _logger.warn("Unable to get ColumnType for " + sourceColumn.type);
            return null;
         }
      }).filter(Objects::nonNull).toArray((x$0) -> {
         return new VsanTestColumn[x$0];
      });
   }

   public static VsanTestRow createVsanTestRow(VsanClusterHealthResultRow resultRow, VsanTestColumn[] columns, Map moRefToNameMap) {
      VsanTestRow row = new VsanTestRow();
      row.rowValues = createVsanTestCells(resultRow.values, columns, moRefToNameMap);
      row.nestedRows = getNestedRows(resultRow, columns, moRefToNameMap);
      return row;
   }

   private static VsanTestCell[] createVsanTestCells(String[] resultRowValues, VsanTestColumn[] columns, Map moRefToNameMap) {
      if (ArrayUtils.isEmpty(resultRowValues)) {
         return null;
      } else {
         String serverGuid = getServerGuid(moRefToNameMap);
         VsanTestCell[] values = new VsanTestCell[resultRowValues.length];

         for(int i = 0; i < resultRowValues.length; ++i) {
            String rowValue = resultRowValues[i];
            if (StringUtils.isEmpty(rowValue)) {
               values[i] = new VsanTestCell();
            } else {
               ColumnType cellType = columns[i].columnType;

               try {
                  values[i] = createVsanTestCell(cellType, rowValue, moRefToNameMap, serverGuid);
               } catch (Exception var9) {
                  _logger.warn("Unable to resolve the column value " + rowValue + ". Skipping it.", var9);
               }
            }
         }

         return values;
      }
   }

   private static VsanTestCell createVsanTestCell(ColumnType cellType, String rowValue, Map moRefToNameMap, String serverGuid) {
      Object cellValue = null;
      switch(cellType) {
      case mor:
         if (rowValue != null && rowValue.length() > 0 && serverGuid != null) {
            cellValue = getObjectWithName(rowValue, moRefToNameMap, serverGuid);
         }
         break;
      case listMor:
         cellValue = parseList(rowValue, serverGuid, (moRefStr) -> {
            return getObjectWithName(moRefStr, moRefToNameMap, serverGuid);
         });
         break;
      case listString:
         cellValue = parseList(rowValue, serverGuid, (str) -> {
            return str;
         });
         break;
      case listFloat:
         cellValue = parseList(rowValue, serverGuid, Float::parseFloat);
         break;
      case listLong:
         cellValue = parseList(rowValue, serverGuid, Long::parseLong);
         break;
      case health:
         cellValue = VsanHealthStatus.valueOf(rowValue);
         break;
      case vsanObjectHealth:
         cellValue = VsanObjectHealthState.fromServerLocalizedString(rowValue);
         break;
      case pspHealth:
         cellValue = PspObjectHealthState.fromServerLocalizedString(rowValue);
         break;
      case vsanObjectHealthv2:
         cellValue = createCompositeHealthValue(rowValue);
         break;
      case dynamic:
         cellType = (ColumnType)EnumUtils.fromStringIgnoreCase(ColumnType.class, rowValue.split(":")[0], ColumnType.unknown);
         rowValue = rowValue.substring(cellType.toString().length() + 1);
         return createVsanTestCell(cellType, rowValue, moRefToNameMap, serverGuid);
      default:
         cellValue = rowValue;
      }

      return new VsanTestCell(cellType, cellValue);
   }

   private static Object parseList(String rowValue, String serverGuid, Function stringParseFunc) {
      return rowValue != null && rowValue.length() != 0 && serverGuid != null ? Arrays.stream(rowValue.split(",")).map(stringParseFunc).collect(Collectors.toList()) : null;
   }

   private static String getServerGuid(Map moRefToNameMap) {
      ManagedObjectReference moRef = (ManagedObjectReference)BaseUtils.getMapNextKey(moRefToNameMap);
      return moRef == null ? null : moRef.getServerGuid();
   }

   private static ObjectWithName getObjectWithName(String morString, Map moRefToNameMap, String serverGuid) {
      ManagedObjectReference moRef = BaseUtils.generateMor(morString, serverGuid);
      String name = (String)moRefToNameMap.get(moRef);
      return new ObjectWithName(name, moRef);
   }

   private static List getNestedRows(VsanClusterHealthResultRow resultRow, VsanTestColumn[] columns, Map moRefToNameMap) {
      return (List)((Stream)Optional.ofNullable(resultRow.nestedRows).map((nestedRows) -> {
         return Arrays.stream(nestedRows);
      }).orElseGet(Stream::empty)).map((nestedRow) -> {
         return createVsanTestRow(nestedRow, columns, moRefToNameMap);
      }).collect(Collectors.toList());
   }

   private static VsanObjectCompositeHealth createCompositeHealthValue(String rowValue) {
      return new VsanObjectCompositeHealth(buildHealthStateV2(rowValue));
   }

   private static VsanObjectHealthStateV2 buildHealthStateV2(String rowValues) {
      VsanObjectHealthStateV2 result = new VsanObjectHealthStateV2();
      if (StringUtils.isBlank(rowValues)) {
         return result;
      } else {
         Arrays.stream(rowValues.split(",")).forEach((stateValue) -> {
            String[] stateValuePair = stateValue.trim().split(":");
            if (!ArrayUtils.isEmpty(stateValuePair) && stateValuePair.length == 2) {
               String state = stateValuePair[0].trim();
               String value = stateValuePair[1].trim();
               if (!StringUtils.isBlank(state) && !StringUtils.isBlank(value)) {
                  try {
                     Utils.setFieldValueCaseInsensitive(result, state, value);
                  } catch (NoSuchFieldException var6) {
                     _logger.error("Can not find state with name [" + state + "]", var6);
                  } catch (IllegalAccessException var7) {
                     _logger.error(String.format("Can not set value [%s] to state [%s]", value, state), var7);
                  }

               } else {
                  _logger.error(String.format("Unable to parse health state and value [%s]", stateValue));
               }
            } else {
               _logger.error("Not expected empty value for column of type vsanObjectHealthv2.");
            }
         });
         return result;
      }
   }
}
