package com.vmware.vsan.client.services.common.data;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vim.KeyValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;

@TsModel
public class LabelData {
   public String key;
   public String value;

   public static List fromKeyValue(KeyValue[] keyValues) {
      List result = new ArrayList();
      if (ArrayUtils.isEmpty(keyValues)) {
         return result;
      } else {
         KeyValue[] var2 = keyValues;
         int var3 = keyValues.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            KeyValue keyValue = var2[var4];
            LabelData label = new LabelData();
            label.key = keyValue.key;
            label.value = keyValue.value;
            result.add(label);
         }

         return result;
      }
   }

   public static List fromKeyValue(List keyValues) {
      return keyValues != null && !keyValues.isEmpty() ? fromKeyValue((KeyValue[])keyValues.toArray(new KeyValue[0])) : Collections.EMPTY_LIST;
   }

   public static KeyValue toKeyValue(LabelData label) {
      return new KeyValue(label.key, label.value);
   }

   public static KeyValue[] toKeyValue(LabelData[] labels) {
      List result = new ArrayList();
      LabelData[] var2 = labels;
      int var3 = labels.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         LabelData label = var2[var4];
         result.add(toKeyValue(label));
      }

      return (KeyValue[])result.toArray(new KeyValue[0]);
   }
}
