package org.macl.ctc.events;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.macl.ctc.Main;
import org.macl.ctc.kits.Demolitionist;
import org.macl.ctc.kits.Kit;
import org.macl.ctc.kits.Spy;

public class Players extends DefaultListener {
    public Players(Main main) {
        super(main);
    }

    @EventHandler
    public void playerJoin(AsyncPlayerPreLoginEvent event) {
        if(world.isUnloading)
            event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
    }

    @EventHandler
    public void playerJoinReal(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        p.teleport(Bukkit.getWorld("world").getSpawnLocation());
    }

    @EventHandler
    public void playerQuit(PlayerQuitEvent event) {
        // Handle player leaving mid game (rebalance teams)
        Player p = event.getPlayer();
        game.stack(p);
    }

    @EventHandler
    public void launch(ProjectileLaunchEvent event) {
        Projectile proj = event.getEntity();
        if(proj instanceof EnderPearl && proj.getShooter() instanceof Player) {
            EnderPearl e = (EnderPearl) proj;
            e.setPassenger((Player)proj.getShooter());
        }
    }

    @EventHandler
    public void land(PlayerTeleportEvent event) {
        if(event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL)
            event.setCancelled(true);
    }

    @EventHandler
    public void fish(PlayerFishEvent event) {
        event.getHook().setVelocity(event.getHook().getVelocity().multiply(1.7));
        if(event.getCaught() instanceof Player) {
            Player c = (Player) event.getCaught();
            Player p = event.getPlayer();
            Vector velo = p.getLocation().getDirection().multiply(-3f);
            double y = p.getLocation().distance(c.getLocation());
            y*=0.14;
            velo.setY(Math.min(y, 1.8));
            velo.setX(velo.getX()*0.3);
            velo.setZ(velo.getZ()*0.3);
            c.setVelocity(velo);
        }
    }

    @EventHandler
    public void entityDamage(EntityDamageEvent event) {
        if(event.getEntity() instanceof Player) {
            Player p = (Player) event.getEntity();
            if(event.getCause() == EntityDamageEvent.DamageCause.VOID)
                p.setHealth(0);
            if(event.getCause() == EntityDamageEvent.DamageCause.LIGHTNING)
                event.setCancelled(true);
            if(main.getKits().get(p.getUniqueId()) != null && main.getKits().get(p.getUniqueId()) instanceof Spy) {
                main.broadcast("spy damaged");
                ((Spy) main.getKits().get(p.getUniqueId())).uninvis();
            }


        }
    }

    public void onEggThrow(PlayerEggThrowEvent e) {
        e.setHatching(false);
    }


        @EventHandler
    public void itemBreak(PlayerItemBreakEvent event) {
        event.getBrokenItem().setDurability((short)3);
    }

    @EventHandler
    public void onEntityHit(EntityDamageByEntityEvent event) {
        if(event.getEntity() instanceof Player) {
            Player p = (Player) event.getEntity();
            if(event.getDamager() instanceof Player) {
                Player attacker = (Player) event.getDamager();
                if(attacker.getInventory().getItemInMainHand().getType() == Material.DIAMOND_PICKAXE)
                    event.setDamage(0.5);
                if(main.getKits().get(attacker.getUniqueId()) != null && main.getKits().get(attacker.getUniqueId()) instanceof Spy && attacker.getInventory().getItemInMainHand().getType() == Material.IRON_HOE) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20*2, 0));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 20 * 3, 2));
                }
            }
            if(event.getDamager() instanceof Arrow)
                event.setDamage(2.25);
            if (event.getDamager() instanceof Snowball)
                event.setDamage(1.25);
            if(event.getDamager() instanceof Egg)
                p.getWorld().createExplosion(event.getEntity().getLocation(), 1.54f, false);
        }
    }

    @EventHandler
    public void pickup(EntityPickupItemEvent event) {
        if(main.game.started)
            event.setCancelled(true);
    }

    @EventHandler
    public void portal(PlayerMoveEvent event) {
        Player p = event.getPlayer();
        if(event.getTo().getBlock().getType() == Material.NETHER_PORTAL)
            game.stack(p);
    }

    @EventHandler
    public void food(FoodLevelChangeEvent event) {
        event.setFoodLevel(20);
    }

    @EventHandler
    public void death(PlayerDeathEvent event) {
        event.setDeathMessage(main.prefix + event.getDeathMessage());
        event.getDrops().clear();
        kit.remove(event.getEntity());
    }

    @EventHandler
    public void respawn(PlayerRespawnEvent event) {
        Player p = event.getPlayer();
        p.teleport(p.getWorld().getSpawnLocation());
        game.respawn(p);
    }


}
