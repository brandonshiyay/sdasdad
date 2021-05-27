package com.vmware.vsan.client.services;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vmodl.LocalizableMessage;
import com.vmware.vim.binding.vmodl.MethodFault;
import com.vmware.vim.binding.vmodl.RuntimeFault;
import com.vmware.vim.vmomi.client.common.UnexpectedStatusCodeException;
import com.vmware.vise.data.query.DataException;
import com.vmware.vsphere.client.vsan.util.MessageBundle;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping({"/proxy"})
public class ProxygenController extends RestControllerBase {
   private static final Logger logger = LoggerFactory.getLogger(ProxygenController.class);
   @Autowired
   private BeanFactory beanFactory;
   @Autowired
   private MessageBundle messages;

   @RequestMapping(
      value = {"/service/{beanIdOrClassName}/{methodName}"},
      method = {RequestMethod.POST},
      consumes = {"application/json"},
      produces = {"application/json"}
   )
   @ResponseBody
   public Object invokeServiceWithJson(@PathVariable("beanIdOrClassName") String beanIdOrClassName, @PathVariable("methodName") String methodName, @RequestBody Map body) throws Exception {
      List rawData = null;

      try {
         rawData = (List)body.get("methodInput");
      } catch (Exception var6) {
         logger.error("service method failed to extract input data", var6);
         return this.handleException(var6);
      }

      return this.invokeService(beanIdOrClassName, methodName, (MultipartFile[])null, rawData);
   }

   @RequestMapping(
      value = {"/service/{beanIdOrClassName}/{methodName}"},
      method = {RequestMethod.POST},
      consumes = {"multipart/form-data"},
      produces = {"application/json"}
   )
   @ResponseBody
   public Object invokeServiceWithMultipartFormData(@PathVariable("beanIdOrClassName") String beanIdOrClassName, @PathVariable("methodName") String methodName, @RequestParam("file") MultipartFile[] files, @RequestParam("methodInput") String rawData) throws Exception {
      List data = null;

      try {
         Gson gson = new Gson();
         data = (List)gson.fromJson(rawData, List.class);
      } catch (Exception var7) {
         logger.error("service method failed to extract input data", var7);
         return this.handleException(var7);
      }

      return this.invokeService(beanIdOrClassName, methodName, files, data);
   }

   private Object invokeService(String beanIdOrClassName, String methodName, MultipartFile[] files, List data) throws Exception {
      try {
         Object bean = null;
         String beanName = null;
         Class beanClass = null;

         try {
            beanClass = Class.forName(beanIdOrClassName);
            beanName = StringUtils.uncapitalize(beanClass.getSimpleName());
         } catch (ClassNotFoundException var18) {
            beanName = beanIdOrClassName;
         }

         try {
            bean = this.beanFactory.getBean(beanName);
         } catch (BeansException var17) {
            bean = this.beanFactory.getBean(beanClass);
         }

         int parametersCount = CollectionUtils.size(data) + (files != null ? 1 : 0);
         Method[] var9 = bean.getClass().getMethods();
         int var10 = var9.length;

         for(int var11 = 0; var11 < var10; ++var11) {
            Method method = var9[var11];
            if (method.getName().equals(methodName) && method.getParameterCount() == parametersCount && method.isAnnotationPresent(TsService.class)) {
               ProxygenSerializer serializer = new ProxygenSerializer();
               Object[] methodInput = serializer.deserializeMethodInput(data, files, method);
               Object result = method.invoke(bean, methodInput);
               Map map = new HashMap();
               map.put("result", serializer.serialize(result));
               return map;
            }
         }
      } catch (Exception var19) {
         logger.error("service method failed to invoke", var19);
         return this.handleException(var19);
      }

      logger.error("service method not found: " + methodName + " @ " + beanIdOrClassName);
      return this.handleException((Throwable)null);
   }

   private Object handleException(Throwable t) {
      if (t instanceof InvocationTargetException) {
         return this.handleException(((InvocationTargetException)t).getTargetException());
      } else if (t instanceof ExecutionException && t.getCause() != t) {
         return this.handleException(t.getCause());
      } else if (t instanceof DataException && t.getCause() != t) {
         return this.handleException(t.getCause());
      } else if (t instanceof UnexpectedStatusCodeException) {
         return ImmutableMap.of("error", this.messages.string("util.dataservice.notRespondingFault"));
      } else if (t instanceof BackendLocalizedException) {
         return ImmutableMap.of("error", t.getLocalizedMessage());
      } else if (t instanceof VsanUiLocalizableException) {
         VsanUiLocalizableException localizableException = (VsanUiLocalizableException)t;
         return ImmutableMap.of("error", this.messages.string(localizableException.getErrorKey(), localizableException.getParams()));
      } else {
         LocalizableMessage[] faultMessage = null;
         String vmodlMessage = null;
         if (t instanceof MethodFault) {
            faultMessage = ((MethodFault)t).getFaultMessage();
            vmodlMessage = ((MethodFault)t).getMessage();
         } else if (t instanceof RuntimeFault) {
            faultMessage = ((RuntimeFault)t).getFaultMessage();
            vmodlMessage = ((RuntimeFault)t).getMessage();
         }

         if (faultMessage != null) {
            LocalizableMessage[] var4 = faultMessage;
            int var5 = faultMessage.length;

            for(int var6 = 0; var6 < var5; ++var6) {
               LocalizableMessage localizable = var4[var6];
               if (localizable.getMessage() != null && !localizable.getMessage().isEmpty()) {
                  return ImmutableMap.of("error", this.localizeFault(localizable.getMessage()));
               }

               if (localizable.getKey() != null && !localizable.getKey().isEmpty()) {
                  return ImmutableMap.of("error", this.localizeFault(localizable.getKey()));
               }
            }
         }

         return StringUtils.isNotBlank(vmodlMessage) ? ImmutableMap.of("error", vmodlMessage) : ImmutableMap.of("error", this.messages.string("vsan.common.generic.error"));
      }
   }

   private String localizeFault(String key) {
      return key;
   }
}
