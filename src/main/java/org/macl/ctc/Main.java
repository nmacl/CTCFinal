package org.macl.ctc;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.macl.ctc.events.Blocks;
import org.macl.ctc.events.Interact;
import org.macl.ctc.events.Players;
import org.macl.ctc.game.*;
import org.macl.ctc.kits.Kit;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.md_5.bungee.api.chat.TextComponent;

public final class Main extends JavaPlugin implements CommandExecutor, Listener {


    public String map = "sandstone";

    public GameManager game;
    public WorldManager worldManager;
    public KitManager kit;
    public StatsManager stats;
    public LeaderboardManager leaderboard;
    public String prefix = ChatColor.GOLD + "[CTC] " + ChatColor.GRAY;

    public void send(Player p, String text, ChatColor color) {
        p.sendMessage(prefix + color + text);
    }

    public void send(Player p, String text) {
        p.sendMessage(prefix + text);
    }

    public void broadcast(String text) {
        Bukkit.broadcastMessage(prefix + text);
    }

    public void broadcast(String text, ChatColor color) {
        Bukkit.broadcastMessage(prefix + color + text);
    }

    public ArrayList<Listener> listens = new ArrayList<Listener>();
    public ArrayList<Material> restricted = new ArrayList<Material>();

    Players playerListener;


    @Override
    public void onEnable() {

        // Plugin startup logic
        this.getCommand("ctc").setExecutor(this);
        getLogger().info("Started!");

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // 2) create stats.yml if it doesn't exist
        File statsFile = new File(getDataFolder(), "stats.yml");
        if (!statsFile.exists()) {
            try (PrintWriter out = new PrintWriter(statsFile)) {
                out.println("stats: {}");                // minimal valid YAML
            } catch (IOException e) {
                getLogger().severe("Could not create stats.yml: " + e.getMessage());
            }
        }

        // 3) now safely load it
        FileConfiguration statsCfg = YamlConfiguration.loadConfiguration(statsFile);
        this.stats = new StatsManager();
        stats.loadAll(statsCfg);


        kit = new KitManager(this);
        worldManager = new WorldManager(this);
        game = new GameManager(this);

        restricted.add(Material.OBSIDIAN);
        restricted.add(Material.NETHERITE_BLOCK);
        restricted.add(Material.LAPIS_ORE);
        restricted.add(Material.REDSTONE_ORE);
        restricted.add(Material.BEDROCK);
        restricted.add(Material.BARRIER);

        new Interact(this);
        new Blocks(this);
        playerListener = new Players(this);

        // Load map from config (set by Docker or manually in config.yml)
        String currentMap = getConfig().getString("current-map", "sandstone");
        map = currentMap;
        getLogger().info("Loading map: " + currentMap);
        worldManager.loadWorld("map", currentMap);

        for (Listener i : listens)
            getServer().getPluginManager().registerEvents(i, this);

        registerEvents();
        getCommand("stats").setExecutor(new org.macl.ctc.commands.StatsCommand(this));

        leaderboard = new LeaderboardManager(this);
        new BukkitRunnable() {
            @Override
            public void run() {
                leaderboard.init();
            }
        }.runTaskTimer(this, 0, 20 * 60);

        // auto start game
        new BukkitRunnable() {
            @Override
            public void run() {
                game.start();
            }
        }.runTaskLater(this, 20L);

    }

