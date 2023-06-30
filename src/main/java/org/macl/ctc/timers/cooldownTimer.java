package org.macl.ctc.timers;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.macl.ctc.kits.Kit;
import org.macl.ctc.kits.Snowballer;

public class cooldownTimer extends BukkitRunnable {
    Kit k;
    public int timer;
    String name;
    ItemStack item;
    short maxDur;
    int initialCount = 0;


    public cooldownTimer(Kit k, int timer, String name, ItemStack item) {
        this.k = k;
        this.timer = timer;
        this.name = name;
        this.item = item;
        this.maxDur = item.getType().getMaxDurability();
        k.cooldowns.put(name, true);
        ItemMeta itemMeta = item.getItemMeta();
        Damageable damage = (Damageable) itemMeta;
        if (itemMeta instanceof Damageable){
            damage.setDamage(maxDur-2);
        }
        this.initialCount = timer;

    }

    public void run() {
        Player p = k.getPlayer();
        if(p == null || k.main.kit.kits.get(p.getUniqueId()) == null) {
            this.cancel();
            return;
        }


        //Ratio = Durability / Initial Count (100/10) = 10 damages per iteration
        int currentCount = initialCount - timer;
        int minDmg = 5; // Minimum value for dmg
        int dmg = (int) Math.ceil((1 - ((double) currentCount / initialCount)) * (maxDur - minDmg)) + minDmg;
        dmg = Math.max(dmg, minDmg);
        dmg = Math.min(dmg, maxDur);

        int slot = p.getInventory().first(item.getType());
        ItemStack stack = p.getInventory().getItem(slot);
        if(stack != null) {
            ItemMeta itemMeta = stack.getItemMeta();
            Damageable damage = (Damageable) itemMeta;
            if(stack.getType() != Material.NETHERITE_SHOVEL ) {
                if (itemMeta instanceof Damageable){
                    damage.setDamage(dmg);
                }
                stack.setItemMeta(itemMeta);
            }
        }


        if(timer == 0) {
            k.cooldowns.put(name, false);
            p.getInventory().remove(item.getType());
            ItemMeta meta = item.getItemMeta();
            if(meta instanceof Damageable) {
                Damageable damage = (Damageable) meta;
                damage.setDamage(0);
            }

            item.setItemMeta(meta);

            p.getInventory().setItem(slot, item);
            this.cancel();
            return;
        }
        if(timer < 40) {
            float playExpValue = 1.0f - ((float) timer / 80.0f);
            k.playExp(playExpValue);
        }
        timer--;
    }
}

