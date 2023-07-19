package org.macl.ctc.kits;

import org.bukkit.*;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.macl.ctc.Main;

import static org.bukkit.Bukkit.getServer;

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
        e.addItem(newItem(Material.FISHING_ROD, ChatColor.GOLD + "Grappling Hook"));
        e.addItem(newItem(Material.FLINT, ChatColor.GRAY + "Void Bomb"));
        e.addItem(newItem(Material.FEATHER, "Wind Ability"));
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
                // Flamethrower sounds
                p.playSound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1.5f);
                p.playSound(p.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 1f, 2f);

                Location start = p.getEyeLocation();
                Vector direction = p.getEyeLocation().getDirection();

                // Calculate the step size to spawn particles along the trajectory
                double stepSize = 0.2; // Adjust this value if needed
                int steps = (int) (maxLength / stepSize);

                for (int i = 0; i < steps; i++) {
                    Location particleLocation = start.clone().add(direction.clone().multiply(i * stepSize));
                    p.getWorld().spawnParticle(Particle.LAVA, particleLocation.subtract(0, 1, 0), 0);


                    double damageRadius = 2.0;
                    for (Entity entity : p.getWorld().getNearbyEntities(particleLocation, damageRadius, damageRadius, damageRadius)) {
                        if (entity instanceof LivingEntity && !entity.equals(p)) {
                            LivingEntity livingEntity = (LivingEntity) entity;

                            livingEntity.damage(damageAmount);

                            livingEntity.setFireTicks(40);
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
                    p.playSound(p.getLocation(), Sound.BLOCK_LANTERN_BREAK, 2f, 0.5f);
                    cancel();
                }
            }
        };
        task.runTaskTimer(main, delay, interval);
    }
}