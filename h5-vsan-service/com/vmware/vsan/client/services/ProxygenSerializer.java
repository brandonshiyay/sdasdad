package com.vmware.vsan.client.services;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.binding.vmodl.data;
import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

public class ProxygenSerializer {
   private static Logger logger = LoggerFactory.getLogger(ProxygenSerializer.class);

   public Object deserialize(Object data, Class type, ProxygenSerializer.ElementType metadata) throws Exception {
      try {
         if (data == null) {
            return type.isPrimitive() ? this.getPrimitiveDefaultValue(type) : null;
         } else if (Map.class.isAssignableFrom(type)) {
            return this.deserializeAsMap(data, metadata);
         } else if (List.class.isAssignableFrom(type)) {
            return this.deserializeAsList(data, metadata);
         } else if (Set.class.isAssignableFrom(type)) {
            return this.deserializeAsSet(data, metadata);
         } else if (Date.class.isAssignableFrom(type)) {
            return new Date(Long.parseLong(data.toString()));
         } else if (Calendar.class.isAssignableFrom(type)) {
            Calendar result = Calendar.getInstance();
            result.setTimeInMillis(Long.parseLong(data.toString()));
            return result;
         } else if (!type.isArray()) {
            if (Enum.class.isAssignableFrom(type)) {
               return type.getMethod("valueOf", String.class).invoke((Object)null, data.toString());
            } else {
               return data instanceof Map ? this.deserializeAsModel((Map)data, type) : this.deserializeAsPrimitive(data, type);
            }
         } else {
            if (type.getComponentType() == Byte.TYPE || type.getComponentType() == Byte.class) {
               if (data instanceof String) {
                  return ((String)data).getBytes();
               }

               if (data instanceof List) {
                  List list = (List)data;
                  byte[] result = new byte[list.size()];

                  for(int i = 0; i < list.size(); ++i) {
                     result[i] = ((Number)list.get(i)).byteValue();
                  }

                  return result;
               }
            }

            return this.deserializeAsArray((List)data, type);
         }
      } catch (Exception var7) {
         throw new IllegalStateException("Cannot deserialize as " + type + ": " + data, var7);
      }
   }

   private Object getPrimitiveDefaultValue(Class type) {
      if (type == Boolean.TYPE) {
         return false;
      } else if (type == Byte.TYPE) {
         return 0;
      } else if (type == Short.TYPE) {
         return Short.valueOf((short)0);
      } else if (type == Integer.TYPE) {
         return 0;
      } else if (type == Long.TYPE) {
         return 0L;
      } else if (type == Float.TYPE) {
         return 0.0F;
      } else {
         return type == Double.TYPE ? 0.0D : type.cast((Object)null);
      }
   }

   private Object deserializeAsPrimitive(Object data, Class type) {
      try {
         if (type != Boolean.TYPE && type != Boolean.class) {
            if (type != Byte.TYPE && type != Byte.class) {
               if (type != Short.TYPE && type != Short.class) {
                  if (type != Integer.TYPE && type != Integer.class) {
                     if (type != Long.TYPE && type != Long.class) {
                        if (type != Float.TYPE && type != Float.class) {
                           return type != Double.TYPE && type != Double.class ? type.cast(data) : ((Number)data).doubleValue();
                        } else {
                           return ((Number)data).floatValue();
                        }
                     } else {
                        return ((Number)data).longValue();
                     }
                  } else {
                     return ((Number)data).intValue();
                  }
               } else {
                  return ((Number)data).shortValue();
               }
            } else {
               return ((Number)data).byteValue();
            }
         } else {
            return Boolean.valueOf(data + "");
         }
      } catch (Exception var4) {
         throw new IllegalArgumentException(String.format("Cannot deserialize primitive %s(%s) as %s", data.getClass(), data, type), var4);
      }
   }

