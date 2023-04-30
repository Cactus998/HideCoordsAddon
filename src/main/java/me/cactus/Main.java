package me.cactus;

import fr.xephi.authme.api.v3.AuthMeApi;
import fr.xephi.authme.events.LoginEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Main extends JavaPlugin implements Listener {
    private FileConfiguration config;

    @Override
    public void onEnable() {
        Plugin authme = Bukkit.getPluginManager().getPlugin("AuthMe");
        if (authme == null) {
            getLogger().warning("AuthMe не найден!");
            Bukkit.getPluginManager().disablePlugin(this);
        } else {
            getLogger().info("Плагин AuthMe найден. Всё работает.");
        }
        Bukkit.getPluginManager().registerEvents(this, this);
        config = getConfig();
        config.addDefault("world-name", "world");
        config.addDefault("spawn-x", 0);
        config.addDefault("spawn-y", 10000);
        config.addDefault("spawn-z", 0);
        config.options().copyDefaults(true);
        saveConfig();
    }

    private final Map<UUID, Location> playerLocations = new HashMap<>();
    private final Map<UUID, GameMode> playerGamemode = new HashMap<>();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Location location = player.getLocation();
        GameMode gamemode = player.getGameMode();
        teleport(player);
        player.setGameMode(GameMode.SPECTATOR);

        if (!playerLocations.containsKey(uuid)) {
            playerLocations.put(uuid, location);
        }

        if (!playerGamemode.containsKey(uuid)) {
            playerGamemode.put(uuid, gamemode);
        }
    }

    private void teleport(Player player) {
        String worldName = config.getString("world-name");
        double spawnX = config.getDouble("spawn-x");
        double spawnY = config.getDouble("spawn-y");
        double spawnZ = config.getDouble("spawn-z");
        World world = Bukkit.getWorld(worldName);
        player.teleport(new Location(world, spawnX, spawnY, spawnZ));
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.SPECTATOR && !AuthMeApi.getInstance().isAuthenticated(player)) {
            teleport(player);
            player.setAllowFlight(true);
            player.setFlying(true);
            player.setFlySpeed(0);
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.SPECTATE) {
            Player player = event.getPlayer();
            if (player.getGameMode() == GameMode.SPECTATOR && !AuthMeApi.getInstance().isAuthenticated(player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onLogin(LoginEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        player.setNoDamageTicks(60);
        Location location = playerLocations.get(uuid);
        GameMode gameMode = playerGamemode.get(uuid);
        player.teleport(location);
        playerLocations.remove(uuid);
        player.setGameMode(gameMode);
        playerGamemode.remove(uuid);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (playerLocations.containsKey(uuid) && playerGamemode.containsKey(uuid)) {
            Location location = playerLocations.get(uuid);
            GameMode gameMode = playerGamemode.get(uuid);
            player.teleport(location);
            playerLocations.remove(uuid);
            player.setGameMode(gameMode);
            playerGamemode.remove(uuid);
        }
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin().equals(this)) {
            playerLocations.forEach((uuid, location) -> Bukkit.getPlayer(uuid).teleport(location));
            playerGamemode.forEach((uuid, gameMode) -> Bukkit.getPlayer(uuid).setGameMode(gameMode));
            }
            Bukkit.shutdown();
        }
    }