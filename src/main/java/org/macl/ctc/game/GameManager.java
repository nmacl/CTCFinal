package org.macl.ctc.game;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.KeyedBossBar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;
import org.macl.ctc.Main;
import org.macl.ctc.kits.Kit;
import org.macl.ctc.kits.Spy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.UUID;

public class GameManager {

    private ArrayList<UUID> stack = new ArrayList<>();

    public boolean started = false;
    public boolean starting = false;
    private BukkitTask lobby;

    private Main main;
    private WorldManager world;
    private KitManager kit;

    public int center = 0;

    public int redCoreHealth = 3; // Number of times the red core needs to be mined
    public int blueCoreHealth = 3; // Number of times the blue core needs to be mined

    private Scoreboard gameScoreboard; // New scoreboard for each game
    private Team red;
    private Team blue;

    public GameManager(Main main) {
        this.main = main;
        this.world = main.worldManager;
        this.kit = main.kit;
        clean();
    }

    public void addTeam(Player p) {
        String name = p.getName();
        int redSize = getRed().getSize();
        int blueSize = getBlue().getSize();

        Bukkit.getLogger().info("Adding player " + name + " to a team. Red team size: " + redSize + ", Blue team size: " + blueSize);

        if (redSize > blueSize) {
            getBlue().addEntry(name);
            Bukkit.getLogger().info("Player " + name + " added to Blue team.");
        } else {
            getRed().addEntry(name);
            Bukkit.getLogger().info("Player " + name + " added to Red team.");
        }

        setup(p);
    }

    public void start() {
        started = true;
        starting = false;
        Collections.shuffle(stack);

        // Initialize a new scoreboard for the game
        gameScoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        register(); // Register teams on the game scoreboard

        // Assign the game scoreboard to all players
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setScoreboard(gameScoreboard);
        }

