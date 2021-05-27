package com.vmware.vsan.client.util.dataservice.query;

import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vise.data.Constraint;
import com.vmware.vise.data.query.Comparator;
import com.vmware.vise.data.query.PropertyConstraint;
import org.apache.commons.lang3.Validate;

public class PropertyConditionStatement extends ConditionStatement {
   PropertyConditionStatement(QueryBuilder queryBuilder, String property, Object value, Comparator comparator) {
      super(queryBuilder);
      Validate.notEmpty(property);
      Validate.notNull(value);
      Validate.notNull(comparator);
      Object strValue;
      if (value instanceof ManagedObjectReference) {
         ManagedObjectReference moRef = (ManagedObjectReference)value;
         strValue = String.format("%s:%s", moRef.getValue(), moRef.getServerGuid());
      } else if (!(value instanceof String) && !(value instanceof String[])) {
         strValue = value.toString();
      } else {
         strValue = value;
      }

      PropertyConstraint constraint = new PropertyConstraint();
      constraint.targetType = this.getActiveTable().type;
      constraint.propertyName = property;
      constraint.comparableValue = strValue;
      constraint.comparator = comparator;
      Constraint combinedConstraint = ConstraintUtils.combineConstraints(this.getActiveTable().constraint, constraint, this.getActiveTable().tmpConjoiner);
      this.getActiveTable().constraint = combinedConstraint;
   }
}
