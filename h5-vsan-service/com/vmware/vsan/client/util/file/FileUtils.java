package com.vmware.vsan.client.util.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Properties;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FileUtils {
   private static final Log logger = LogFactory.getLog(FileUtils.class);

   public static String createPath(String... chunks) {
      return StringUtils.joinWith(File.separator, chunks);
   }

   public static String createPathWithBase(String basePath, String... chunks) {
      return createPath((String[])ArrayUtils.insert(0, chunks, new String[]{basePath}));
   }

   public static Properties loadPropertiesFile(String filePath) {
      Properties properties = new Properties();

      try {
         FileInputStream in = new FileInputStream(filePath);
         Throwable var3 = null;

         try {
            properties.load(in);
         } catch (Throwable var14) {
            var3 = var14;
            throw var14;
         } finally {
            if (in != null) {
               if (var3 != null) {
                  try {
                     in.close();
                  } catch (Throwable var13) {
                     var3.addSuppressed(var13);
                  }
               } else {
                  in.close();
               }
            }

         }
      } catch (FileNotFoundException var16) {
         logger.warn("Could not find properties file with filename.");
      } catch (Exception var17) {
         logger.warn("Unhandled exception loading properties file.");
      }

      return properties;
   }
}
