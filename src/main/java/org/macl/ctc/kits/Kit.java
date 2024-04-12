package org.macl.ctc.kits;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.macl.ctc.Main;

import java.util.ArrayList;
import java.util.HashMap;

public class Kit implements Listener {
    public Main main;
    KitType type;
    Player p;
    PlayerInventory e;

    public Material wool = Material.WHITE_WOOL;

    public HashMap<String, Boolean> cooldown = new HashMap<String, Boolean>();
    private HashMap<String, Cooldown> cooldowns = new HashMap<>();

    public void cancelAllCooldowns() {
        for (Cooldown cooldown : cooldowns.values()) {
            cooldown.cancel();
        }
        cooldowns.clear(); // Optionally clear all entries if they are no longer needed
    }

    public void setHearts(double d) {
        AttributeInstance playerAttribute = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        playerAttribute.setBaseValue(d);
    }

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

    /**
     * Sets a cooldown for a specific ability.
     * @param ability The name of the ability.
     * @param seconds The duration of the cooldown in seconds.
     * @param startSound The sound to play when the cooldown starts.
     * @param endSound The sound to play when the cooldown ends.
     */
    public void setCooldown(String ability, int seconds, Sound startSound, Sound endSound) {
        if (cooldowns.containsKey(ability)) {
            cooldowns.get(ability).cancel();  // Cancel any existing cooldown
        }
        Cooldown cooldown = new Cooldown(this, p, ability, seconds, startSound, endSound, () -> {
            p.sendMessage(ChatColor.GREEN + "Cooldown for " + ability + " is over!");
            cooldowns.remove(ability);  // Remove the cooldown when it's finished
        });
        cooldowns.put(ability, cooldown);
    }

    /**
     * Checks if a specific ability is on cooldown.
     * @param ability The name of the ability to check.
     * @return true if the ability is currently on cooldown, false otherwise.
     */
    public boolean isOnCooldown(String ability) {
        Cooldown cooldown = cooldowns.get(ability);
        return cooldown != null && cooldown.isActive();
    }
    class Cooldown {
        private Player player;
        private BukkitTask task;
        private boolean active;
        private int originalTime;
        private String abilityName;
        private int timeLeft;  // Declare timeLeft as an instance variable

        public Cooldown(Kit kit, Player player, String abilityName, int seconds, Sound startSound, Sound endSound, Runnable onComplete) {
            this.player = player;
            this.abilityName = abilityName;
            this.originalTime = seconds;
            this.active = true;
            this.timeLeft = seconds;  // Initialize timeLeft here
            player.playSound(player.getLocation(), startSound, 1, 1);

            task = new BukkitRunnable() {
                public void run() {
                    if (!active) {
                        this.cancel();
                        return;
                    }
                    if (timeLeft <= 0) {
                        onComplete.run();
                        player.playSound(player.getLocation(), endSound, 1, 1);
                        active = false;
                        this.cancel();
                    } else {
                        timeLeft--;  // Update timeLeft here
                    }
                    kit.updateCooldowns();  // Notify the Kit to update the action bar
                }
            }.runTaskTimer(main, 0L, 20L);  // Make sure kit can provide access to main
        }

        public String getProgressIndicator() {
            double progress = (double) (originalTime - timeLeft) / originalTime;
            return createProgressDots(progress);
        }

        private String createProgressDots(double progress) {
            int stages = 9; // Total number of stages for detailed progression
            int currentStage = (int) (progress * stages);
            int[] thresholds = {1, 3, 5, 6, 7, 8}; // Thresholds for changing dot colors
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 3; i++) {
                if (currentStage >= thresholds[i + 3]) {
                    sb.append(ChatColor.GREEN).append("● ");
                } else if (currentStage >= thresholds[i]) {
                    sb.append(ChatColor.YELLOW).append("● ");
                } else {
                    sb.append(ChatColor.RED).append("● ");
                }
            }
            return sb.toString().trim();
        }

        public boolean isActive() {
            return active;
        }

        public void cancel() {
            if (task != null) {
                task.cancel();
                active = false;
            }
        }
    }

    public void updateCooldowns() {
        StringBuilder message = new StringBuilder();
        cooldowns.forEach((ability, cooldown) -> {
            if (cooldown.isActive()) {
                message.append(ChatColor.WHITE).append(ChatColor.BOLD).append(ability.toUpperCase()).append(" ");
                message.append(cooldown.getProgressIndicator()).append(" ");
            }
        });
        if (!message.toString().isEmpty()) {
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message.toString()));
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
