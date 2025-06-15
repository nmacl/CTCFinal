package org.macl.ctc.kits;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.macl.ctc.Main;

import java.util.ArrayList;

public class Grandma extends Kit {

    ItemStack healCookies = newItem(Material.COOKIE, ChatColor.RED + "Heal Cookies", 1);

    public Grandma(Main main, Player p, KitType type) {
        super(main, p, type);
        p.removePotionEffect(PotionEffectType.SPEED);
        PlayerInventory e = p.getInventory();
        ArrayList<Enchantment> enchants = new ArrayList<Enchantment>();
        enchants.add(Enchantment.KNOCKBACK);
        enchants.add(Enchantment.DAMAGE_ALL);
        e.addItem(newItemEnchants(Material.STICK, ChatColor.DARK_PURPLE + "Cane", enchants, 3));
        e.addItem(healCookies);
        e.addItem(healCookies);
        e.addItem(healCookies);
        e.setLeggings(newItemEnchanted(Material.GOLDEN_LEGGINGS, ChatColor.GOLD + "Clean Pants", Enchantment.PROTECTION_ENVIRONMENTAL, 1));
        e.setChestplate(newItem(Material.LEATHER_CHESTPLATE, "Ironed Tutu"));
        giveWool();
        giveWool();
        regenItem("heal", healCookies, 8, 3, 1);
        setHearts(24);
    }

    public void heart() {
        int cookies = p.getInventory().first(Material.COOKIE);
        p.getInventory().getItem(cookies).setAmount(p.getInventory().getItem(cookies).getAmount() - 1);
        p.getWorld().spawnParticle(Particle.HEART, p.getLocation().add(0,2,0), 10);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_VILLAGER_AMBIENT, 1f, 1f);
        if(p.getHealth() + 4.0 > 20)
            p.setHealth(p.getMaxHealth());
        else
            p.setHealth(p.getHealth() + 4.0);

    }



}
