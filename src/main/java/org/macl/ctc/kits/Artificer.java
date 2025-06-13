package org.macl.ctc.kits;

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

public class Artificer extends Kit {
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

            @Override
            public void run() {
                if (count++ >= reps) {
                    cancel();
                    p.getWorld().playSound(p.getLocation(), Sound.BLOCK_LANTERN_BREAK, 2f, 0.5f);
                    return;
                }
                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1.5f);
                Location start = p.getEyeLocation();
                Vector dir = start.getDirection();
                for (int i = 0; i * step <= maxLen; i++) {
                    Location loc = start.clone().add(dir.clone().multiply(i * step));
                    RayTraceResult hit = p.getWorld().rayTraceBlocks(start, dir, i * step);
                    if (hit != null && hit.getHitBlock() != null) break;
                    p.getWorld().spawnParticle(Particle.FLAME, loc.subtract(0, 0, 0), 0);
                    for (Entity ent : p.getWorld().getNearbyEntities(loc, damageRad, damageRad, damageRad)) {
                        if (ent instanceof LivingEntity && !ent.equals(p) && p.hasLineOfSight(ent)) {
                            ((LivingEntity) ent).damage(damage);
                            ent.setFireTicks(40);
                        }
                    }
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
        if (isOnCooldown("VoidBomb")) return;
        setCooldown("VoidBomb", 9, Sound.ENTITY_GENERIC_EXPLODE, () -> {
            p.getInventory().setItem(2, newItem(Material.FLINT, ChatColor.GRAY + "Void Bomb"));
        });
        // show used icon
        p.getInventory().setItem(2, newItem(Material.COBWEB, ChatColor.DARK_GRAY + "Void Bombing..."));

        Vector dir = p.getEyeLocation().getDirection();
        Item bomb = p.getWorld().dropItem(p.getEyeLocation(), new ItemStack(Material.BLACK_CONCRETE_POWDER));
        bomb.setVelocity(dir.multiply(1.5));

        new BukkitRunnable() {
            List<Block> toRemove = new ArrayList<>();
            int tick = 0;
            final int maxIter = 60;
            final double rad = 4;

            @Override
            public void run() {
                if (bomb.isOnGround() || tick >= maxIter) {
                    Location c = bomb.getLocation();
                    for (BlockFace face : BlockFace.values()) {
                        Block nb = c.getBlock().getRelative(face);
                        if (nb.getType() != Material.AIR) nb.setType(Material.BLACK_CONCRETE);
                    }
                    bomb.remove();
                    cancel();
                    return;
                }
                tick++;
            }
        }.runTaskTimer(main, 0, 1);
    }

    // Updraft: 12s cooldown, single ability
    public void useUpdraft() {
        if (isOnCooldown("Updraft")) return;
        setCooldown("Updraft", 8, Sound.ENTITY_HORSE_BREATHE, () -> {
            p.getInventory().setItem(2, newItem(Material.FEATHER, ChatColor.WHITE + "Updraft"));
        });
        p.getInventory().setItem(2, newItem(Material.GUNPOWDER, ChatColor.GRAY + "Updraft..."));

        Vector v = p.getLocation().getDirection().multiply(0.5).setY(2);
        p.setVelocity(v);
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 20 * 8, 1));
        for (Entity ent : p.getWorld().getNearbyEntities(p.getLocation(), 6, 6, 6)) {
            if (ent != p) ent.setVelocity(ent.getLocation().toVector().subtract(p.getLocation().toVector()).normalize().multiply(2));
        }
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_HORSE_BREATHE, 10f, 0.8f);
    }
}
