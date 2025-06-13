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
        setHearts(16);
    }

    public void codSniper() {
        if (isOnCooldown("cod")) return;
        setCooldown("cod", 3, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);

        final Set<UUID> hitThisShot = new HashSet<>();
        Entity cod = p.getWorld().spawnEntity(p.getEyeLocation(), EntityType.COD);

        // Pre‑compute colour once so it doesn’t flip every tick
        Particle.DustOptions dust =
                main.game.redHas(p)
                        ? new Particle.DustOptions(Color.fromRGB(255, 0, 0), 3.0F)   // red team
                        : new Particle.DustOptions(Color.fromRGB(0, 0, 255), 3.0F);  // blue team

        Vector velocity = p.getLocation().getDirection().multiply(2.5);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                /* Lifetime safeguard */
                if (!cod.isValid() || ++ticks > 100) { cod.remove(); cancel(); return; }

                /* Push the fish forward */
                cod.setVelocity(velocity);

                /* ✨ trail particles */
                cod.getWorld().spawnParticle(
                        Particle.REDSTONE,
                        cod.getLocation(),
                        10,                 // count
                        0.35, 0.2, 0.35,    // xyz spread
                        0.2,                // extra (speed)
                        dust);

                /* Collision check */
                for (Entity e : cod.getNearbyEntities(0.7, 0.7, 0.7)) {
                    if (e instanceof Player target
                            && target != p
                            && hitThisShot.add(target.getUniqueId())) {

                        target.setHealth(Math.max(0.5, target.getHealth() - 6));
                        target.damage(1, p);
                        p.getWorld().playSound(target.getLocation(),
                                Sound.BLOCK_BELL_USE, 1f, 1f);
                        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BELL_USE, 1f, 1f);

                        cod.remove(); cancel(); return;
                    }
                }
            }
        }.runTaskTimer(main, 0L, 1L);
    }


    public void pufferfishBomb() {
        if(isOnCooldown("pufferfish"))
            return;
        setCooldown("pufferfish", 10, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);

        // Create pufferfish item to throw
        ItemStack pufferfishItem = new ItemStack(Material.PUFFERFISH);
        // Spawn further away from player to avoid immediate collision
        Location throwLoc = p.getEyeLocation().add(p.getLocation().getDirection().multiply(1.5));

        Item thrownEggItem = p.getWorld().dropItem(throwLoc, pufferfishItem);
        // Throw it like a grenade with proper arc
        Vector throwVelocity = p.getLocation().getDirection().multiply(1.5);
        throwVelocity.setY(throwVelocity.getY() + 0.3); // Add upward arc
        thrownEggItem.setVelocity(throwVelocity);

        // Play throwing sound
        p.playSound(p.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1f, 0.8f);

        BukkitTask t = new BukkitRunnable() {
            int timer = 0; // Local timer variable
            @Override
            public void run() {
                timer++;

                // Check if item is still valid
                if (!thrownEggItem.isValid()) {
                    this.cancel();
                    return;
                }

                double y = thrownEggItem.getVelocity().getY();

                // Add minimum timer to prevent immediate explosion on player (at least 1 second)
                // Improved landing detection - check if on ground or timer exceeded
                if (timer >= 2 && (Math.abs(y) < 0.1 || timer >= 10 || thrownEggItem.isOnGround())) {
                    Location bombLoc = thrownEggItem.getLocation();

                    // Spawn pufferfish
                    for(int i = 0; i < 10; i++) {
                        Entity pufferfish = p.getWorld().spawnEntity(bombLoc, EntityType.PUFFERFISH);

                        // Give pufferfish random velocity for spread
                        Vector randomVel = new Vector(
                                (Math.random() - 0.1) * 2,
                                Math.random() * 0.1,
                                (Math.random() - 0.1) * 2
                        );
                        pufferfish.setVelocity(randomVel);
                    }

                    // Add explosion effect
                    p.getWorld().playSound(bombLoc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
                    p.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, bombLoc, 3);

                    thrownEggItem.remove();
                    this.cancel();
                }
            }
        }.runTaskTimer(main, 0L, 5L);
        registerTask(t);
    }
}