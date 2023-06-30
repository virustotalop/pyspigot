/*
 *    Copyright 2023 magicmq
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

package dev.magicmq.pyspigot.manager.listener;

import dev.magicmq.pyspigot.PySpigot;
import dev.magicmq.pyspigot.manager.script.Script;
import dev.magicmq.pyspigot.manager.script.ScriptManager;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.EventExecutor;
import org.python.core.PyBaseCode;
import org.python.core.PyException;
import org.python.core.PyFunction;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ListenerManager {

    private static ListenerManager manager;

    private final List<DummyListener> registeredScripts;

    private ListenerManager() {
        registeredScripts = new ArrayList<>();
    }

    public void registerEvent(PyFunction function, Class<? extends Event> eventClass) {
        registerEvent(function, eventClass, "NORMAL", false);
    }

    public void registerEvent(PyFunction function, Class<? extends Event> eventClass, String priorityString) {
        registerEvent(function, eventClass, priorityString, false);
    }

    public void registerEvent(PyFunction function, Class<? extends Event> eventClass, boolean ignoreCancelled) {
        registerEvent(function, eventClass, "NORMAL", ignoreCancelled);
    }

    public void registerEvent(PyFunction function, Class<? extends Event> eventClass, String priorityString, boolean ignoreCancelled) {
        Script script = ScriptManager.get().getScript(((PyBaseCode) function.__code__).co_filename);
        DummyListener listener = get().getListener(script);
        if (listener == null) {
            listener = new DummyListener(script);
            registeredScripts.add(listener);
        } else {
            if (listener.containsEvent(eventClass)) {
                throw new UnsupportedOperationException("Tried to register an event listener, but " + eventClass.getSimpleName() + " is already registered!");
            }
        }
        EventPriority priority = EventPriority.valueOf(priorityString);
        EventExecutor executor = (listenerInner, event) -> {
            try {
                if (!eventClass.isAssignableFrom(event.getClass())) {
                    return;
                }

                function._jcall(new Object[]{event});
            } catch (PyException e) {
                ScriptManager.get().handleScriptException(script, e, "Error when executing event listener");
            } catch (Throwable t) {
                throw new EventException(t);
            }
        };

        listener.addEvent(function, eventClass);
        Bukkit.getPluginManager().registerEvent(eventClass, listener, priority, executor, PySpigot.get(), ignoreCancelled);
    }

    public void unregisterEvent(PyFunction function) {
        String scriptName = ((PyBaseCode) function.__code__).co_filename;
        DummyListener dummyListener = getListener(scriptName);
        Class<? extends Event> event = dummyListener.removeEvent(function);
        removeFromHandlers(event, dummyListener);
        if (dummyListener.isEmpty()) {
            registeredScripts.remove(dummyListener);
        }
    }

    public DummyListener getListener(String scriptName) {
        for (DummyListener listener : registeredScripts) {
            if (listener.getScript().getName().equals(scriptName))
                return listener;
        }

        return null;
    }

    public DummyListener getListener(Script script) {
        for (DummyListener listener : registeredScripts) {
            if (listener.getScript().equals(script))
                return listener;
        }

        return null;
    }

    public void unregisterEvents(Script script) {
        DummyListener listener = getListener(script);
        if (listener != null) {
            for (Class<? extends Event> event : listener.getEvents()) {
                removeFromHandlers(event, listener);
            }
            registeredScripts.remove(listener);
        }
    }

    private void removeFromHandlers(Class<? extends Event> clazz, DummyListener listener) {
        try {
            Method method = clazz.getDeclaredMethod("getHandlerList");
            HandlerList list = (HandlerList) method.invoke(null);
            list.unregister(listener);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public static ListenerManager get() {
        if (manager == null)
            manager = new ListenerManager();
        return manager;
    }
}
