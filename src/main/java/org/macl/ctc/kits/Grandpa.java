package org.macl.ctc.kits;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.macl.ctc.Main;

public class Grandpa extends Kit {

    int maxAmmo = 2;
    int reloadTime = 3;
    int ammo = 2;
    public BukkitRunnable reloadingTask;
    public BukkitRunnable unboozeTask;
    public BukkitRunnable reboozeTask;

    public Grandpa(Main main, Player p, KitType type) {
        super(main, p, type);
        p.removePotionEffect(PotionEffectType.SPEED);
        PlayerInventory e = p.getInventory();
        e.setHelmet(newItem(Material.IRON_HELMET, ChatColor.DARK_GREEN + "Veteran's Helmet"));
        e.addItem(newItem(Material.PRISMARINE_SHARD, ChatColor.GRAY + "Slugged Shotgun"));
        e.addItem(newItem(Material.HONEY_BOTTLE, ChatColor.GOLD + "Booze"));
        e.addItem(newItem(Material.LADDER, "Old Ladder", 24));
        giveWool();
        giveWool();

    }

    /*@EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (reloadingTask != null && !reloadingTask.isCancelled()) {
            reloadingTask.cancel();
            reloadingTask = null;
        }
        if (unboozeTask != null && !unboozeTask.isCancelled()) {
            unboozeTask.cancel();
            unboozeTask = null;

        }
        if (reboozeTask != null && !reboozeTask.isCancelled()) {
            reboozeTask.cancel();
            reboozeTask = null;
        } //move to players event file whenever i dont suck at code
    }*/

            public void shootGun() {
        if (ammo > 0) {
            ammo -= 1;
            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1f, 0.1f);
            Location playerLocation = p.getLocation();
            Location bulletLocation = playerLocation.clone().add(0, p.getEyeHeight() - 0.5, 0);
            Vector direction = playerLocation.getDirection();

            SmallFireball bullet = p.getWorld().spawn(bulletLocation, SmallFireball.class);

            if (ammo == 0) {
                e.setItem(0, newItem(Material.LIGHT_GRAY_DYE, ChatColor.RED + "Reload"));
            }

            new BukkitRunnable() {
                int ticks = 0;
                double speed = 5; // Adjust the speed as needed

                @Override
                public void run() {
                    ticks++;
                    if (ticks >= 5) { // Remove bullet after 50 ticks (2.5 seconds)
                        bullet.remove();
                        cancel();
                        return;
                    }

                    Vector velocity = direction.clone().multiply(speed);
                    bullet.setVelocity(velocity);
                }
            }.runTaskTimer(main, 0L, 1L);
        }
    }

    public void reloadGun() {
        e.setItem(0, (newItem(Material.GRAY_DYE, ChatColor.GREEN + "Reloading...")));
        p.playSound(p.getLocation(),Sound.ITEM_ARMOR_EQUIP_LEATHER,1f,1);

        reloadingTask = new BukkitRunnable() {

            @Override
            public void run() {
                p.playSound(p.getLocation(),Sound.ITEM_ARMOR_EQUIP_IRON,1f,0.5f);
                e.setItem(0, (newItem(Material.PRISMARINE_SHARD, ChatColor.GRAY + "Slugged Shotgun")));
                ammo = maxAmmo;
            }
        }
        ;reloadingTask.runTaskLater(main, 20*reloadTime);
    }

    public void drinkBooze() {
            e.setItem(1, newItemEnchanted(Material.GLASS_BOTTLE, ChatColor.DARK_RED + "Empty Flask", Enchantment.DAMAGE_ALL, 3));
            p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 20*12, 0));
            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20*18, 2));
            p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20*1, 0));
            reloadTime = 1;
            maxAmmo = 3;
            ammo = maxAmmo;
            p.playSound(p.getLocation(),Sound.ENTITY_GENERIC_DRINK,1f,0.2f);
            if (p.getHealth() < 8) {
                p.damage(p.getHealth()-0.5);
            } else
                p.damage(8);

            unboozeTask = new BukkitRunnable() { // Handles booze reload buff duration

                @Override
                public void run() {
                    reloadTime = 3;
                    maxAmmo = 2;
                    e.setItem(1, newItemEnchanted(Material.GLASS_BOTTLE, ChatColor.RED + "Empty Flask", Enchantment.DAMAGE_ALL, 1));
                    p.playSound(p.getLocation(),Sound.ENTITY_PLAYER_BURP,2f,1f);
                    reboozeTask = new BukkitRunnable() { // Handles booze return cooldown

                        @Override
                        public void run() {
                            e.setItem(1, newItem(Material.HONEY_BOTTLE, ChatColor.GOLD + "Booze"));
                            p.playSound(p.getLocation(),Sound.BLOCK_BREWING_STAND_BREW,1f,1f);
                            // canBooze = true;
                        }
                    };reboozeTask.runTaskLater(main, 20 * 18);
                }
            };unboozeTask.runTaskLater(main, 20 * 12);

        }
    }



