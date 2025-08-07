package org.macl.ctc.kits;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ShulkerBullet;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.macl.ctc.Main;

import java.awt.*;
import java.util.ArrayList;
import java.util.Random;

public class Grandpa extends Kit {

    private int maxAmmo = 2;
    private int reloadTime = 3;
    private int ammo = maxAmmo;

    private int maxPepperAmmo = 3;
    private int pepperReloadTime = 9;
    private int pepperAmmo = maxPepperAmmo;

    public boolean fallImmune = false;

    public Grandpa(Main main, Player p, KitType type) {
        super(main, p, type);
        p.removePotionEffect(PotionEffectType.SPEED);
        e = p.getInventory();

        // Set up player's inventory
        e.setHelmet(newItem(Material.IRON_HELMET, ChatColor.DARK_GREEN + "Veteran's Helmet"));
        e.setItem(0, newItem(Material.PRISMARINE_SHARD, ChatColor.GRAY + "Slugged Shotgun"));
        e.setItem(1, newItem(Material.PRISMARINE_CRYSTALS, ChatColor.YELLOW + "Peppergun"));
        e.setItem(2, newItem(Material.HONEY_BOTTLE, ChatColor.GOLD + "Booze"));
        e.setItem(3, newItem(Material.LADDER, "Old Ladder", 24));
        giveWool();
        giveWool();
        setHearts(20); // Set health since you didn't specify | ( ͡° ͜ʖ ͡°)
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
            bullet.setVelocity(dir.multiply(1.6));

            // knockback
            double airLaunchModifier = -0.2;
            if (!p.isOnGround()) {
                airLaunchModifier = -1.15;
            }

            Vector r = dir.multiply((airLaunchModifier));
            r.setY(r.getY() * 1.2);
            p.setVelocity(p.getVelocity().add(r));

            fallImmune = true;

            if (ammo == 0) {
                e.setItem(0, newItem(Material.LIGHT_GRAY_DYE,
                        ChatColor.RED + "Reload"));
                reloadGun();
            }
        } else if (!isOnCooldown("Shotgun")) {
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

        int rng = (int) Math.round(Math.random());
        int extraReloadTime = 0;

        if (rng == 0) {
            extraReloadTime = 2;
        }

        main.broadcast(Integer.toString(extraReloadTime));

        setCooldown("Shotgun", reloadTime + extraReloadTime, Sound.ITEM_ARMOR_EQUIP_IRON, () -> {
            // onComplete!
            ammo = maxAmmo;
            e.setItem(0, newItem(Material.PRISMARINE_SHARD,
                    ChatColor.GRAY + "Slugged Shotgun"));
            p.playSound(p.getLocation(),
                    Sound.ITEM_ARMOR_EQUIP_IRON,
                    1f, 0.5f);
        });
    }

    public void shootPepper() {
        if (pepperAmmo > 0) {
            pepperAmmo--;
            p.getWorld().playSound(p.getLocation(),
                    Sound.ENTITY_FIREWORK_ROCKET_BLAST,
                    1f, 1.5f);

            ArrayList<ShulkerBullet> shots = new ArrayList<ShulkerBullet>();

            Vector dir = p.getLocation().getDirection();
            Vector randDir = dir;
            for (int i = 0; i < 12; ++i) {
                ShulkerBullet bullet = p.getWorld()
                        .spawn(p.getEyeLocation().subtract(0, 0.5, 0),
                                ShulkerBullet.class);
                bullet.setGravity(false);
                shots.add(bullet);
                bullet.setShooter(p);
                bullet.setVelocity(randDir.multiply(1.5));
                randDir = randomizeVectorAngle(dir,20);


            }

            new BukkitRunnable() {
                int ticks = 0;
                final int duration = 8;

                public void run() {
                    ticks++;
                    if (ticks >= duration) {
                        for (ShulkerBullet b : shots) {
                            b.remove();
                        }
                        cancel();
                    }
                }

            }.runTaskTimer(main, 0, 1);

            // knockback
            Vector r = dir.multiply((-0.1));
            p.setVelocity(r);
            fallImmune = true;

            if (pepperAmmo == 0) {
                e.setItem(1, newItem(Material.LIGHT_GRAY_DYE,
                        ChatColor.RED + "Reloading..."));
                reloadPepper();
            }
        } else if (!isOnCooldown("Peppergun")) {
            reloadPepper();
        }
    }

