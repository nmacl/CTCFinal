package org.macl.ctc;

import net.md_5.bungee.api.chat.ClickEvent;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.macl.ctc.events.Blocks;
import org.macl.ctc.events.Interact;
import org.macl.ctc.events.Players;
import org.macl.ctc.game.GameManager;
import org.macl.ctc.game.KitManager;
import org.macl.ctc.game.WorldManager;
import org.macl.ctc.kits.Kit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.md_5.bungee.api.chat.TextComponent;

public final class Main extends JavaPlugin implements CommandExecutor {


    public String map = "sandstone";

    public GameManager game;
    public WorldManager worldManager;
    public KitManager kit;
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

    @Override
    public void onEnable() {
        // Plugin startup logic
        this.getCommand("ctc").setExecutor(this);
        getLogger().info("Started!");
        kit = new KitManager(this);
        worldManager = new WorldManager(this);
        game = new GameManager(this);

        restricted.add(Material.OBSIDIAN);
        restricted.add(Material.NETHERITE_BLOCK);
        restricted.add(Material.LAPIS_ORE);
        restricted.add(Material.REDSTONE_ORE);

        new Interact(this);
        new Blocks(this);
        new Players(this);

        worldManager.loadWorld("map", "sandstone");

        for(Listener i : listens)
            getServer().getPluginManager().registerEvents(i, this);
        new BukkitRunnable() {
            @Override
            public void run() {
                TextComponent message = new TextComponent("discord click this");
                TextComponent link = new TextComponent("https://discord.gg/Qeme8MUXBY");
                link.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://discord.gg/Qeme8MUXBY"));
                link.setColor(net.md_5.bungee.api.ChatColor.AQUA);
                link.setUnderlined(true);

                message.addExtra(link);

                Bukkit.spigot().broadcast(message);
            }
        }.runTaskTimer(this, 0L, 180*20);
        // Setup map / game
    }
    /*private Map<Location, Location> teleporterPairs = new HashMap<>();
    public boolean isTeleporterLocation(Location loc) {
        return teleporterPairs.containsKey(loc) || teleporterPairs.containsValue(loc);
    }

    // Handle teleportation between paired locations
    public void handleTeleport(Player player, Location currentLocation) {
        Location destination = teleporterPairs.getOrDefault(currentLocation, null);
        if (destination == null) {
            // If not found as key, it might be the value in a key-value pair
            for (Map.Entry<Location, Location> entry : teleporterPairs.entrySet()) {
                if (entry.getValue().equals(currentLocation)) {
                    destination = entry.getKey();
                    break;
                }
            }
        }
        if (destination != null && player.hasPermission("teleporter.use")) {
            player.teleport(destination.add(0, 1, 0)); // Teleport just above to prevent embedding in the block
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        }
    }

    // Register a pair of teleporters
    public void registerTeleporterPair(Location loc1, Location loc2) {
        teleporterPairs.put(loc1, loc2);
    }

    // Unregister teleporters when destroyed or deactivated
    public void unregisterTeleporter(Location loc) {
        teleporterPairs.remove(loc);
        teleporterPairs.values().remove(loc);
    }*/

    @Override
    public void onDisable() {
        getLogger().info("Ended");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
            Player p = (Player) sender;
            if(!p.isOp())
                return false;

            if(args[0].equalsIgnoreCase("reset")) {
                game.stop(p);
            } else if(args[0].equalsIgnoreCase("teleport")) {
                World w = Bukkit.getWorld(args[1]);
                p.teleport(w.getSpawnLocation());
            } else if(args[0].equalsIgnoreCase("start")) {
                game.start();
            }
            // change later so I can do maps (world manager)
            if(args[0].equalsIgnoreCase("red")) {
                String Map = args[1];
                worldManager.setRed(p, Map);
            } else if(args[0].equalsIgnoreCase("blue")) {
                String Map = args[1];
                worldManager.setBlue(p, Map);
            } else if(args[0].equalsIgnoreCase("center")) {
                String Map = args[1];
                worldManager.setCenter(p, Map);
            }  else if(args[0].equalsIgnoreCase("direction")) {
                broadcast(p.getLocation().getDirection().toString());
            } else if(args[0].equalsIgnoreCase("kit")) {
                kit.openMenu(p);
            } else if(args[0].equalsIgnoreCase("map")) {
                map = args[1];
                worldManager.loadWorld("map", map);
                broadcast(args[1]);
            } else if (args[0].equalsIgnoreCase("tp")) {
                if(Bukkit.getWorld(args[1]) != null) {
                    p.teleport(Bukkit.getWorld(args[1]).getSpawnLocation());
                    broadcast("teleport");
                }
            }
        }

        // If the player (or console) uses our command correct, we can return true
        return true;
    }

    public void fakeExplode(Player p, Location l, int maxDamage, int maxDistance, boolean damage1, boolean fire) {
        Location center = l.add(0, 1, 0); // Center of explosion
        World world = center.getWorld();
        world.createExplosion(center, 2f, damage1, fire); // Visual explosion only

        int numberOfRays = 6; // Total number of rays to cast
        double[] offsets = {0, 1, 2, 3, 4, 5}; // Vertical offsets for ray casting

        for (org.bukkit.entity.Entity entity : world.getNearbyEntities(center, maxDistance, maxDistance, maxDistance)) {
            if (entity instanceof Player) {
                Player player = (Player) entity;
                if(player.getUniqueId() == p.getUniqueId())
                    continue;
                Location playerLocation = player.getLocation();

                double distance = center.distance(playerLocation);
                if (distance <= maxDistance) {
                    double raysHit = 0;

                    // Perform multiple ray traces at different heights
                    for (double offset : offsets) {
                        Location rayStart = center.clone().add(0, offset, 0);
                        RayTraceResult result = world.rayTraceBlocks(rayStart, playerLocation.toVector().subtract(rayStart.toVector()).normalize(), distance, FluidCollisionMode.NEVER, true);

                        if (result == null) { // No block in the way for this ray
                            raysHit++;
                        }
                    }

                    if (raysHit > 0) {
                        Bukkit.broadcastMessage("Rays hit: " + raysHit);
                        double damageFactor = raysHit / numberOfRays;
                        double damage = maxDamage * (1 - (distance / maxDistance)) * damageFactor;
                        Bukkit.broadcastMessage("Attempting to damage " + player.getName() + " with " + damage + " damage.");

                        // Apply proportional damage to the player
                        if (player.getHealth() - damage < 0) {
                            player.setHealth(0); // Ensure health does not go negative
                        } else {
                            player.setHealth(player.getHealth() - damage);
                        }
                    } else {
                        Bukkit.broadcastMessage(player.getName() + " is fully protected by obstacles.");
                    }
                }
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
}