        for (Iterator<UUID> iterator = stack.iterator(); iterator.hasNext();) {
            UUID uuid = iterator.next();
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) {
                iterator.remove();
                continue;
            }
            addTeam(p);
        }

        main.broadcast("The game has begun! Destroy the other team's core to win!");
        stack.clear();

        // Initialize the scoreboard
        updateScoreboard(0, 0);
        center = 0;
    }

    private void setup(Player p) {
        teleportSpawn(p);
        AttributeInstance attribute = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        attribute.setBaseValue(20.0);
        p.setHealth(20);
        for (PotionEffect g : p.getActivePotionEffects())
            p.removePotionEffect(g.getType());
        new BukkitRunnable() {
            @Override
            public void run() {
                kit.openMenu(p);
            }
        }.runTaskLater(main, 3L);
    }

    public void clearPlayerDisplays() {
        // Clear the action bar for all players
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (Iterator<KeyedBossBar> it = Bukkit.getBossBars(); it.hasNext(); ) {
                BossBar bossBar = it.next();
                bossBar.removePlayer(player);
            }
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));

            // Clear the sidebar display
            Scoreboard scoreboard = player.getScoreboard();
            if (scoreboard != null) {
                Objective sidebarObjective = scoreboard.getObjective(DisplaySlot.SIDEBAR);
                if (sidebarObjective != null) {
                    sidebarObjective.unregister(); // Unregister the sidebar objective
                }
            }

            // Do not override the game scoreboard
            // Optionally reset player's scoreboard to main scoreboard after the game ends
            if (!started) {
                player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            }
        }
    }

    public void stop(Player p) {
        started = false;
        starting = false;
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p1 : Bukkit.getOnlinePlayers()) {
                    if (kit.kits.get(p1.getUniqueId()) != null) {
                        kit.kits.get(p1.getUniqueId()).cancelAllCooldowns();
                        kit.kits.get(p1.getUniqueId()).cancelAllRegen();
                    }
                }
                clean();
                world.clean(p);
                redCoreHealth = 3;
                blueCoreHealth = 3;
                kit.kits.clear();
                center = 0;
                // Clear the game scoreboard
                gameScoreboard = null;
            }
        }.runTaskLater(main, 60L);
    }

    public void stack(Player p) {
        p.teleport(p.getWorld().getSpawnLocation());
        if (started) {
            addTeam(p);
            return;
        }
        if (stack.contains(p.getUniqueId())) {
            main.send(p, "You have been removed from the stack");
            stack.remove(p.getUniqueId());
            if (stack.size() < 2 && lobby != null) {
                starting = false;
                lobby.cancel();
                main.broadcast("The game cannot begin until another player joins the stack!");
            }
        } else {
            stack.add(p.getUniqueId());
            if (stack.size() >= 2 && !starting && !started) {
                starting = true;
                lobby = new LobbyTimer(10).runTaskTimer(main, 0L, 20L);
            }
            main.send(p, "You have been added to the stack");
        }
    }

    public void respawn(final Player p) {
        if (!started || p.getWorld().getName().equalsIgnoreCase("world"))
            return;
        new BukkitRunnable() {
            int count = 8;

            public void run() {
                if (p == null || !p.isOnline()) {
                    resetPlayer(p, true);
                    this.cancel();
                    return;
                }
                count--;
                p.setLevel(count);
                if (count == 0) {
                    teleportSpawn(p);
                    kit.openMenu(p);
                    this.cancel();
                }
            }
        }.runTaskTimer(main, 0L, 20L);
    }


    public class LobbyTimer extends BukkitRunnable {
        int count = 60;

        public LobbyTimer(int count) {
            this.count = count;
        }

        public void run() {
            if (count % 5 == 0 || (count <= 5 && count != 0)) {
                main.broadcast("The game will begin in " + count + " seconds!");
            }
            if (count == 0) {
                this.cancel();
                start();
                stack.clear();
            }
            count--;
        }
    }

    public String centerString(int red, int blue) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < red; i++)
            builder.append(ChatColor.RED).append(ChatColor.BOLD).append("█").append(ChatColor.WHITE);
        for (int i = 0; i < blue; i++)
            builder.append(ChatColor.BLUE).append(ChatColor.BOLD).append("█").append(ChatColor.WHITE);
        for (int i = red + blue; i < 9; i++)
            builder.append(ChatColor.DARK_GRAY).append(ChatColor.BOLD).append("█").append(ChatColor.WHITE);

        return builder.toString();
    }

    public void resetCenter() {
        // SCHEDULER IMPLEMENTED BECAUSE BLOCK BREAK ISN'T DETECTED ON GET BLOCK AT
        if (!started)
            return;
        Bukkit.getScheduler().runTaskLater(main, () -> {
            int redCount = 0;
            int blueCount = 0;

            ArrayList<Location> centers = world.getCenter();
            for (Location locs : centers) {
                Block b = locs.getWorld().getBlockAt(locs);
                if (b.getType() == Material.RED_WOOL)
                    redCount++;
                else if (b.getType() == Material.BLUE_WOOL)
                    blueCount++;
            }

            // Update the scoreboard with the new center status
            updateScoreboard(redCount, blueCount);

            if (center != 0 && !(redCount >= 5 || blueCount >= 5)) {
                main.broadcast("The center has been reset!");
                center = 0;
            }
            if (redCount >= 5 && center != 1) {
                main.broadcast("Red has captured the center!", ChatColor.RED);
                center = 1;
            }
            if (blueCount >= 5 && center != 2) {
                main.broadcast("Blue has captured the center!", ChatColor.BLUE);
                center = 2;
            }

            switch (center) {
                case 0:
                    for (Player p : getReds())
                        p.getInventory().remove(Material.DIAMOND_PICKAXE);
                    for (Player p : getBlues())
                        p.getInventory().remove(Material.DIAMOND_PICKAXE);
                    break;
                case 1:
                    for (Player p : getBlues())
                        p.getInventory().remove(Material.DIAMOND_PICKAXE);
                    for (Player p : getReds())
                        if (!(main.getKits().get(p.getUniqueId()) instanceof Spy))
                            p.getInventory().setItem(8, main.coreCrush());
                    break;
                case 2:
                    for (Player p : getReds())
                        p.getInventory().remove(Material.DIAMOND_PICKAXE);
                    for (Player p : getBlues())
                        if (!(main.getKits().get(p.getUniqueId()) instanceof Spy))
                            p.getInventory().setItem(8, main.coreCrush());
                    break;
            }
        }, 2L);
    }

    // ******************************//
    //         TEAM MANAGER          //
    // ******************************//

    public boolean resetPlayer(Player p, boolean quit) {
        // Super reset the player as if they never joined the server
        // 1. Clear Inventory and Armor
        p.getInventory().clear();
        p.getInventory().setArmorContents(null);

        // 2. Reset Health and Food
        AttributeInstance attribute = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        attribute.setBaseValue(20.0);
        p.setHealth(attribute.getDefaultValue());
        p.setFoodLevel(20);
        p.setSaturation(20);

        // 3. Reset Experience and Level
        p.setTotalExperience(0);
        p.setExp(0);
        p.setLevel(0);

        // 4. Remove Potion Effects
        for (PotionEffect effect : p.getActivePotionEffects())
            p.removePotionEffect(effect.getType());

        // 5. Reset Fire Ticks and Other States
        p.setFireTicks(0);
        p.setFallDistance(0);
        p.setRemainingAir(p.getMaximumAir());

        // 6. Reset Ender Chest
        p.getEnderChest().clear();

        // 7. Remove from Teams
        boolean wasOnTeam = false;
        if (redHas(p)) {
            getRed().removeEntry(p.getName());
            wasOnTeam = true;
        }
        if (blueHas(p)) {
            getBlue().removeEntry(p.getName());
            wasOnTeam = true;
        }

        // 8. Reset Player Location to Default World Spawn
        World defaultWorld = Bukkit.getWorld("world");
        if (defaultWorld == null) {
            defaultWorld = Bukkit.createWorld(new WorldCreator("world"));
        }
        p.teleport(defaultWorld.getSpawnLocation());

        // 9. Reset Kit Data
        Kit playerKit = kit.kits.remove(p.getUniqueId());
        if (playerKit != null) {
            playerKit.cancelAllCooldowns();
            playerKit.cancelAllRegen();
        }

        // 10. Clear Player Display (Action Bar, Boss Bars, etc.)
        clearPlayerDisplaysForPlayer(p);

        // 11. Reset Player Scoreboard to Main Scoreboard
        p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());

        // 12. Send Leave Message if Quitting
        if (quit) {
            main.broadcast(p.getName() + " has left the game!");
        }

        return wasOnTeam;
    }

    private void clearPlayerDisplaysForPlayer(Player player) {
        // Remove Boss Bars
        for (Iterator<KeyedBossBar> it = Bukkit.getBossBars(); it.hasNext(); ) {
            BossBar bossBar = it.next();
            bossBar.removePlayer(player);
        }
        // Clear Action Bar
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));

        // Clear Sidebar Display
        Scoreboard scoreboard = player.getScoreboard();
        if (scoreboard != null) {
            Objective sidebarObjective = scoreboard.getObjective(DisplaySlot.SIDEBAR);
            if (sidebarObjective != null) {
                sidebarObjective.unregister(); // Unregister the sidebar objective
            }
        }
    }

    public ArrayList<Player> getReds() {
        ArrayList<Player> reds = new ArrayList<>();
        if (getRed() != null) {
            for (String s : getRed().getEntries())
                if (Bukkit.getPlayer(s) != null)
                    reds.add(Bukkit.getPlayer(s));
        }
        return reds;
    }

    public ArrayList<Player> getBlues() {
        ArrayList<Player> blues = new ArrayList<>();
        if (getBlue() != null) {
            for (String s : getBlue().getEntries())
                if (Bukkit.getPlayer(s) != null)
                    blues.add(Bukkit.getPlayer(s));
        }
        return blues;
    }

    public boolean redHas(Player p) {
        return getRed() != null && getRed().hasEntry(p.getName());
    }

    public boolean blueHas(Player p) {
        return getBlue() != null && getBlue().hasEntry(p.getName());
    }

    public Team getRed() {
        return gameScoreboard != null ? gameScoreboard.getTeam("red") : null;
    }

    public Team getBlue() {
        return gameScoreboard != null ? gameScoreboard.getTeam("blue") : null;
    }

    public void clean() {
        // Unregister teams and objectives from the gameScoreboard
        if (gameScoreboard != null) {
            for (Team t : gameScoreboard.getTeams())
                t.unregister();

            for (Objective obj : gameScoreboard.getObjectives())
                obj.unregister();
        }

        red = null;
        blue = null;

        // Reset the game scoreboard
        gameScoreboard = null;

        for (Player p : Bukkit.getOnlinePlayers()) {
            resetPlayer(p, false);
            // Reset the player's scoreboard to main scoreboard
            p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
        clearPlayerDisplays();
    }

    public static final String RED_TEAM_NAME = "red";
    public static final String BLUE_TEAM_NAME = "blue";

    public void register() {
        Scoreboard board = gameScoreboard;
        red = board.getTeam(RED_TEAM_NAME);
        blue = board.getTeam(BLUE_TEAM_NAME);

        // Register teams if they do not already exist
        if (red == null) {
            red = board.registerNewTeam(RED_TEAM_NAME);
        }
        if (blue == null) {
            blue = board.registerNewTeam(BLUE_TEAM_NAME);
        }

        // Set properties for the red team
        if (red != null) {
            red.setColor(ChatColor.RED);
            red.setPrefix(ChatColor.RED.toString());
            red.setDisplayName(ChatColor.RED + "Red");
            red.setCanSeeFriendlyInvisibles(true);
            red.setAllowFriendlyFire(false);
        } else {
            Bukkit.getLogger().warning("Failed to register or retrieve the red team.");
        }

        // Set properties for the blue team
        if (blue != null) {
            blue.setColor(ChatColor.BLUE);
            blue.setPrefix(ChatColor.BLUE.toString());
            blue.setDisplayName(ChatColor.BLUE + "Blue");
            blue.setCanSeeFriendlyInvisibles(true);
            blue.setAllowFriendlyFire(false);
        } else {
            Bukkit.getLogger().warning("Failed to register or retrieve the blue team.");
        }
    }

    public void teleportSpawn(Player p) {
        World mapWorld = Bukkit.getWorld("map");
        if (mapWorld == null) {
            mapWorld = Bukkit.createWorld(new WorldCreator("map"));
        }
        p.teleport(mapWorld.getSpawnLocation());
        if (redHas(p))
            p.teleport(world.getRed());
        if (blueHas(p))
            p.teleport(world.getBlue());
    }

    /**
     * Updates the scoreboard with the current center status and core health.
     *
     * @param redCount  Number of center blocks captured by red team
     * @param blueCount Number of center blocks captured by blue team
     */
    public void updateScoreboard(int redCount, int blueCount) {
        if (gameScoreboard == null) {
            return;
        }

        Objective objective = gameScoreboard.getObjective("GameInfo");
        if (objective == null) {
            objective = gameScoreboard.registerNewObjective("GameInfo", "dummy", ChatColor.GOLD + "" + ChatColor.BOLD + "Game Status");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        // Clear existing scores
        for (String entry : gameScoreboard.getEntries()) {
            gameScoreboard.resetScores(entry);
        }

        // Add core health display with labels "Core Health", "Blue:", and "Red:"
        String coreHealthHeader = ChatColor.GOLD + "Core Health";
        String blueCore = ChatColor.BLUE + "Blue: " + heartString(blueCoreHealth);
        String redCore = ChatColor.RED + "Red: " + heartString(redCoreHealth);

        // Add center status display
        String centerStatus = ChatColor.GREEN + "Center Control:";
        String centerControl = centerString(redCount, blueCount);

        // Set scores with center control first, and core health below
        objective.getScore(" ").setScore(7); // Blank line for spacing
        objective.getScore(centerStatus).setScore(6);
        objective.getScore(centerControl).setScore(5);
        objective.getScore("  ").setScore(4); // Another blank line
        objective.getScore(coreHealthHeader).setScore(3);
        objective.getScore(blueCore).setScore(2);
        objective.getScore(redCore).setScore(1);
    }

    // Override method that only updates the core health
    public void updateScoreboard() {
        if (gameScoreboard == null) {
            return;
        }

        Objective objective = gameScoreboard.getObjective("GameInfo");
        if (objective == null) {
            objective = gameScoreboard.registerNewObjective("GameInfo", "dummy", ChatColor.GOLD + "" + ChatColor.BOLD + "Game Status");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        // Clear only the core health related scores
        gameScoreboard.resetScores(ChatColor.BLUE + "Blue: " + heartString(blueCoreHealth));
        gameScoreboard.resetScores(ChatColor.RED + "Red: " + heartString(redCoreHealth));

        // Add core health display with labels "Core Health", "Blue:", and "Red:"
        String coreHealthHeader = ChatColor.GOLD + "Core Health";
        String blueCore = ChatColor.BLUE + "Blue: " + heartString(blueCoreHealth);
        String redCore = ChatColor.RED + "Red: " + heartString(redCoreHealth);

        // Update the scores for core health without modifying the center control
        objective.getScore("  ").setScore(4); // Ensure this blank line remains
        objective.getScore(coreHealthHeader).setScore(3);
        objective.getScore(blueCore).setScore(2);
        objective.getScore(redCore).setScore(1);
    }


    /**
     * Generates a string of hearts representing the core health.
     *
     * @param health The current health of the core
     * @return A string representing the core health with hearts
     */
    private String heartString(int health) {
        StringBuilder builder = new StringBuilder();
        int maxHealth = 3; // Maximum core health
        for (int i = 0; i < health; i++) {
            builder.append("❤ ");
        }
        for (int i = health; i < maxHealth; i++) {
            builder.append(ChatColor.DARK_GRAY).append("❤ ").append(ChatColor.RESET);
        }
        return builder.toString().trim();
    }
}
