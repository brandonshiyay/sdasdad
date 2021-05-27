package com.vmware.vsan.client.util.dataservice.query;

import com.vmware.vise.data.query.Conjoiner;

abstract class ConditionStatement extends FinalStatement {
   ConditionStatement(QueryBuilder queryBuilder) {
      super(queryBuilder);
   }

   public WhereStatement and() {
      return new WhereStatement(this.queryBuilder, Conjoiner.AND);
   }

   public WhereStatement or() {
      return new WhereStatement(this.queryBuilder, Conjoiner.OR);
   }

   public OrderByStatement orderBy(String property) {
      return new OrderByStatement(this.queryBuilder, property, true);
   }

   public OrderByStatement orderBy(String property, boolean ascending) {
      return new OrderByStatement(this.queryBuilder, property, ascending);
   }

   public LimitStatement limit(int from, int maxCount) {
      return new LimitStatement(this.queryBuilder, from, maxCount);
   }

   public LimitStatement limit(int from) {
      return new LimitStatement(this.queryBuilder, from, (Integer)null);
   }

   public JoinStatement join(String type) {
      return new JoinStatement(this.queryBuilder, type);
   }

   public JoinStatement join(Class clazz) {
      return new JoinStatement(this.queryBuilder, clazz);
   }
}
