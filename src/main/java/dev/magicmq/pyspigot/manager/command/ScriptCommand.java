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

package dev.magicmq.pyspigot.manager.command;

import dev.magicmq.pyspigot.PySpigot;
import dev.magicmq.pyspigot.manager.script.Script;
import dev.magicmq.pyspigot.manager.script.ScriptManager;
import dev.magicmq.pyspigot.util.CommandAliasHelpTopic;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.help.*;
import org.bukkit.plugin.Plugin;
import org.python.core.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Represents a command belonging to a script.
 * @see org.bukkit.command.TabExecutor
 * @see org.bukkit.command.defaults.BukkitCommand
 */
public class ScriptCommand implements TabExecutor {

    private final Script script;
    private final PyFunction commandFunction;
    private final PyFunction tabFunction;
    private final String name;
    private final String label;
    private final String description;
    private final String prefix;
    private final String usage;
    private final List<String> aliases;
    private final String permission;
    private final String permissionMessage;

    private final PluginCommand bukkitCommand;
    private List<HelpTopic> helps;

    /**
     *
     * @param script The script to which this command belongs
     * @param commandFunction The command function that should be called when the command is executed
     * @param tabFunction The tab function that should be called for tab completion of the command. Can be null
     * @param name The name of the command to register
     * @param description The description of the command. Use an empty string for no description
     * @param prefix The prefix of the command
     * @param usage The usage message for the command
     * @param aliases A List of String containing all the aliases for this command. Use an empty list for no aliases
     * @param permission The required permission node to use this command. Can be null
     * @param permissionMessage The message do display if there is insufficient permission to run the command. Can be null
     */
    public ScriptCommand(Script script, PyFunction commandFunction, PyFunction tabFunction, String name, String description, String prefix, String usage, List<String> aliases, String permission, String permissionMessage) {
        this.script = script;
        this.commandFunction = commandFunction;
        this.tabFunction = tabFunction;
        this.name = name;
        this.label = name.toLowerCase();
        this.description = description;
        this.prefix = prefix;
        this.usage = usage;
        this.aliases = aliases.stream().map(String::toLowerCase).collect(Collectors.toList());
        aliases.removeIf(label::equalsIgnoreCase);
        this.permission = permission;
        this.permissionMessage = permissionMessage;

        this.bukkitCommand = initBukkitCommand();
        initHelp();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        try {
            PyObject[] parameters = Py.javas2pys(sender, label, args);
            PyObject result = commandFunction.__call__(parameters[0], parameters[1], parameters[2]);
            if (result instanceof PyBoolean)
                return ((PyBoolean) result).getBooleanValue();
            else
                script.getLogger().log(Level.SEVERE, "Script command function '" + commandFunction.__name__ + "' should return a boolean!");
        } catch (PyException exception) {
            ScriptManager.get().handleScriptException(script, exception, "Unhandled exception when executing command '" + label + "'");
            //Mimic Bukkit behavior
            sender.sendMessage(ChatColor.RED + "An internal error occurred while attempting to perform this command");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (tabFunction != null) {
            try {
                PyObject[] parameters = Py.javas2pys(sender, alias, args);
                PyObject result = tabFunction.__call__(parameters[0], parameters[1], parameters[2]);
                if (result instanceof PyList) {
                    PyList pyList = (PyList) result;
                    ArrayList<String> toReturn = new ArrayList<>();
                    for (Object object : pyList) {
                        if (object instanceof String)
                            toReturn.add((String) object);
                        else {
                            script.getLogger().log(Level.SEVERE, "Script tab complete function '" + tabFunction.__name__ + "' should return a list of str!");
                            return Collections.emptyList();
                        }
                    }
                    return toReturn;
                }
            } catch (PyException exception) {
                ScriptManager.get().handleScriptException(script, exception,  "Unhandled exception when tab completing command '" + label + "'");
            }
        }
        return Collections.emptyList();
    }

    /**
     * Get the script associated with this command.
     * @return The script associated with this command
     */
    public Script getScript() {
        return script;
    }

    /**
     * Get the name of this command.
     * @return The name of this command
     */
    public String getName() {
        return name;
    }

    protected void register(SimpleCommandMap map) {
        map.register(prefix, bukkitCommand);
        bukkitCommand.register(map);
    }

    protected void unregister(SimpleCommandMap map, Map<String, Command> knownCommands) {
        bukkitCommand.unregister(map);
        knownCommands.remove(label);
        knownCommands.remove(prefix + ":" + label);
        for (String alias : aliases) {
            knownCommands.remove(alias);
            knownCommands.remove(prefix + ":" + alias);
        }

        removeHelp();
    }

    private PluginCommand initBukkitCommand() {
        try {
            final Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            constructor.setAccessible(true);
            final PluginCommand bukkitCommand = constructor.newInstance(name, PySpigot.get());
            bukkitCommand.setLabel(this.label);
            bukkitCommand.setDescription(this.description);
            bukkitCommand.setUsage(this.usage);
            bukkitCommand.setAliases(this.aliases);
            bukkitCommand.setPermission(this.permission);
            bukkitCommand.setPermissionMessage(this.permissionMessage);
            bukkitCommand.setExecutor(this);
            return bukkitCommand;
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            //This should not happen, reflection checks done on plugin enable
            throw new RuntimeException("Unhandled exception when initializing command '" + name + "'", e);
        }
    }

    private void initHelp() {
        helps = new ArrayList<>();
        HelpMap helpMap = Bukkit.getHelpMap();
        HelpTopic helpTopic = new GenericCommandHelpTopic(bukkitCommand);
        helpMap.addTopic(helpTopic);
        helps.add(helpTopic);

        HelpTopic aliases = helpMap.getHelpTopic("Aliases");
        if (aliases instanceof IndexHelpTopic) {
            aliases.getFullText(Bukkit.getConsoleSender());
            try {
                Field topics = IndexHelpTopic.class.getDeclaredField("allTopics");
                topics.setAccessible(true);
                List<HelpTopic> aliasTopics = new ArrayList<>((Collection<HelpTopic>) topics.get(aliases));
                for (String alias : this.aliases) {
                    HelpTopic toAdd = new CommandAliasHelpTopic("/" + alias, "/" + label, helpMap);
                    aliasTopics.add(toAdd);
                    helps.add(toAdd);
                }
                aliasTopics.sort(HelpTopicComparator.helpTopicComparatorInstance());
                topics.set(aliases, aliasTopics);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                //This should not happen, reflection checks done on plugin enable
                throw new RuntimeException("Unhandled exception when initializing command '" + name + "'", e);
            }
        }
    }

    private void removeHelp() {
        Bukkit.getHelpMap().getHelpTopics().removeAll(helps);

        HelpTopic aliases = Bukkit.getHelpMap().getHelpTopic("Aliases");
        if (aliases instanceof IndexHelpTopic) {
            try {
                Field topics = IndexHelpTopic.class.getDeclaredField("allTopics");
                topics.setAccessible(true);
                List<HelpTopic> aliasTopics = new ArrayList<>((Collection<HelpTopic>) topics.get(aliases));
                aliasTopics.removeAll(helps);
                topics.set(aliases, aliasTopics);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                //This should not happen, reflection checks done on plugin enable
                throw new RuntimeException("Unhandled exception when unregistering command '" + name + "'", e);
            }
        }
    }
}
