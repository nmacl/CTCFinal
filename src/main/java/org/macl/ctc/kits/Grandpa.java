package org.macl.ctc.kits;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.macl.ctc.Main;

import java.util.ArrayList;

public class Grandpa extends Kit{

    public Grandpa(Main main, Player p, KitType type){
        super(main, p, type);
        p.removePotionEffect(PotionEffectType.SPEED);
        PlayerInventory e = p.getInventory();
        e.setHelmet(newItem(Material.LEATHER_HELMET, ChatColor.DARK_GREEN + "Veteran's Helmet"));
        e.addItem(newItem(Material.PRISMARINE_SHARD, ChatColor.GRAY + "Slugged Shotgun"));
        e.addItem(newItem(Material.HONEY_BOTTLE, ChatColor.GOLD + "Booze"));
        e.addItem(newItem(Material.LADDER, "Old Ladder", 24));
        giveWool();
        giveWool();

    }
}
