package org.macl.ctc.kits;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.macl.ctc.Main;

public class Spy extends Kit {

    Location detonate;

    boolean invis = true;

    public Location getDetonate() {
        return detonate;
    }

    public void setDetonate(Location detonate) {
        this.detonate = detonate;
    }

    public void addDetonate() {
        if(!(main.getKits().get(p.getUniqueId()) != null && main.getKits().get(p.getUniqueId()) instanceof Spy))
            return;
        p.getInventory().setItem(2, newItem(Material.BLAZE_ROD, ChatColor.DARK_GREEN + "Detonator"));
        int rod = p.getInventory().first(Material.BLAZE_ROD);
        p.getInventory().getItem(rod).addUnsafeEnchantment(Enchantment.FIRE_ASPECT, 1);
        setHearts(12);
    }

    public Spy(Main main, Player p, KitType type) {
        super(main, p, type);
        p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 99999999, 99999));
        p.getInventory().addItem(newItem(Material.IRON_HOE, ChatColor.GREEN + "Poison Dagger"));
        p.getInventory().addItem(newItem(Material.ENDER_PEARL, ChatColor.DARK_PURPLE + "Cloak (Teleport)", 3));
        //p.getInventory().addItem(newItem(Material.BLAZE_ROD, ChatColor.DARK_GREEN + "Detonator"));
        p.getInventory().addItem(newItem(Material.RED_CANDLE, ChatColor.DARK_RED + "Remote Explosive", 1));
        giveWool();
        giveWool();
        regenItem("cloak", newItem(Material.ENDER_PEARL, ChatColor.DARK_PURPLE + "Cloak (Teleport)"), 35, 3, 1);
        BukkitTask inv = new spyInvis().runTaskTimer(main, 0L, 1L);
        this.registerTask(inv);
        p.getInventory().remove(Material.DIAMOND_PICKAXE);
        setHearts(14);
    }

    public void detonate() {
        if(detonate != null) {
            p.getInventory().remove(Material.BLAZE_ROD);
            BukkitTask detonateTimer = new detonateTimer().runTaskTimer(main, 0, 1L);
            registerTask(detonateTimer);
        }
    }


    public void uninvis() {
        this.invis = false;
    }

    public class spyInvis extends BukkitRunnable {
        int ticks = 0;

        public void run() {
            if(!p.isOnline() || p.isDead() || (main.getKits().get(p.getUniqueId()) == null || !(main.getKits().get(p.getUniqueId()) instanceof Spy))) {
                this.cancel();
                return;
            }
            if(invis && ticks >= 60) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 999999, 99999));
            } else if(!invis){
                p.removePotionEffect(PotionEffectType.INVISIBILITY);
                ticks = 0;
                invis = true;
            }

            ticks++;
        }
    }

    public class detonateTimer extends BukkitRunnable {
        int timer = 0;

        public void run() {
            if(detonate == null) {
                this.cancel();
                return;
            }
            if(timer == 35) {
                main.fakeExplode(p, getDetonate(), 18, 6, true, false,true, "spy");
                p.getWorld().createExplosion(getDetonate(), 2f, false, true);
                this.cancel();
                detonate = null;

                BukkitTask add = new BukkitRunnable() {

                    int count = 15;
                    public void run() {
                        count--;
                        p.setLevel(count);
                        if(count == 0) {
                            this.cancel();
                            p.getInventory().addItem(newItem(Material.RED_CANDLE, ChatColor.DARK_RED + "Remote Explosive", 1));
                        }
                    }
                }.runTaskTimer(main, 0L, 20L);
                registerTask(add);
                return;
            }
            detonate.getWorld().playSound(detonate, Sound.BLOCK_NOTE_BLOCK_HAT, 1f, (float) (0.1*timer));
            timer++;
        }
    }
}