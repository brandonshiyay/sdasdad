package com.vmware.vsphere.client.vsan.health;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.vsan.binding.vim.cluster.VsanClusterHealthAction;
import com.vmware.vim.vsan.binding.vim.cluster.VsanClusterHealthGroup;
import com.vmware.vim.vsan.binding.vim.cluster.VsanClusterHealthResultBase;
import com.vmware.vim.vsan.binding.vim.cluster.VsanClusterHealthResultTable;
import com.vmware.vim.vsan.binding.vim.cluster.VsanClusterHealthResultValues;
import com.vmware.vim.vsan.binding.vim.cluster.VsanClusterHealthTest;
import com.vmware.vsphere.client.vsan.health.util.VsanHealthUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

@TsModel
public class VsanTestData {
   private static final String TEST_ID_SEPARATOR = ".";
   public String testId;
   public String silenceTestId;
   public String testName;
   public boolean inProgress;
   public String testDescription;
   public String testShortDescription;
   public Integer numberOfHealthyEntities;
   public Integer numberOfAllEntities;
   public VsanHealthStatus status;
   /** @deprecated */
   @Deprecated
   public List details;
   public List instances;
   public List subtests;
   public String helpId;
   public List actions;

   public VsanTestData() {
   }

   public VsanTestData(VsanClusterHealthGroup healthGroup, Calendar timeStamp, Map moRefToNameMap, boolean isVsphereHealth) {
      this.testId = healthGroup.groupId;
      this.testName = healthGroup.groupName;
      this.inProgress = BooleanUtils.isTrue(healthGroup.inProgress);
      VsanHealthStatus testStatus = VsanHealthStatus.valueOf(healthGroup.groupHealth);
      this.status = testStatus;
      this.details = this.getTestDetails(healthGroup.groupDetails, moRefToNameMap);
      this.instances = this.getTestInstancesDetails(healthGroup.groupDetails, timeStamp, testStatus, moRefToNameMap);
      this.subtests = new ArrayList();
      if (healthGroup.groupTests != null) {
         VsanClusterHealthTest[] var6 = healthGroup.groupTests;
         int var7 = var6.length;

         for(int var8 = 0; var8 < var7; ++var8) {
            VsanClusterHealthTest test = var6[var8];
            this.subtests.add(new VsanTestData(test, timeStamp, moRefToNameMap, true, isVsphereHealth));
         }
      }

   }

   public VsanTestData(VsanClusterHealthTest test, Calendar timeStamp, Map moRefToNameMap, boolean initializeInstances, boolean isVsphereHealth) {
      this.testId = test.testId;
      this.silenceTestId = isVsphereHealth ? test.testId : this.getShortenedId(test.testId);
      this.testName = test.testName;
      this.testDescription = test.testDescription;
      this.testShortDescription = test.testShortDescription;
      this.numberOfHealthyEntities = test.testHealthyEntities;
      this.numberOfAllEntities = test.testAllEntities;
      VsanHealthStatus healthStatus = VsanHealthStatus.valueOf(test.testHealth);
      this.status = healthStatus;
      this.details = this.getTestDetails(test.testDetails, moRefToNameMap);
      if (initializeInstances) {
         this.instances = this.getTestInstancesDetails(test.testDetails, timeStamp, healthStatus, moRefToNameMap);
      }

      this.helpId = test.testId;
      if (test.testActions != null) {
         this.actions = new ArrayList(test.testActions.length);
         VsanClusterHealthAction[] var7 = test.testActions;
         int var8 = var7.length;

         for(int var9 = 0; var9 < var8; ++var9) {
            VsanClusterHealthAction vlha = var7[var9];
            this.actions.add(vlha);
         }
      }

   }

   private List getTestInstancesDetails(VsanClusterHealthResultBase[] testDetails, Calendar timeStamp, VsanHealthStatus testStatus, Map moRefToNameMap) {
      List result = new ArrayList();
      VsanTestInstanceDetails instance = new VsanTestInstanceDetails();
      result.add(instance);
      instance.timestamp = timeStamp;
      instance.status = testStatus;
      if (testDetails == null) {
         return result;
      } else {
         instance.details = (List)Arrays.stream(testDetails).map((detail) -> {
            if (detail instanceof VsanClusterHealthResultTable) {
               return VsanHealthUtil.createTestTable((VsanClusterHealthResultTable)detail, moRefToNameMap);
            } else {
               return detail instanceof VsanClusterHealthResultValues ? this.createTestTable((VsanClusterHealthResultValues)detail) : null;
            }
         }).filter((table) -> {
            return table != null;
         }).collect(Collectors.toList());
         return result;
      }
   }

   private List getTestDetails(VsanClusterHealthResultBase[] testDetails, Map moRefToNameMap) {
      if (testDetails == null) {
         return new ArrayList();
      } else {
         List testTables = new ArrayList();
         VsanClusterHealthResultBase[] var4 = testDetails;
         int var5 = testDetails.length;

         for(int var6 = 0; var6 < var5; ++var6) {
            VsanClusterHealthResultBase baseResult = var4[var6];
            VsanTestTable testTable = null;
            if (baseResult instanceof VsanClusterHealthResultTable) {
               testTable = VsanHealthUtil.createTestTable((VsanClusterHealthResultTable)baseResult, moRefToNameMap);
            } else if (baseResult instanceof VsanClusterHealthResultValues) {
               testTable = this.createTestTable((VsanClusterHealthResultValues)baseResult);
            }

            testTables.add(testTable);
         }

         return testTables;
      }
   }

   private VsanTestTable createTestTable(VsanClusterHealthResultValues parameters) {
      VsanTestTable testTable = new VsanTestTable();
      testTable.showHeader = false;
      testTable.title = parameters.label;
      VsanTestColumn column = new VsanTestColumn("", ColumnType.string);
      testTable.columns = new VsanTestColumn[]{column};
      List rows = new ArrayList();
      if (parameters.values == null) {
         return testTable;
      } else {
         String[] var5 = parameters.values;
         int var6 = var5.length;

         for(int var7 = 0; var7 < var6; ++var7) {
            String parameter = var5[var7];
            rows.add(this.createVsanTestRow(parameter));
         }

         testTable.rows = (VsanTestRow[])rows.toArray(new VsanTestRow[rows.size()]);
         return testTable;
      }
   }

   private VsanTestRow createVsanTestRow(String value) {
      VsanTestRow row = new VsanTestRow();
      VsanTestCell cell = new VsanTestCell(ColumnType.string, value);
      row.rowValues = new VsanTestCell[]{cell};
      return row;
   }

   private String getShortenedId(String testId) {
      return StringUtils.isBlank(testId) ? testId : testId.substring(testId.lastIndexOf(".") + 1);
   }
}
