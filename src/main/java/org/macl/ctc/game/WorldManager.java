package org.macl.ctc.game;

import org.apache.commons.io.FileUtils;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.macl.ctc.Main;
import org.bukkit.Location;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Map;

public class WorldManager {

    public boolean isUnloading = false;
    public ArrayList<Location> center;
    Main main;
    GameManager game;
    private World w;

    public WorldManager(Main main) {
        this.main = main;
        this.game = main.game;
    }

    public void loadWorld(String worldName, String replacement) {
        // Unload the world if it's already loaded
        if (Bukkit.getWorld(worldName) != null) {
            Bukkit.unloadWorld(worldName, false);
        }

        // Paths to the world folders
        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
        File replacementFolder = new File(Bukkit.getWorldContainer(), replacement);

        // Perform file operations asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(main, () -> {
            // Delete the existing world folder
            if (worldFolder.exists()) {
                try {
                    FileUtils.deleteDirectory(worldFolder);
                    main.broadcast("Deleted existing world folder: " + worldName);
                } catch (IOException e) {
                    main.broadcast("Failed to delete world folder: " + worldName);
                    e.printStackTrace();
                    return;
                }
            }

            // Check if the replacement folder exists
            if (!replacementFolder.exists()) {
                main.broadcast("Replacement folder does not exist: " + replacement);
                return;
            }

            // Copy the replacement world folder
            try {
                FileUtils.copyDirectory(replacementFolder, worldFolder);
                main.broadcast("Copied replacement world folder: " + replacement + " to " + worldName);
            } catch (IOException e) {
                main.broadcast("Failed to copy replacement world: " + replacement);
                e.printStackTrace();
                return;
            }

            // Load the world synchronously after copying is complete
            Bukkit.getScheduler().runTask(main, () -> {

                // Check if replacement is "shattered" or "graveyard"
                if (replacement.equalsIgnoreCase("shattered") || replacement.equalsIgnoreCase("graveyard")) {
                    // Determine the environment
                    String environment = replacement.equalsIgnoreCase("shattered") ? "end" : "nether";

                    // Use Multiverse-Core command to import the world
                    String command = "mv import " + worldName + " " + environment;
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                    main.broadcast("Imported world '" + worldName + "' using Multiverse-Core as " + environment + " world.");

                    // Remove the world from Multiverse-Core configuration
                    removeWorldFromMultiverseConfig(worldName);

                    // Reload Multiverse-Core configuration
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv reload");

                    // Wait briefly to ensure the world is loaded
                    new BukkitRunnable() {
                        int attempts = 0;

                        @Override
                        public void run() {
                            World importedWorld = Bukkit.getWorld(worldName);
                            if (importedWorld != null) {
                                main.broadcast("World '" + worldName + "' has been successfully loaded.");
                                center = getCenter();
                                cancel();
                            } else {
                                attempts++;
                                if (attempts >= 10) { // Retry up to 10 times
                                    main.broadcast("Failed to load world '" + worldName + "' after importing with Multiverse-Core.");
                                    cancel();
                                }
                            }
                        }
                    }.runTaskTimer(main, 20L, 20L); // Check every second
                } else {
                    // Use WorldCreator to load the world
                    WorldCreator worldCreator = new WorldCreator(worldName);

                    // Create or load the world
                    World createdWorld = Bukkit.createWorld(worldCreator);

                    // Verify that the world was loaded
                    if (createdWorld != null) {
                        main.broadcast("World '" + worldName + "' has been successfully loaded.");
                        isUnloading = false;
                        center = getCenter();
                    } else {
                        main.broadcast("Failed to load world '" + worldName + "'.");
                    }
                }
            });
        });

        main.broadcast("Loading " + replacement + "...");
    }

    private void removeWorldFromMultiverseConfig(String worldName) {
        Plugin mvPlugin = Bukkit.getServer().getPluginManager().getPlugin("Multiverse-Core");
        if (mvPlugin != null) {
            File multiverseConfigFile = new File(mvPlugin.getDataFolder(), "worlds.yml");
            FileConfiguration multiverseConfig = YamlConfiguration.loadConfiguration(multiverseConfigFile);

            // Remove the world entry
            multiverseConfig.set("worlds." + worldName, null);

            // Save the configuration
            try {
                multiverseConfig.save(multiverseConfigFile);
                main.broadcast("Removed world '" + worldName + "' from Multiverse-Core configuration.");
            } catch (IOException e) {
                main.broadcast("Failed to remove world '" + worldName + "' from Multiverse-Core configuration.");
                e.printStackTrace();
            }
        } else {
            main.broadcast("Multiverse-Core is not installed or not enabled.");
        }
    }

    public void clean(Player p1) {
        isUnloading = true;
        // fix this to kick back with bungee cord
        boolean red = false;
        if(main.game.redHas(p1))
            red = true;
        else
            red = false;

        String text = "";
        if(red) {
            text = p1.getName() + ChatColor.RED + " has destroyed the enemy core! Congratulations red team! Thanks for playing :)";
            //for(Player p : game.getReds())
                //main.getStats().recordWin(p.getUniqueId());
        }
        else {
            text = p1.getName() + ChatColor.BLUE + " has destroyed the enemy core! Congratulations blue team! Thanks for playing :)";
            //for(Player p : game.getBlues())
                //main.getStats().recordWin(p.getUniqueId());
        }

        for(Player p : Bukkit.getOnlinePlayers()) {
            p.teleport(Bukkit.getWorld("world").getSpawnLocation());
        }


        main.broadcast(text);

        loadWorld("map", main.map);
        //unload map

        //replace map with world creator x

        //load map

        //create command to teleport there

    }

    public Location getRed() {
        String path = main.map + ".red";

        // Get the world name from the config
        String worldName = main.getConfig().getString(path + ".world");
        if (worldName == null) {
            main.broadcast("World name for red location is not set in config.");
            return null;
        }

        // Get the world
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            main.broadcast("World '" + worldName + "' is not loaded.");
            return null;
        }

        // Check if coordinates are set
        if (!main.getConfig().contains(path + ".x") ||
                !main.getConfig().contains(path + ".y") ||
                !main.getConfig().contains(path + ".z")) {
            main.broadcast("Red location coordinates are not fully set in config.");
            return null;
        }

        // Get the location components
        double x = main.getConfig().getDouble(path + ".x");
        double y = main.getConfig().getDouble(path + ".y");
        double z = main.getConfig().getDouble(path + ".z");
        float yaw = (float) main.getConfig().getDouble(path + ".yaw");
        float pitch = (float) main.getConfig().getDouble(path + ".pitch");

        // Create the location
        Location loc = new Location(world, x, y, z, yaw, pitch);

        return loc;
    }


    public Location getBlue() {
        String path = main.map + ".blue";

        // Get the world name from the config
        String worldName = main.getConfig().getString(path + ".world");
        if (worldName == null) {
            main.broadcast("World name for blue location is not set in config.");
            return null;
        }

        // Get the world
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            main.broadcast("World '" + worldName + "' is not loaded.");
            return null;
        }

        // Check if coordinates are set
        if (!main.getConfig().contains(path + ".x") ||
                !main.getConfig().contains(path + ".y") ||
                !main.getConfig().contains(path + ".z")) {
            main.broadcast("Blue location coordinates are not fully set in config.");
            return null;
        }

        // Get the location components
        double x = main.getConfig().getDouble(path + ".x");
        double y = main.getConfig().getDouble(path + ".y");
        double z = main.getConfig().getDouble(path + ".z");
        float yaw = (float) main.getConfig().getDouble(path + ".yaw");
        float pitch = (float) main.getConfig().getDouble(path + ".pitch");

        // Create the location
        Location loc = new Location(world, x, y, z, yaw, pitch);

        return loc;
    }

    public ArrayList<Location> getCenter() {
        ArrayList<Location> centers = new ArrayList<>();
        String path = main.map + ".center";

        // Get the world name from the config
        String worldName = main.getConfig().getString(path + ".world");
        if (worldName == null) {
            main.broadcast("World name for center location is not set in config.");
            return centers;  // Return empty list
        }

        // Get the world
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            main.broadcast("World '" + worldName + "' is not loaded.");
            return centers;  // Return empty list
        }

        // Check if coordinates are set
        if (!main.getConfig().contains(path + ".x") ||
                !main.getConfig().contains(path + ".y") ||
                !main.getConfig().contains(path + ".z")) {
            main.broadcast("Center location coordinates are not fully set in config.");
            return centers;  // Return empty list
        }

        // Get the location components
        double x = main.getConfig().getDouble(path + ".x");
        double y = main.getConfig().getDouble(path + ".y");
        double z = main.getConfig().getDouble(path + ".z");

        // Create the central location
        Location centerLoc = new Location(world, x, y, z);

        // Add the center location and its surrounding blocks
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Location loc = new Location(
                        world,
                        centerLoc.getBlockX() + dx + 0.5, // Adding 0.5 to center the block
                        centerLoc.getBlockY(),
                        centerLoc.getBlockZ() + dz + 0.5  // Adding 0.5 to center the block
                );
                centers.add(loc);
            }
        }
        return centers;
    }


    public void setRed(Player p, String s) {

        Location loc = p.getLocation();
        main.send(p, p.getLocation().toString());
        main.send(p, "Red spawn set", ChatColor.RED);

        main.getConfig().set(s + ".red.world", "map");
        main.getConfig().set(s + ".red.x", loc.getX());
        main.getConfig().set(s + ".red.y", loc.getY());
        main.getConfig().set(s + ".red.z", loc.getZ());
        main.getConfig().set(s + ".red.yaw", loc.getYaw());
        main.getConfig().set(s + ".red.pitch", loc.getPitch());

        main.saveConfig();
    }

    public void setBlue(Player p, String s) {

        Location loc = p.getLocation();
        main.send(p, p.getLocation().toString());

        main.send(p, "Blue spawn set", ChatColor.BLUE);

        main.getConfig().set(s + ".blue.world", "map");
        main.getConfig().set(s + ".blue.x", loc.getX());
        main.getConfig().set(s + ".blue.y", loc.getY());
        main.getConfig().set(s + ".blue.z", loc.getZ());
        main.getConfig().set(s + ".blue.yaw", loc.getYaw());
        main.getConfig().set(s + ".blue.pitch", loc.getPitch());

        main.saveConfig();
    }

    public void setCenter(Player p, String Map) {

        Location loc = p.getLocation();
        main.send(p, p.getLocation().toString());

        main.send(p, "Center set");

        main.getConfig().set(Map + ".center.world", loc.getWorld().getName());
        main.getConfig().set(Map + ".center.x", loc.getX());
        main.getConfig().set(Map + ".center.y", loc.getY());
        main.getConfig().set(Map + ".center.z", loc.getZ());
        main.getConfig().set(Map + ".center.yaw", loc.getYaw());
        main.getConfig().set(Map + ".center.pitch", loc.getPitch());

        main.saveConfig();
    }

}

