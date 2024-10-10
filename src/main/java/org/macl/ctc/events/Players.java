package org.macl.ctc.events;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.macl.ctc.Main;
import org.macl.ctc.kits.*;

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
        game.resetPlayer(p, false);
    }

    @EventHandler
    public void playerQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        main.broadcast("quit event " + game.resetPlayer(p, true));
    }

    @EventHandler
    public void launch(ProjectileLaunchEvent event) {
        Projectile proj = event.getEntity();
        if(proj instanceof EnderPearl && proj.getShooter() instanceof Player) {
            EnderPearl e = (EnderPearl) proj;
            e.setPassenger((Player)proj.getShooter());
        }
        if(proj instanceof Arrow && proj.getShooter() instanceof Player) {
            Player p = (Player) proj.getShooter();
            if(kit.kits.get(p.getUniqueId()) instanceof Archer) {
                Archer a = (Archer) kit.kits.get(p.getUniqueId());
                a.shoot(event);
            }
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
            if(main.getKits().get(p.getUniqueId()) != null && main.getKits().get(p.getUniqueId()) instanceof Spy) {
                ((Spy) main.getKits().get(p.getUniqueId())).uninvis();
            }
        }
    }

    @EventHandler
    public void projectileHit(ProjectileHitEvent event) {
        if(event.getEntity() instanceof Arrow && event.getEntity().getShooter() instanceof Player) {
            Player p = (Player) event.getEntity().getShooter();
            if(kit.kits.get(p.getUniqueId()) instanceof Archer) {
                Archer a = (Archer) kit.kits.get(p.getUniqueId());
                a.bHit(event);
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
        if(event.getDamager() instanceof Arrow) {
            Arrow arrow = (Arrow) event.getDamager();
            event.setDamage(2.25);
        }
        if (event.getDamager() instanceof Snowball)
            event.setDamage(1.25);
        if(event.getDamager() instanceof Egg)
            event.getEntity().getWorld().createExplosion(event.getEntity().getLocation(), 2f, false, true);

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
                if(event.getDamager() instanceof Egg) {
                    if(p.getHealth() - 6 < 0) {
                        p.setHealth(0);
                    } else {
                        p.setHealth(p.getHealth() - 6);
                    }
                }
            }
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
        if(!main.game.started)
            return;
        double strength = 1.5;
        if(event.getTo().getWorld() != world.getRed().getWorld())
            return;
        if(game.redHas(p) && (event.getTo().distance(world.getBlue()) < 30)) {
            p.sendMessage("away");
            Vector dir = world.getBlue().toVector().subtract(p.getLocation().toVector()).normalize();
            dir.multiply(-1);
            p.setVelocity(dir.multiply(strength));
        }
        if(game.blueHas(p) && (event.getTo().distance((world.getRed())) < 30)) {
            p.sendMessage("away");
            Vector dir = world.getRed().toVector().subtract(p.getLocation().toVector()).normalize();
            dir.multiply(-1);
            p.setVelocity(dir.multiply(strength));
        }
    }

    @EventHandler
    public void food(FoodLevelChangeEvent event) {
        event.setFoodLevel(20);
    }

    @EventHandler
    public void death(PlayerDeathEvent event) {
        event.setDeathMessage(main.prefix + event.getDeathMessage());
        event.getDrops().clear();
        if(kit.kits.get(event.getEntity().getUniqueId()) != null) {
            kit.kits.get(event.getEntity().getUniqueId()).cancelAllCooldowns();
            kit.kits.get(event.getEntity().getUniqueId()).cancelAllRegen();
        }
        kit.remove(event.getEntity());
    }

    @EventHandler
    public void respawn(PlayerRespawnEvent event) {
        Player p = event.getPlayer();
        p.teleport(p.getWorld().getSpawnLocation());
        game.respawn(p);
        AttributeInstance playerAttribute = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        playerAttribute.setBaseValue(20);
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if(event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION)
                event.setDamage(0);
            if(event.getCause() == EntityDamageEvent.DamageCause.LIGHTNING)
                event.setDamage(3);
            // Check if the player is wearing any armor
            if (isWearingArmor(player)) {
                // Calculate damage as if no armor is worn
                event.setDamage(EntityDamageEvent.DamageModifier.ARMOR, 0);
            }
        }
    }

    private boolean isWearingArmor(Player player) {
        return player.getInventory().getArmorContents() != null &&
                (player.getInventory().getHelmet() != null ||
                        player.getInventory().getChestplate() != null ||
                        player.getInventory().getLeggings() != null ||
                        player.getInventory().getBoots() != null);
    }


}
