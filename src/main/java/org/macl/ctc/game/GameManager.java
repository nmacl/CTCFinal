package org.macl.ctc.game;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;
import org.macl.ctc.Main;
import org.macl.ctc.kits.Kit;
import org.macl.ctc.kits.Spy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

public class GameManager {

    ArrayList<UUID> stack = new ArrayList<UUID>();

    public boolean started = false;
    public boolean starting = false;
    BukkitTask lobby;

    Main main;
    WorldManager world;
    KitManager kit;

    public int center = 0;

    public GameManager(Main main) {
        this.main = main;
        this.world = main.worldManager;
        this.kit = main.kit;
        clean();
    }

    public void start() {
        started = true;
        Collections.shuffle(stack);
        for(UUID uuid : stack) {
            Player p = Bukkit.getPlayer(uuid);
            if(p == null) {
                stack.remove(uuid);
                continue;
            }
            String name = p.getName();
            int redSize = getRed().getSize();
            int blueSize = getBlue().getSize();
            if(redSize > blueSize)
                getBlue().addEntry(name);
            else if(redSize == blueSize)
                getRed().addEntry(name);
            else if(redSize < blueSize)
                getBlue().addEntry(name);
            setup(p);
        }

        main.broadcast("The game has begun! Destroy the other teams core (obsidian) to win!");
        stack.clear();
        Objective objective = scoreboard().registerNewObjective("sidebar", "dummy", main.prefix + "Center Status");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        Score onlineName = objective.getScore(centerString(0,0));
        onlineName.setScore(1);
        center = 0;
    }
    private void setup(Player p) {
        teleportSpawn(p);
        kit.openMenu(p);
        p.setHealth(20);
        for(PotionEffect g : p.getActivePotionEffects())
            p.removePotionEffect(g.getType());
    }

    public void stop(Player p) {
        world.clean(p);
        clean();
        kit.kits.clear();
        started = false;
        starting = false;
        center = 0;
    }
    public void stack(Player p) {
        p.teleport(p.getWorld().getSpawnLocation());
        if(stack.contains(p.getUniqueId())) {
            main.send(p, "You have been removed from the stack");
            stack.remove(p.getUniqueId());
            if(stack.size() < 2 && lobby != null) {
                starting = false;
                lobby.cancel();
                main.broadcast("The game cannot begin until another player joins the stack!");
            }
        } else {
            stack.add(p.getUniqueId());
            if (stack.size() >= 2 && starting == false) {
                starting = true;
                lobby = new LobbyTimer(5).runTaskTimer(main, 0L, 20L);
            }
            main.send(p, "You have been added to the stack");
        }
    }

