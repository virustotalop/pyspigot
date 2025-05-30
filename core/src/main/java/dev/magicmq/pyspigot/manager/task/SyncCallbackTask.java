/*
 *    Copyright 2025 magicmq
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package dev.magicmq.pyspigot.manager.task;

import dev.magicmq.pyspigot.manager.script.Script;
import dev.magicmq.pyspigot.manager.script.ScriptManager;
import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyFunction;
import org.python.core.PyNone;
import org.python.core.PyObject;
import org.python.core.ThreadState;

import java.util.logging.Level;

/**
 * Represents an async task with a synchronous callback defined by a script.
 */
public class SyncCallbackTask extends Task {

    private final PyFunction callbackFunction;

    private Callback callback;

    /**
     *
     * @param script The script associated with this task
     * @param function The script function that should be called when the async task executes
     * @param callbackFunction The script function that should be called for the synchronous callback
     * @param functionArgs Any arguments that should be passed to the function
     */
    public SyncCallbackTask(Script script, PyFunction function, PyFunction callbackFunction, Object[] functionArgs, long delay) {
        super(script, function, functionArgs, true, delay);
        this.callbackFunction = callbackFunction;
    }

    /**
     * Called internally when the task executes.
     */
    @Override
    public void run() {
        try {
            Py.setSystemState(script.getInterpreter().getSystemState());
            ThreadState threadState = Py.getThreadState(script.getInterpreter().getSystemState());

            PyObject outcome;
            if (functionArgs != null) {
                PyObject[] pyObjects = Py.javas2pys(functionArgs);
                outcome = function.__call__(threadState, pyObjects);
            } else {
                outcome = function.__call__(threadState);
            }

            callback = new Callback(this, outcome);
            callback.setTaskId(TaskManager.get().runSyncCallbackImpl(callback));
            synchronized (this) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    script.getLogger().log(Level.SEVERE, "Async thread was interrupted in callback task!");
                    e.printStackTrace();
                }
            }
        } catch (PyException e) {
            ScriptManager.get().handleScriptException(script, e, "Error when executing task #" + taskId);
        } finally {
            TaskManager.get().taskFinished(this);
        }
    }

    /**
     * Prints a representation of this SyncCallbackTask in string format, including the task ID, if it is async, and delay (if applicable)
     * @return A string representation of the SyncCallbackTask
     */
    @Override
    public String toString() {
        if (callback == null)
            return String.format("SyncCallbackTask[Task ID: %d, Async: %b, Delay: %d]", taskId, async, (int) delay);
        else
            return String.format("SyncCallbackTask[Task ID: %d, Async: %b, Delay: %d, Callback: %s]", taskId, async, (int) delay, callback.toString());
    }

    /**
     * The synchronous callback, which runs after the asynchronous task finishes
     */
    private static class Callback implements Runnable {

        private final SyncCallbackTask task;
        private final PyObject outcome;

        private int taskId;

        /**
         *
         * @param task The asynchronous portion of the task
         * @param outcome The value(s) returned from the function called during the asynchronous portion of the task
         */
        private Callback(SyncCallbackTask task, PyObject outcome) {
            this.task = task;
            this.outcome = outcome;
        }

        /**
         * Set the task ID for this task.
         * @param taskId The task ID to set
         */
        public void setTaskId(int taskId) {
            this.taskId = taskId;
        }

        /**
         * Called internally when the task executes.
         */
        @Override
        public void run() {
            Py.setSystemState(task.script.getInterpreter().getSystemState());
            ThreadState threadState = Py.getThreadState(task.script.getInterpreter().getSystemState());

            try {
                if (outcome instanceof PyNone)
                    task.callbackFunction.__call__(threadState);
                else
                    task.callbackFunction.__call__(threadState, outcome);
            } catch (PyException e) {
                ScriptManager.get().handleScriptException(task.script, e, "Error when executing task #" + taskId);
            } finally {
                synchronized (task) {
                    task.notify();
                }
            }
        }

        /**
         * Prints a representation of this Callback in string format, including the task ID
         * @return A string representation of the Callback
         */
        @Override
        public String toString() {
            return String.format("Callback[Task ID: %d]", taskId);
        }
    }
}
