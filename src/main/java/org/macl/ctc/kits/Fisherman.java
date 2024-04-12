package org.macl.ctc.kits;

import net.minecraft.world.damagesource.DamageSource;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.macl.ctc.Main;

import java.util.ArrayList;
import java.util.UUID;

public class Fisherman extends Kit {

    public Fisherman(Main main, Player p, KitType type) {
        super(main, p, type);
        e.addItem(newItem(Material.FISHING_ROD, ChatColor.GOLD + "Fishing Rod"));
        e.setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));
        e.setHelmet(new ItemStack(Material.TURTLE_HELMET));
        e.setBoots(new ItemStack(Material.LEATHER_BOOTS));
        e.addItem(newItem(Material.PUFFERFISH, ChatColor.YELLOW + "Pufferfish Bomb"));
        e.addItem(newItem(Material.COD, ChatColor.LIGHT_PURPLE + "Cod Sniper"));
        giveWool();
        giveWool();
        setHearts(16);
    }
    int timer = 0;
    int cod = 0;

    ArrayList<Location> locs = new ArrayList<Location>();
    Location shooter;

    ArrayList<UUID> lits = new ArrayList<UUID>();

    public void codSniper() {
        if (isOnCooldown("cod")) {
            return;
        }
        setCooldown("cod", 10, Sound.ITEM_GOAT_HORN_SOUND_3, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
        Location shooter = p.getLocation();  // Store shooter's location
        Entity codEntity = p.getWorld().spawnEntity(p.getEyeLocation(), EntityType.COD);  // Spawn the COD at the eye location to avoid hitting the shooter
        Vector velocity = p.getLocation().getDirection().multiply(2.5);  // Set the initial velocity

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!codEntity.isValid()) {  // Check if the entity is still valid
                    this.cancel();
                    return;
                }

                codEntity.setVelocity(velocity);
                Particle.DustOptions dustOptions = main.game.redHas(p) ?
                        new Particle.DustOptions(Color.fromRGB(255, 0, 0), 3.0F) :
                        new Particle.DustOptions(Color.fromRGB(0, 0, 255), 3.0F);
                p.getWorld().spawnParticle(Particle.REDSTONE, codEntity.getLocation(), 10, 0.35, 0.2, 0.35, 0.2, dustOptions);

                for (Entity entity : codEntity.getNearbyEntities(0.7, 0.7, 0.7)) {
                    if (entity instanceof Player && entity != p) {  // Exclude the shooter
                        Player hitPlayer = (Player) entity;

                        if (!lits.contains(hitPlayer.getUniqueId())) {
                            double damage = 6;
                            if(hitPlayer.getHealth() - 6 == 0)
                                hitPlayer.setHealth(0);
                            else
                                hitPlayer.setHealth(hitPlayer.getHealth() - 6);
                            hitPlayer.damage(1);

                            hitPlayer.getWorld().playSound(hitPlayer.getLocation(), Sound.BLOCK_BELL_USE, 5f, 5f);
                            lits.add(hitPlayer.getUniqueId());
                            lits.clear();
                            this.cancel();  // Stop the task
                            return;
                        }
                    }
                }
            }
        }.runTaskTimer(main, 0L, 1L);
    }


    public void pufferfishBomb() {
        if(isOnCooldown("pufferfish"))
            return;
        setCooldown("pufferfish", 10, Sound.ENTITY_PUFFER_FISH_BLOW_UP, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
        ItemStack eggItem = e.getItemInMainHand();
        Location pLoc = p.getEyeLocation();

        ItemStack throwStack = new ItemStack(eggItem);
        throwStack.setAmount(1);

        Item thrownEggItem = p.getWorld().dropItem(pLoc, throwStack);
        thrownEggItem.setVelocity(pLoc.getDirection());
        e.setItemInMainHand(eggItem);

        new BukkitRunnable() {
            @Override
            public void run() {
                timer++;
                double y = thrownEggItem.getVelocity().getY();
                if(y == 0 || timer == 10) {
                    this.cancel();
                    for(int i = 0; i < 10; i++)
                        p.getWorld().spawnEntity(thrownEggItem.getLocation(), EntityType.PUFFERFISH);
                    thrownEggItem.remove();
                }
            }
        }.runTaskTimer(main, 0L, 5L);
    }
}
