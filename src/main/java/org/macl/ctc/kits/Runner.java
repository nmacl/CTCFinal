package org.macl.ctc.kits;

import net.minecraft.world.entity.animal.Cod;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.macl.ctc.Main;

import java.util.ArrayList;

public class Runner extends Kit {
    Material wool = (main.game.redHas(p)) ? Material.RED_WOOL : Material.BLUE_WOOL;
    ItemStack sword = newItem(Material.STONE_SWORD, ChatColor.GOLD + "Block Run");

    public Runner(Main main, Player p, KitType type) {
        super(main, p, type);
        e.setItem(0, sword);
        p.getInventory().setItem(1, newItem(Material.CLOCK, ChatColor.WHITE + "Polar Deflection Field"));
        e.setLeggings(new ItemStack(Material.IRON_LEGGINGS, 1));
        e.setBoots(new ItemStack(Material.LEATHER_BOOTS, 1));
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 999999999, 0));
        giveWool();
        giveWool();
        setHearts(16);
    }

    ArrayList<Block> blocks = new ArrayList<Block>();
    public void blockRun() {
        if(!isOnCooldown("Block Run")) {
            setCooldown("Block Run", 18, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
            new BukkitRunnable()
            {
                int timer = 0;
                int exp = 50;
                @Override
                public void run() {
                    timer++;
                    if(main.getKits().get(p.getUniqueId()) == null) {
                        this.cancel();
                        return;
                    }
                    if(timer < 7*20) {
                        exp++;
                        playExp((exp)*0.01f);
                        for(int i = -1; i <= 3; i++) {
                            for(int j = -1; j <= 1; j++) {
                                for(int z = -1; z <= 3; z++) {
                                    Block b = p.getLocation().add(i,-1,z).getBlock();
                                    if(b.getType() == Material.AIR) {
                                        b.setType(wool);
                                        blocks.add(b);
                                    }
                                }
                            }
                        }
                    } else {
                        for(Block b : blocks)
                            b.setType(Material.AIR);
                        this.cancel();
                        return;
                    }
                }
            }.runTaskTimer(main, 0L, 1L);
        }
    }
    int ticks = 0;

    ArrayList<Entity> es = new ArrayList<>();

    boolean polarOn = false;

    public void polarField() {
        if(isOnCooldown("Deflection Field"))
            return;
        setCooldown("Deflection Field", 16, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
        new BukkitRunnable() {
            @Override
            public void run() {
                double t = 0;

                ArrayList<Projectile> proj = new ArrayList<Projectile>();

                if(ticks > 6*20 || p.isDead()) {
                    ticks = 0;
                    this.cancel();
                    es.clear();
                    return;
                }
                for(Entity e : p.getNearbyEntities(2.5, 2.5, 2.5)) {
                    if(es.contains(e)) continue;
                    if(e instanceof Cod)
                        e.remove();
                    if(e instanceof Player) {
                        Player f = (Player) e;
                        Vector velo = f.getLocation().getDirection().multiply(-0.27f);
                        velo.setY(f.getVelocity().getY() + 0.03);
                        e.setVelocity(velo);
                    }
                    if(e instanceof Projectile) {
                        es.add(e);
                        Projectile projectile = (Projectile) e;
                        projectile.setBounce(true);
                        projectile.setShooter(null);
                        if(!proj.contains(projectile)) {
                            Vector v = e.getVelocity();
                            v.multiply(-0.67f);
                            v.setY(Math.abs(v.getY()));
                            projectile.setVelocity(v);
                        }
                    }
                }

                Location loc = p.getLocation();
                p.getWorld().spawnParticle(Particle.CLOUD, loc, 5);
                ticks++;
            }
        }.runTaskTimer(main, 0L, 1L);

    }

}
