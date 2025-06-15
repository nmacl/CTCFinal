package org.macl.ctc.kits;

import net.minecraft.world.item.alchemy.Potion;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.macl.ctc.Main;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Artificer extends Kit {

    int voidBombRadius = 7;
    boolean voidReady = false;

    public Artificer(Main main, Player p, KitType type) {
        super(main, p, type);
        p.removePotionEffect(PotionEffectType.SPEED);
        PlayerInventory inv = p.getInventory();
        inv.setHelmet(new ItemStack(Material.LEATHER_HELMET));
        inv.setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));
        inv.setLeggings(new ItemStack(Material.CHAINMAIL_LEGGINGS));
        inv.setBoots(new ItemStack(Material.LEATHER_BOOTS));

        // ability items
        inv.setItem(0, newItem(Material.RIB_ARMOR_TRIM_SMITHING_TEMPLATE, ChatColor.RED + "Flamethrower"));
        ItemStack frost = new ItemStack(Material.TIPPED_ARROW);
        PotionMeta meta = (PotionMeta) frost.getItemMeta();
        meta.setBasePotionData(new PotionData(PotionType.SLOWNESS));
        meta.setDisplayName(ChatColor.BLUE + "Frost Dagger");
        frost.setItemMeta(meta);
        inv.setItem(1, frost);
        inv.setItem(2, newItem(Material.FEATHER, ChatColor.WHITE + "Updraft"));
        addVoidFragments(1);
        giveWool(); giveWool();
    }

    // Flamethrower: 9s cooldown, does short burst of fire
    public void useFlamethrower() {
        if (isOnCooldown("Flamethrower")) return;
        // Start cooldown and reset item on finish
        setCooldown("Flamethrower", 9, Sound.BLOCK_AMETHYST_CLUSTER_HIT, () -> {
            p.getInventory().setItem(0, newItem(Material.RIB_ARMOR_TRIM_SMITHING_TEMPLATE, ChatColor.RED + "Flamethrower"));
        });
        // Show recharging icon
        p.getInventory().setItem(0, newItem(Material.HOST_ARMOR_TRIM_SMITHING_TEMPLATE, ChatColor.AQUA + "Recharging..."));

        // Burst effect
        new BukkitRunnable() {
            int count = 0;
            final int reps = 25;
            final double step = 0.2;
            final double maxLen = 10;
            final double damageRad = 0.7;
            final int damage = 2;
            boolean canGiveVoid = false;
            int hits = 0;

            @Override
            public void run() {
                if (count++ >= reps) {
                    cancel();
                    p.getWorld().playSound(p.getLocation(), Sound.BLOCK_LANTERN_BREAK, 2f, 0.5f);
                    return;
                }
                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1.5f);
                Location start = p.getEyeLocation().subtract(0, 0.5, 0);
                Vector dir = start.getDirection();


                boolean hitPlayer = false;


                for (int i = 0; i * step <= maxLen; i++) {
                    Location loc = start.clone().add(dir.clone().multiply(i * step));
                    RayTraceResult hit = p.getWorld().rayTraceBlocks(start, dir, i * step);
                    if (hit != null && hit.getHitBlock() != null) break;
                    p.getWorld().spawnParticle(Particle.FLAME, loc.subtract(0, 0, 0), 0);
                    for (Entity ent : p.getWorld().getNearbyEntities(loc, damageRad, damageRad, damageRad)) {
                        if (ent instanceof LivingEntity && !ent.equals(p) && p.hasLineOfSight(ent)) {
                            ((LivingEntity) ent).damage(damage);
                            hitPlayer = true;
                            if (hits >= 3) {
                                hits = 0;
                                canGiveVoid = true;
                            }
                            ent.setFireTicks(40);
                        }
                    }
                }

                if (hitPlayer) {
                    hits++;
                }

                if (canGiveVoid) {
                    addVoidFragments(1);
                    canGiveVoid = false;
                }
            }
        }.runTaskTimer(main, 0, 2);
    }

    // Frost dagger: simple projectile, 5s cooldown
    public void useFrostDagger() {
        if (isOnCooldown("FrostDagger")) return;
        setCooldown("FrostDagger", 7, Sound.BLOCK_POWDER_SNOW_BREAK, () -> {});
        SpectralArrow arrow = p.launchProjectile(SpectralArrow.class);
        arrow.setShooter(p);
        arrow.setVelocity(p.getLocation().getDirection().multiply(1.5));
    }

    // Void bomb: spawns block, explodes after landing, 8s cooldown
    public void useVoidBomb() {
        if (!voidReady) {
            return;
        }

        voidReady = false;

        p.getInventory().remove(Material.FLINT);
        addVoidFragments(1);

        Arrow arrow = p.launchProjectile(Arrow.class);
        arrow.setShooter(p);

        Vector dir = p.getEyeLocation().getDirection();
//        Item bomb = p.getWorld().dropItem(p.getEyeLocation(), new ItemStack(Material.BLACK_CONCRETE_POWDER));



//        arrow.addPassenger(bomb);
//        arrow.addCustomEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20*2000, 1),true);
        arrow.setVelocity(dir.multiply(1.5));

        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_CHICKEN_EGG, 10f, 0.8f);

        new BukkitRunnable() {
            int tick = 0;
            final int maxIter = 50;
            final double initRad = 0.3;
//            final double finalRad = 4.7;
            Location bombLoc = null;
            final int particles = 450;
            final Particle.DustOptions blackDust = new Particle.DustOptions(Color.fromRGB(0, 0, 0), 2);
            double rad = initRad;
            boolean landed = false;

            @Override
            public void run() {
//                double finalParticles;
                if (landed) {
                    rad = rad + 0.3;
                }
                if (rad > voidBombRadius) {
                    rad = voidBombRadius;
                }
//                rad = (initRad + (finalRad * (double) ((tick / maxIter)))); THIS WORKED LIKE 5 MINUTES AGO????
//                if (rad > 5.0) {
//                    rad = 5.0;
//                }
//                finalParticles = particles + (350 * ((double) tick / maxIter));

                arrow.getWorld().spawnParticle(Particle.REDSTONE, arrow.getLocation(), 10, blackDust);

                if (arrow.isOnGround() || !arrow.isValid()) {
//                    main.broadcast("on ground");
                    bombLoc = arrow.getLocation(); // need to round this so the particles start from the center of the block
                    if (!landed) {
//                        main.broadcast("landed");
                        landed = true;
                        bombLoc.getWorld().playSound(bombLoc, Sound.BLOCK_BEACON_ACTIVATE, 20f, 0.7f);
                    }
//                    bomb.remove();
                }
//                main.broadcast(Double.toString((initRad + (finalRad * (double) ((tick / maxIter))))));
                if (bombLoc == null) {
//                    main.broadcast("bombloc null");
                    return;
                } else if ((tick % 3) == 0) {
                    for (int i = 0; i < particles; i++) {

                        double theta = Math.random() * 2 * Math.PI;
                        double phi = Math.acos(2 * Math.random() - 1);
                        double x = rad * Math.sin(phi) * Math.cos(theta);
                        double y = rad * Math.sin(phi) * Math.sin(theta);
                        double z = rad * Math.cos(phi);

                        Location particleLocation = bombLoc.clone().add(x, y, z);
                        Objects.requireNonNull(bombLoc.getWorld()).spawnParticle(Particle.REDSTONE, particleLocation, 1, blackDust);
                    }
                }
                if (tick >= maxIter) {
                    voidDeleteBlocks(bombLoc);
                    cancel();
                }

                tick++;
            }
        }.runTaskTimer(main, 0, 1);
    }

    public void addVoidFragments(int amount) {
        if (!voidReady) {
            int stack = 0;



            if (p.getInventory().getItem(3) != null) {
                stack = p.getInventory().getItem(3).getAmount();
            }

            if ((stack + amount) >= 25) {
                p.getInventory().setItem(3, newItem(Material.FLINT, ChatColor.GRAY + "Void Bomb"));
                p.sendMessage(ChatColor.GRAY +"Void Bomb is charged...");
                voidReady = true;
            } else {
                p.getInventory().setItem(3, newItem(Material.DISC_FRAGMENT_5, ChatColor.DARK_AQUA + "Void Fragment", stack + amount));
            }

        } else {
            p.getInventory().setItem(3, newItem(Material.FLINT, ChatColor.GRAY + "Void Bomb"));
        }
    }

    public void voidDeleteBlocks(Location loc) {
        loc.getWorld().playSound(loc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 10f, 0.5f);

        for(Location l : sphere(loc, voidBombRadius + 1))
            if (l.getBlock().getType() != Material.AIR) { // replace non air border with black concrete
                if (!main.restricted.contains(l.getBlock().getType())) { // if restricted, dont at all
                    l.getBlock().setType(Material.BLACK_CONCRETE);
                }
            }
        for(Location l : sphere(loc, voidBombRadius))
            if (!main.restricted.contains(l.getBlock().getType())) {
                l.getBlock().setType(Material.AIR );
            }
        for (Entity ent : Objects.requireNonNull(loc.getWorld()).getNearbyEntities(loc, voidBombRadius, voidBombRadius, voidBombRadius)) {
            if (ent instanceof Player) if (((Player) ent).getGameMode() != GameMode.SPECTATOR) ((Player) ent).setHealth(0.0);
        }
    }

    // Updraft: 12s cooldown, single ability
    public void useUpdraft() {
        if (isOnCooldown("Updraft")) return;
        setCooldown("Updraft", 8, Sound.ENTITY_HORSE_BREATHE, () -> {
            p.getInventory().setItem(2, newItem(Material.FEATHER, ChatColor.WHITE + "Updraft"));
        });
        p.getInventory().setItem(2, newItem(Material.GUNPOWDER, ChatColor.GRAY + "Updraft..."));

        addVoidFragments(2);

        Vector v = p.getLocation().getDirection().multiply(0.5).setY(2);
        p.setVelocity(v);
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 20 * 6, 1));
        for (Entity ent : p.getWorld().getNearbyEntities(p.getLocation(), 4, 4, 4)) {
            if (ent != p) ent.setVelocity(ent.getLocation().toVector().subtract(p.getLocation().toVector()).normalize().multiply(1.5));
        }
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_HORSE_BREATHE, 10f, 0.8f);

        double startRadius = 1;
        double endRadius = 5;
        int duration = 10;
        int particles = 30;
        Location center = p.getLocation().clone().add(0, 2, 0); // Adjust the starting location

        double increment = 2 * Math.PI / particles;
        double radiusIncrement = (endRadius - startRadius) / duration;

        new BukkitRunnable() {
            double currentRadius = startRadius;
            int iterations = 0;
            double vOffset = 0.3;

            @Override
            public void run() {
                if (iterations >= duration) {
                    vOffset = 0.3;
                    cancel();
                    return;
                }

                for (int j = 0; j < particles; j++) {
                    double angle = j * increment;
                    double x = center.getX() + currentRadius * Math.cos(angle);
                    double z = center.getZ() + currentRadius * Math.sin(angle);
                    Location particleLocation = new Location(center.getWorld(), x, center.getY(), z);
                    p.getWorld().spawnParticle(Particle.CLOUD, particleLocation, 0, 0, vOffset, 0);

                }
                vOffset -= 0.04;
                currentRadius += radiusIncrement;
                iterations++;
            }
        }.runTaskTimer(main, 0, 1);

    }

    public ArrayList<Location> sphere(Location location, int radius) {
        ArrayList<Location> blocks = new ArrayList<Location>();
        World world = location.getWorld();
        int X = location.getBlockX();
        int Y = location.getBlockY();
        int Z = location.getBlockZ();
        int radiusSquared = radius * radius;


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


