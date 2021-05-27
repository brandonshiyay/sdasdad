package com.vmware.vsan.client.util.dataservice.query;

import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vise.data.query.Comparator;
import com.vmware.vise.data.query.Conjoiner;
import java.net.URI;
import java.util.Collection;

public class WhereStatement extends AbstractStatement {
   WhereStatement(QueryBuilder queryBuilder) {
      super(queryBuilder);
   }

   WhereStatement(QueryBuilder queryBuilder, Conjoiner conjoiner) {
      super(queryBuilder);
      this.getActiveTable().tmpConjoiner = conjoiner;
   }

   public IdConditionStatement idEquals(String id, String serverGuid) {
      return new IdConditionStatement(this.queryBuilder, id, serverGuid);
   }

   public IdConditionStatement idEquals(URI uri) {
      return new IdConditionStatement(this.queryBuilder, uri);
   }

   public IdConditionStatement idEquals(ManagedObjectReference moRef) {
      return new IdConditionStatement(this.queryBuilder, moRef);
   }

   public PropertyConditionStatement propertyEquals(String property, Object value) {
      return new PropertyConditionStatement(this.queryBuilder, property, value, Comparator.EQUALS);
   }

   public PropertyConditionStatement propertyIsGreaterOrEquals(String property, int value) {
      return new PropertyConditionStatement(this.queryBuilder, property, value, Comparator.GREATER_OR_EQUALS);
   }

   public PropertyConditionStatement propertyContains(String property, String value) {
      return new PropertyConditionStatement(this.queryBuilder, property, value, Comparator.CONTAINS);
   }

   public PropertyConditionStatement propertyEqualsAnyOf(String property, String[] value) {
      return new PropertyConditionStatement(this.queryBuilder, property, value, Comparator.EQUALS_ANY_OF);
   }

   public PropertyConditionStatement propertyEqualsAnyOf(String property, Collection value) {
      return new PropertyConditionStatement(this.queryBuilder, property, value.toArray(new String[0]), Comparator.EQUALS_ANY_OF);
   }
}
