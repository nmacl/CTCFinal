package org.macl.ctc.kits;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.macl.ctc.Main;

import java.util.ArrayList;
import java.util.HashMap;

public class Kit implements Listener {
    public Main main;
    KitType type;
    Player p;
    PlayerInventory e;

    public Material wool = Material.WHITE_WOOL;

    public HashMap<String, Boolean> cooldowns = new HashMap<String, Boolean>();
    public Kit(Main main, Player p, KitType type) {
        this.main = main;
        this.type = type;
        this.p = p;
        this.e = p.getInventory();
        main.getServer().getPluginManager().registerEvents(this, main);
        p.getInventory().clear();
        p.getInventory().setArmorContents(null);
        for(PotionEffect e : p.getActivePotionEffects())
            p.removePotionEffect(e.getType());
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 99999999, 0));
        if(main.game.redHas(p) && main.game.center == 1)
            p.getInventory().setItem(8, main.coreCrush());
        if(main.game.blueHas(p) && main.game.center == 2)
            p.getInventory().setItem(8, main.coreCrush());
        if(main.game.redHas(p)) {
            wool = Material.RED_WOOL;
        }
        if(main.game.blueHas(p)) {
            wool = Material.BLUE_WOOL;
        }

    }

    //Setup with parameters instead of arguments String[] params

    public ItemStack newItem(Material m, String name) {
        ItemStack item = new ItemStack(m);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack newItem(Material m, String name, int stack) {
        ItemStack item = new ItemStack(m,stack);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack newItemEnchanted(Material m, String name, Enchantment enchant, int level) {
        ItemStack item = new ItemStack(m);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        item.addUnsafeEnchantment(enchant, level);
        return item;
    }

    public ItemStack newItemEnchants(Material m, String name, ArrayList<Enchantment> enchants, int level) {
        ItemStack item = new ItemStack(m);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        for(Enchantment enchant : enchants)
            item.addUnsafeEnchantment(enchant, level);
        return item;
    }

    public void giveWool() {
        Material wool = Material.WHITE_WOOL;
        if(main.game.redHas(p))
            wool = Material.RED_WOOL;
        if(main.game.blueHas(p))
            wool = Material.BLUE_WOOL;
        p.getInventory().addItem(new ItemStack(wool, 64));
    }

    public void playExp(float level) {
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, level);
    }

    public boolean has(Player p1) {
        if(p1.getUniqueId() == p.getUniqueId())
            return true;
        return false;
    }

    public Player getPlayer() {
        return p;
    }

}
