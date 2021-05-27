package com.vmware.vsphere.client.vsan.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.vim.binding.vmodl.MethodFault;
import com.vmware.vim.binding.vmodl.RuntimeFault;
import com.vmware.vsan.client.services.async.AsyncUserSessionService;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.RecursiveToStringStyle;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {
   private static final Logger _logger = LoggerFactory.getLogger(Utils.class);
   private static final String STR_NULL = "null";
   private static MessageBundle MESSAGE_BUNDLE;
   private static AsyncUserSessionService USER_SESSION_SERVICE;

   public static void setMessageBundle(MessageBundle messageBundle) {
      MESSAGE_BUNDLE = messageBundle;
   }

   public static void setUserSessionService(AsyncUserSessionService userSessionService) {
      USER_SESSION_SERVICE = userSessionService;
   }

   public static String getLocalizedString(String key) {
      return MESSAGE_BUNDLE.string(key);
   }

   public static String getLocalizedString(String key, String... params) {
      return MESSAGE_BUNDLE.string(key, params);
   }

   public static MethodFault getMethodFault(Throwable e) {
      if (e == null) {
         return null;
      } else if (e instanceof MethodFault) {
         return (MethodFault)e;
      } else {
         MethodFault methodFault = new MethodFault();
         methodFault.setMessage(e.getMessage());
         methodFault.initCause(e);
         if (e instanceof RuntimeFault) {
            methodFault.setFaultCause((RuntimeFault)e);
         }

         return methodFault;
      }
   }

   public static List arrayToList(Object... array) {
      return array != null ? Arrays.asList(array) : Collections.EMPTY_LIST;
   }

   public static JsonNode getJsonRootNode(String jsonStr) {
      if (StringUtils.isEmpty(jsonStr)) {
         return null;
      } else {
         ObjectMapper mapper = new ObjectMapper();
         JsonNode rootNode = null;

         try {
            rootNode = mapper.readTree(jsonStr);
         } catch (Exception var4) {
         }

         return rootNode;
      }
   }

   public static String toString(Object o) {
      return o != null ? ToStringBuilder.reflectionToString(o, new RecursiveToStringStyle()) : "null";
   }

   public static Locale getCurrentLocale() {
      Locale locale = Locale.US;

      try {
         String languageTag = USER_SESSION_SERVICE.getUserSession().locale.replaceAll("_", "-");
         locale = Locale.forLanguageTag(languageTag);
      } catch (Throwable var2) {
         _logger.error("Cannot determine current locale, fallback to default: {}", locale, var2);
      }

      return locale;
   }

   public static void setFieldValueCaseInsensitive(Object obj, String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
      if (obj == null) {
         throw new IllegalArgumentException("Obj value is required");
      } else {
         Field[] publicFields = obj.getClass().getFields();
         if (ArrayUtils.isEmpty(publicFields)) {
            throw new NoSuchFieldException();
         } else {
            Optional foundFieldOpt = Arrays.stream(publicFields).filter((field) -> {
               return field.getName().equalsIgnoreCase(fieldName);
            }).findFirst();
            if (!foundFieldOpt.isPresent()) {
               throw new NoSuchFieldException();
            } else {
               ((Field)foundFieldOpt.get()).set(obj, value);
            }
         }
      }
   }
}
