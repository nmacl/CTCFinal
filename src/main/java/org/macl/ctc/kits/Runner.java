package org.macl.ctc.kits;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
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
import org.macl.ctc.timers.cooldownTimer;

import java.util.ArrayList;

public class Runner extends Kit {
    Material wool = (main.game.redHas(p)) ? Material.RED_WOOL : Material.BLUE_WOOL;
    ItemStack sword = newItemEnchanted(Material.STONE_SWORD, ChatColor.GOLD + "Block Run", Enchantment.DAMAGE_ALL, 1);

    public Runner(Main main, Player p, KitType type) {
        super(main, p, type);
        e.setItem(0, sword);
        p.getInventory().setItem(1, newItem(Material.CLOCK, ChatColor.WHITE + "Polar Deflection Field"));
        e.setLeggings(new ItemStack(Material.IRON_LEGGINGS, 1));
        e.setBoots(new ItemStack(Material.LEATHER_BOOTS, 1));
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 999999999, 0));
        cooldowns.put("blockRun", false);
        giveWool();
        giveWool();
    }
    ArrayList<Block> blocks = new ArrayList<Block>();
    public void blockRun() {
        if(!cooldowns.get("blockRun")) {
            new cooldownTimer(this, 18*20, "blockRun", sword)
            {
                int exp = 50;
                @Override
                public void run() {
                    if(main.getKits().get(p.getUniqueId()) == null || !(main.getKits().get(p.getUniqueId()) instanceof Runner)) {
                        this.cancel();
                        return;
                    }
                    super.run();
                    if(timer > 13*20) {
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
                        return;
                    }
                }
            }.runTaskTimer(main, 0L, 1L);
        }
    }
    int ticks = 0;

    ArrayList<Entity> es = new ArrayList<>();

    public void polarField() {
        // cooldown
        new BukkitRunnable() {
            @Override
            public void run() {
                double t = 0;

                ArrayList<Projectile> proj = new ArrayList<Projectile>();

                if(ticks > 8*20) {
                    ticks = 0;
                    this.cancel();
                    es.clear();
                    return;
                }
                for(Entity e : p.getNearbyEntities(4, 4, 4)) {
                    if(es.contains(e)) continue;
                    if(e instanceof Player) {
                        Player f = (Player) e;
                        Vector velo = f.getLocation().getDirection().multiply(-0.25f);
                        velo.setY(velo.getY() + 0.75);
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
                for(int r = 5; r > -3; r--) {

                    t = t + Math.PI/4;
                    double x = r*Math.cos(t);
                    double y = 1;
                    y = r*0.5;
                    double z = r*Math.sin(t);
                    loc.add(x,y,z);
                    p.getWorld().spawnParticle(Particle.CLOUD, loc, 5);
                }
                ticks++;
            }
        }.runTaskTimer(main, 0L, 1L);

    }

}
