package com.vmware.vsan.client.util.dataservice.query;

import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vise.data.query.Conjoiner;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;

public class FromStatement extends FinalStatement {
   FromStatement(QueryBuilder queryBuilder, String type) {
      super(queryBuilder);
      Validate.notEmpty(type);
      this.addTable(type);
   }

   FromStatement(QueryBuilder queryBuilder, Class clazz) {
      super(queryBuilder);
      Validate.notNull(clazz);
      this.addTable(clazz.getSimpleName());
   }

   FromStatement(QueryBuilder queryBuilder, ManagedObjectReference moRef) {
      super(queryBuilder);
      Validate.notNull(moRef);
      this.addTable(moRef.getType(), ConstraintUtils.createObjectIdentityConstraint(moRef.getType(), moRef));
   }

   FromStatement(QueryBuilder queryBuilder, Collection moRefs) {
      super(queryBuilder);
      Validate.notEmpty(moRefs);
      String type = ((ManagedObjectReference)moRefs.iterator().next()).getType();
      List constraints = (List)moRefs.stream().map((moRef) -> {
         return ConstraintUtils.createObjectIdentityConstraint(moRef.getType(), moRef);
      }).collect(Collectors.toList());
      this.addTable(type, ConstraintUtils.createCompositeConstraint(Conjoiner.OR, (Collection)constraints));
   }

   public WhereStatement where() {
      return new WhereStatement(this.queryBuilder);
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

   public LimitStatement limit(int maxCount) {
      return new LimitStatement(this.queryBuilder, 0, maxCount);
   }

   public JoinStatement join(String type) {
      return new JoinStatement(this.queryBuilder, type);
   }

   public JoinStatement join(Class clazz) {
      return new JoinStatement(this.queryBuilder, clazz);
   }
}
