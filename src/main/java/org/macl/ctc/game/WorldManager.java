package org.macl.ctc.game;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.macl.ctc.Main;
import org.bukkit.Location;

import java.awt.*;
import java.util.ArrayList;

public class WorldManager {

    public boolean isUnloading = false;
    public String map = "map";

    public Location red;
    public Location blue;
    public ArrayList<Location> center;

    Main main;
    GameManager game;
    private World w;

    public WorldManager(Main main) {
        this.main = main;
        this.game = main.game;
        w = main.getServer().getWorld("map");
        // main.getConfig().getLocation(Bukkit.getWorld(map).getName() + ".red");
        center = getCenter();
    }

    // CLeanup world by unloading then loading world. Should be implemented with file systems to grab unloaded alternate maps.
    public void clean(Player p1) {
        isUnloading = true;
        // fix this to kick back with bungee cord
        boolean red = false;
        if(main.game.redHas(p1))
            red = true;
        else
            red = false;

        String text = "";
        if(red)
            text = p1.getName() + ChatColor.RED + " has destroyed the enemy core! Congratulations red team! Thanks for playing :)";
        else
            text = p1.getName() + ChatColor.BLUE + " has destroyed the enemy core! Congratulations blue team! Thanks for playing :)";

        for(Player p : Bukkit.getOnlinePlayers()) {
            p.teleport(Bukkit.getWorld("world").getSpawnLocation());
        }

        main.broadcast(text);

        // Allow time for server to load everyone into new world
        new BukkitRunnable() {

            public void run() {
                for(Chunk c : w.getLoadedChunks()) {
                    for(Entity e : c.getEntities()) {
                        if(!(e instanceof Player))
                            e.remove();
                    }
                }

                main.getServer().unloadWorld("map", false);

                main.getServer().dispatchCommand(Bukkit.getConsoleSender(), "mvload map");

                main.broadcast("load success");

                isUnloading = false;
            }
        }.runTaskLater(main, 40L);

    }

    public void setRed(Player p) {
        main.send(p, "Red spawn set", ChatColor.RED);
        main.getConfig().set(p.getLocation().getWorld().getName() + ".red", p.getLocation());
        main.saveConfig();
    }

    public void setBlue(Player p) {
        main.send(p, "Blue spawn set", ChatColor.BLUE);
        main.getConfig().set(p.getLocation().getWorld().getName() + ".blue", p.getLocation());
        main.saveConfig();
    }

    public void setCenter(Player p) {
        main.send(p, "Center set");
        main.getConfig().set(p.getLocation().getWorld().getName() + ".center", p.getLocation());
        main.saveConfig();
    }

    public Location getRed() {
        return new Location(Bukkit.getWorld("map"), 0, 75, 152);
    }

    public Location getBlue() {
        return new Location(Bukkit.getWorld("map"), 4, 75, -147);
    }

    public ArrayList<Location> getCenter() {
        ArrayList<Location> centers = new ArrayList<Location>();
        Location l = main.getConfig().getLocation(map + ".center");
        for(int x = -1; x < 2; x++) {
            for(int z = -1; z < 2; z++) {
                Location loc = new Location(Bukkit.getWorld(map), l.getBlockX() + x, l.getBlockY(), l.getBlockZ() + z);
                centers.add(loc);
            }
        }
        return centers;
    }

}
