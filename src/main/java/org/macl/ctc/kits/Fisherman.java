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

public class Fisherman extends Kit {


    public Fisherman(Main main, Player p, KitType type) {
        super(main, p, type);
        e.addItem(newItem(Material.FISHING_ROD, ChatColor.GOLD + "Fishing Rod"));
        e.setChestplate(new ItemStack(Material.DIAMOND_LEGGINGS));
        e.setHelmet(new ItemStack(Material.GOLDEN_HELMET));
        e.addItem(newItem(Material.PUFFERFISH, ChatColor.YELLOW + "Pufferfish Bomb"));
        e.addItem(newItem(Material.COD, ChatColor.LIGHT_PURPLE + "Cod Sniper"));
        giveWool();
        giveWool();
    }
    int timer = 0;
    int cod = 0;

    ArrayList<Location> locs = new ArrayList<Location>();

    public void codSniper() {
        Entity e = p.getWorld().spawnEntity(p.getEyeLocation(), EntityType.COD);
        Vector m = p.getLocation().getDirection().multiply(3);

        new BukkitRunnable() {
            @Override
            public void run() {
                p.getWorld().playSound(e.getLocation(), Sound.BLOCK_PACKED_MUD_FALL, 3f, 3f);
                e.setVelocity(m);
                Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(255, 0, 0), 1.0F);
                locs.add(e.getLocation());
                for(Location loc : locs)
                    p.getWorld().spawnParticle(Particle.REDSTONE, loc, 10, 0.35,0.2,0.35,0, dustOptions);
                cod++;
                if(cod >= 20*4) {
                    this.cancel();
                    cod = 0;
                    locs.clear();
                    return;
                }
                for(Entity e : e.getNearbyEntities(0.3, 0.3, 0.3)) {
                    if(e instanceof Player) {
                        Player p1 = (Player) e;
                        if(p.getUniqueId() == p1.getUniqueId())
                            continue;
                        if(p1.getHealth() - 16f >= 0)
                            p1.setHealth(p1.getHealth() - 16f);
                        else
                            p1.setHealth(0);
                        p1.getWorld().playSound(p1.getLocation(), Sound.BLOCK_NOTE_BLOCK_COW_BELL, 5f, 5f);
                        p1.getWorld().playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_COW_BELL, 5f, 5f);
                        e.remove();
                    }
                }
            }
        }.runTaskTimer(main, 0L, 1L);
    }


    public void pufferfishBomb() {
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
                    return;
                }
                double y = thrownEggItem.getVelocity().getY();
                main.broadcast("Y Velo: " + y);
                if(y == 0) {
                    this.cancel();
                    for(int i = 0; i < 10; i++)
                        p.getWorld().spawnEntity(thrownEggItem.getLocation(), EntityType.PUFFERFISH);
                    thrownEggItem.remove();
                }

            }
        }.runTaskTimer(main, 0L, 5L);
    }
}
