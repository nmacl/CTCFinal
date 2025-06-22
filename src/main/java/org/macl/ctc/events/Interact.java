package org.macl.ctc.events;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
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
                break;
            case RIGHT_CLICK_BLOCK:
                rightClick(event);
                break;
            case RIGHT_CLICK_AIR:
                rightClick(event);
                break;
            case LEFT_CLICK_BLOCK:
                leftClick(event);
                break;
            case LEFT_CLICK_AIR:
                leftClick(event);
                break;
            default:
                break;

        }
    }

    @EventHandler
    public void interact(PlayerInteractEntityEvent event) {
        Entity e = event.getRightClicked();
        Player p = event.getPlayer();

//        main.broadcast("I am a pear");

        if (e instanceof Player pe) {
            if (kit.kits.get(pe.getUniqueId()) != null) {

                if (!(p.getInventory().getItemInMainHand().getType() == Material.AIR)) return;

                Kit k = kit.kits.get(pe.getUniqueId());
//                main.broadcast("I have acquired a pear");
                if (k instanceof Lumberjack) {

                    if (main.game.sameTeam(
                            p.getUniqueId(),
                            pe.getUniqueId())) {
//                        main.broadcast("I am looking at the pear passengers");
                        if (pe.getPassengers().isEmpty()) {
                            pe.addPassenger(p);
//                            main.broadcast("I am adding a person to passengers");
//
                        }
                    }
                }
            }
        }
    }


    private void leftClick(PlayerInteractEvent event) {

    }

    boolean second = false;

    private void rightClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        // 2) only care about right‐click
        Action a = event.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) return;

        Player p = event.getPlayer();
        Material m = event.getPlayer().getInventory().getItemInMainHand().getType();
        if (kit.kits.get(p.getUniqueId()) != null) {
            Kit k = kit.kits.get(p.getUniqueId());
            if(k instanceof Grandpa) {
                Grandpa ga = (Grandpa) k;
                if(m == Material.PRISMARINE_SHARD)
                    ga.shootGun();
                else if(m == Material.HONEY_BOTTLE)
                    ga.drinkBooze();
                else if(m == Material.PRISMARINE_CRYSTALS)
                    ga.shootPepper();
            }
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
                if(m == Material.EGG) {
                    if(kit.kits.get(p.getUniqueId()).isOnCooldown("egg")) {
                        event.setCancelled(true);
                    } else {
                        kit.kits.get(p.getUniqueId()).setCooldown("egg", 2, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
                    }
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
                if(m == Material.SLIME_BALL)
                    r.platform();
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

            if(k instanceof Engineer) {
                Engineer e = (Engineer) k;
                if(m == Material.IRON_SHOVEL)
                    e.overload();
            }

            if(k instanceof Artificer) {
                Artificer art = (Artificer) k;
                if (m == Material.RIB_ARMOR_TRIM_SMITHING_TEMPLATE) {
                    art.useFlamethrower();
                    event.setCancelled(true);
                }
                else if (m == Material.TIPPED_ARROW) {
                    art.useFrostDagger();
                    event.setCancelled(true);
                }
                else if (m == Material.FLINT) {
                    art.useVoidBomb();
                    event.setCancelled(true);
                }
                else if (m == Material.FEATHER) {
                    art.useUpdraft();
                    event.setCancelled(true);
                }
            }

            if (k instanceof Lumberjack jack) {
                if (m == Material.GOLDEN_AXE) {
                    jack.sawBlocks();
                    event.setCancelled(true);

                }
                if (m == Material.OAK_LOG) {
                    jack.chuckLog();
                    event.setCancelled(true);
                }
                if (m == Material.HONEYCOMB) {
                    jack.useMysticSap();
                    event.setCancelled(true);
                }
            }



        }
    }

    @EventHandler
    public void itemSwitch(PlayerItemHeldEvent event) {
        Player p = event.getPlayer();
        if(kit.kits.get(p.getUniqueId()) != null && kit.kits.get(p.getUniqueId()) instanceof Archer) {
            Archer a = (Archer) kit.kits.get(p.getUniqueId());
            a.handleItemSwitch(event);
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
                        p.getLocation().getWorld().createExplosion(event.getClickedBlock().getLocation(), 5f, true);
                        main.fakeExplode(p, l, 24, 8, true, true);
                    }
                }.runTaskLater(main, 20*2L);
            }
        }
    }

}
