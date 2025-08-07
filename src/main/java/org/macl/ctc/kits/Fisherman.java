package org.macl.ctc.kits;

import net.minecraft.world.damagesource.DamageSource;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.macl.ctc.Main;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Fisherman extends Kit {

    public Fisherman(Main main, Player p, KitType type) {
        super(main, p, type);
        e.addItem(newItem(Material.FISHING_ROD, ChatColor.GOLD + "Fishing Rod"));
        e.setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));
        e.setHelmet(new ItemStack(Material.TURTLE_HELMET));
        e.setBoots(new ItemStack(Material.LEATHER_BOOTS));
        e.addItem(newItem(Material.PUFFERFISH, ChatColor.YELLOW + "Pufferfish Bomb"));
        e.addItem(newItem(Material.COD, ChatColor.LIGHT_PURPLE + "Cod Sniper"));
        giveWool();
        giveWool();
        setHearts(18);
    }

    public void codSniper() {
        if (isOnCooldown("cod")) return;
        setCooldown("cod", 3, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);

        final Set<UUID> hitThisShot = new HashSet<>();
        Entity cod = p.getWorld().spawnEntity(p.getEyeLocation(), EntityType.COD);

        // Pre-compute colour once so it doesn’t flip every tick
        Particle.DustOptions dust =
                main.game.redHas(p)
                        ? new Particle.DustOptions(Color.fromRGB(255, 0, 0), 3.0F)
                        : new Particle.DustOptions(Color.fromRGB(0, 0, 255), 3.0F);

        Vector velocity = p.getLocation().getDirection().multiply(2.5);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                // Lifetime safeguard
                if (!cod.isValid() || ++ticks > 100) {
                    cod.remove();
                    cancel();
                    return;
                }

                // Push the fish forward
                cod.setVelocity(velocity);

                // ✨ trail particles
                cod.getWorld().spawnParticle(
                        Particle.REDSTONE,
                        cod.getLocation(),
                        10,
                        0.35, 0.2, 0.35,
                        0.2,
                        dust
                );

                // Collision check
                for (Entity e : cod.getNearbyEntities(0.7, 0.7, 0.7)) {
                    if (e instanceof Player target
                            && !target.getUniqueId().equals(p.getUniqueId())
                            && hitThisShot.add(target.getUniqueId())) {

                        double airborneDamage = target.isOnGround() ? 0 : 2;
                        // first drop them near-zero but never kill
                        target.setHealth(Math.max(0.5, target.getHealth() - (6 + airborneDamage)));
                        // then deal the killing blow
                        target.damage(1, p);

                        // **record the kill if they died**
                        if (target.isDead()) {
                            main.getStats().recordKill(p);
                        }

                        p.getWorld().playSound(target.getLocation(),
                                Sound.BLOCK_BELL_USE, 1f, 1f);
                        p.getWorld().playSound(p.getLocation(),
                                Sound.BLOCK_BELL_USE, 1f, 1f);

                        cod.remove();
                        cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(main, 0L, 1L);
    }



    public void pufferfishBomb() {
        if (isOnCooldown("pufferfish")) return;
        setCooldown("pufferfish", 10, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);

        // 1️⃣  throw the “grenade” item
        Location throwLoc = p.getEyeLocation()
                .add(p.getLocation().getDirection().multiply(1.5));
        Item thrown = p.getWorld().dropItem(throwLoc,
                new ItemStack(Material.PUFFERFISH));
        Vector vel = p.getLocation().getDirection().multiply(1.5);
        vel.setY(vel.getY() + 0.3);
        thrown.setVelocity(vel);
        thrown.setPickupDelay(Integer.MAX_VALUE);           // players can’t grab it
        p.playSound(p.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1f, 0.8f);

        // 2️⃣  wait until the item slows or hits the ground, then detonate
        BukkitTask t = new BukkitRunnable() {
            int timer = 0;
            @Override
            public void run() {
                timer++;
                if (!thrown.isValid()) { cancel(); return; }

                boolean lowVel  = Math.abs(thrown.getVelocity().getY()) < 0.1;
                boolean tooLong = timer >= 10;
                if (timer >= 2 && (lowVel || tooLong || thrown.isOnGround())) {

                    // --- DETONATION ---
                    Location bombLoc = thrown.getLocation();

                    // 3️⃣  spawn a tight “cloud” of fake pufferfish
                    double clusterRadius = 0.6;           // keeps fish close together
                    for (int i = 0; i < 10; i++) {
                        PufferFish fish = (PufferFish) p.getWorld()
                                .spawnEntity(bombLoc, EntityType.PUFFERFISH);

                        // keep them alive & stationary for ~2 s
                        fish.setPuffState(2);      // 0=deflated, 1=half, 2=full
                        fish.setInvulnerable(true);
                        fish.setRemainingAir(999999);
                        fish.setAware(false);             // disable AI flopping
                        fish.setVelocity(new Vector(
                                (Math.random() - 0.5) * 0.2,
                                0.05 + Math.random() * 0.05,
                                (Math.random() - 0.5) * 0.2));

                        // schedule cleanup so they don’t hang around forever
                        new BukkitRunnable() {
                            @Override
                            public void run() { fish.remove(); }
                        }.runTaskLater(main, 140L);         // 2 s later
                    }

                    // 4️⃣  SFX + actual damage
                    p.getWorld().playSound(bombLoc,
                            Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
                    p.getWorld().spawnParticle(Particle.EXPLOSION_LARGE,
                            bombLoc, 3);

                    // ~5-block lethal circle (6 dmg @ center → 0 @ 5 blocks)
                    main.fakeExplode(
                            p, bombLoc,
                            6,        // maxDamage
                            5,        // maxDistance (radius)
                            false,    // no fire
                            false,    // no block break
                            false     // no ally damage
                    );

                    thrown.remove();
                    cancel();
                }
            }
        }.runTaskTimer(main, 0L, 2L);     // check every 2 ticks

        registerTask(t);
    }


}