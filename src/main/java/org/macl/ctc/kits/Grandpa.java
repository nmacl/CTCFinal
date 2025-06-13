package org.macl.ctc.kits;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.macl.ctc.Main;

public class Grandpa extends Kit {

    private int maxAmmo = 2;
    private int reloadTime = 5;
    private int ammo = maxAmmo;

    public boolean fallImmune = false;

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
        setHearts(20); // Set health since you didn't specify
    }

    public void shootGun() {
        if (ammo > 0) {
            ammo--;
            p.getWorld().playSound(p.getLocation(),
                    Sound.ENTITY_FIREWORK_ROCKET_BLAST,
                    1f, 0.1f);

            Vector dir = p.getLocation().getDirection();
            SmallFireball bullet = p.getWorld()
                    .spawn(p.getEyeLocation().subtract(0, 0.5, 0),
                            SmallFireball.class);
            bullet.setShooter(p);
            bullet.setVelocity(dir.multiply(1.5));

            // knockback
            Vector r = dir.multiply((-1));
            r.setY(r.getY() * 1.7);
            p.setVelocity(r);

            fallImmune = true;


            if (ammo == 0) {
                e.setItem(0, newItem(Material.LIGHT_GRAY_DYE,
                        ChatColor.RED + "Reload"));
                reloadGun();
            }
        } else if (!isOnCooldown("Reload")) {
            reloadGun();
        }
    }

    private void reloadGun() {
        // just fire off one Kit cooldown
        e.setItem(0, newItem(Material.GRAY_DYE,
                ChatColor.GREEN + "Reloading..."));
        p.playSound(p.getLocation(),
                Sound.ITEM_ARMOR_EQUIP_LEATHER,
                1f, 1f);

        setCooldown("Reload", reloadTime, Sound.ITEM_ARMOR_EQUIP_IRON, () -> {
            // onComplete!
            ammo = maxAmmo;
            e.setItem(0, newItem(Material.PRISMARINE_SHARD,
                    ChatColor.GRAY + "Slugged Shotgun"));
            p.playSound(p.getLocation(),
                    Sound.ITEM_ARMOR_EQUIP_IRON,
                    1f, 0.5f);
        });
    }

    public void drinkBooze() {
        // Don't start if no booze or already in booze cooldown
        if (!e.contains(Material.HONEY_BOTTLE) || isOnCooldown("Booze")) return;

        // 1) Consume & show empty flask
        e.setItem(1, newItemEnchanted(
                Material.GLASS_BOTTLE,
                ChatColor.DARK_RED + "Empty Flask",
                Enchantment.DAMAGE_ALL, 3
        ));

        // 2) Apply effects
        p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 20 * 12, 0));
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 18, 2));
        p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20 * 1, 0));

        // 3) Cancel any reload in progress & give one extra shell
        cancelCooldown("Reload");
        ammo = maxAmmo + 1;  // three shells total (maxAmmo stays at 2)
        e.setItem(0, newItem(
                Material.PRISMARINE_SHARD,
                ChatColor.GRAY + "Slugged Shotgun"
        ));

        // 4) Play the drink sound & self-damage
        p.playSound(p.getLocation(),
                Sound.ENTITY_GENERIC_DRINK,
                1f, 0.2f);

        // Fixed damage logic - prevent killing player with 0.5 health
        if (p.getHealth() <= 8) {
            p.setHealth(Math.max(0.5, p.getHealth() - 4)); // Reduce damage if low health
        } else {
            p.damage(8);
        }

        // 5) Start single "Booze" timer
        setCooldown("Booze", 12, Sound.ENTITY_PLAYER_BURP, () -> {
            // effect ends → restore the booze bottle
            e.setItem(1, newItem(
                    Material.HONEY_BOTTLE,
                    ChatColor.GOLD + "Booze"
            ));
            p.playSound(p.getLocation(),
                    Sound.ENTITY_PLAYER_BURP,
                    2f, 1f);
        });
    }
}