package org.macl.ctc.kits;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.macl.ctc.Main;

public class Grandpa extends Kit {

    private int maxAmmo = 2;
    private int reloadTime = 3;
    private int ammo = maxAmmo;

    public Grandpa(Main main, Player p, KitType type) {
        super(main, p, type);
        p.removePotionEffect(PotionEffectType.SPEED);
        e = p.getInventory();

        // Set up player's inventory
        e.setHelmet(newItem(Material.IRON_HELMET, ChatColor.DARK_GREEN + "Veteran's Helmet"));
        e.setItem(0, newItem(Material.PRISMARINE_SHARD, ChatColor.GRAY + "Slugged Shotgun"));
        e.setItem(1, newItem(Material.HONEY_BOTTLE, ChatColor.GOLD + "Booze"));
        e.setItem(2, newItem(Material.LADDER, "Old Ladder", 24));
        giveWool();
        giveWool();
    }

    public void shootGun() {
        if (ammo > 0) {
            ammo--;
            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1f, 0.1f);

            // Apply negative velocity for knockback effect
            Vector direction = p.getLocation().getDirection().clone();

            // Spawn bullet (SmallFireball)
            Location bulletLocation = p.getLocation().add(0, p.getEyeHeight() - 0.5, 0);
            SmallFireball bullet = p.getWorld().spawn(bulletLocation, SmallFireball.class);
            bullet.setShooter(p);
            bullet.setVelocity(direction.multiply(1.5)); // Adjust speed as needed

            Vector knockback = direction.multiply(-1).setY(1.7); // Adjust Y for upward momentum
            p.setVelocity(knockback);

            if (ammo == 0) {
                e.setItem(0, newItem(Material.LIGHT_GRAY_DYE, ChatColor.RED + "Reload"));
                reloadGun();
            }
        } else if (!isOnCooldown("Reload")) {
            reloadGun();
        }
    }

    public void reloadGun() {
        if (isOnCooldown("Reload")) {
            return;
        }

        e.setItem(0, newItem(Material.GRAY_DYE, ChatColor.GREEN + "Reloading..."));
        p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1);

        // Use the cooldown system from the Kit class
        setCooldown("Reload", reloadTime, Sound.ITEM_ARMOR_EQUIP_IRON);

        new BukkitRunnable() {
            @Override
            public void run() {
                p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1f, 0.5f);
                e.setItem(0, newItem(Material.PRISMARINE_SHARD, ChatColor.GRAY + "Slugged Shotgun"));
                ammo = maxAmmo;
            }
        }.runTaskLater(main, 20 * reloadTime);
    }

    public void drinkBooze() {
        if (e.contains(Material.HONEY_BOTTLE)) {
            e.setItem(1, newItemEnchanted(Material.GLASS_BOTTLE, ChatColor.DARK_RED + "Empty Flask", Enchantment.DAMAGE_ALL, 3));

            // Apply potion effects
            p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 20 * 12, 0));
            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 18, 2));
            p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20 * 1, 0));

            // Adjust weapon stats
            reloadTime = 1;
            maxAmmo = 3;
            ammo = maxAmmo;

            p.playSound(p.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1f, 0.2f);
            if (p.getHealth() < 8) {
                p.setHealth(0.5);
            } else {
                p.damage(8);
            }

            // Set cooldown for the booze effect
            setCooldown("Booze", 12, Sound.ENTITY_PLAYER_BURP);

            new BukkitRunnable() {
                @Override
                public void run() {
                    reloadTime = 3;
                    maxAmmo = 2;
                    e.setItem(1, newItemEnchanted(Material.GLASS_BOTTLE, ChatColor.RED + "Empty Flask", Enchantment.DAMAGE_ALL, 1));
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_BURP, 2f, 1f);

                    // Start cooldown before booze can be used again
                    setCooldown("Booze Recharge", 18, Sound.BLOCK_BREWING_STAND_BREW);

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            e.setItem(1, newItem(Material.HONEY_BOTTLE, ChatColor.GOLD + "Booze"));
                            p.playSound(p.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 1f, 1f);
                        }
                    }.runTaskLater(main, 20 * 18); // Booze returns after 18 seconds
                }
            }.runTaskLater(main, 20 * 12); // Booze effect lasts 12 seconds
        }
    }
}
