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

package dev.magicmq.pyspigot;

import dev.magicmq.pyspigot.command.PySpigotCommand;
import dev.magicmq.pyspigot.config.PluginConfig;
import dev.magicmq.pyspigot.manager.command.CommandManager;
import dev.magicmq.pyspigot.manager.config.ConfigManager;
import dev.magicmq.pyspigot.manager.libraries.LibraryManager;
import dev.magicmq.pyspigot.manager.listener.ListenerManager;
import dev.magicmq.pyspigot.manager.placeholder.PlaceholderManager;
import dev.magicmq.pyspigot.manager.protocol.ProtocolManager;
import dev.magicmq.pyspigot.manager.script.ScriptManager;
import dev.magicmq.pyspigot.manager.task.TaskManager;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * Main class of the plugin.
 */
public class PySpigot extends JavaPlugin {

    private static PySpigot instance;

    /**
     * Can be used by scripts to access the {@link ListenerManager}.
     */
    public static ListenerManager listener;
    /**
     * Can be used by scripts to access the {@link CommandManager}.
     */
    public static CommandManager command;
    /**
     * Can be used by scripts to access the {@link TaskManager}.
     */
    public static TaskManager scheduler;
    /**
     * Can be used by scripts to access the {@link ConfigManager}.
     */
    public static ConfigManager config;
    /**
     * Can be used by scripts to access the {@link ProtocolManager}.
     */
    public static ProtocolManager protocol;
    /**
     * Can be used by scripts to access the {@link PlaceholderManager}.
     */
    public static PlaceholderManager placeholder;

    @Override
    public void onEnable() {
        instance = this;

        getConfig().options().copyDefaults(true);
        saveDefaultConfig();
        reloadConfig();

        getCommand("pyspigot").setExecutor(new PySpigotCommand());

        ScriptManager.get();
        listener = ListenerManager.get();
        command = CommandManager.get();
        scheduler = TaskManager.get();
        config = ConfigManager.get();

        if (isProtocolLibAvailable())
            protocol = ProtocolManager.get();

        if (isPlaceholderApiAvailable())
            placeholder = PlaceholderManager.get();

        try {
            checkReflection();
        } catch (NoSuchMethodException | NoSuchFieldException e) {
            getLogger().log(Level.SEVERE, "Error when accessing CraftBukkit (Are you on a supported MC version?), PySpigot will not work correctly.");
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        ScriptManager.get().shutdown();

        LibraryManager.get().shutdown();
    }

    /**
     * Reload the plugin configuration.
     */
    public void reload() {
        reloadConfig();
        PluginConfig.reload();
    }

    /**
     * Check if ProtocolLib is available on the server.
     * @return True if ProtocolLib is loaded and enabled, false if otherwise
     */
    public boolean isProtocolLibAvailable() {
        return Bukkit.getPluginManager().getPlugin("ProtocolLib") != null;
    }

    /**
     * Check if PlacehodlerAPI is available on the server.
     * @return True if PlaceholderAPI is loaded and enabled, false if otherwise
     */
    public boolean isPlaceholderApiAvailable() {
        return Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    private void checkReflection() throws NoSuchMethodException, NoSuchFieldException {
        //Check reflection for commands
        PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
        Bukkit.getServer().getClass().getDeclaredField("commandMap");
        SimpleCommandMap.class.getDeclaredField("knownCommands");
    }

    /**
     * Get the instance of this plugin.
     * @return The instance
     */
    public static PySpigot get() {
        return instance;
    }
}
