package org.macl.ctc.kits;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.macl.ctc.Main;

import java.util.ArrayList;
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
    }
    int timer = 0;
    int cod = 0;

    ArrayList<Location> locs = new ArrayList<Location>();
    Location shooter;

    boolean canCod = true;
    ArrayList<UUID> lits = new ArrayList<UUID>();

    public void codSniper() {
        if(!canCod)
            return;
        canCod = false;
        shooter = p.getLocation();
        Entity e = p.getWorld().spawnEntity(p.getEyeLocation(), EntityType.COD);
        Vector m = p.getLocation().getDirection().multiply(3);

        new BukkitRunnable() {
            @Override
            public void run() {
                canCod = true;
                p.playSound(p.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_1, 1f, 1f);
            }
        }.runTaskLater(main, 20*4);

        new BukkitRunnable() {
            @Override
            public void run() {
                p.getWorld().playSound(e.getLocation(), Sound.BLOCK_PACKED_MUD_FALL, 3f, 3f);
                e.setVelocity(m);
                Particle.DustOptions dustOptions = (main.game.redHas(p)) ? new Particle.DustOptions(Color.fromRGB(255, 0, 0), 3.0F) : new Particle.DustOptions(Color.fromRGB(0, 0, 255), 3.0F);
                locs.add(e.getLocation());
                for(Location loc : locs)
                    p.getWorld().spawnParticle(Particle.REDSTONE, loc, 1, 0.35,0.2,0.35,0, dustOptions);
                cod++;
                if(cod >= 20*3.5 || e == null) {
                    shooter = null;
                    this.cancel();
                    cod = 0;
                    locs.clear();
                    lits.clear();
                    return;
                }
                for(Entity e : e.getNearbyEntities(0.7, 0.7, 0.7)) {
                    if(e instanceof Player) {
                        Player p1 = (Player) e;
                        if(p.getUniqueId() == p1.getUniqueId() && !lits.contains(p1.getUniqueId()))
                            continue;
                        double health = shooter.distance(p1.getLocation());
                        health = health*0.7;
                        if(health > 12)
                            health = 12;
                        if(p1.getHealth() - health >= 0)
                            p1.setHealth(p1.getHealth() - health);
                        else
                            p1.setHealth(0);
                        p1.getWorld().playSound(p1.getLocation(), Sound.BLOCK_BELL_USE, 5f, 5f);
                        p1.getWorld().playSound(p.getLocation(), Sound.BLOCK_BELL_USE, 5f, 5f);
                        e.remove();
                        lits.add(p1.getUniqueId());
                    }
                }
            }
        }.runTaskTimer(main, 0L, 1L);
    }


    boolean canPufferfish = true;
    public void pufferfishBomb() {
        if(!canPufferfish)
            return;
        canPufferfish = false;
        ItemStack eggItem = e.getItemInMainHand();
        Location pLoc = p.getEyeLocation();

        ItemStack throwStack = new ItemStack(eggItem);
        throwStack.setAmount(1);

        Item thrownEggItem = p.getWorld().dropItem(pLoc, throwStack);
        thrownEggItem.setVelocity(pLoc.getDirection());
        e.setItemInMainHand(eggItem);


        new BukkitRunnable() {
            @Override
            public void run() {
                timer++;
                if(timer == 20 || thrownEggItem == null || thrownEggItem.getLocation().getY() <= -64) {
                    this.cancel();
                    p.getWorld().strikeLightning(thrownEggItem.getLocation());
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            canPufferfish = true;
                            p.playSound(p.getLocation(), Sound.ENTITY_PUFFER_FISH_BLOW_UP, 1f, 1f);
                        }
                    }.runTaskLater(main, 20*10);
                    return;
                }
                double y = thrownEggItem.getVelocity().getY();
                if(y == 0) {
                    this.cancel();
                    for(int i = 0; i < 10; i++)
                        p.getWorld().spawnEntity(thrownEggItem.getLocation(), EntityType.PUFFERFISH);
                    thrownEggItem.remove();
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            canPufferfish = true;
                            p.playSound(p.getLocation(), Sound.ENTITY_PUFFER_FISH_BLOW_UP, 1f, 1f);                        }
                    }.runTaskLater(main, 20*10);
                }

            }
        }.runTaskTimer(main, 0L, 5L);
    }
}