    private void registerEvents() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        if (stats != null) {
            File statsFile = new File(getDataFolder(), "stats.yml");
            FileConfiguration statsCfg = YamlConfiguration.loadConfiguration(statsFile);
            stats.saveAll(statsCfg);
            try {
                statsCfg.save(statsFile);
            } catch (IOException e) {
                getLogger().severe("Failed to save stats.yml: " + e.getMessage());
            }
        }
        getLogger().info("Ended");
    }

    private String formatLine(String label, int value) {
        return ChatColor.YELLOW
                + String.format("%-14s", label + ":")
                + ChatColor.WHITE
                + value;
    }

    private String formatLine(String label, String value) {
        return ChatColor.YELLOW
                + String.format("%-14s", label + ":")
                + ChatColor.WHITE
                + value;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
            Player p = (Player) sender;
            if (!p.isOp())
                return false;

            if (args[0].equalsIgnoreCase("reset")) {
                game.stop(p);
            } else if (args[0].equalsIgnoreCase("teleport")) {
                World w = Bukkit.getWorld(args[1]);
                p.teleport(w.getSpawnLocation());
            } else if (args[0].equalsIgnoreCase("start")) {
                game.start();
            }
            // change later so I can do maps (world manager)
            if (args[0].equalsIgnoreCase("red")) {
                String Map = args[1];
                worldManager.setRed(p, Map);
            } else if (args[0].equalsIgnoreCase("blue")) {
                String Map = args[1];
                worldManager.setBlue(p, Map);
            } else if (args[0].equalsIgnoreCase("center")) {
                String Map = args[1];
                worldManager.setCenter(p, Map);
            } else if (args[0].equalsIgnoreCase("direction")) {
                broadcast(p.getLocation().getDirection().toString());
            } else if (args[0].equalsIgnoreCase("kit")) {
                kit.openMenu(p);
            } else if (args[0].equalsIgnoreCase("map")) {
                map = args[1];
                worldManager.loadWorld("map", map);
                broadcast(args[1]);
            } else if (args[0].equalsIgnoreCase("tp")) {
                if (Bukkit.getWorld(args[1]) != null) {
                    p.teleport(Bukkit.getWorld(args[1]).getSpawnLocation());
                    broadcast("teleport");
                }
            } else if (args[0].equalsIgnoreCase("join")) {
                if (args.length < 2) {
                    send(p, "Usage: /ctc join <red|blue>", ChatColor.RED);
                } else if (args[1].equalsIgnoreCase("red")) {
                    game.joinRed(p);
                    send(p, "You have joined the Red team!", ChatColor.RED);
                } else if (args[1].equalsIgnoreCase("blue")) {
                    game.joinBlue(p);
                    send(p, "You have joined the Blue team!", ChatColor.BLUE);
                } else {
                    send(p, "Unknown team: " + args[1] + ". Use red or blue.", ChatColor.RED);
                }
                return true;
            }
        }

        // If the player (or console) uses our command correct, we can return true
        return true;
    }


    public void fakeExplode(
            Player p,
            Location l,
            int maxDamage,
            int maxDistance,
            boolean fire,
            boolean breaksBlocks,
            boolean damagesAllies
    ) {
        // 1) Clone the origin so we don't shift 'l' itself
        Location center = l.clone().add(0, 1, 0);
        World world = center.getWorld();

        // 2) Spawn the visual/ambient explosion
        world.createExplosion(center, 2f, fire, breaksBlocks);
        Bukkit.broadcastMessage("[DEBUG] Explosion at " + center);

        // 3) Prepare ray-trace heights
        final int numberOfRays = 6;
        double[] offsets = {0, 1, 2, 3, 4, 5};

        // 4) Loop through nearby entities only once
        for (Entity e : world.getNearbyEntities(center, maxDistance, maxDistance, maxDistance)) {
            if (!(e instanceof Player)) continue;
            Player target = (Player) e;

            // 4a) Never hit yourself
            if (target.getUniqueId().equals(p.getUniqueId())) {
                Bukkit.broadcastMessage("[DEBUG] Skipping self");
                continue;
            }

            // 4b) Skip same-team if we're not supposed to damage allies
            if (!damagesAllies && game.sameTeam(p.getUniqueId(), target.getUniqueId())) {
                Bukkit.broadcastMessage("[DEBUG] Skipping ally: " + target.getName());
                continue;
            }

            // 4c) Distance check
            double distance = center.distance(target.getLocation());
            if (distance > maxDistance) {
                Bukkit.broadcastMessage("[DEBUG] Out of range: " + target.getName() + " @ " + String.format("%.2f", distance));
                continue;
            }

            // 5) Count how many unobstructed rays hit
            int raysHit = 0;
            for (double offset : offsets) {
                Location start = center.clone().add(0, offset, 0);
                Vector dir = target.getLocation().toVector().subtract(start.toVector()).normalize();
                RayTraceResult result = world.rayTraceBlocks(
                        start,
                        dir,
                        distance,
                        FluidCollisionMode.NEVER,
                        true
                );
                if (result == null) {
                    raysHit++;
                }
            }

            Bukkit.broadcastMessage("[DEBUG] Rays hit on " + target.getName() + ": " + raysHit + "/" + numberOfRays);

            if (raysHit == 0) {
                Bukkit.broadcastMessage("[DEBUG] " + target.getName() + " is fully protected by obstacles.");
                continue;
            }

            // 6) If any rays got through, apply proportional damage
            double damageFactor = (double) raysHit / numberOfRays;
            double damage = maxDamage * (1 - (distance / maxDistance)) * damageFactor;
            Bukkit.broadcastMessage("[DEBUG] Calculated damage for " + target.getName()
                    + ": base=" + maxDamage
                    + ", dist=" + String.format("%.2f", distance)
                    + ", factor=" + String.format("%.2f", damageFactor)
                    + " => damage=" + String.format("%.2f", damage));

            double newHealth = target.getHealth() - damage;

            if (newHealth <= 0) {
                // target dies → record the kill for *you*
                Bukkit.broadcastMessage("[DEBUG] " + target.getName() + " would die (health after: " + newHealth + ")");
                target.setHealth(0);
                stats.recordKill(p);
                Bukkit.broadcastMessage("[DEBUG] Recorded kill for " + p.getName());
            } else {
                // just hurt them
                playerListener.tagLastDamager(target, p);
                target.setHealth(newHealth);
                Bukkit.broadcastMessage("[DEBUG] Damaged " + target.getName()
                        + " down to " + String.format("%.2f", newHealth));
            }
        }
    }


    public ItemStack coreCrush() {
        ItemStack crusher = new ItemStack(Material.DIAMOND_PICKAXE, 1);
        ItemMeta meta = crusher.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "CORE CRUSHER");
        meta.addEnchant(Enchantment.DIG_SPEED, 5, false);
        crusher.setItemMeta(meta);
        return crusher;
    }

    public HashMap<UUID, Kit> getKits() {
        return kit.kits;
    }

    public StatsManager getStats() {
        return stats;
    }
}
