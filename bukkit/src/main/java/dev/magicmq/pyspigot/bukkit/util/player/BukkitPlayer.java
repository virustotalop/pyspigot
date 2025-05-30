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

package dev.magicmq.pyspigot.bukkit.util.player;


import dev.magicmq.pyspigot.util.player.PlayerAdapter;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.entity.Player;

/**
 * A wrapper for the Bukkit {@link org.bukkit.entity.Player} class.
 */
public class BukkitPlayer implements PlayerAdapter {

    private final Player player;

    /**
     *
     * @param player The Bukkit Player
     */
    public BukkitPlayer(Player player) {
        this.player = player;
    }

    @Override
    public boolean hasPermission(String permission) {
        return player.hasPermission(permission);
    }

    @Override
    public void sendMessage(BaseComponent[] message) {
        player.spigot().sendMessage(message);
    }
}