   private Object deserializeAsModel(Map data, Class type) throws Exception {
      Map kvPairs = data;
      Object instance = type.getConstructor().newInstance();
      Iterator var5 = data.keySet().iterator();

      while(true) {
         label49:
         while(var5.hasNext()) {
            String key = (String)var5.next();
            Method[] var7 = type.getMethods();
            int var8 = var7.length;

            int var9;
            for(var9 = 0; var9 < var8; ++var9) {
               Method setter = var7[var9];
               if (setter.getName().equals("set" + Character.toUpperCase(key.charAt(0)) + key.substring(1))) {
                  Annotation[][] annotations = setter.getParameterAnnotations();
                  if (annotations.length == 1) {
                     ProxygenSerializer.ElementType innerMeta = null;
                     Annotation[] var13 = annotations[0];
                     int var14 = var13.length;

                     for(int var15 = 0; var15 < var14; ++var15) {
                        Annotation a = var13[var15];
                        if (a instanceof ProxygenSerializer.ElementType) {
                           innerMeta = (ProxygenSerializer.ElementType)a;
                           break;
                        }
                     }

                     try {
                        setter.invoke(instance, this.deserialize(kvPairs.get(key), setter.getParameterTypes()[0], innerMeta));
                        continue label49;
                     } catch (Exception var18) {
                        throw new IllegalArgumentException(String.format("Cannot deserialize property '%s' in %s", key, type), var18);
                     }
                  }
               }
            }

            Field[] var19 = type.getFields();
            var8 = var19.length;

            for(var9 = 0; var9 < var8; ++var9) {
               Field field = var19[var9];
               if (field.getName().equals(key)) {
                  try {
                     field.set(instance, this.deserialize(kvPairs.get(key), field.getType(), (ProxygenSerializer.ElementType)field.getAnnotation(ProxygenSerializer.ElementType.class)));
                     continue label49;
                  } catch (Exception var17) {
                     throw new IllegalArgumentException(String.format("Cannot deserialize property '%s' in %s", key, type), var17);
                  }
               }
            }

            logger.warn("No field/setter found for property '" + key + "' in " + type);
         }

         return instance;
      }
   }

   private Object deserializeAsArray(List data, Class type) throws Exception {
      List source = data;
      Object[] result = (Object[])((Object[])Array.newInstance(type.getComponentType(), data.size()));

      for(int i = 0; i < result.length; ++i) {
         result[i] = this.deserialize(source.get(i), type.getComponentType(), (ProxygenSerializer.ElementType)null);
      }

      return result;
   }

   private Object deserializeAsSet(Object data, ProxygenSerializer.ElementType metadata) throws Exception {
      if (metadata == null) {
         logger.debug("Deserializing set without metadata. This may be due to forgotten annotation on a field or parameter or a return value may consist of nested collections. Returning raw set instance as best effort: " + data);
         return new HashSet((List)data);
      } else {
         List source = (List)data;
         Set result = new HashSet(source.size());
         Iterator var5 = source.iterator();

         while(var5.hasNext()) {
            Object val = var5.next();
            result.add(this.deserialize(val, metadata.value(), (ProxygenSerializer.ElementType)null));
         }

         return result;
      }
   }

   private Object deserializeAsList(Object data, ProxygenSerializer.ElementType metadata) throws Exception {
      if (metadata == null) {
         logger.debug("Deserializing list without metadata. This may be due to forgotten annotation on a field or parameter or a return value may consist of nested collections. Returning raw list instance as best effort: " + data);
         return data;
      } else {
         List source = (List)data;
         List result = new ArrayList(source.size());
         Iterator var5 = source.iterator();

         while(var5.hasNext()) {
            Object val = var5.next();
            result.add(this.deserialize(val, metadata.value(), (ProxygenSerializer.ElementType)null));
         }

         return result;
      }
   }

   private Object deserializeAsMap(Object data, ProxygenSerializer.ElementType metadata) throws Exception {
      if (metadata == null) {
         logger.debug("Deserializing map without metadata. This may be due to forgotten annotation on a field or parameter or a return value may consist of nested collections. Returning raw map instance as best effort: " + data);
         return data;
      } else {
         boolean deserializeKey = metadata.key() != Void.TYPE;
         Map source = (Map)data;
         Map result = new HashMap();
         Iterator var6 = source.keySet().iterator();

         while(var6.hasNext()) {
            Object key = var6.next();
            if (deserializeKey) {
               result.put(this.deserialize(key, metadata.key(), (ProxygenSerializer.ElementType)null), this.deserialize(source.get(key), metadata.value(), (ProxygenSerializer.ElementType)null));
            } else {
               result.put(key, this.deserialize(source.get(key), metadata.value(), (ProxygenSerializer.ElementType)null));
            }
         }

         return result;
      }
   }

