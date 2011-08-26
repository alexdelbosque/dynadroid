package org.dynadroid.utils;

import java.util.ArrayList;
import java.util.List;

public abstract class HttpDelegate {
    private List<HttpDelegateTask> completeTasks = new ArrayList<HttpDelegateTask>();

    public abstract void completed(HttpCall httpCall);

    void addCompleteTask(HttpDelegateTask task) {
        completeTasks.add(task);
    }
    void complete(HttpCall httpCall) {
        for (HttpDelegateTask task : completeTasks) {
            task.doTask();
        }
        this.completed(httpCall);
    }
}
