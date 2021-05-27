package com.vmware.vsan.client.util.dataservice.query;

import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vise.data.Constraint;
import com.vmware.vise.data.query.Comparator;
import com.vmware.vise.data.query.CompositeConstraint;
import com.vmware.vise.data.query.Conjoiner;
import com.vmware.vise.data.query.ObjectIdentityConstraint;
import com.vmware.vise.data.query.PropertyConstraint;
import com.vmware.vise.data.query.RelationalConstraint;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class ConstraintUtils {
   private static final Log logger = LogFactory.getLog(ConstraintUtils.class);

   public static ObjectIdentityConstraint createObjectIdentityConstraint(String type, Object target) {
      if (target instanceof ManagedObjectReference) {
         ManagedObjectReference moRef = (ManagedObjectReference)target;
         if (moRef.getServerGuid().isEmpty()) {
            logger.warn("An objectIdentityConstraint is created for moRef: " + moRef.getValue() + " without serverGuid. This will bring no result in multiVC env");
         }
      }

      ObjectIdentityConstraint objectIdentityConstraint = new ObjectIdentityConstraint();
      objectIdentityConstraint.targetType = type;
      objectIdentityConstraint.target = target;
      return objectIdentityConstraint;
   }

   public static CompositeConstraint createCompositeConstraint(Conjoiner conjoiner, Constraint... constraints) {
      CompositeConstraint objectIdentityConstraint = new CompositeConstraint();
      objectIdentityConstraint.conjoiner = conjoiner;
      objectIdentityConstraint.nestedConstraints = constraints;
      return objectIdentityConstraint;
   }

   public static CompositeConstraint createCompositeConstraint(Conjoiner conjoiner, Collection constraints) {
      return createCompositeConstraint(conjoiner, (Constraint[])constraints.toArray(new Constraint[0]));
   }

   public static RelationalConstraint createRelationalConstraint(Constraint originalConstraint, String type, String relation) {
      RelationalConstraint relationalConstraint = new RelationalConstraint();
      relationalConstraint.hasInverseRelation = true;
      relationalConstraint.relation = relation;
      relationalConstraint.targetType = type;
      relationalConstraint.constraintOnRelatedObject = originalConstraint;
      return relationalConstraint;
   }

   public static Constraint combineConstraints(Constraint originalConstraint, Constraint newConstraint, Conjoiner conjoiner) {
      return logicallyCombineConstraints(originalConstraint, newConstraint, conjoiner);
   }

   private static Constraint logicallyCombineConstraints(Constraint originalConstraint, Constraint newConstraint, Conjoiner conjoiner) {
      Constraint result = originalConstraint;
      if (originalConstraint == null) {
         result = newConstraint;
      } else if (originalConstraint instanceof CompositeConstraint) {
         CompositeConstraint cc = (CompositeConstraint)originalConstraint;
         if (cc.conjoiner == conjoiner) {
            extendCompositeConstraint(cc, newConstraint);
         } else if (cc.conjoiner == Conjoiner.OR) {
            List nestedConstraints = new ArrayList(Arrays.asList(cc.nestedConstraints));
            Constraint lastNestedConstraint = (Constraint)nestedConstraints.get(nestedConstraints.size() - 1);
            if (lastNestedConstraint instanceof CompositeConstraint) {
               extendCompositeConstraint((CompositeConstraint)lastNestedConstraint, newConstraint);
            } else {
               CompositeConstraint compositeConstraint = createCompositeConstraint(conjoiner, lastNestedConstraint, newConstraint);
               nestedConstraints.remove(lastNestedConstraint);
               nestedConstraints.add(compositeConstraint);
               cc.nestedConstraints = (Constraint[])nestedConstraints.toArray(new Constraint[0]);
            }
         } else if (cc.conjoiner == Conjoiner.AND) {
            result = createCompositeConstraint(conjoiner, originalConstraint, newConstraint);
         }
      } else {
         result = createCompositeConstraint(conjoiner, originalConstraint, newConstraint);
      }

      return (Constraint)result;
   }

   public static Constraint convertConstraint(Constraint constraint) {
      if (constraint instanceof CompositeConstraint) {
         if (Arrays.stream(((CompositeConstraint)constraint).nestedConstraints).anyMatch((nestedConstraint) -> {
            return nestedConstraint instanceof CompositeConstraint;
         })) {
            Arrays.stream(((CompositeConstraint)constraint).nestedConstraints).forEach((nestedConstraint) -> {
               convertConstraint(nestedConstraint);
            });
         } else {
            ((CompositeConstraint)constraint).nestedConstraints = (Constraint[])Arrays.stream(((CompositeConstraint)constraint).nestedConstraints).map((propConstraint) -> {
               return convertEqualsAnyOfConstraintToCompositeConstraint(propConstraint);
            }).toArray((x$0) -> {
               return new Constraint[x$0];
            });
         }
      } else if (constraint instanceof PropertyConstraint) {
         return convertEqualsAnyOfConstraintToCompositeConstraint(constraint);
      }

      return constraint;
   }

   private static Constraint convertEqualsAnyOfConstraintToCompositeConstraint(Constraint propConstraint) {
      if (propConstraint instanceof PropertyConstraint && ((PropertyConstraint)propConstraint).comparator.equals(Comparator.EQUALS_ANY_OF)) {
         List constraints = new ArrayList();
         String[] var2 = (String[])((String[])((PropertyConstraint)propConstraint).comparableValue);
         int var3 = var2.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            String val = var2[var4];
            PropertyConstraint pConstraint = new PropertyConstraint();
            pConstraint.targetType = propConstraint.targetType;
            pConstraint.propertyName = ((PropertyConstraint)propConstraint).propertyName;
            pConstraint.comparableValue = val;
            pConstraint.comparator = Comparator.EQUALS;
            constraints.add(pConstraint);
         }

         Constraint combinedPropertyConstraint = createCompositeConstraint(Conjoiner.OR, (Collection)constraints);
         return combinedPropertyConstraint;
      } else {
         return propConstraint;
      }
   }

   private static void extendCompositeConstraint(CompositeConstraint compositeConstraint, Constraint newConstraint) {
      List constraints = new ArrayList(Arrays.asList(compositeConstraint.nestedConstraints));
      List allConstraints = new ArrayList();
      allConstraints.addAll(constraints);
      allConstraints.add(newConstraint);
      compositeConstraint.nestedConstraints = (Constraint[])allConstraints.toArray(new Constraint[0]);
   }
}
