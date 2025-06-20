package org.macl.ctc.events;

import net.minecraft.world.item.alchemy.Potion;
import org.bukkit.*;
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
import org.bukkit.projectiles.ProjectileSource;
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
        p.sendTitle(
                ChatColor.BOLD + "Go into the Nether portal",       // title
                "to start the game",               // subtitle
                10,    // fadeIn ticks
                70,    // stay ticks
                20     // fadeOut ticks
        );

        // then a chat message with the Discord link
        p.sendMessage(ChatColor.AQUA + "Join our Discord: https://discord.gg/Qeme8MUXBY");
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
        if(event.getDamager() instanceof SpectralArrow) {
            SpectralArrow arrow = (SpectralArrow) event.getDamager();
            event.setDamage(2.25);
            if(arrow.getShooter() instanceof Player) {
                Player shooter = (Player) arrow.getShooter();
                main.broadcast(shooter.getName());
                if(kit.kits.get(shooter.getUniqueId()) instanceof Artificer && event.getEntity() instanceof Player) {
                    Player shot = (Player) event.getEntity();
                    Kit shooterkit = kit.kits.get(shooter.getUniqueId());
                    if (shooterkit instanceof Artificer) ((Artificer) shooterkit).addVoidFragments(4);
                    main.broadcast(shot.getName());
                    shot.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 12));
                    shot.sendTitle(
                            ChatColor.AQUA + "***** YOU'VE BEEN FROZEN! *****",
                            "",
                            5, 25, 5
                    );
                }
            }
        }

        if (event.getDamager() instanceof SmallFireball) {
            event.setCancelled(true);
            Entity e = event.getEntity();
            if (e instanceof Player) {
                ((Player) e).damage(6.0);
            }
        }

        if (event.getDamager() instanceof ShulkerBullet) {
            event.setCancelled(true);
            Entity e = event.getEntity();
            if (e instanceof Player) {
                ((Player) e).damage(0.4);
                ((Player) e).setNoDamageTicks(0);
                ((Player) e).addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20 * 6, 1));
            }
        }
        if (event.getDamager() instanceof Snowball)
            event.setDamage(1.2);

        if (event.getDamager() instanceof Egg) {
            event.setCancelled(true);  // suppress vanilla egg “knockback damage”
            Egg egg = (Egg) event.getDamager();
            ProjectileSource shooterSrc = egg.getShooter();

            // If both shooter and victim are players, apply team check
            if (event.getEntity() instanceof Player && shooterSrc instanceof Player) {
                Player victim = (Player) event.getEntity();
                Player shooter = (Player) shooterSrc;

                boolean sameTeam = main.game.sameTeam(
                        victim.getUniqueId(),
                        shooter.getUniqueId()
                );

                if (sameTeam) {
                    // same-team → just explosion effect
                    victim.getWorld().createExplosion(victim.getLocation(), 2f, false, true);
                } else {
                    // enemy → subtract 7 HP, kill if ≤ 0
                    double newHealth = victim.getHealth() - 8.5;
                    if (newHealth > 0) {
                        victim.setHealth(newHealth);
                    } else {
                        victim.setHealth(0.0);  // this will trigger a death
                    }
                    victim.getWorld().createExplosion(victim.getLocation(), 2f, false, true);

                }
            } else {
                // non-player victim or shooter → explosion only
                event.getEntity()
                        .getWorld()
                        .createExplosion(
                                event.getEntity().getLocation(),
                                2f,
                                false,  // no fire
                                true    // break blocks
                        );
            }
            return;
        }
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
        PotionEffect slowness = p.getPotionEffect(PotionEffectType.SLOW);
        // so hacky but we ball
        // getPotionEffect returns null if they don't have that effect
        if (slowness != null && slowness.getAmplifier() > 10) {
            // amplifier is zero‐based (0 = Slowness I, 1 = Slowness II, …)
            event.setCancelled(true);
        }
        if(!main.game.started)
            return;
        double strength = 1.5;
        if(event.getTo().getWorld() != world.getRed().getWorld())
            return;
        if(game.redHas(p) && (event.getTo().distance(world.getBlue()) < 10)) {
            p.sendMessage("away");
            Vector dir = world.getBlue().toVector().subtract(p.getLocation().toVector()).normalize();
            dir.multiply(-1);
            p.setVelocity(dir.multiply(strength));
        }
        if(game.blueHas(p) && (event.getTo().distance((world.getRed())) < 10)) {
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

    @EventHandler(ignoreCancelled = true)
    public void onItemDurabilityLoss(PlayerItemDamageEvent event) {
        // prevent *any* item from losing durability
        event.setCancelled(true);
    }

    @EventHandler
    public void death(PlayerDeathEvent event) {
        event.setDeathMessage(main.prefix + event.getDeathMessage());
        event.getDrops().clear();
        if(kit.kits.get(event.getEntity().getUniqueId()) != null) {
            Kit k = kit.kits.get(event.getEntity().getUniqueId());
            k.cancelAllRegen();
            k.cancelAllCooldowns();
            k.cancelAllTasks();
        }
        kit.remove(event.getEntity());
    }

    @EventHandler
    public void respawn(PlayerRespawnEvent event) {
        Player p = event.getPlayer();
        game.respawn(event.getPlayer());   // nothing else
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
            if (isWearingArmor(player)
                    && event.isApplicable(EntityDamageEvent.DamageModifier.ARMOR)) {

                event.setDamage(EntityDamageEvent.DamageModifier.ARMOR, 0);
            }
            if(event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                if(kit.kits.get(event.getEntity().getUniqueId()) instanceof Grandpa) {
                    Grandpa g = (Grandpa) kit.kits.get(event.getEntity().getUniqueId());
                    if(g.fallImmune) {
                        g.fallImmune = false;
                        event.setDamage(0);
                    }
                }
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
