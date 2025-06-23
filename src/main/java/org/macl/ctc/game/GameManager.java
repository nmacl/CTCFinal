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

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.ToIntFunction;

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
        main.getStats().recordGamePlayed(p);
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

        // Add each shuffled player to a team
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

        // --- TIP SCHEDULING ---

        // 1) Define your tips as (title, subtitle) pairs
        List<String[]> tips = Arrays.asList(
                new String[]{ "",                              ChatColor.AQUA   + "Press right-click to use an ability" },
                new String[]{ "",                              ChatColor.GREEN  + "Place your colored wool in the netherite center" },
                new String[]{ "",                              ChatColor.RED  +   "Capturing the center gives you the diamond pickaxe" },
                new String[]{ ChatColor.BOLD + "Have fun!",    ChatColor.GOLD   + "Use the diamond pickaxe to destroy the enemy team's obsidian" }
        );

        // 2) Timing constants (in ticks)
        final int FADE_IN  = 20;
        final int STAY     = 60;
        final int FADE_OUT = 30;
        final long INTERVAL = FADE_IN + STAY + FADE_OUT;  // ensures no overlap

        // 3) Schedule a repeating task
        new BukkitRunnable() {
            int index = 0;
            @Override
            public void run() {
                if (index >= tips.size()) {
                    // All tips shown ⇒ stop repeating
                    cancel();
                    return;
                }

                // Broadcast the current tip to everyone on the "map" world
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!p.getWorld().getName().equals("map")) continue;
                    String title    = tips.get(index)[0];
                    String subtitle = tips.get(index)[1];
                    p.sendTitle(title, subtitle, FADE_IN, STAY, FADE_OUT);
                }

                index++;
            }
        }
                // first run 80 ticks (4s) after start(), then every INTERVAL ticks
                .runTaskTimer(main, /* initialDelay= */120L, /* period= */INTERVAL);
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
    private void broadcastLiveGameAwards() {
        // 1) Grab all online players
        List<Player> alive = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (alive.isEmpty()) return;

        StatsManager sm = main.getStats();

        // 2) Utility to find everyone tied for the top of a stat
        BiFunction<ToIntFunction<Player>, String, Void> award = (statFn, label) -> {
            int best = alive.stream()
                    .mapToInt(statFn)
                    .max()
                    .orElse(0);

            List<String> names = alive.stream()
                    .filter(p -> statFn.applyAsInt(p) == best)
                    .map(Player::getName)
                    .toList();

            String joined = String.join(", ", names);
            main.broadcast(
                    label
                            + ChatColor.WHITE + joined
                            + ChatColor.GRAY + " (" + best + ")",
                    ChatColor.GOLD
            );
            return null;
        };

        // 3) Broadcast with your colors & labels
        main.broadcast(ChatColor.GOLD + "=== Live Game Awards ===");

        award.apply(
                p -> sm.get(p.getUniqueId()).kills(),
                ChatColor.RED +   "⫸ Offensive MVP(s): "
        );

        award.apply(
                p -> sm.get(p.getUniqueId()).captures(),
                ChatColor.AQUA +  "⫸ Defense MVP(s): "
        );

        award.apply(
                p -> sm.get(p.getUniqueId()).coreCracks(),
                ChatColor.DARK_PURPLE + "⫸ Core MVP(s): "
        );

        award.apply(
                p -> sm.get(p.getUniqueId()).deaths(),
                ChatColor.DARK_RED + "⫸ LVP(s): "
        );

        main.broadcast(ChatColor.GOLD + "=========================");
    }



    public void stop(Player stopper) {
        started  = false;
        starting = false;

        boolean redWin = redHas(stopper);
        Collection<Player> winners = redWin ? getReds() : getBlues();
        for (Player p : winners) {
            main.getStats().recordWin(p);
        }

        broadcastLiveGameAwards();

        new BukkitRunnable() {
            @Override public void run() {

                // 1) wipe the map FIRST
                world.clean(stopper);      // or world.cleanAll() if it affects blocks only

                // 2) now reset all players once
                for (Player pl : Bukkit.getOnlinePlayers()) {
                    if (kit.kits.containsKey(pl.getUniqueId())) {
                        kit.kits.get(pl.getUniqueId()).cancelAllCooldowns();
                        kit.kits.get(pl.getUniqueId()).cancelAllRegen();
                        kit.kits.get(pl.getUniqueId()).cancelAllTasks();
                    }
                }
                clean();                   // <- teleports everyone to “world” exactly once

                redCoreHealth  = 3;
                blueCoreHealth = 3;
                kit.kits.clear();
                center = 0;
                gameScoreboard = null;
            }
        }.runTaskLater(main, 60L);
    }


    public void stack(Player p) {
        p.teleport(p.getWorld().getSpawnLocation());
        if (started) {
            // IMPORTANT: Set the player's scoreboard to the game scoreboard BEFORE adding to team
            if (gameScoreboard != null) {
                p.setScoreboard(gameScoreboard);
            }
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

    public Player getRandomTeammate(Player p) {
        List<Player> teammates = new ArrayList<>();

        // Check if player is on red or blue team
        boolean isPlayerRed = main.game.redHas(p);
        boolean isPlayerBlue = main.game.blueHas(p);

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online != p && online.isOnline() &&
                    !online.getGameMode().equals(GameMode.SPECTATOR)) {

                // Add teammate if they're on the same team
                if ((isPlayerRed && main.game.redHas(online)) ||
                        (isPlayerBlue && main.game.blueHas(online))) {
                    teammates.add(online);
                }
            }
        }

        if (!teammates.isEmpty()) {
            return teammates.get(new Random().nextInt(teammates.size()));
        }

        return null;
    }
    public boolean sameTeam(UUID uuid1, UUID uuid2) {
        Player p1 = Bukkit.getPlayer(uuid1);
        Player p2 = Bukkit.getPlayer(uuid2);

        // if either player isn’t online or doesn’t exist, they can’t be on the same team
        if (p1 == null || p2 == null) {
            return false;
        }

        if (!main.game.started) { // change conditions to make testing outside matches possible
            Scoreboard score = Bukkit.getScoreboardManager().getMainScoreboard();
            String p1Team = "1";
            String p2Team = "2";

            boolean sameTeam;

            for (Team team : score.getTeams()) {
                if (team.hasEntry(p1.getName())) {
                    p1Team = team.getName();
                }
                if (team.hasEntry(p2.getName())) {
                    p2Team = team.getName();
                }
            }

            sameTeam = p1Team.equals(p2Team);

            return sameTeam;
        }


        // both on red?
        boolean bothRed = getRed() != null
                && getRed().hasEntry(p1.getName())
                && getRed().hasEntry(p2.getName());

        // both on blue?
        boolean bothBlue = getBlue() != null
                && getBlue().hasEntry(p1.getName())
                && getBlue().hasEntry(p2.getName());

        return bothRed || bothBlue;
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

    public void resetCenter(@Nullable Player capturer) {
        if (!started) return;

        Bukkit.getScheduler().runTaskLater(main, () -> {
            int redCount = 0, blueCount = 0;
            for (Location loc : world.getCenter()) {
                Block b = loc.getWorld().getBlockAt(loc);
                if      (b.getType() == Material.RED_WOOL)  redCount++;
                else if (b.getType() == Material.BLUE_WOOL) blueCount++;
            }

            updateScoreboard(redCount, blueCount);

            // did we lose control?
            if (center != 0 && redCount < 5 && blueCount < 5) {
                main.broadcast("The center has been reset!");
                center = 0;
            }

            // red capture event
            if (redCount >= 5 && center != 1) {
                main.broadcast("Red has captured the center!", ChatColor.RED);
                center = 1;

                // only record if someone explicitly triggered it
                if (capturer != null) {
                    main.getStats().recordCapture(capturer.getUniqueId());
                    StatsManager.PlayerStats ps = main.getStats().get(capturer.getUniqueId());
                    capturer.sendMessage(ChatColor.RED +
                            "You captured the center! Total captures: " + ps.captures());
                }
            }

            // blue capture event
            if (blueCount >= 5 && center != 2) {
                main.broadcast("Blue has captured the center!", ChatColor.BLUE);
                center = 2;

                if (capturer != null) {
                    main.getStats().recordCapture(capturer.getUniqueId());
                    StatsManager.PlayerStats ps = main.getStats().get(capturer.getUniqueId());
                    capturer.sendMessage(ChatColor.BLUE +
                            "You captured the center! Total captures: " + ps.captures());
                }
            }

            // give or remove pickaxes as before
            switch (center) {
                case 0 -> {
                    getReds().forEach(p -> p.getInventory().remove(Material.DIAMOND_PICKAXE));
                    getBlues().forEach(p -> p.getInventory().remove(Material.DIAMOND_PICKAXE));
                }
                case 1 -> {
                    getBlues().forEach(p -> p.getInventory().remove(Material.DIAMOND_PICKAXE));
                    getReds().forEach(p -> {
                        if (!(main.getKits().get(p.getUniqueId()) instanceof Spy))
                            p.getInventory().setItem(8, main.coreCrush());
                    });
                }
                case 2 -> {
                    getReds().forEach(p -> p.getInventory().remove(Material.DIAMOND_PICKAXE));
                    getBlues().forEach(p -> {
                        if (!(main.getKits().get(p.getUniqueId()) instanceof Spy))
                            p.getInventory().setItem(8, main.coreCrush());
                    });
                }
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
            playerKit.cancelAllTasks();
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
