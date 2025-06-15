package org.macl.ctc.kits;

import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.macl.ctc.Main;

import java.util.ArrayList;

public class Lumberjack extends Kit {

    ItemStack logChuck = newItem(Material.OAK_LOG, ChatColor.GOLD + "Log Chuck", 1);
    ItemStack mysticSap = newItem(Material.HONEYCOMB, ChatColor.GOLD + "Mystic Sap", 1);

    int usage = 50;

    public Lumberjack(Main main, Player p, KitType type) {
        super(main, p, type);

        ArrayList<Enchantment> enchants = new ArrayList<Enchantment>();
        enchants.add(Enchantment.DAMAGE_ALL);

        p.removePotionEffect(PotionEffectType.SPEED);
        p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 99999999, 0,true,false));
        PlayerInventory e = p.getInventory();
        e.addItem(newItemEnchants(Material.GOLDEN_AXE, ChatColor.YELLOW + "Chain-Axe", enchants, 1));
        e.addItem(logChuck);
        e.addItem(logChuck);
        e.addItem(mysticSap);
        e.setHelmet(newItem(Material.LEATHER_HELMET, ChatColor.GOLD + "Lumberjack Cap"));
        e.setChestplate(newItem(Material.IRON_CHESTPLATE, ChatColor.GREEN + "Oiled-Up Shirt"));
        giveWool();
        giveWool();
        regenItem("Log", logChuck, 12, 2, 1);
    }

    public void sawBlocks() {
        ItemStack item = e.getItem(0);

        int dmg = usage*20;

        usage--;

        ItemMeta itemMeta = item.getItemMeta();
        Damageable damage = (Damageable) itemMeta;

        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BAMBOO_FALL, 1.0f, 0.3f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_IRON_DOOR_CLOSE, 1.0f, 0.3f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BAMBOO_FALL, 1.6f, 1.6f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_IRON_DOOR_CLOSE, 1.6f, 1.6f);

        if (itemMeta instanceof Damageable){
            if(dmg != item.getType().getMaxDurability())
                damage.setDamage(dmg);
        }
        RayTraceResult hitBlock = p.getWorld().rayTraceBlocks(p.getEyeLocation(), p.getEyeLocation().getDirection(), 5);
        RayTraceResult hitEntity = p.getWorld().rayTraceEntities(p.getEyeLocation(),p.getEyeLocation().getDirection().multiply(3),5);

        p.getWorld().spawnParticle(Particle.CRIT, p.getLocation().add(0, 1, 0), 3, 0.2, 0.2, 0.2, 0.1);

        if (hitBlock != null && hitBlock.getHitBlock() != null) { //Handle Block Destruction
            if (!main.restricted.contains(hitBlock.getHitBlock().getType())) {
                hitBlock.getHitBlock().setType(Material.AIR);
                p.getWorld().spawnParticle(Particle.CRIT, hitBlock.getHitBlock().getLocation().add(0.5,0.5,0.5), 20, 1, 0.1, 0.1, 0.1
                );
            }
        }

        if (hitEntity != null && hitEntity.getHitEntity() != null) { // Handle Damage!!
            Entity e = hitEntity.getHitEntity();
            if (e instanceof Player) {

                if (e.getUniqueId() == p.getUniqueId()) {
                    return;
                }

                if (!main.game.sameTeam(
                        p.getUniqueId(),
                        e.getUniqueId()
                )){
                    ((Player) e).damage(0.5);
                }
            }
        }
    }



}
