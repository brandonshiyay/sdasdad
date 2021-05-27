package com.vmware.vsan.client.services.common;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vim.Task;
import com.vmware.vim.binding.vim.TaskInfo;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vsan.client.services.common.data.TaskInfoData;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcConnection;
import com.vmware.vsphere.client.vsan.util.Utils;
import java.util.concurrent.TimeoutException;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TaskService {
   private Logger logger = LoggerFactory.getLogger(TaskService.class);
   private static final int MAX_TRIES = 100;
   private static final int POLL_DELAY = 1000;
   @Autowired
   private VcClient vcClient;

   public Object getResult(ManagedObjectReference taskRef) throws TimeoutException, InterruptedException {
      return this.getResult(taskRef, 100, 1000);
   }

   public Object getResult(ManagedObjectReference taskRef, int retries, int delay) throws TimeoutException, InterruptedException {
      Validate.notNull(taskRef);
      VcConnection vcConnection = this.vcClient.getConnection(taskRef.getServerGuid());
      Throwable var5 = null;

      try {
         Task task = (Task)vcConnection.createStub(Task.class, taskRef);
         int i = 0;

         while(i < retries) {
            TaskInfo taskInfo = task.getInfo();
            switch(taskInfo.getState()) {
            case success:
               Object var21 = taskInfo.getResult();
               return var21;
            case error:
               Exception var9 = taskInfo.getError();
               return var9;
            default:
               Thread.sleep((long)delay);
               ++i;
            }
         }

         throw new TimeoutException(Utils.getLocalizedString("vsan.task.timeout"));
      } catch (Throwable var19) {
         var5 = var19;
         throw var19;
      } finally {
         if (vcConnection != null) {
            if (var5 != null) {
               try {
                  vcConnection.close();
               } catch (Throwable var18) {
                  var5.addSuppressed(var18);
               }
            } else {
               vcConnection.close();
            }
         }

      }
   }

   @TsService
   public TaskInfoData getInfo(ManagedObjectReference taskRef) {
      Validate.notNull(taskRef);
      TaskInfo taskInfo = this.getTaskInfo(taskRef);
      return TaskInfoData.fromTaskInfo(taskInfo);
   }

   private TaskInfo getTaskInfo(ManagedObjectReference taskRef) {
      VcConnection vcConnection = this.vcClient.getConnection(taskRef.getServerGuid());
      Throwable var3 = null;

      TaskInfo var5;
      try {
         Task task = (Task)vcConnection.createStub(Task.class, taskRef);
         var5 = task.getInfo();
      } catch (Throwable var14) {
         var3 = var14;
         throw var14;
      } finally {
         if (vcConnection != null) {
            if (var3 != null) {
               try {
                  vcConnection.close();
               } catch (Throwable var13) {
                  var3.addSuppressed(var13);
               }
            } else {
               vcConnection.close();
            }
         }

      }

      return var5;
   }
}
