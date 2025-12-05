package org.macl.ctc.kits;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.macl.ctc.Main;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.Callable;

public class Grandma extends Kit {

    ItemStack healCookies = newItem(Material.COOKIE, ChatColor.RED + "Heal Cookies", 3);
    ItemStack scooterItem = newItem(Material.MINECART,ChatColor.GRAY + "Scooter",1);
    ItemStack classicCane = newItem(Material.BLAZE_ROD,ChatColor.GOLD + "Classic Cane");

    boolean isClassy = false;
    boolean ridingScooter = false;
    int hitCount = 0;
    int maxHitCount = 4;
    Minecart scooterEntity;
    int explodeScooter;
    BukkitTask caneTask;
    BukkitTask scooterTask;

    public Grandma(Main main, Player p, KitType type) {
        super(main, p, type);
        p.removePotionEffect(PotionEffectType.SPEED);
        PlayerInventory e = p.getInventory();
        giveCane(hitCount);
        e.addItem(healCookies);
        e.addItem(scooterItem);
//        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 99999999, 0));
        e.setLeggings(newItemEnchanted(Material.GOLDEN_LEGGINGS, ChatColor.GOLD + "Golden Pantaloons", Enchantment.SWIFT_SNEAK, 1));
        e.setChestplate(newItem(Material.LEATHER_CHESTPLATE, "Flower Dress"));
        giveWool();
        giveWool();
        setCookieRegen(0);
        setHearts(24);
    }

    public void heart() {
        if (isOnCooldown("Heal")) return;

        int cookies = p.getInventory().first(Material.COOKIE);

        setCooldown("Heal",4,Sound.ENTITY_VILLAGER_CELEBRATE);
        World world = p.getWorld();
        p.getInventory().getItem(cookies).setAmount(p.getInventory().getItem(cookies).getAmount() - 1);
        world.spawnParticle(Particle.HEART, p.getLocation().add(0,2,0), 10);
        world.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_AMBIENT, 1f, 0.8f);

        double radius = 2.5;

        for (double t = 0; t <= Math.PI*radius; t += 0.31) {
            Vector angle = new Vector(radius,0,0);

            Vector newAngle = angle.rotateAroundY(t);

            world.spawnParticle(Particle.HEART, p.getLocation().add(newAngle).add(0,0.5,0), 1);

        }

        double healing = 6.0;

