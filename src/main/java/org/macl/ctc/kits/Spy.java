package org.macl.ctc.kits;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
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
        new regenPearl().runTaskTimer(main, 0L, 20L);
        new spyInvis().runTaskTimer(main, 0L, 1L);
        p.getInventory().remove(Material.DIAMOND_PICKAXE);
    }

    public void detonate() {
        if(detonate != null) {
            p.getInventory().remove(Material.BLAZE_ROD);
            new detonateTimer().runTaskTimer(main, 0, 1L);
        }
    }


    public void uninvis() {
        this.invis = false;
    }

    public class spyInvis extends BukkitRunnable {
        int ticks = 0;

        public void run() {
            if(p.isDead()) {
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
                detonate.getWorld().createExplosion(detonate, 4f, true);
                this.cancel();
                detonate = null;
                new BukkitRunnable() {

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
                return;
            }
            detonate.getWorld().playSound(detonate, Sound.BLOCK_NOTE_BLOCK_HAT, 1f, (float) (0.1*timer));
            timer++;
        }

    }

    public class regenPearl extends BukkitRunnable {
        int timer = 0;

        public void run() {
            timer++;
            if(p.isDead() || main.getKits().get(p.getUniqueId()) != null && main.getKits().get(p.getUniqueId()) instanceof Spy) {
                this.cancel();
                return;
            }
            if(timer == 45) {
                int pearl = p.getInventory().first(Material.ENDER_PEARL);
                if(p.getInventory().getItem(pearl).getAmount() == 3)
                    return;

                p.getInventory().setItem(pearl, newItem(Material.ENDER_PEARL, ChatColor.DARK_PURPLE + "Cloak (Teleport)", p.getInventory().getItem(pearl).getAmount()+1));
            }

        }
    }



}