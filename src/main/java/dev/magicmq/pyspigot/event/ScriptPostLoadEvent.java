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

package dev.magicmq.pyspigot.event;

import dev.magicmq.pyspigot.manager.script.Script;
import org.bukkit.event.HandlerList;

/**
 * Called after a script finished loading and has been run.
 * <p>
 * This event is only called when a script loads successfully. It can safely be assumed that when this event is called, its associated script is running.
 */
public class ScriptPostLoadEvent extends ScriptEvent {

    private static final HandlerList handlers = new HandlerList();

    /**
     *
     * @param script The script that has loaded and is running
     */
    public ScriptPostLoadEvent(Script script) {
        super(script);
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}