   public Object[] deserializeMethodInput(List data, MultipartFile[] files, Method method) throws Exception {
      List result = new ArrayList();
      Class[] parameterTypes = method.getParameterTypes();
      ProxygenSerializer.ElementType[] metadata = getElementMetadata(method);
      Queue dataQueue = new LinkedList(data);
      Queue filesQueue = files != null ? new LinkedList(Arrays.asList(files)) : new LinkedList();

      for(int i = 0; i < parameterTypes.length; ++i) {
         Class type = parameterTypes[i];
         ProxygenSerializer.ElementType metadataEntry = metadata[i];
         if (MultipartFile.class.isAssignableFrom(type)) {
            if (filesQueue.isEmpty()) {
               throw new IllegalStateException("Not enough files uploaded");
            }

            result.add(filesQueue.poll());
         } else if (type.isArray() && MultipartFile.class.isAssignableFrom(type.getComponentType())) {
            result.add(filesQueue.toArray(new MultipartFile[filesQueue.size()]));
            filesQueue.clear();
         } else {
            if (dataQueue.isEmpty()) {
               throw new IllegalStateException("Not enough arguments");
            }

            Object dataEntry = dataQueue.poll();
            result.add(this.deserialize(dataEntry, type, metadataEntry));
         }
      }

      if (result.size() != parameterTypes.length) {
         throw new IllegalStateException("Service method parameters count (" + parameterTypes.length + ") do not match provided input length (" + result.size() + ")");
      } else if (!filesQueue.isEmpty()) {
         throw new IllegalStateException("Not all MultipartFiles are handled");
      } else {
         return result.toArray();
      }
   }

   public Object serialize(Object data) throws Exception {
      if (data == null) {
         return data;
      } else {
         HashMap serialized;
         if (data instanceof ManagedObjectReference) {
            ManagedObjectReference mor = (ManagedObjectReference)data;
            serialized = new HashMap();
            serialized.put("type", mor.getType());
            serialized.put("serverGuid", mor.getServerGuid());
            serialized.put("value", mor.getValue());
            return Collections.unmodifiableMap(serialized);
         } else if (data instanceof Enum) {
            return ((Enum)data).name();
         } else if (data instanceof Date) {
            return ((Date)data).getTime();
         } else if (data instanceof Calendar) {
            return ((Calendar)data).getTimeInMillis();
         } else {
            Object[] sourceArray = null;
            if (data.getClass().isArray()) {
               Class componentType = data.getClass().getComponentType();
               if (componentType.isPrimitive()) {
                  List dataList = null;
                  int var7;
                  int var8;
                  if (Boolean.TYPE.isAssignableFrom(componentType)) {
                     boolean[] dataArr = (boolean[])((boolean[])data);
                     dataList = new ArrayList(dataArr.length);
                     boolean[] var34 = dataArr;
                     var7 = dataArr.length;

                     for(var8 = 0; var8 < var7; ++var8) {
                        boolean el = var34[var8];
                        dataList.add(el);
                     }
                  } else if (Byte.TYPE.isAssignableFrom(componentType)) {
                     byte[] dataArr = (byte[])((byte[])data);
                     dataList = new ArrayList(dataArr.length);
                     byte[] var30 = dataArr;
                     var7 = dataArr.length;

                     for(var8 = 0; var8 < var7; ++var8) {
                        byte el = var30[var8];
                        dataList.add(el);
                     }
                  } else if (Character.TYPE.isAssignableFrom(componentType)) {
                     char[] dataArr = (char[])((char[])data);
                     dataList = new ArrayList(dataArr.length);
                     char[] var29 = dataArr;
                     var7 = dataArr.length;

                     for(var8 = 0; var8 < var7; ++var8) {
                        char el = var29[var8];
                        dataList.add(el);
                     }
                  } else if (Double.TYPE.isAssignableFrom(componentType)) {
                     double[] dataArr = (double[])((double[])data);
                     dataList = new ArrayList(dataArr.length);
                     double[] var26 = dataArr;
                     var7 = dataArr.length;

                     for(var8 = 0; var8 < var7; ++var8) {
                        double el = var26[var8];
                        dataList.add(el);
                     }
                  } else if (Float.TYPE.isAssignableFrom(componentType)) {
                     float[] dataArr = (float[])((float[])data);
                     dataList = new ArrayList(dataArr.length);
                     float[] var23 = dataArr;
                     var7 = dataArr.length;

                     for(var8 = 0; var8 < var7; ++var8) {
                        float el = var23[var8];
                        dataList.add(el);
                     }
                  } else if (Integer.TYPE.isAssignableFrom(componentType)) {
                     int[] dataArr = (int[])((int[])data);
                     dataList = new ArrayList(dataArr.length);
                     int[] var21 = dataArr;
                     var7 = dataArr.length;

                     for(var8 = 0; var8 < var7; ++var8) {
                        int el = var21[var8];
                        dataList.add(el);
                     }
                  } else if (Long.TYPE.isAssignableFrom(componentType)) {
                     long[] dataArr = (long[])((long[])data);
                     dataList = new ArrayList(dataArr.length);
                     long[] var19 = dataArr;
                     var7 = dataArr.length;

                     for(var8 = 0; var8 < var7; ++var8) {
                        long el = var19[var8];
                        dataList.add(el);
                     }
                  } else {
                     if (!Short.TYPE.isAssignableFrom(componentType)) {
                        throw new IllegalArgumentException("Unknown primitive type?!?!?");
                     }

                     short[] dataArr = (short[])((short[])data);
                     dataList = new ArrayList(dataArr.length);
                     short[] var6 = dataArr;
                     var7 = dataArr.length;

                     for(var8 = 0; var8 < var7; ++var8) {
                        short el = var6[var8];
                        dataList.add(el);
                     }
                  }

                  sourceArray = dataList.toArray();
               } else {
                  sourceArray = (Object[])((Object[])data);
               }
            } else if (data instanceof Collection) {
               sourceArray = ((Collection)data).toArray();
            }

            int var31;
            int var36;
            if (sourceArray != null) {
               List list = new ArrayList();
               Object[] var33 = sourceArray;
               var31 = sourceArray.length;

               for(var36 = 0; var36 < var31; ++var36) {
                  Object o = var33[var36];
                  list.add(this.serialize(o));
               }

               return list;
            } else if (!(data instanceof Map)) {
               if (data.getClass().getAnnotation(TsModel.class) == null && data.getClass().getAnnotation(data.class) == null) {
                  return data;
               } else {
                  serialized = new HashMap();
                  Method[] var28 = data.getClass().getMethods();
                  var31 = var28.length;

                  String propertyName;
                  for(var36 = 0; var36 < var31; ++var36) {
                     Method method = var28[var36];
                     propertyName = getPropertyName(method);
                     if (propertyName != null) {
                        serialized.put(propertyName, this.serialize(method.invoke(data)));
                     }
                  }

                  Field[] var32 = data.getClass().getFields();
                  var31 = var32.length;

                  for(var36 = 0; var36 < var31; ++var36) {
                     Field field = var32[var36];
                     propertyName = getPropertyName(field);
                     if (propertyName != null) {
                        serialized.put(propertyName, this.serialize(field.get(data)));
                     }
                  }

                  return serialized;
               }
            } else {
               Map sourceMap = (Map)data;
               Map resultMap = new HashMap();
               Iterator var27 = sourceMap.keySet().iterator();

               while(var27.hasNext()) {
                  Object key = var27.next();
                  resultMap.put(this.serialize(key), this.serialize(sourceMap.get(key)));
               }

               return resultMap;
            }
         }
      }
   }