        Collection<Entity> entities = p.getNearbyEntities(radius,radius,radius);
        // heal self
        if(p.getHealth() + healing > 20)
            p.setHealth(p.getMaxHealth());
        else
            p.setHealth(p.getHealth() + healing);
        //heal allies
        for (Entity e : entities) {
            if (e instanceof Player pe) {

                if (!(main.game.sameTeam(p.getUniqueId(),pe.getUniqueId()))) continue;

                if(pe.getHealth() + healing > 20)
                    pe.setHealth(pe.getMaxHealth());
                else
                    pe.setHealth(pe.getHealth() + healing);
            }
        }
    }

    public void setCookieRegen(int time) {
        RegenItem c = regenItem("Cookie", healCookies, 14, 3, 1);
    }

    public void giveCane(int stacks) {
        PlayerInventory e = p.getInventory();
        ItemStack cane = newItem(Material.STICK,ChatColor.DARK_PURPLE + "Cane");
        cane.addUnsafeEnchantment(Enchantment.DAMAGE_ALL,1);
        cane.addUnsafeEnchantment(Enchantment.KNOCKBACK,1);
        cane.setAmount(stacks + 1);
        e.setItem(0,cane);

    }

    public void giveClassicCane() {
        PlayerInventory e = p.getInventory();
        ItemStack cc = classicCane.clone();
        cc.addUnsafeEnchantment(Enchantment.DAMAGE_ALL,5);
        cc.addUnsafeEnchantment(Enchantment.KNOCKBACK,10);
        cc.setAmount(maxHitCount + 1);
        e.setItem(0,cc);

        p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20 * 1, 0));
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 3f, 0.5f);

        BukkitTask timeoutTask = new BukkitRunnable() {
            int time = 8;
            public void run() {
                time--;
                p.setLevel(time);
                float pitch = 0.9f - ((float) time * 0.1f);
                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.1f, pitch);
                if (time <= 0) {
                    isClassy = false;
                    p.getWorld().playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1.0f);
                    giveCane(hitCount);
                    cancel();
                }
            }
        }.runTaskTimer(main,0,20L);
        registerTask(timeoutTask);

        caneTask = timeoutTask;
    }

    public void onCaneHit() {
        hitCount++;
        giveCane(hitCount);
        if (hitCount >= maxHitCount) {
            giveClassicCane();
            hitCount = 0;
            isClassy = true;

        }
    }

    public void onClassicCaneHit () {
        giveCane(hitCount);
        p.setLevel(0);
        isClassy = false;
        if (caneTask != null) {
            caneTask.cancel();
        }

        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 4f, 2.0f);
        p.getWorld().spawnParticle(Particle.SMOKE_LARGE,p.getLocation(),20,0.0,0.0,0.0,5.0);

        if (p.getHealth() <= 4) { //Sacrifice health for big knockback
            p.setHealth(Math.max(0.5, p.getHealth() - 4));
        } else {
            p.damage(4);
            p.setNoDamageTicks(0);
        }

        main.fakeExplode(p,p.getLocation(),0,1,false,false,false, "cane");

    }

    public void useScooter() {

        if (isOnCooldown("Scooter")) return;

        setScooterHotbar();

        Minecart scooter = p.getWorld().spawn(p.getLocation(),Minecart.class);
        scooter.addPassenger(p);
        scooterProcess sp = new scooterProcess();
        sp.scooter = scooter;
        scooterEntity = scooter;
        ridingScooter = true;
        BukkitTask spTask = sp.runTaskTimer(main,0,1L);
        registerTask(spTask);
    }

    public void setScooterHotbar() {
        PlayerInventory inv = p.getInventory();
        ItemStack blowUp = newItem(Material.FIREWORK_STAR,ChatColor.DARK_RED + "Explode Scooter");
        ItemStack jump = newItem(Material.FEATHER,ChatColor.GREEN + "Scooter Jump");
        inv.setItem(0,blowUp);
        inv.setItem(2,jump);
    }

    public void cleanScooterHotbar() {
        PlayerInventory inv = p.getInventory();
        if (isClassy) {
            ItemStack cc = classicCane.clone();
            cc.addUnsafeEnchantment(Enchantment.DAMAGE_ALL,4);
            cc.addUnsafeEnchantment(Enchantment.KNOCKBACK,8);
            cc.setAmount(maxHitCount + 1);
            inv.setItem(0,cc);
        } else {
            giveCane(hitCount);
        }
        inv.setItem(2,scooterItem);
    }

    public class scooterProcess extends BukkitRunnable {
        int time = 0;
        int maxTime = 15 * 20;
        Minecart scooter;

        public void run() {
            if (!scooter.isValid()) {
                blowUp();
            }

            if (scooter.getPassengers().isEmpty()) {
                despawn();
            }

            if (time <= maxTime) {
                time++;
                scooter.getWorld().spawnParticle(Particle.SMOKE_LARGE, scooter.getLocation().add(0.0,0.1,0.0), 2,0.1,0.1,0.1);
//                p.setLevel((int) (maxTime / 20));
                handleScooterMovement();
                p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 2, 1));
                scooter.setFallDistance(0f);
            } else {
                despawn();
            }
        }

        public void blowUp() {
            main.fakeExplode(p,p.getLocation().add(0.0,1.0,0.0),14,4,true,true,true, "scooter");

            scooter.remove();
            cleanScooterHotbar();
            setCooldown("Scooter",15,Sound.BLOCK_BREWING_STAND_BREW);
            cancel();
        }

        public void despawn() {
            scooter.remove();
            cleanScooterHotbar();
            setCooldown("Scooter",15,Sound.BLOCK_BREWING_STAND_BREW);
            cancel();
        }

        public void handleScooterMovement() {
            Vector velocity = p.getEyeLocation().getDirection().setY(0).normalize().multiply(1.0);
            double fallVel = scooter.getVelocity().getY();
            scooter.setRotation(p.getEyeLocation().getYaw() + 90,0);
            scooter.setDerailedVelocityMod(new Vector(1.1,0.0,1.1));
            scooter.setVelocity(velocity.add(new Vector(0,fallVel,0)));
        }
    }

    public void scooterJump () {
        if (isOnCooldown("SJump")) return;
        setCooldown("SJump",5,Sound.ENTITY_HORSE_JUMP);
        if (scooterEntity != null) {
            scooterEntity.setVelocity(new Vector(0,1.0,0));
        }
    }

    public void scooterExplode() {
        if (scooterEntity != null && !scooterEntity.getPassengers().isEmpty()) {
            scooterEntity.removePassenger(scooterEntity.getPassengers().get(0));
        }

        main.fakeExplode(p,p.getLocation().add(0.0,1.0,0.0),18,6,false,true,true, "scooter");
        p.getWorld().createExplosion(p.getLocation(), 3f, false, true);
    }
}
