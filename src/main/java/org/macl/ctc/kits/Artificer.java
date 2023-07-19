package org.macl.ctc.kits;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.*;
import org.bukkit.*;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.macl.ctc.Main;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Artificer extends Kit {

    private int delay = 0;
    private int interval = 2;

    public Artificer(Main main, Player p, KitType type) {
        super(main, p, type);
        p.removePotionEffect(PotionEffectType.SPEED);
        PlayerInventory e = p.getInventory();
        e.setHelmet(new ItemStack(Material.LEATHER_HELMET));
        e.setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));
        e.setLeggings(new ItemStack(Material.CHAINMAIL_LEGGINGS));
        e.setBoots(new ItemStack(Material.LEATHER_BOOTS));
        e.addItem(newItem(Material.RIB_ARMOR_TRIM_SMITHING_TEMPLATE, ChatColor.RED + "Flamethrower"));
        e.addItem(new ItemStack(Material.TIPPED_ARROW) {{
            PotionMeta meta = (PotionMeta) getItemMeta();
            assert meta != null;
            meta.setBasePotionData(new PotionData(PotionType.SLOWNESS));
            meta.setDisplayName(ChatColor.BLUE + "Frost Dagger");
            setItemMeta(meta);
        }});
        e.addItem(newItem(Material.FLINT, ChatColor.GRAY + "Void Bomb"));
        e.addItem(newItem(Material.FEATHER, ChatColor.WHITE + "Updraft"));
        giveWool();
        giveWool();
    }

    public void flamethrowerShoot() {
        PlayerInventory e = p.getInventory();
        e.setItem(0, newItem(Material.HOST_ARMOR_TRIM_SMITHING_TEMPLATE, ChatColor.AQUA + "Recharging..."));
        new BukkitRunnable() {
            @Override
            public void run() {
                e.setItem(0, (newItem(Material.RIB_ARMOR_TRIM_SMITHING_TEMPLATE, ChatColor.RED + "Flamethrower")));
                p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_HIT, 10f, 1f);
            }
        }.runTaskLater(main, 20 * 9);
        BukkitRunnable task = new BukkitRunnable() {
            int count = 0;
            int repetitions = 25;
            int maxLength = 10;
            int damageAmount = 2;

            @Override
            public void run() {

                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1.5f);
                p.getWorld().playSound(p.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 1f, 2f);

                Location start = p.getEyeLocation();
                Vector direction = p.getEyeLocation().getDirection();


                double stepSize = 0.2;
                int steps = (int) (maxLength / stepSize);

                for (int i = 0; i < steps; i++) {
                    Location particleLocation = start.clone().add(direction.clone().multiply(i * stepSize));

                    RayTraceResult rayTrace = p.getWorld().rayTraceBlocks(p.getEyeLocation(), direction, i * stepSize);
                    if (rayTrace != null && rayTrace.getHitBlock() != null) {

                        break;
                    } else {

                        p.getWorld().spawnParticle(Particle.LAVA, particleLocation.subtract(0, 1, 0), 0);


                        double damageRadius = 0.7;
                        for (Entity entity : p.getWorld().getNearbyEntities(particleLocation, damageRadius, damageRadius, damageRadius)) {
                            if (entity instanceof LivingEntity && !entity.equals(p)) {
                                LivingEntity livingEntity = (LivingEntity) entity;
                                if (p.hasLineOfSight(entity)) {

                                    livingEntity.damage(damageAmount);

                                    livingEntity.setFireTicks(40);
                                }
                            }
                        }
                    }
                }
                if (count >= repetitions) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                        }
                    }.runTaskLater(main, 0);
                }
                count++;
                if (count >= repetitions) {
                    p.getWorld().playSound(p.getLocation(), Sound.BLOCK_LANTERN_BREAK, 2f, 0.5f);
                    cancel();
                }
            }
        };
        task.runTaskTimer(main, delay, interval);
    }

    public void upHeave() {
        e.setItem(3, newItem(Material.GUNPOWDER, ChatColor.GRAY + "Updraft..."));
        p.setVelocity(p.getLocation().getDirection().multiply(0.5f));
        p.setVelocity(p.getVelocity().setY(2f));
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 20 * 8, 1));
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_HORSE_BREATHE, 10f, 0.8f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_HORSE_BREATHE, 10f, 0.4f);

        for (Entity entity : p.getWorld().getEntities()) {
            if (entity != p && entity.getLocation().distance(p.getLocation()) <= 6) {
                Vector direction = entity.getLocation().toVector().subtract(p.getLocation().toVector()).normalize();
                entity.setVelocity(direction.multiply(2));
            }
        }

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
                    p.spawnParticle(Particle.CLOUD, particleLocation, 0, 0, vOffset, 0);

                }
                vOffset -= 0.04;
                currentRadius += radiusIncrement;
                iterations++;
            }
        }.runTaskTimer(main, 0, 1);
        new BukkitRunnable() {
            @Override
            public void run() {
                e.setItem(3, newItem(Material.FEATHER, ChatColor.WHITE + "Updraft"));
                p.playSound(p.getLocation(), Sound.ENTITY_HORSE_BREATHE, 1, 1);

            }
        }.runTaskLater(main, 20 * 12);
    }

    int frostLevel = 1;
    public void frostThrow() {
        SpectralArrow dagger = p.launchProjectile(SpectralArrow.class);


    }

    public void voidBomb(Location initialLocation, Vector direction) {
        ItemStack blackConcretePowder = new ItemStack(Material.BLACK_CONCRETE_POWDER);
        Item item = initialLocation.getWorld().dropItem(p.getEyeLocation(), blackConcretePowder);
        item.setVelocity(direction.multiply(1.5));

        new BukkitRunnable() {

            List<Block> blocksToRemove = new ArrayList<>();
            double maxRadius = 4.0;
            int maxIterations = 60;
            int iteration = 0;

            @Override
            public void run() {
                if (item.isOnGround()) {
                    Location center = item.getLocation();

                    if (iteration < maxIterations) {
                        double radius = (double) iteration / maxIterations * maxRadius;

                        for (double x = -radius; x <= radius; x += 0.5) {
                            for (double y = -radius; y <= radius; y += 0.5) {
                                for (double z = -radius; z <= radius; z += 0.5) {
                                    if (x * x + y * y + z * z <= radius * radius) {
                                        center.getWorld().spawnParticle(Particle.SQUID_INK, center.getX() + x, center.getY() + y + 1, center.getZ() + z, 0);
                                        Location blockLocation = center.clone().add(x, y, z);
                                        Block block = blockLocation.getBlock();
                                        blocksToRemove.add(block);
                                    }
                                }
                            }
                        }

                        iteration++;
                    } else {
                        // Update neighboring blocks before removing blocks
                        for (Block block : blocksToRemove) {
                            for (BlockFace face : BlockFace.values()) {
                                Block neighbor = block.getRelative(face);
                                if (neighbor.getType() != Material.AIR && neighbor.getType() != Material.BLACK_CONCRETE) {
                                    neighbor.setType(Material.BLACK_CONCRETE);
                                }
                            }
                        }

                        // Now remove the blocks
                        for (Block block : blocksToRemove) {
                            block.setType(Material.AIR);
                        }
                    }
                }
            }
        }.runTaskTimer(main, 0, 1);
    }
}
