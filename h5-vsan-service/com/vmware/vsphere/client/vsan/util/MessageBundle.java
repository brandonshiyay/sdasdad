package com.vmware.vsphere.client.vsan.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MessageBundle {
   private static final Logger logger = LoggerFactory.getLogger(MessageBundle.class);
   private String bundlePath;

   public MessageBundle() {
      this("vsanservice");
   }

   public MessageBundle(String bundlePath) {
      this.bundlePath = bundlePath;
   }

   public String string(String key) {
      return this.string(key, (Object[])null);
   }

   public String string(String key, Object... parameters) {
      String formatString = this.loadResourceBundle().getString(key);
      if (parameters != null && parameters.length != 0) {
         formatString = formatString.replaceAll("'", "''");
         return MessageFormat.format(formatString, parameters);
      } else {
         return formatString;
      }
   }

   public String string(String key, String... parameters) {
      return this.string(key, (Object[])parameters);
   }

   private ResourceBundle loadResourceBundle() {
      try {
         return ResourceBundle.getBundle(this.bundlePath, Utils.getCurrentLocale(), this.getClass().getClassLoader(), new MessageBundle.UTF8Control());
      } catch (MissingResourceException var2) {
         throw new IllegalStateException("Cannot load module: " + this.bundlePath, var2);
      }
   }

   private static class UTF8Control extends Control {
      private UTF8Control() {
      }

      public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload) throws IllegalAccessException, InstantiationException, IOException {
         String bundleName = this.toBundleName(baseName, locale);
         ResourceBundle bundle = null;
         if (format.equals("java.class")) {
            try {
               Class bundleClass = loader.loadClass(bundleName);
               if (!ResourceBundle.class.isAssignableFrom(bundleClass)) {
                  throw new ClassCastException(bundleClass.getName() + " cannot be cast to ResourceBundle");
               }

               bundle = (ResourceBundle)bundleClass.newInstance();
            } catch (ClassNotFoundException var19) {
            }
         } else {
            if (!format.equals("java.properties")) {
               throw new IllegalArgumentException("unknown format: " + format);
            }

            final String resourceName = this.toResourceName(bundleName, "properties");
            final ClassLoader classLoader = loader;
            final boolean reloadFlag = reload;
            InputStream stream = null;

            try {
               stream = (InputStream)AccessController.doPrivileged(new PrivilegedExceptionAction() {
                  public InputStream run() throws IOException {
                     InputStream is = null;
                     if (reloadFlag) {
                        URL url = classLoader.getResource(resourceName);
                        if (url != null) {
                           URLConnection connection = url.openConnection();
                           if (connection != null) {
                              connection.setUseCaches(false);
                              is = connection.getInputStream();
                           }
                        }
                     } else {
                        is = classLoader.getResourceAsStream(resourceName);
                     }

                     return is;
                  }
               });
            } catch (PrivilegedActionException var18) {
               throw (IOException)var18.getException();
            }

            if (stream != null) {
               try {
                  bundle = new PropertyResourceBundle(new InputStreamReader(stream, "UTF-8"));
               } finally {
                  stream.close();
               }
            }
         }

         return (ResourceBundle)bundle;
      }

      public Locale getFallbackLocale(String baseName, Locale locale) {
         return null;
      }

      // $FF: synthetic method
      UTF8Control(Object x0) {
         this();
      }
   }
}
