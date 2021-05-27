package com.vmware.vsan.client.services.common.data;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vim.TaskInfo;
import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@TsModel
public class TaskInfoData {
   private static final Log _logger = LogFactory.getLog(TaskInfoData.class);
   public String status;
   public Object result;
   public String descriptionId;
   public String exception;
   public int progress;

   public static TaskInfoData fromTaskInfo(TaskInfo taskInfo) {
      Validate.notNull(taskInfo);
      TaskInfoData taskInfoData = new TaskInfoData();
      taskInfoData.status = taskInfo.state.toString();
      taskInfoData.result = taskInfo.result;
      taskInfoData.descriptionId = taskInfo.descriptionId;
      if (taskInfo.error != null) {
         taskInfoData.exception = taskInfo.error.getLocalizedMessage();
      }

      Integer progress = taskInfo.progress;
      if (progress != null) {
         taskInfoData.progress = progress;
      } else {
         switch(taskInfo.state) {
         case error:
         case success:
            taskInfoData.progress = 100;
            break;
         case queued:
            taskInfoData.progress = 0;
         case running:
            _logger.warn("Strange... the task's state is 'running' but the progress is not set.");
            taskInfoData.progress = 0;
            break;
         default:
            _logger.warn("Unknown TaskInfo.state: " + taskInfo.state);
         }
      }

      return taskInfoData;
   }
}