   private static String getPropertyName(Method method) throws NoSuchMethodException {
      String getterPrefix = null;
      if (method.getName().startsWith("get") && method.getName().length() > "get".length()) {
         getterPrefix = "get";
      } else if (Boolean.class.isAssignableFrom(method.getReturnType()) && method.getName().startsWith("is") && method.getName().length() > "is".length()) {
         getterPrefix = "is";
      }

      if (getterPrefix == null) {
         return null;
      } else if (method.getParameterTypes().length != 0) {
         return null;
      } else {
         return (method.getModifiers() & 8) != 0 ? null : Character.toLowerCase(method.getName().charAt(getterPrefix.length())) + method.getName().substring(getterPrefix.length() + 1);
      }
   }

   private static String getPropertyName(Field field) {
      return (field.getModifiers() & 8) != 0 ? null : field.getName();
   }

   private static ProxygenSerializer.ElementType[] getElementMetadata(Method method) {
      Annotation[][] parameterAnnotations = method.getParameterAnnotations();
      ProxygenSerializer.ElementType[] typedAnnotations = new ProxygenSerializer.ElementType[parameterAnnotations.length];

      for(int i = 0; i < typedAnnotations.length; ++i) {
         Annotation[] var4 = parameterAnnotations[i];
         int var5 = var4.length;

         for(int var6 = 0; var6 < var5; ++var6) {
            Annotation a = var4[var6];
            if (a instanceof ProxygenSerializer.ElementType) {
               typedAnnotations[i] = (ProxygenSerializer.ElementType)a;
               break;
            }
         }
      }

      return typedAnnotations;
   }

   @Documented
   @Retention(RetentionPolicy.RUNTIME)
   @Target({java.lang.annotation.ElementType.PARAMETER, java.lang.annotation.ElementType.FIELD})
   public @interface ElementType {
      Class value();

      Class key() default void.class;
   }

   private static class MethodPrefix {
      static final String GET = "get";
      static final String IS = "is";
   }
}
