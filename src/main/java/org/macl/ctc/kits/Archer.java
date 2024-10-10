package org.macl.ctc.kits;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.macl.ctc.Main;
import org.macl.ctc.kits.Kit;
import org.macl.ctc.kits.KitType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class Archer extends Kit {
    public void shoot(ProjectileLaunchEvent event) {
        ArrowType inHand = ArrowType.FLAME;
        switch(p.getInventory().getHeldItemSlot()) {
            case 0:
                inHand = ArrowType.FLAME;
                break;
            case 1:
                inHand = ArrowType.LIGHTNING;
                break;
            case 2:
                inHand = ArrowType.TELEPORT;
                break;
            case 3:
                inHand = ArrowType.ICE;
                break;
            case 4:
                inHand = ArrowType.CYCLONE;
                break;
            case 5:
                inHand = ArrowType.GRAVITY;
                break;
        }

        if(inHand == ArrowType.CYCLONE) {
            new cycloneTimer(event.getEntity()).runTaskTimer(main, 0L, 1L);
        }
        if(inHand != ArrowType.FLAME) {
            p.getInventory().setItemInMainHand(null);
            if(!canShoot.get(inHand)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    public class cycloneTimer extends BukkitRunnable {

        private int seconds = 20*6;

        Entity a;

        public cycloneTimer(Entity e) {
            this.a = e;
        }

        //north = -z, south = +z, east = +x, west = -x
        @Override
        public void run() {
            if(seconds == 0) {
                a.remove();
                this.cancel();
            }
            Random random = new Random();
            for(Entity e : a.getNearbyEntities(3,3,3)) {
                int number = random.nextInt(2) - 1;
                if(e instanceof Player) {
                    Player p2 = (Player) e;
                    if(!p2.getName().equalsIgnoreCase(p.getName()))
                        e.setVelocity(new Vector(number*Math.random()*0.3, Math.abs(Math.random()+0.2)-0.2, number*Math.random()*0.3));
                }

            }
            a.getWorld().spawnParticle(Particle.CLOUD, a.getLocation(), 5);
            a.getWorld().playSound(a.getLocation(), Sound.ENTITY_HORSE_BREATHE, 1, 1);
            seconds--;
        }
    }

    public void gravity(Location loc) {
        World world = loc.getWorld();

        // Apply effects to nearby players
        for(Entity e : world.getNearbyEntities(loc, 4, 4, 4)) {
            if(e instanceof Player) {
                LivingEntity f = (LivingEntity) e;
                f.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 100, 2));
                f.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 120, 0));
            }
        }

        // Process blocks within a 4 block radius
        int radius = 3; // Radius for block picking
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block block = world.getBlockAt(loc.getBlockX() + x, loc.getBlockY() + y, loc.getBlockZ() + z);
                    Material blockType = block.getType();
                    if (!blockType.isAir() && blockType.isSolid() && !main.restricted.contains(block.getType())) {
                        Location blockLocation = block.getLocation();

                        // Convert the block to a falling block
                        FallingBlock fallingBlock = world.spawnFallingBlock(blockLocation, block.getBlockData());
                        fallingBlock.setDropItem(false); // Prevents the block from dropping as an item
                        fallingBlock.setHurtEntities(true); // Optional: Falling blocks will hurt entities they fall on

                        // Simulate levitation effect on the block
                        fallingBlock.setVelocity(new Vector(0, 1.5, 0)); // Upward velocity to simulate levitation

                        // Set the original block location to air
                        block.setType(Material.AIR);

                    }
                }
            }
        }
    }



    public class iceTimer extends BukkitRunnable {

        int timer = 0;
        Location loc;
        ArrayList<Material> sphere = new ArrayList<Material>();
        public iceTimer(Location loc) {
            this.loc = loc;
            for(Location l : sphere(loc, 4, true)) {
                sphere.add(l.getBlock().getType());
            }
        }

        @Override
        public void run() {
            if(timer < 5*20) {
                for(Location l : sphere(loc, 4, true))
                    l.getBlock().setType(Material.ICE);
            }  else {
                for(int i = 0; i < sphere.size(); i++)
                    sphere(loc, 4, true).get(i).getBlock().setType(sphere.get(i));
                this.cancel();
            }
            timer++;
        }

    }

    public ArrayList<Location> sphere(Location location, int radius, boolean hollow) {
        ArrayList<Location> blocks = new ArrayList<Location>();
        World world = location.getWorld();
        int X = location.getBlockX();
        int Y = location.getBlockY();
        int Z = location.getBlockZ();
        int radiusSquared = radius * radius;

        if (hollow) {
            for (int x = X - radius; x <= X + radius; x++) {
                for (int y = Y - radius; y <= Y + radius; y++) {
                    for (int z = Z - radius; z <= Z + radius; z++) {
                        if ((X - x) * (X - x) + (Y - y) * (Y - y) + (Z - z) * (Z - z) <= radiusSquared) {
                            Location block = new Location(world, x, y, z);
                            blocks.add(block);
                        }
                    }
                }
            }
            return makeHollow(blocks, true);
        } else {
            for (int x = X - radius; x <= X + radius; x++) {
                for (int y = Y - radius; y <= Y + radius; y++) {
                    for (int z = Z - radius; z <= Z + radius; z++) {
                        if ((X - x) * (X - x) + (Y - y) * (Y - y) + (Z - z) * (Z - z) <= radiusSquared) {
                            Location block = new Location(world, x, y, z);
                            blocks.add(block);
                        }
                    }
                }
            }
            return blocks;
        }
    }

    private ArrayList<Location> makeHollow(ArrayList<Location> blocks, boolean sphere){
        ArrayList<Location> edge = new ArrayList<Location>();
        if(!sphere){
            for(Location l : blocks){
                World w = l.getWorld();
                int X = l.getBlockX();
                int Y = l.getBlockY();
                int Z = l.getBlockZ();
                Location front = new Location(w, X + 1, Y, Z);
                Location back = new Location(w, X - 1, Y, Z);
                Location left = new Location(w, X, Y, Z + 1);
                Location right = new Location(w, X, Y, Z - 1);
                if(!(blocks.contains(front) && blocks.contains(back) && blocks.contains(left) && blocks.contains(right))){
                    edge.add(l);
                }
            }
            return edge;
        } else {
            for(Location l : blocks){
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
                if(!(blocks.contains(front) && blocks.contains(back) && blocks.contains(left) && blocks.contains(right) && blocks.contains(top) && blocks.contains(bottom))){
                    edge.add(l);
                }
            }
            return edge;
        }
    }

    private ArrayList<Material> restricted = main.restricted;

    public enum ArrowType {
        FLAME, LIGHTNING, TELEPORT, ICE, CYCLONE, GRAVITY
    }

    private ArrowType curArrow = ArrowType.FLAME;

    String lightning = ChatColor.YELLOW + "Lightning";
    String teleport = ChatColor.GREEN + "Teleport";
    String ice = ChatColor.DARK_AQUA + "Ice";
    String cyclone = ChatColor.WHITE + "Cyclone";
    String gravity = ChatColor.GRAY + "Gravity";
    String flame = ChatColor.RED + "Flame";

    HashMap<ArrowType, Boolean> canShoot = new HashMap<ArrowType, Boolean>();

    public Archer(Main main, Player p, KitType archer) {
        super(main, p, archer);
        p.getInventory().setHeldItemSlot(0);
        setupInitialInventory(p);
    }

    private void setupInitialInventory(Player p) {
        initializeCanShoot();
        p.getInventory().clear(); // Clear existing inventory items

        ItemStack bow = newItemEnchanted(Material.BOW, flame, Enchantment.ARROW_INFINITE, 1);
        ItemMeta bowMeta = bow.getItemMeta();
        setArrows();
        bowMeta.addEnchant(Enchantment.ARROW_FIRE, 1, false);
        bow.setItemMeta(bowMeta);
        p.getInventory().setItem(0, bow);
        p.getInventory().setChestplate(new ItemStack(Material.GOLDEN_CHESTPLATE));
        ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
        LeatherArmorMeta meta = (LeatherArmorMeta) helmet.getItemMeta();

        if (meta != null) {
            // Set the color of the helmet to red
            if(this.wool == Material.RED_WOOL)
                meta.setColor(Color.RED);
            if(this.wool == Material.BLUE_WOOL)
                meta.setColor(Color.BLUE);
            helmet.setItemMeta(meta);
        }
        p.getInventory().setHelmet(helmet);
        giveWool();
        giveWool();
    }

    private void initializeCanShoot() {
        for (ArrowType arrowType : ArrowType.values()) {
            canShoot.put(arrowType, true);
        }
    }

    public void cooldown(int seconds, ArrowType arrowType) {
        canShoot.put(arrowType, false);
        ItemStack arrow = p.getInventory().getItem(arrowTypeToSlot(arrowType));

        new BukkitRunnable() {
            @Override
            public void run() {
                canShoot.put(arrowType, true);
                int slot = arrowTypeToSlot(arrowType);
                ItemStack item = p.getInventory().getItem(slot);
                if (item != null) {
                    ItemMeta meta = item.getItemMeta();
                    meta.addEnchant(Enchantment.ARROW_INFINITE, 1, true);  // true to allow unsafe enchantments
                    item.setItemMeta(meta);
                    p.getInventory().setItem(slot, item);
                    playExp(1);
                }
            }
        }.runTaskLater(main, seconds * 20);  // Convert seconds to game ticks
    }

    private int arrowTypeToSlot(ArrowType arrowType) {
        return switch (arrowType) {
            case LIGHTNING -> 1;
            case TELEPORT -> 2;
            case ICE -> 3;
            case CYCLONE -> 4;
            case GRAVITY -> 5;
            default -> 0;  // Default case for FLAME
        };
    }

    private String arrowTypeToName(ArrowType arrowType) {
        // Convert ArrowType to its display name
        return switch (arrowType) {
            case FLAME -> flame;
            case LIGHTNING -> lightning;
            case TELEPORT -> teleport;
            case ICE -> ice;
            case CYCLONE -> cyclone;
            case GRAVITY -> gravity;
        };
    }

    public void setArrows() {
        ItemStack item;
        ItemMeta meta;

        // Loop over all ArrowTypes and set the items in the inventory with or without enchantment based on canShoot
        for (ArrowType arrowType : ArrowType.values()) {
            int slot = arrowTypeToSlot(arrowType);
            String arrowName = arrowTypeToName(arrowType);
            item = newItem(Material.ARROW, arrowName);  // Assuming newItem creates a new ItemStack

            if (canShoot.get(arrowType)) {
                meta = item.getItemMeta();
                meta.addEnchant(Enchantment.ARROW_INFINITE, 1, true);  // Unsafe enchantment is allowed
                item.setItemMeta(meta);
            }

            p.getInventory().setItem(slot, item);
        }
    }
    public void setFlame() {
        if (canShoot.get(ArrowType.FLAME)) {
            ItemStack bow = newItemEnchanted(Material.BOW, ChatColor.RED + "Flame Bow", Enchantment.ARROW_INFINITE, 1);
            ItemMeta bowMeta = bow.getItemMeta();
            bowMeta.addEnchant(Enchantment.ARROW_FIRE, 1, false);
            bow.setItemMeta(bowMeta);
            p.getInventory().setItem(0, bow);
            curArrow = ArrowType.FLAME;
        }
    }

    public void setLightning() {
        if (canShoot.get(ArrowType.LIGHTNING)) {
            ItemStack bow = newItemEnchanted(Material.BOW, lightning, Enchantment.ARROW_INFINITE, 1);
            p.getInventory().setItem(1, bow);
            curArrow = ArrowType.LIGHTNING;
        }
    }

    public void setTeleport() {
        if (canShoot.get(ArrowType.TELEPORT)) {
            ItemStack bow = newItemEnchanted(Material.BOW, teleport, Enchantment.ARROW_INFINITE, 1);
            p.getInventory().setItem(2, bow);
            curArrow = ArrowType.TELEPORT;
        }
    }

    public void setIce() {
        if (canShoot.get(ArrowType.ICE)) {
            ItemStack bow = newItemEnchanted(Material.BOW, ice, Enchantment.ARROW_INFINITE, 1);
            p.getInventory().setItem(3, bow);
            curArrow = ArrowType.ICE;
        }
    }

    public void setCyclone() {
        if (canShoot.get(ArrowType.CYCLONE)) {
            ItemStack bow = newItemEnchanted(Material.BOW, cyclone, Enchantment.ARROW_INFINITE, 1);
            p.getInventory().setItem(4, bow);
            curArrow = ArrowType.CYCLONE;
        }
    }

    public void setGravity() {
        if (canShoot.get(ArrowType.GRAVITY)) {
            ItemStack bow = newItemEnchanted(Material.BOW, gravity, Enchantment.ARROW_INFINITE, 1);
            p.getInventory().setItem(5, bow);
            curArrow = ArrowType.GRAVITY;
        }
    }


    public void bHit(ProjectileHitEvent event) {
        if(curArrow == ArrowType.FLAME)
            return;
        cooldown(8, curArrow);
        if(event.getHitBlock() == null) {
            switch(curArrow) {
                case LIGHTNING -> {
                    event.getHitEntity().getLocation().getWorld().strikeLightning(event.getHitEntity().getLocation());
                    break;
                }
                case ICE -> {
                    new iceTimer(event.getHitEntity().getLocation()).runTaskTimer(main, 0L, 1L);
                    break;
                }
                case GRAVITY -> {
                    gravity(event.getHitEntity().getLocation());
                    break;
                }
                case CYCLONE -> {
                    main.broadcast("cyclone");
                    break;
                }
                case TELEPORT -> {
                    Location hitLocation = event.getHitEntity().getLocation();
                    Location pLoc = p.getLocation();
                    event.getHitEntity().teleport(pLoc);
                    p.teleport(hitLocation);
                    main.broadcast("teleport");
                    break;
                }
            }
        } else {
            switch(curArrow) {
                case FLAME -> {
                    event.getHitBlock().setType(Material.FIRE);
                    break;
                }
                case LIGHTNING -> {
                    event.getHitBlock().getLocation().getWorld().strikeLightning(event.getHitBlock().getLocation());
                    break;
                }
                case ICE -> {
                    new iceTimer(event.getHitBlock().getLocation()).runTaskTimer(main, 0L, 1L);
                    break;
                }
                case GRAVITY -> {
                    gravity(event.getHitBlock().getLocation());
                    break;
                }
                case CYCLONE -> {
                    main.broadcast("cyclone");
                    break;
                }
            }
        }
    }

    public void handleItemSwitch(PlayerItemHeldEvent event) {
        setArrows();
        switch(event.getNewSlot()) {
            case 0:
                setFlame();
                break;
            case 1:
                setLightning();
                break;
            case 2:
                setTeleport();
                break;
            case 3:
                setIce();
                break;
            case 4:
                setCyclone();
                break;
            case 5:
                setGravity();
                break;
        }
    }
}
