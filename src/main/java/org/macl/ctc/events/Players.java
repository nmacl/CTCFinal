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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.macl.ctc.Main;
import org.macl.ctc.kits.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
        if(main.game.started)
            game.addTeam(p);

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
            if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
                event.setDamage(p.getHealth());
            }
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

    // Maps victim UUID → the last time they were damaged by a player (with weapon info)
    public final Map<UUID, DamageContext> lastDamager = new ConcurrentHashMap<>();

    // Holds the killer and what weapon they used
    public record DamageContext(Player killer, WeaponType type, long time) {}
    public enum WeaponType { SWORD, BOW, FISHING_ROD, FIREBALL, PEPPER, OTHER }
    public void tagLastDamager(Player victim, Player killer) {
        lastDamager.put(
                victim.getUniqueId(),
                new DamageContext(killer, WeaponType.OTHER, System.currentTimeMillis())
        );
    }

    @EventHandler
    public void onEntityHit(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player victim) {
            Player killer = null;
            WeaponType wt = WeaponType.OTHER;
            Entity damager = event.getDamager();

            // direct melee
            if (damager instanceof Player dk) {
                killer = dk;
                Material mat = dk.getInventory().getItemInMainHand().getType();
                if (mat.name().endsWith("_SWORD"))      wt = WeaponType.SWORD;
                else if (mat == Material.FISHING_ROD)    wt = WeaponType.FISHING_ROD;
                else                                     wt = WeaponType.OTHER;
            }
            // arrow shot
            else if (damager instanceof Arrow arr && arr.getShooter() instanceof Player dk) {
                killer = dk;
                wt = WeaponType.BOW;
            }

            if (killer != null && !killer.getUniqueId().equals(victim.getUniqueId())) {
                lastDamager.put(victim.getUniqueId(),
                        new DamageContext(killer, wt, System.currentTimeMillis()));
            }
        }
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
        if (event.getDamager() instanceof Player p) {
            // on Cane (stick) hit
//            main.broadcast("registered damaged");
            if(main.getKits().get(p.getUniqueId()) != null && main.getKits().get(p.getUniqueId()) instanceof Grandma g && p.getInventory().getItemInMainHand().getType() == Material.STICK) {
                g.onCaneHit();
//                main.broadcast("grandma hit with cane");
            }
            //on Classic Cane (blaze rod) hit
            else if(main.getKits().get(p.getUniqueId()) != null && main.getKits().get(p.getUniqueId()) instanceof Grandma g && p.getInventory().getItemInMainHand().getType() == Material.BLAZE_ROD) {
                g.onClassicCaneHit();

//                 new BukkitRunnable() {
//
//                     public void run() {
//                         Vector velocity = event.getEntity().getVelocity();
//                         Vector upVec = new Vector(0.0f,0.5f,0.0f);
//                         event.getEntity().setVelocity(velocity.add(upVec));}
//
//                 }.runTaskLater(main,1L);



//                main.broadcast("grandma hit with classic cane");
            }
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
        // Removed teleport - let players hang out after game ends
        // if(!main.game.started && p.getWorld().getName() == "map")
        //     p.teleport(Bukkit.getWorld("world").getSpawnLocation());
        if(event.getTo().getBlock().getType() == Material.NETHER_PORTAL)
            game.stack(p);
        PotionEffect slowness = p.getPotionEffect(PotionEffectType.SLOW);
        // so hacky but we ball
        // getPotionEffect returns null if they don't have that effect
        if (slowness != null && slowness.getAmplifier() > 10) {
            // amplifier is zero‐based (0 = Slowness I, 1 = Slowness II, …)
            event.getPlayer().setFreezeTicks(10);
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
        Player victim = event.getEntity();

        // 1) Record the death
        main.getStats().recordDeath(victim);

        // 2) Lookup & clear the last damager if within the last 10s
        DamageContext ctx = lastDamager.remove(victim.getUniqueId());
        Player killer = null;
        WeaponType wt = null;
        if (ctx != null && System.currentTimeMillis() - ctx.time() <= 10_000) {
            killer = ctx.killer();
            wt    = ctx.type();
            // 3) Record the kill
            main.getStats().recordKill(killer);
        }

        // 4) Build a custom message
        String msg;
        EntityDamageEvent causeEvt = victim.getLastDamageCause();

        if (killer != null) {
            // PvP kill
            switch (wt) {
                case SWORD       -> msg = killer.getName() + " viciously slashed "   + victim.getName() + "!";
                case BOW         -> msg = killer.getName() + " sniped "               + victim.getName() + " from afar!";
                case FISHING_ROD -> msg = killer.getName() + " reeled in "             + victim.getName() + " like a fish!";
                default          -> msg = killer.getName() + " eliminated "            + victim.getName() + "!";
            }
        } else if (causeEvt != null) {
            // Environmental or knock-off attribution
            switch (causeEvt.getCause()) {
                case VOID                      -> msg = (ctx != null
                        ? ctx.killer().getName() + " knocked " + victim.getName() + " into the void!"
                        : victim.getName() + " fell into the void.");
                case FALL                      -> msg = (ctx != null
                        ? ctx.killer().getName() + " sent "   + victim.getName() + " plummeting!"
                        : victim.getName() + " hit the ground too hard.");
                case LAVA                      -> msg = victim.getName() + " tried to swim in lava.";
                case DROWNING                  -> msg = victim.getName() + " forgot how to swim.";
                case FIRE, FIRE_TICK           -> msg = victim.getName() + " was burned to a crisp.";
                case ENTITY_EXPLOSION,
                        BLOCK_EXPLOSION           -> msg = victim.getName() + " got blown up.";
                case LIGHTNING                 -> msg = victim.getName() + " was struck by lightning.";
                default                        -> msg = victim.getName() + " died mysteriously.";
            }
        } else {
            // Fallback
            msg = victim.getName() + " died.";
        }

        // 5) Broadcast & suppress the default message
        event.setDeathMessage("");
        main.broadcast(msg);
        // 6) Cleanup drops & kit state
        event.getDrops().clear();
        Kit kitObj = kit.kits.remove(victim.getUniqueId());
        if (kitObj != null) {
            kitObj.cancelAllCooldowns();
            kitObj.cancelAllRegen();
            kitObj.cancelAllTasks();
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player p = event.getPlayer();

        if (!main.game.started) return;

        // 1) Determine where they should reappear after respawn:
        Location teamSpawn;
        if (main.game.redHas(p))      teamSpawn = world.getRed();
        else if (main.game.blueHas(p)) teamSpawn = world.getBlue();
        else                            teamSpawn = Bukkit.getWorld("world").getSpawnLocation();

        // 2) Set that as the respawn point (this happens *before* the player actually appears)
        event.setRespawnLocation(teamSpawn);

        // 3) One tick later, put them into Spectator and start the 8s countdown
        Bukkit.getScheduler().runTaskLater(main, () -> {
            p.setGameMode(GameMode.SPECTATOR);

            // 4) Countdown runnable: after 8s, back to Survival
            new BukkitRunnable() {
                int timer = 8;
                @Override
                public void run() {
                    if (!p.isOnline()) { cancel(); return; }

                    if (timer <= 1) {
                        // back to life
                        p.setGameMode(GameMode.SURVIVAL);
                        // teleport again just to be safe
                        p.teleport(teamSpawn);
                        main.kit.openMenu(p);
                        cancel();
                    } else {
                        // show a little “respawning in Xs” subtitle
                        p.sendTitle("", ChatColor.YELLOW + "Respawning in " + (timer-1) + "s", 0, 20, 0);
                        timer--;
                    }
                }
            }.runTaskTimer(main, 20L, 20L);
        }, 1L);
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
