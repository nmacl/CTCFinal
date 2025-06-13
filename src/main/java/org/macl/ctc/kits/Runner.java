package org.macl.ctc.kits;

import net.minecraft.world.entity.animal.Cod;
import net.minecraft.world.entity.projectile.FishingHook;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.macl.ctc.Main;

import java.util.ArrayList;
import java.util.List;

public class Runner extends Kit {
    Material wool = (main.game.redHas(p)) ? Material.RED_WOOL : Material.BLUE_WOOL;
    ItemStack sword = newItem(Material.STONE_SWORD, ChatColor.GOLD + "Block Run");

    public Runner(Main main, Player p, KitType type) {
        super(main, p, type);
        e.setItem(0, sword);
        p.getInventory().setItem(1, newItem(Material.CLOCK, ChatColor.WHITE + "Polar Deflection Field"));
        p.getInventory().setItem(2, newItem(Material.SLIME_BALL, ChatColor.DARK_GREEN + "Platform"));
        e.setLeggings(new ItemStack(Material.IRON_LEGGINGS, 1));
        e.setBoots(new ItemStack(Material.LEATHER_BOOTS, 1));
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 999999999, 0));
        giveWool();
        giveWool();
        setHearts(16);
    }

    ArrayList<Block> blocks = new ArrayList<Block>();
    public void blockRun() {
        if (!isOnCooldown("Block Run")) {
            setCooldown("Block Run", 18, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);

            // grab the BukkitTask and register it
            BukkitTask runTask = new BukkitRunnable() {
                int timer = 0;
                int exp = 50;

                @Override
                public void run() {
                    timer++;
                    // if the player no longer has a kit, stop
                    if (main.getKits().get(p.getUniqueId()) == null) {
                        this.cancel();
                        return;
                    }
                    if (timer < 7 * 20) {
                        exp++;
                        playExp(exp * 0.01f);
                        for (int i = -1; i <= 3; i++) {
                            for (int j = -1; j <= 1; j++) {
                                for (int z = -1; z <= 3; z++) {
                                    Block b = p.getLocation().add(i, -1, z).getBlock();
                                    if (b.getType() == Material.AIR) {
                                        b.setType(wool);
                                        blocks.add(b);
                                    }
                                }
                            }
                        }
                    } else {
                        // clean up
                        for (Block b : blocks) b.setType(Material.AIR);
                        this.cancel();
                    }
                }
            }.runTaskTimer(main, 0L, 1L);

            registerTask(runTask);
        }
    }

    int ticks = 0;
    ArrayList<Entity> es = new ArrayList<>();

    public void polarField() {
        if(isOnCooldown("Deflection Field"))
            return;
        setCooldown("Deflection Field", 16, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);

        // Register the task properly
        BukkitTask fieldTask = new BukkitRunnable() {
            @Override
            public void run() {
                ArrayList<Projectile> proj = new ArrayList<Projectile>();

                if(ticks > 6*20 || p.isDead()) {
                    ticks = 0;
                    this.cancel();
                    es.clear();
                    return;
                }
                for(Entity e : p.getNearbyEntities(2.5, 2.5, 2.5)) {
                    if(es.contains(e)) continue;

                    if(e instanceof Projectile) {
                        es.add(e);
                        Projectile projectile = (Projectile) e;
                        if(!(projectile instanceof FishHook || projectile instanceof FishingHook)) {
                            projectile.setBounce(true);
                            projectile.setShooter(null);
                        }
                        if(!proj.contains(projectile)) {
                            Vector v = e.getVelocity();
                            v.multiply(-0.67f);
                            v.setY(Math.abs(v.getY()));
                            projectile.setVelocity(v);
                        }
                    } else {
                        // Handle all other entities (players, mobs, etc.)

                        // Calculate radial direction from player to target
                        Vector direction = e.getLocation().toVector().subtract(p.getLocation().toVector()).normalize();

                        // Apply radial knockback with lift
                        Vector knockback = direction.multiply(0.8f);
                        knockback.setY(0.4f); // Add upward lift

                        e.setVelocity(knockback);

                        // Spawn particle trail on the knocked back entity
                        new BukkitRunnable() {
                            int trailTicks = 0;
                            @Override
                            public void run() {
                                if(trailTicks > 20 || e.isDead()) { // Trail for 1 second
                                    this.cancel();
                                    return;
                                }
                                e.getWorld().spawnParticle(Particle.CRIT, e.getLocation().add(0, 1, 0), 3, 0.2, 0.2, 0.2, 0.1);
                                trailTicks++;
                            }
                        }.runTaskTimer(main, 0L, 1L);
                    }
                }

                Location loc = p.getLocation();
                p.getWorld().spawnParticle(Particle.CLOUD, loc, 5);
                ticks++;
            }
        }.runTaskTimer(main, 0L, 1L);

        // Register the task
        registerTask(fieldTask);
    }

    /* keep this alongside your other per‑ability fields if you later decide
       you want to remember which blocks were placed so they can be removed */
    private final List<Block> platformBlocks = new ArrayList<>();

    /**
     * Spawns a 5×5 concrete platform two blocks beneath the player, then
     * starts a 12‑second cooldown (“Platform”).
     */
    public void platform() {
        // 1. cooldown gate
        if (isOnCooldown("Platform")) return;
        setCooldown("Platform", 12, Sound.BLOCK_AMETHYST_BLOCK_PLACE);

        // 2. make sure the player won’t take fall damage immediately afterwards
        p.setFallDistance(0);

        // 3. build the platform
        Location base = p.getLocation();                     // current position

        for (int x = -2; x <= 2; x++) {                      // -2 … +2  (5 blocks)
            for (int z = -2; z <= 2; z++) {
                Block b = base.clone().add(x, -2, z).getBlock();
                Material current = b.getType();

                // --- new safety check -------------------------------------------------
                if (main.restricted.contains(current)) {     // don’t touch restricted materials
                    continue;
                }
                // ---------------------------------------------------------------------

                if (current == Material.AIR) {               // place only into empty space
                    if (main.game.redHas(p)) {
                        b.setType(Material.RED_CONCRETE, false);
                    } else if (main.game.blueHas(p)) {
                        b.setType(Material.BLUE_CONCRETE, false);
                    } else {
                        b.setType(Material.BLACK_CONCRETE, false);
                    }
                }
            }
        }
    }

}
