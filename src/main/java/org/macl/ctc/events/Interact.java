package org.macl.ctc.events;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.macl.ctc.Main;
import org.macl.ctc.kits.*;

public class Interact extends DefaultListener {
    public Interact(Main main) {
        super(main);
    }

    @EventHandler
    public void interact(PlayerInteractEvent event) {
        switch(event.getAction()) {
            case PHYSICAL:
                physical(event);
            case RIGHT_CLICK_BLOCK:
                rightClick(event);
            case RIGHT_CLICK_AIR:
                rightClick(event);
            case LEFT_CLICK_BLOCK:
                leftClick(event);
            case LEFT_CLICK_AIR:
                leftClick(event);
            default:
                break;

        }
    }

    private void leftClick(PlayerInteractEvent event) {

    }

    boolean second = false;


    private void rightClick(PlayerInteractEvent event) {
        if(event.getHand() == EquipmentSlot.OFF_HAND) return;
        if(second == false && event.getAction() == Action.RIGHT_CLICK_BLOCK)
            second = true;
        else if(second && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            second = false;
            return;
        }

        Player p = event.getPlayer();
        Material m = event.getPlayer().getInventory().getItemInMainHand().getType();
        if (kit.kits.get(p.getUniqueId()) != null) {
            Kit k = kit.kits.get(p.getUniqueId());
            if (k instanceof Snowballer) {
                Snowballer snowball = (Snowballer) k;
                if (m == Material.GOLDEN_HOE)
                    snowball.launch();
                else if (m == Material.WOODEN_SWORD)
                    snowball.shootSnowball();
            }
            if(k instanceof Grandma) {
                Grandma g = (Grandma) k;
                if(m == Material.COOKIE)
                    g.heart();
            }
            if(k instanceof Spy) {
                Spy s = (Spy) k;
                if(m == Material.BLAZE_ROD)
                    s.detonate();
            }
            if(k instanceof Demolitionist) {
                Demolitionist d = (Demolitionist) k;
                if(m == Material.CARROT_ON_A_STICK)
                    d.launchSheep();
                if(m == Material.EGG)
                    if(d.cooldowns.get("egg")) {
                        event.setCancelled(true);
                    } else {
                        d.cooldowns.put("egg", true);
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                d.cooldowns.put("egg", false);
                            }
                        }.runTaskLater(main, 6*20L);
                    }
            }
            if(k instanceof Builder) {
                Builder b = (Builder) k;
                if(m == Material.DIAMOND_SHOVEL)
                    b.openMenu();
            }
            if(k instanceof Runner) {
                Runner r = (Runner) k;
                if(m == Material.STONE_SWORD)
                    r.blockRun();
                if(m == Material.CLOCK)
                    r.polarField();
            }
            if(k instanceof Tank) {
                Tank t = (Tank) k;
                if(m == Material.NETHERITE_SHOVEL)
                    t.gatling(event.getBlockFace());
                if(m == Material.FLINT)
                    t.exit();
                if(m == Material.FLINT_AND_STEEL) {
                    t.hellfire();
                    event.setCancelled(true);
                }
            }
            if(k instanceof Fisherman) {
                Fisherman f = (Fisherman) k;
                if(m == Material.PUFFERFISH)
                    f.pufferfishBomb();
                if(m == Material.COD)
                    f.codSniper();
            }
            if (k instanceof Grandpa) {
                Grandpa gr = (Grandpa) k;
                /*if (m == Material.PRISMARINE_SHARD)
                    gr.shootGun();
                if (m == Material.HONEY_BOTTLE)
                    gr.drinkBooze(); */
            }
        }

    }

    public void physical(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        Location l = p.getLocation();

        if(event.getClickedBlock().getType() != null) {
            Material m = event.getClickedBlock().getType();
            if(m == Material.LIGHT_WEIGHTED_PRESSURE_PLATE && game.started == false) {
                double x, y, z;
                x = l.getDirection().getX();
                y = l.getDirection().getY();
                z = l.getDirection().getZ();
                x *= 4;
                y = Math.abs(y);
                y *= 8;
                z *= 4;

                p.setVelocity(new Vector(x,y,z));
            }

            if(m == Material.STONE_PRESSURE_PLATE) {
                event.getClickedBlock().setType(Material.HEAVY_WEIGHTED_PRESSURE_PLATE);
                event.setCancelled(true);
                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BLAZE_HURT, 10f, 3f);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        event.getClickedBlock().getLocation().getWorld().getBlockAt(event.getClickedBlock().getLocation()).setType(Material.AIR);
                        p.getLocation().getWorld().createExplosion(event.getClickedBlock().getLocation(), 5f);
                    }
                }.runTaskLater(main, 20*2L);
            }
        }
    }

}