    private void reloadPepper() {
        // just fire off one Kit cooldown
        e.setItem(1, newItem(Material.LIGHT_GRAY_DYE,
                ChatColor.GREEN + "Repeppering..."));
        p.playSound(p.getLocation(),
                Sound.ITEM_ARMOR_EQUIP_LEATHER,
                1f, 1.5f);

        setCooldown("Peppergun", pepperReloadTime, Sound.ITEM_ARMOR_EQUIP_CHAIN, () -> {
            // onComplete!
            pepperAmmo = maxPepperAmmo;
            e.setItem(1, newItem(Material.PRISMARINE_CRYSTALS,
                    ChatColor.YELLOW + "Peppergun"));
            p.playSound(p.getLocation(),
                    Sound.ITEM_ARMOR_EQUIP_CHAIN,
                    1f, 1.5f);
        });
    }

    public void drinkBooze() {
        // Don't start if no booze or already in booze cooldown
        if (!e.contains(Material.HONEY_BOTTLE) || isOnCooldown("Booze")) return;

        // 1) Consume & show empty flask
        e.setItem(2, newItemEnchanted(
                Material.GLASS_BOTTLE,
                ChatColor.DARK_RED + "Empty Flask",
                Enchantment.DAMAGE_ALL, 3
        ));

        // 2) Apply effects
        p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 20 * 12, 0));
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 18, 2));
        p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20 * 1, 0));

        // 3) Cancel any reload in progress & give one extra shell
        cancelCooldown("Shotgun");
        ammo = maxAmmo + 1;  // three shells total (maxAmmo stays at 2)
        e.setItem(0, newItem(
                Material.PRISMARINE_SHARD,
                ChatColor.GRAY + "Slugged Shotgun"
        ));

        cancelCooldown("Peppergun");
            // onComplete!
            pepperAmmo = maxPepperAmmo;
            e.setItem(1, newItem(Material.PRISMARINE_CRYSTALS,
                    ChatColor.YELLOW + "Peppergun"));

        // 4) Play the drink sound & self-damage
        p.playSound(p.getLocation(),
                Sound.ENTITY_GENERIC_DRINK,
                1f, 0.2f);

        // Fixed damage logic - prevent killing player with 0.5 health
        if (p.getHealth() <= 5) {
            p.setHealth(Math.max(0.5, p.getHealth() - 4)); // Reduce damage if low health
        } else {
            p.damage(5);
        }

        // 5) Start single "Booze" timer
        setCooldown("Booze", 12, Sound.ENTITY_PLAYER_BURP, () -> {
            // effect ends → restore the booze bottle
            e.setItem(2, newItem(
                    Material.HONEY_BOTTLE,
                    ChatColor.GOLD + "Booze"
            ));
            p.playSound(p.getLocation(),
                    Sound.ENTITY_PLAYER_BURP,
                    2f, 1f);
        });
    }

    public static Vector randomizeVectorAngle(Vector vec, double maxAngleDeg) {
        if (vec.length() == 0) return vec.clone(); // avoid division by zero

        Random random = new Random();

        // Normalize vector
        Vector direction = vec.clone().normalize();
        double length = vec.length();

        // Generate a random axis perpendicular to the vector
        Vector randomVec = new Vector(random.nextDouble(), random.nextDouble(), random.nextDouble()).normalize();
        Vector axis = direction.clone().crossProduct(randomVec).normalize();

        if (axis.length() == 0) {
            // If axis is zero (very rare), pick arbitrary perpendicular
            axis = direction.getX() != 0 ? new Vector(0, 1, 0) : new Vector(1, 0, 0);
        }

        // Random angle in radians
        double maxAngleRad = Math.toRadians(maxAngleDeg);
        double angle = (random.nextDouble() * 2 - 1) * maxAngleRad;

        // Rotate the direction around the axis
        Vector rotated = rotateAroundAxis(direction, axis, angle);
        return rotated.multiply(length);
    }

    // Rodrigues' rotation formula
    private static Vector rotateAroundAxis(Vector v, Vector axis, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        double x = v.getX(), y = v.getY(), z = v.getZ();
        double u = axis.getX(), vA = axis.getY(), w = axis.getZ();

        return new Vector(
                (u*(u*x + vA*y + w*z)*(1 - cos) + x*cos + (-w*y + vA*z)*sin),
                (vA*(u*x + vA*y + w*z)*(1 - cos) + y*cos + (w*x - u*z)*sin),
                (w*(u*x + vA*y + w*z)*(1 - cos) + z*cos + (-vA*x + u*y)*sin)
        );
    }
}