    public void respawn(final Player p) {
        if(main.game.started == false || p.getWorld().getName().equalsIgnoreCase("world"))
            return;
        new BukkitRunnable() {
            int count = 8;

            public void run() {
                if(p == null) {
                    this.cancel();
                    return;
                }
                count--;
                p.setLevel(count);
                if(count == 0) {
                    if(redHas(p))
                        p.teleport(world.getRed());
                    if(blueHas(p))
                        p.teleport(world.getBlue());
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
            if(count % 5 == 0 || (count <= 5 && count != 0)) {
                main.broadcast("The game will begin in " + count + " seconds!");
            }
            if(count == 0) {
                this.cancel();
                start();
                stack.clear();
            }
            count--;
        }
    }

    public String centerString(int red, int blue) {
        StringBuilder builder = new StringBuilder();

        for(int i = 0; i < red; i++)
            builder.append(ChatColor.RED + ""  +ChatColor.BOLD + "||" + ChatColor.WHITE);
        for(int i = 0; i < blue; i++)
            builder.append(ChatColor.BLUE + "" + ChatColor.BOLD + "||" + ChatColor.WHITE);
        for(int i = red+blue; i < 9; i++)
            builder.append(ChatColor.BOLD + "||");

        return builder.toString();
    }


    public void resetCenter() {
        //SCHEDULER IMPLEMENTED BECAUSE BLOCK BREAK ISNT DETECTED ON GET BLOCKAT
        if(!started)
            return;
        Bukkit.getScheduler().runTaskLater(main, new Runnable() {
            public void run() {
                int red = 0;
                int blue = 0;


                ArrayList<Location> centers = world.getCenter();
                for (Location locs : centers) {
                    Block b = locs.getWorld().getBlockAt(locs);
                    if (b.getType() == Material.RED_WOOL)
                        red++;
                    else if (b.getType() == Material.BLUE_WOOL)
                        blue++;

                }

                for(Objective obj : scoreboard().getObjectives())
                    obj.unregister();


                Objective objective = scoreboard().registerNewObjective("sidebar", "dummy", main.prefix + "Center");
                objective.setDisplaySlot(DisplaySlot.SIDEBAR);

                Score onlineName = objective.getScore(centerString(red,blue));
                onlineName.setScore(1);

                if (center != 0 && !(red >= 5 || blue >= 5)) {
                    main.broadcast("The center has been reset!");
                    center = 0;
                }
                if (red >= 5 && center != 1) {
                    main.broadcast("Red has captured the center!", ChatColor.RED);
                    center = 1;
                }
                if (blue >= 5 && center != 2) {
                    main.broadcast("Blue has captured the center!", ChatColor.BLUE);
                    center = 2;
                }

                switch (center) {
                    case(0):
                        for(Player p : getReds())
                            p.getInventory().remove(Material.DIAMOND_PICKAXE);
                        for(Player p : getBlues())
                            p.getInventory().remove(Material.DIAMOND_PICKAXE);
                        break;
                    case(1):
                        for(Player p : getBlues())
                            p.getInventory().remove(Material.DIAMOND_PICKAXE);
                        for(Player p : getReds())
                            if(!(main.getKits().get(p.getUniqueId()) != null && main.getKits().get(p.getUniqueId()) instanceof Spy))
                                p.getInventory().setItem(8, main.coreCrush());
                        break;
                    case(2):
                        for(Player p : getReds())
                            p.getInventory().remove(Material.DIAMOND_PICKAXE);
                        for(Player p : getBlues())
                            if(!(main.getKits().get(p.getUniqueId()) != null && main.getKits().get(p.getUniqueId()) instanceof Spy))
                                p.getInventory().setItem(8, main.coreCrush());
                        break;

                }
            }
        }, 2L);
    }

    //******************************//
    //           TEAM MANAGER        //
    //******************************//

    public ArrayList<Player> getReds() {
        ArrayList<Player> reds = new ArrayList<Player>();
        for(String s : getRed().getEntries())
            if(Bukkit.getPlayer(s) != null)
                reds.add(Bukkit.getPlayer(s));
        return reds;
    }

    public ArrayList<Player> getBlues() {
        ArrayList<Player> blues = new ArrayList<Player>();
        for(String s : getBlue().getEntries())
            if(Bukkit.getPlayer(s) != null)
                blues.add(Bukkit.getPlayer(s));
        return blues;
    }
    // Maybe add some checks whether the player is offline? If they're on a team but not online kick em?
    public boolean redHas(Player p) {
        for(String p1 : getRed().getEntries()) {
            if(p.getName().equalsIgnoreCase(p1))
                return true;
        }
        return false;
    }
    public boolean blueHas(Player p) {
        for(String p1 : getBlue().getEntries()) {
            if(p.getName().equalsIgnoreCase(p1))
                return true;
        }
        return false;
    }
    public Team getRed() {
        return red;
    }

    public Team getBlue() {
        return blue;
    }

    Team red;
    Team blue;

    public void clean() {
        for(Team t : scoreboard().getTeams())
            t.unregister();

        for(Objective obj : scoreboard().getObjectives())
            obj.unregister();

        register();

        for(Player p : Bukkit.getOnlinePlayers()) {
            PlayerInventory e = p.getInventory();
            e.setArmorContents(null);
            e.clear();
            p.setHealth(20);
            p.setFireTicks(0);
            for(PotionEffect potions : p.getActivePotionEffects())
                p.removePotionEffect(potions.getType());
        }
    }

    public void register() {
        Scoreboard board = scoreboard();
        board.registerNewTeam("red");
        board.registerNewTeam("blue");
        red = board.getTeam("red");
        blue = board.getTeam("blue");
        red.setColor(ChatColor.RED);
        blue.setColor(ChatColor.BLUE);
        red.setPrefix(ChatColor.RED + "");
        blue.setPrefix(ChatColor.BLUE + "");
        red.setDisplayName(ChatColor.RED + "Red");
        blue.setDisplayName(ChatColor.BLUE + "Blue");
        red.setCanSeeFriendlyInvisibles(true);
        //red.setAllowFriendlyFire(false);
        blue.setCanSeeFriendlyInvisibles(true);
        //blue.setAllowFriendlyFire(false);
    }
    public Scoreboard scoreboard() {
        return Bukkit.getScoreboardManager().getMainScoreboard();
    }

    public void teleportSpawn(Player p) {
        p.teleport(Bukkit.getWorld("map").getSpawnLocation());
        if(getRed().hasEntry(p.getName()))
            p.teleport(world.getRed());
        if(getBlue().hasEntry(p.getName()))
            p.teleport(world.getBlue());
    }
}

