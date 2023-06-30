package org.macl.ctc.kits;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.macl.ctc.Main;
import org.macl.ctc.timers.cooldownTimer;

public class Snowballer extends Kit {

    public ItemStack rocketJump = newItem(Material.GOLDEN_HOE, ChatColor.YELLOW + "Rocket Jump");

    public Snowballer(Main main, Player p, KitType type) {
        super(main, p, type);
        cooldowns.put("rocketCool", false);
        e.addItem(newItem(Material.WOODEN_SWORD, ChatColor.DARK_BLUE + "Snowball Launcher"));
        e.addItem(rocketJump);
        e.setBoots(newItemEnchanted(Material.DIAMOND_BOOTS, "Feather Boots", Enchantment.PROTECTION_FALL, 7));
        giveWool();
        giveWool();
    }
    public void shootSnowball() {
        p.launchProjectile(Snowball.class);
    }

    public class rocketTrail extends BukkitRunnable {
        public float lastFall = 0.1f;
        double r = 1;
        double m = 0;
        double t = 0;
        public void run() {
            Location l = p.getLocation();

            if(p.getFallDistance() == 0)
                for(int i = 0; i < 4; i++) {
                    m = m + Math.PI/32;
                    double x = r*Math.cos(m);
                    double y = r;
                    double z = r*Math.sin(m);
                    l.add(x,y,z);

                    p.getWorld().spawnParticle(Particle.WATER_BUBBLE,l,1);

                    l.subtract(x,y,z);
                }

            lastFall = p.getFallDistance();
            t++;
            Location loc = p.getLocation();
            loc.add(-0.3, -0.5, 0);

            p.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,l,1);

            if(t == 20*2)
                this.cancel();
        }
    }

    public void launch() {
        if(!cooldowns.get("rocketCool")) {
            p.setVelocity(p.getLocation().getDirection().multiply(2.3f));
            new rocketTrail().runTaskTimer(main, 0L, 1L);
            new cooldownTimer(this, 15*20, "rocketCool", rocketJump).runTaskTimer(main, 0L, 1L);
            p.playSound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1f);
        }
    }



    // CLEAN AND TURN INTO A KIT METHOD!

}
