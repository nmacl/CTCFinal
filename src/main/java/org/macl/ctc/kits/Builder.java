package org.macl.ctc.kits;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.macl.ctc.Main;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Builder extends Kit {

    boolean stairs = false;
    boolean tower = false;
    boolean bridge = false;

    Inventory inv;


    public Builder(Main main, Player p, KitType type) {
        super(main, p, type);
        // TODO Auto-generated constructor stub
        PlayerInventory e = p.getInventory();
        e.addItem(newItem(Material.DIAMOND_SHOVEL, ChatColor.DARK_BLUE + "Hammer"));
        e.setHelmet(newItem(Material.GOLDEN_HELMET, ChatColor.GOLD + "Hard Hat"));
        e.setChestplate(newItem(Material.LEATHER_CHESTPLATE, ChatColor.YELLOW + "Shirt"));
        e.setBoots(newItem(Material.IRON_BOOTS, ChatColor.DARK_GRAY + "Boots"));
        e.addItem(newItem(Material.SHEARS, ChatColor.DARK_GRAY + "Shears"));
        giveWool();
        giveWool();
        giveWool();
        giveWool();
        inv = Bukkit.createInventory(p, 9, main.prefix + "BuildTools");
        inv.setItem(0, newItem(Material.OAK_SLAB, ChatColor.DARK_GREEN + "3x2 wool bridge with packed ice"));
        inv.setItem(1, newItem(Material.OAK_STAIRS, ChatColor.LIGHT_PURPLE + "5x5 wool stairs"));
        inv.setItem(2, newItem(Material.LADDER, ChatColor.GOLD + "3x3 wool tower"));
    }

    public void openMenu() {
        p.openInventory(inv);
    }

    public void stairs() {
        if (stairs)
            return;
        BukkitTask t = new BuildStair(main, p.getLocation(), p.getFacing(), p).runTaskTimer(main, 0, 4L);
        registerTask(t);
    }

    public void tower() {
        if (tower)
            return;
        BukkitTask t = new BuildTower(main, p.getLocation(), p).runTaskTimer(main, 0L, 4L);
        registerTask(t);
    }

    public void bridge() {
        if (bridge)
            return;
        BukkitTask t = new BuildBridge(main, p.getLocation(), p.getFacing(), p).runTaskTimer(main, 0L, 4L);
        registerTask(t);
    }

    public class BuildStair extends BukkitRunnable {

        private final Main main;

        private int temp = 0;
        private Location loc;
        private Player p;
        private BlockFace dir;
        private Material woolType;

        public BuildStair(Main main, Location loc, BlockFace dire, Player p) {
            this.main = main;
            this.loc = loc;
            this.p = p;
            this.dir = dire;
            woolType = (main.game.redHas(p)) ? Material.ORANGE_WOOL : Material.LIGHT_BLUE_WOOL;
            loc.setY(loc.getBlockY() - 1);
            if (dir == BlockFace.NORTH) {
                loc.setZ(loc.getBlockZ() - 1);
            } else if (dir == BlockFace.SOUTH) {
                loc.setZ(loc.getBlockZ() + 1);
            } else if (dir == BlockFace.WEST) {
                loc.setX(loc.getBlockX() - 1);
            } else if (dir == BlockFace.EAST) {
                loc.setX(loc.getBlockX() + 1);
            }
            stairs = true;
        }

        //north = -z, south = +z, east = +x, west = -x
        public void run() {
            Location tempLoc = loc;
            World w = loc.getWorld();
            if (temp <= 5) {
                if (dir == BlockFace.NORTH) {
                    tempLoc.setZ(loc.getBlockZ() - 1);
                    for (int i = -5; i < 5; i++) {
                        if (main.restricted.contains(w.getBlockAt(tempLoc.getBlockX() + i, tempLoc.getBlockY() + temp, tempLoc.getBlockZ()).getType()))
                            continue;
                        w.getBlockAt(tempLoc.getBlockX() + i, tempLoc.getBlockY() + temp, tempLoc.getBlockZ()).setType(woolType);
                    }
                } else if (dir == BlockFace.SOUTH) {
                    tempLoc.setZ(loc.getBlockZ() + 1);
                    for (int i = -5; i < 5; i++) {
                        if (main.restricted.contains(w.getBlockAt(tempLoc.getBlockX() + i, tempLoc.getBlockY() + temp, tempLoc.getBlockZ()).getType()))
                            continue;
                        w.getBlockAt(tempLoc.getBlockX() + i, tempLoc.getBlockY() + temp, tempLoc.getBlockZ()).setType(woolType);
                    }
                } else if (dir == BlockFace.WEST) {
                    tempLoc.setX(loc.getBlockX() - 1);
                    for (int i = -5; i < 5; i++) {
                        if (main.restricted.contains(w.getBlockAt(tempLoc.getBlockX(), tempLoc.getBlockY() + temp, tempLoc.getBlockZ() + i).getType()))
                            continue;
                        w.getBlockAt(tempLoc.getBlockX(), tempLoc.getBlockY() + temp, tempLoc.getBlockZ() + i).setType(woolType);
                    }
                } else if (dir == BlockFace.EAST) {
                    tempLoc.setX(loc.getBlockX() + 1);
                    for (int i = -5; i < 5; i++) {
                        if (main.restricted.contains(w.getBlockAt(tempLoc.getBlockX(), tempLoc.getBlockY() + temp, tempLoc.getBlockZ() + i).getType()))
                            continue;
                        w.getBlockAt(tempLoc.getBlockX(), tempLoc.getBlockY() + temp, tempLoc.getBlockZ() + i).setType(woolType);
                    }
                }
                w.playSound(loc, Sound.BLOCK_GLASS_PLACE, 10, temp);

                //loc.subtract(0, temp, 0);
            } else {
                if (temp == 30) {
                    this.cancel();
                    stairs = false;
                    return;
                }
            }
            int first = inv.first(Material.OAK_STAIRS);
            inv.getItem(first).setAmount(30 - temp);
            temp++;
        }
    }

    public class BuildTower extends BukkitRunnable {

        private int temp = 30;
        private Location loc;
        private Material woolType;

        public BuildTower(Main main, Location loc, Player p) {
            this.loc = loc;
            woolType = (main.game.redHas(p)) ? Material.ORANGE_WOOL : Material.LIGHT_BLUE_WOOL;
            loc.setY(loc.getBlockY() - 1);
            for (Location b : circle(loc, 3, false)) {
                if(!main.restricted.contains(loc.getWorld().getBlockAt(b).getType()))
                    loc.getWorld().getBlockAt(b).setType(woolType);
            }
            tower = true;
        }

        //north = -z, south = +z, east = +x, west = -x
        public void run() {
            if (temp >= 23) {
                Location tempLoc = loc;
                tempLoc.setY(loc.getY() + 1);
                World w = loc.getWorld();
                for (Location b : circle(tempLoc, 3, true)) {
                    if (main.restricted.contains(w.getBlockAt(b).getType()))
                        continue;
                    w.getBlockAt(b).setType(woolType);
                }
                if(!main.restricted.contains(w.getBlockAt(loc.getBlockX() + 2, tempLoc.getBlockY(), loc.getBlockZ()).getType()))
                    w.getBlockAt(loc.getBlockX() + 2, tempLoc.getBlockY(), loc.getBlockZ()).setType(Material.LADDER);
                Location effectLoc = new Location(loc.getWorld(), loc.getX(), loc.getY() + 1, loc.getZ());
                w.playSound(loc, Sound.BLOCK_LADDER_PLACE, 10, temp);
                w.playEffect(effectLoc, Effect.SMOKE, temp);
            } else {
                if (temp == 0) {
                    this.cancel();
                    tower = false;
                    return;
                }
            }
            int first = inv.first(Material.LADDER);
            inv.getItem(first).setAmount(temp);
            temp--;
        }
    }

    public class BuildBridge extends BukkitRunnable {

        private int temp = 30;
        private Location loc;
        private BlockFace dir;
        private Material woolType;

        public BuildBridge(Main main, Location loc, BlockFace dire, Player p) {
            this.loc = loc;
            this.dir = dire;
            woolType = (main.game.redHas(p)) ? Material.ORANGE_WOOL : Material.LIGHT_BLUE_WOOL;
            loc.setY(loc.getBlockY() - 1);
        }

        //north = -z, south = +z, east = +x, west = -x
        public void run() {
            if (temp >= 18) {
                Location tempLoc = loc;
                World w = loc.getWorld();
                if (dir == BlockFace.NORTH) {
                    tempLoc.setZ(loc.getBlockZ() - 1);
                    if (!main.restricted.contains(w.getBlockAt(tempLoc).getType())) {
                        w.getBlockAt(tempLoc).setType(Material.PACKED_ICE);
                    }
                    if (!main.restricted.contains(w.getBlockAt(tempLoc.getBlockX() + 1, tempLoc.getBlockY(), tempLoc.getBlockZ()).getType())) {
                        w.getBlockAt(tempLoc.getBlockX() + 1, tempLoc.getBlockY(), tempLoc.getBlockZ()).setType(woolType);
                    }
                    if (!main.restricted.contains(w.getBlockAt(tempLoc.getBlockX() - 1, tempLoc.getBlockY(), tempLoc.getBlockZ()).getType())) {
                        w.getBlockAt(tempLoc.getBlockX() - 1, tempLoc.getBlockY(), tempLoc.getBlockZ()).setType(woolType);
                    }
                } else if (dir == BlockFace.SOUTH) {
                    tempLoc.setZ(loc.getBlockZ() + 1);
                    if (!main.restricted.contains(w.getBlockAt(tempLoc).getType())) {
                        w.getBlockAt(tempLoc).setType(Material.PACKED_ICE);
                    }
                    if (!main.restricted.contains(w.getBlockAt(tempLoc.getBlockX() + 1, tempLoc.getBlockY(), tempLoc.getBlockZ()).getType())) {
                        w.getBlockAt(tempLoc.getBlockX() + 1, tempLoc.getBlockY(), tempLoc.getBlockZ()).setType(woolType);
                    }
                    if (!main.restricted.contains(w.getBlockAt(tempLoc.getBlockX() - 1, tempLoc.getBlockY(), tempLoc.getBlockZ()).getType())) {
                        w.getBlockAt(tempLoc.getBlockX() - 1, tempLoc.getBlockY(), tempLoc.getBlockZ()).setType(woolType);
                    }
                } else if (dir == BlockFace.WEST) {
                    tempLoc.setX(loc.getBlockX() - 1);
                    if (!main.restricted.contains(w.getBlockAt(tempLoc).getType())) {
                        w.getBlockAt(tempLoc).setType(Material.PACKED_ICE);
                    }
                    if (!main.restricted.contains(w.getBlockAt(tempLoc.getBlockX(), tempLoc.getBlockY(), tempLoc.getBlockZ() + 1).getType())) {
                        w.getBlockAt(tempLoc.getBlockX(), tempLoc.getBlockY(), tempLoc.getBlockZ() + 1).setType(woolType);
                    }
                    if (!main.restricted.contains(w.getBlockAt(tempLoc.getBlockX(), tempLoc.getBlockY(), tempLoc.getBlockZ() - 1).getType())) {
                        w.getBlockAt(tempLoc.getBlockX(), tempLoc.getBlockY(), tempLoc.getBlockZ() - 1).setType(woolType);
                    }
                } else if (dir == BlockFace.EAST) {
                    tempLoc.setX(loc.getBlockX() + 1);
                    if (!main.restricted.contains(w.getBlockAt(tempLoc).getType())) {
                        w.getBlockAt(tempLoc).setType(Material.PACKED_ICE);
                    }
                    if (!main.restricted.contains(w.getBlockAt(tempLoc.getBlockX(), tempLoc.getBlockY(), tempLoc.getBlockZ() + 1).getType())) {
                        w.getBlockAt(tempLoc.getBlockX(), tempLoc.getBlockY(), tempLoc.getBlockZ() + 1).setType(woolType);
                    }
                    if (!main.restricted.contains(w.getBlockAt(tempLoc.getBlockX(), tempLoc.getBlockY(), tempLoc.getBlockZ() - 1).getType())) {
                        w.getBlockAt(tempLoc.getBlockX(), tempLoc.getBlockY(), tempLoc.getBlockZ() - 1).setType(woolType);
                    }
                }

                Location effectLoc = new Location(loc.getWorld(), loc.getX(), loc.getY() + 1, loc.getZ());
                w.playSound(loc, Sound.BLOCK_WOOL_PLACE, 10, temp);
                w.playEffect(effectLoc, Effect.SMOKE, temp);
            } else {
                if (temp == 0) {
                    this.cancel();
                    bridge = false;
                    return;
                }
            }
            int first = inv.first(Material.OAK_SLAB);
            inv.getItem(first).setAmount(temp);
            temp--;
        }
    }

    private static Set<Location> makeHollow(Set<Location> blocks, boolean sphere) {
        Set<Location> edge = new HashSet<Location>();
        if (!sphere) {
            for (Location l : blocks) {
                World w = l.getWorld();
                int X = l.getBlockX();
                int Y = l.getBlockY();
                int Z = l.getBlockZ();
                Location front = new Location(w, X + 1, Y, Z);
                Location back = new Location(w, X - 1, Y, Z);
                Location left = new Location(w, X, Y, Z + 1);
                Location right = new Location(w, X, Y, Z - 1);
                if (!(blocks.contains(front) && blocks.contains(back) && blocks.contains(left) && blocks.contains(right))) {
                    edge.add(l);
                }
            }
            return edge;
        } else {
            for (Location l : blocks) {
                World w = l.getWorld();
                int X = l.getBlockX();
                int Y = l.getBlockY();
                int Z = l.getBlockZ();
                Location front = new Location(w, X + 1, Y, Z);
                Location back = new Location(w, X - 1, Y, Z);
                Location left = new Location(w, X, Y, Z + 1);
                Location right = new Location(w, X, Y, Z - 1);
                Location top = new Location(w, X, Y + 1, Z);
                Location bottom = new Location(w, X, Y - 1, Z);
                if (!(blocks.contains(front) && blocks.contains(back) && blocks.contains(left) && blocks.contains(right) && blocks.contains(top) && blocks.contains(bottom))) {
                    edge.add(l);
                }
            }
            return edge;
        }
    }

    public static Set<Location> circle(Location location, int radius, boolean hollow) {
        Set<Location> blocks = new HashSet<Location>();
        World world = location.getWorld();
        int X = location.getBlockX();
        int Y = location.getBlockY();
        int Z = location.getBlockZ();
        int radiusSquared = radius * radius;

        if (hollow) {
            for (int x = X - radius; x <= X + radius; x++) {
                for (int z = Z - radius; z <= Z + radius; z++) {
                    if ((X - x) * (X - x) + (Z - z) * (Z - z) <= radiusSquared) {
                        Location block = new Location(world, x, Y, z);
                        blocks.add(block);
                    }
                }
            }
            return makeHollow(blocks, false);
        } else {
            for (int x = X - radius; x <= X + radius; x++) {
                for (int z = Z - radius; z <= Z + radius; z++) {
                    if ((X - x) * (X - x) + (Z - z) * (Z - z) <= radiusSquared) {
                        Location block = new Location(world, x, Y, z);
                        blocks.add(block);
                    }
                }
            }
            return blocks;
        }
    }
}