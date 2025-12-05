package org.macl.ctc.events;

import org.bukkit.*;
import org.bukkit.Color;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.boss.BossBar;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.util.Vector;
import org.macl.ctc.Main;
import org.macl.ctc.kits.Engineer;
import org.macl.ctc.kits.Spy;
import org.macl.ctc.kits.Tank;

import java.awt.*;
import java.util.ArrayList;
import java.util.UUID;


public class Blocks extends DefaultListener {

    public Blocks(Main main) {
        super(main);
    }

    @EventHandler
    public void worldSave(WorldSaveEvent event) {
        main.broadcast("saving. this shouldn't be happening.");
    }

    @EventHandler
    public void worldUnload(WorldUnloadEvent event) {
        main.broadcast("unload");
    }

    @EventHandler
    public void blockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        if(main.restricted.contains(event.getBlock().getType()) && main.game.started)
            event.setCancelled(true);
        game.resetCenter(null);

        event.setDropItems(false);

        // Additional game logic, e.g., resetting center (if necessary)

        // Handling obsidian specific logic
        if(block.getType() == Material.OBSIDIAN && main.game.started && player.getInventory().getItemInMainHand().getType() == Material.DIAMOND_PICKAXE) {
            ArrayList<Material> nearbyBlocks = getNearbyBlocks(block.getLocation(), 5);  // Assume getNearbyBlocks is implemented elsewhere
            for(Material m : nearbyBlocks) {
                if((game.redHas(player) && game.center == 1 && m == Material.LAPIS_ORE) ||
                        (game.blueHas(player) && game.center == 2 && m == Material.REDSTONE_ORE)) {
                    // Decrement core health and update boss bar here directly, if applicable
                    main.getStats().recordCoreCrack(player);
                    if (m == Material.LAPIS_ORE) {
                        game.blueCoreHealth--;
                        main.broadcast(ChatColor.BLUE + "The blue core has been damaged!");
                        triggerEffects(player.getLocation());
                        game.updateScoreboard();
                        break;
                    } else if (m == Material.REDSTONE_ORE) {
                        game.redCoreHealth--;
                        main.broadcast(ChatColor.RED + "The red core has been damaged!");
                        triggerEffects(player.getLocation());
                        game.updateScoreboard();
                        break;
                    }
                }
            }
            if(game.blueCoreHealth <= 0 || game.redCoreHealth <= 0) {
                main.broadcast(ChatColor.BOLD + "A core has been destroyed!");
                game.stop(player);
            }
        }
    }
    private void triggerEffects(Location location) {
        blowBackPlayers(location, 8);
        launchFirework(location);
        playWitherSoundForAll();
    }

    private void blowBackPlayers(Location center, double radius) {
        for (Player p : center.getWorld().getPlayers()) {
            if (p.getLocation().distance(center) <= radius) {
                Vector direction = p.getLocation().getDirection().normalize().multiply(-1);
                p.setVelocity(direction.multiply(1.5).setY(1));
            }
        }
    }

    private void launchFirework(Location location) {
        Firework firework = location.getWorld().spawn(location, Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.setPower(2);
        meta.addEffect(FireworkEffect.builder().withColor(Color.WHITE).with(FireworkEffect.Type.BURST).build());
        firework.setFireworkMeta(meta);
        firework.detonate();
    }

    private void playWitherSoundForAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0F, 1.0F);
        }
    }
    @EventHandler
    public void blockPlace(BlockPlaceEvent event) {
        Block b = event.getBlock();
        Player p = event.getPlayer();
        game.resetCenter(p);
        if (b.getType() == Material.RED_CANDLE)
            if (main.getKits().get(p.getUniqueId()) != null && main.getKits().get(p.getUniqueId()) instanceof Spy) {
                Spy s = (Spy) main.getKits().get(p.getUniqueId());
                if (s.getDetonate() != null)
                    return;
                s.setDetonate(b.getLocation());
                s.addDetonate();
                p.sendMessage(ChatColor.GREEN + "Remote explosive activated! Right click blaze rod to activate");
            }

        if(main.getKits().get(p.getUniqueId()) != null && main.getKits().get(p.getUniqueId()) instanceof Tank && (b.getType() == Material.BLUE_STAINED_GLASS_PANE || b.getType() == Material.RED_STAINED_GLASS_PANE)) {
            Tank t = (Tank) main.getKits().get(p.getUniqueId());
            t.shield(event.getBlock(), p.getFacing());
        }
        if(main.getKits().get(p.getUniqueId()) != null && main.getKits().get(p.getUniqueId()) instanceof Engineer) {
            Engineer e = (Engineer) main.getKits().get(p.getUniqueId());
            if(event.getBlock().getType() == Material.DISPENSER) {
                e.turret(b.getLocation());
            }
        }

    }


    @EventHandler
    public void blockBurn(BlockBurnEvent event) {
        if(main.restricted.contains(event.getBlock().getType()) && main.game.started)
            event.setCancelled(true);
        game.resetCenter(null);
    }

    @EventHandler
    public void blockExplode(BlockExplodeEvent event) {
        Block b = event.getBlock();
        Material m = b.getType();
        if(main.restricted.contains(m))
            event.setCancelled(true);
        if(m == Material.RED_STAINED_GLASS_PANE || m == Material.BLUE_STAINED_GLASS_PANE)
            event.setCancelled(true);
        event.setYield(0);
        game.resetCenter(null);
    }

    public ArrayList<Material> getNearbyBlocks(Location location, int radius) {
        ArrayList<Material> blocks = new ArrayList<Material>();
        for(int x = location.getBlockX() - radius; x <= location.getBlockX() + radius; x++) {
            for(int y = location.getBlockY() - radius; y <= location.getBlockY() + radius; y++) {
                for(int z = location.getBlockZ() - radius; z <= location.getBlockZ() + radius; z++) {
                    blocks.add(location.getWorld().getBlockAt(x, y, z).getType());
                }
            }
        }
        return blocks;
    }

    @EventHandler
    public void hit(ProjectileHitEvent event) {
        Block b = event.getHitBlock();
        if(b == null || b.getType() == Material.RED_STAINED_GLASS_PANE || b.getType() == Material.BLUE_STAINED_GLASS_PANE)
            return;
        if(event.getEntity().getShooter() instanceof Player && event.getEntity() instanceof Egg) {
            Player p = (Player) event.getEntity().getShooter();
            if(b != null) {
                main.fakeExplode(p, b.getLocation(), 8, 6, false, true,true, "grenade");
            }

        }
        if(event.getEntity().getShooter() instanceof Player && event.getEntity() instanceof Arrow) {
            Player p = (Player) event.getEntity().getShooter();
            if(event.getEntity() != null)
                event.getEntity().remove();
        }
    }


}
