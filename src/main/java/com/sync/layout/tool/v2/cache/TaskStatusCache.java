package com.sync.layout.tool.v2.cache;

import java.util.concurrent.ConcurrentHashMap;

public class TaskStatusCache {
    public static ConcurrentHashMap<String, Boolean> taskStatusCache = new ConcurrentHashMap<>();


    public static Boolean getStatus(String taskName) {
        Boolean status = taskStatusCache.get(taskName);
        if (status == null) {
            return true;
        }

        return status;
    }

    public static void updateTaskStatus(String taskName, boolean status) {
        taskStatusCache.put(taskName, status);
    }
}
