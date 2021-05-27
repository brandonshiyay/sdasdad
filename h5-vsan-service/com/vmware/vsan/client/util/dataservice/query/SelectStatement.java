package com.vmware.vsan.client.util.dataservice.query;

import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.apache.commons.lang3.Validate;

public class SelectStatement extends AbstractStatement {
   SelectStatement(QueryBuilder queryBuilder, List properties) {
      super(queryBuilder);
      Validate.noNullElements(properties);
      this.getContext().properties = properties;
   }

   public FromStatement from(String type) {
      return new FromStatement(this.queryBuilder, type);
   }

   public FromStatement from(Class clazz) {
      return new FromStatement(this.queryBuilder, clazz);
   }

   public FromStatement from(ManagedObjectReference moRef) {
      return new FromStatement(this.queryBuilder, moRef);
   }

   public FromStatement from(Collection moRefs) {
      return new FromStatement(this.queryBuilder, moRefs);
   }

   public FromStatement from(ManagedObjectReference... moRefs) {
      return this.from((Collection)Arrays.asList(moRefs));
   }
}
