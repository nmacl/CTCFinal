package org.macl.ctc.events;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Player;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.util.Vector;
import org.macl.ctc.Main;
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
        if(main.restricted.contains(event.getBlock().getType()))
            event.setCancelled(true);
        event.setDropItems(false);
        game.resetCenter();
        Player p = event.getPlayer();
        Block b = event.getBlock();

        if(b.getType() == Material.OBSIDIAN && main.game.started && p.getInventory().getItemInMainHand().getType() == Material.DIAMOND_PICKAXE) {
            ArrayList<Material> blocs = getNearbyBlocks(b.getLocation(), 5);
            for(Material m : blocs) {
                //FIX CONSOLE ERROR, maybe cancel event?
                if(game.redHas(p) && game.center == 1 && m == Material.LAPIS_ORE)
                    game.stop(p);
                if(game.blueHas(p) && game.center == 2 && m == Material.REDSTONE_ORE)
                    game.stop(p);
            }
        }
    }

    @EventHandler
    public void blockPlace(BlockPlaceEvent event) {
        Block b = event.getBlock();
        Player p = event.getPlayer();
        game.resetCenter();
        if (b.getType() == Material.RED_CANDLE)
            if (main.getKits().get(p.getUniqueId()) != null && main.getKits().get(p.getUniqueId()) instanceof Spy) {
                Spy s = (Spy) main.getKits().get(p.getUniqueId());
                if (s.getDetonate() != null)
                    return;
                s.setDetonate(b.getLocation());
                s.addDetonate();
                p.sendMessage(ChatColor.GREEN + "Remote explosive deployed! Right click blaze rod to activate");
            }

        if(main.getKits().get(p.getUniqueId()) != null && main.getKits().get(p.getUniqueId()) instanceof Tank && (b.getType() == Material.BLUE_STAINED_GLASS_PANE || b.getType() == Material.RED_STAINED_GLASS_PANE)) {
            Tank t = (Tank) main.getKits().get(p.getUniqueId());
            t.shield(event.getBlock(), p.getFacing());
        }
    }


    @EventHandler
    public void blockBurn(BlockBurnEvent event) {
        if(main.restricted.contains(event.getBlock().getType()) && main.game.started)
            event.setCancelled(true);
        game.resetCenter();
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
        game.resetCenter();
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
            if(b != null)
                b.getLocation().getWorld().createExplosion(b.getLocation().add(0, 1.3, 0), 1.9f);

        }
        if(event.getEntity().getShooter() instanceof Player && event.getEntity() instanceof Arrow) {
            Player p = (Player) event.getEntity().getShooter();
            if (event.getEntity() != null)
                event.getEntity().remove();
        }

        if(event.getEntity().getShooter() instanceof Player && event.getEntity() instanceof SpectralArrow) {
            Player p = (Player) event.getEntity().getShooter();
            if(event.getEntity() != null)
                event.getEntity().remove();

        }
    }


}
