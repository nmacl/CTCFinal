package org.macl.ctc.kits;

import com.google.common.collect.Multimap;
import net.minecraft.world.item.Item;
import org.bukkit.*;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.entity.ShulkerBullet;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.macl.ctc.Main;

import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EventListener;
import java.util.Objects;

public class Lumberjack extends Kit {

    ItemStack logChuck = newItem(Material.OAK_LOG, ChatColor.GOLD + "Log Chuck", 1);
    ItemStack mysticSap = newItem(Material.HONEYCOMB, ChatColor.GREEN + "Mystic Sap", 1);

    boolean isSawing = false;

    int mysticSapUses = 0;

    public Lumberjack(Main main, Player p, KitType type) {
        super(main, p, type);

        BukkitTask process = new lumberjackProcess().runTaskTimer(main,0,1);
        this.registerTask(process);

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
        regenItem("Log", logChuck, 10, 2, 1);
        setHearts(24.0);
    }


    public void useMysticSap() {
        if (isOnCooldown("Sap")) return;
        setCooldown("Sap", 26, Sound.ITEM_BOTTLE_FILL, () -> {
            p.getInventory().setItem(2, mysticSap);
        });

        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.0f);

        p.getInventory().setItem(2, newItem(Material.POTATO, ChatColor.GRAY + "Sapped!"));

        mysticSapUses = 2;
        p.setLevel(mysticSapUses);

        mysticSapProcess m = new mysticSapProcess();
        m.parent = p;
        m.lifetime = 12*20;
        m.consumable = false;
        m.rad = 4;
        BukkitTask mTask = m.runTaskTimer(main,0,1);
        registerTask(mTask); // we want the main sap field to go away when Jack dies, but not the consumable ones
    }

    private class mysticSapProcess extends BukkitRunnable {
        int particles = 50;
        double rad = 3.0;
        int current_lifetime = 0;
        int lifetime = 20*8;
        boolean consumable = false;
        boolean active = true;
        Location loc;
        Entity parent;

        public void run() {

            if (!active) {

                if (!parent.isValid()) {
                    active = true;
                    loc = loc.getBlock().getLocation().add(0.5,0.5,0.5);
                    main.broadcast("Parent no longer valid");
                } else {

                    loc = parent.getLocation().add(parent.getVelocity());
                    createGreenParticles();
                }

                return;
            }

            if (current_lifetime <= lifetime) {
                current_lifetime++;
                handleMysticSap();
            } else {
                if (!consumable) {
                    mysticSapUses = 0;
                    p.setLevel(mysticSapUses);
                }
                cancel();
            }
        }

        public void createGreenParticles() {
            Objects.requireNonNull(loc.getWorld()).spawnParticle(Particle.VILLAGER_HAPPY, loc, 5,0.0,0.0,0.0,0.0);
        }

        public void createHeadParticles() {
            Particle.DustOptions teamDust = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 2);;
            if(main.game.redHas(p))
                teamDust = new Particle.DustOptions(Color.fromRGB(255, 0, 0), 2);
            if(main.game.blueHas(p))
                teamDust = new Particle.DustOptions(Color.fromRGB(0, 0, 255), 2);


            Location locAdd = loc.clone().add(0.0,2.5,0.0);

            Objects.requireNonNull(loc.getWorld()).spawnParticle(Particle.REDSTONE, locAdd, 1, teamDust);
        }

        public void createRingParticles() {
//            Particle.DustOptions teamDust = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 2);;
//            if(main.game.redHas(p))
//                teamDust = new Particle.DustOptions(Color.fromRGB(255, 0, 0), 2);
//            if(main.game.blueHas(p))
//                teamDust = new Particle.DustOptions(Color.fromRGB(0, 0, 255), 2);

            for (int i = 0; i < particles; i++) {

                double theta = Math.random() * 2 * Math.PI;
                double x = rad * Math.cos(theta);
                double z = rad * Math.sin(theta);

                Location particleLocation = loc.clone().add(x, 0.5, z);
                Objects.requireNonNull(loc.getWorld()).spawnParticle(Particle.COMPOSTER, particleLocation, 1);
            }
        }

        public void addPotionEffect(Player p,PotionEffect e) {
            if (!p.hasPotionEffect(e.getType())) {
                    p.addPotionEffect(e);
            } else if ((p.getPotionEffect(e.getType()).getDuration() < 5)) {
                p.addPotionEffect(e);
            }
        }

        public void handleMysticSap() {

            if (parent.isValid()) {
                loc = parent.getLocation();
            }

            createHeadParticles();
            createRingParticles();

            Location trueLoc = loc.clone().add(0, 0.5, 0);

            Collection<Entity> entities = trueLoc.getWorld().getNearbyEntities(trueLoc, rad,rad,rad);
            for (Entity e : entities) {
                if (e instanceof Player p2) {
                    if (main.game.sameTeam(p.getUniqueId(),p2.getUniqueId())) { //same team logic
                        if (!consumable) { // give constant regen for 2 sec
                            addPotionEffect(p2,new PotionEffect(PotionEffectType.REGENERATION, 40, 1));
                        } else { // give 4 absorption hearts for 12 secs
                            addPotionEffect(p2,new PotionEffect(PotionEffectType.ABSORPTION, 20*12, 1));
                            p.getWorld().playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 1.0f);
                            cancel();
                        }
                    } else { // other team logic
                        if (!consumable) { // give constant poison 3 for 2 sec
                            addPotionEffect(p2,new PotionEffect(PotionEffectType.POISON, 40, 2));
                        } else { // give poison for 6 sec
                            addPotionEffect(p2,new PotionEffect(PotionEffectType.POISON, 20*8, 0));
                            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BREEZE_DEATH, 1.0f, 0.9f);
                            cancel();
                        }
                    }
                }
            }
        }

    }

    public void chuckLog() {

        if (isOnCooldown("Chuck")) return;
        setCooldown("Chuck", 1, Sound.BLOCK_WOOD_HIT, () -> {});

        int logs = p.getInventory().first(Material.OAK_LOG);
        p.getInventory().getItem(logs).setAmount(p.getInventory().getItem(logs).getAmount() - 1);

        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_WOOD_BREAK, 1.0f, 0.8f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_EGG_THROW, 1.0f, 0.2f);

        Vector dir = p.getLocation().getDirection();
        BlockData block = Material.OAK_LOG.createBlockData();
        FallingBlock log = p.getWorld().spawnFallingBlock(
                p.getEyeLocation().subtract(0.0,0.5,0.0),
                block
                );


        mysticSapProcess s;

        if (mysticSapUses > 0) {
            mysticSapUses--;

            p.setLevel(mysticSapUses);
            s = new mysticSapProcess();
            s.lifetime = 20 * 20;
            s.active = false;
            s.consumable = true;
            s.rad = 2;
            s.parent = log;
            s.runTaskTimer(main,0,1);

        }

        log.setVelocity(dir.multiply(1.8));
        if (!p.getPassengers().isEmpty()) {
            for (Entity e : p.getPassengers()) {
                log.addPassenger(e);
            }
        }

        new BukkitRunnable() {

            Location lastLoc;

            public void run() {
                if (!log.isValid()) {
                    logExplode(lastLoc);

                    cancel();
                } else {
                    lastLoc = log.getLocation();
                }

            }

        }.runTaskTimer(main, 0, 1);

    }

    public void logExplode(Location loc) {
        loc.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 2.0f, 0.8f);
        main.fakeExplode(p,loc,12,3,false,false,false);
    }

    public void sawBlocks() {
        if (getAxeMeta() != null) {
            if (getAxeDamage() >= 32){
                return;
            }
        }

        setAxeDamage(1);
        isSawing = true;

        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BAMBOO_FALL, 1.0f, 0.3f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_IRON_DOOR_CLOSE, 1.0f, 0.3f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BAMBOO_FALL, 1.6f, 1.6f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_IRON_DOOR_CLOSE, 1.6f, 1.6f);

        Vector vec = p.getEyeLocation().getDirection().normalize().multiply(0.5);

        RayTraceResult hit = p.getWorld().rayTraceBlocks(p.getEyeLocation().add(vec), p.getEyeLocation().getDirection(), 4.5);
        RayTraceResult hitEntity = p.getWorld().rayTraceEntities(p.getEyeLocation().add(vec), p.getEyeLocation().getDirection(), 4.5);

        p.getWorld().spawnParticle(Particle.CRIT, p.getLocation().add(0, 1, 0), 3, 0.2, 0.2, 0.2, 0.1);

        if (hit != null && hit.getHitBlock() != null) { //Handle Block Destruction
            if (!main.restricted.contains(hit.getHitBlock().getType())) {
                hit.getHitBlock().setType(Material.AIR);
                hit.getHitBlock().breakNaturally();
                p.getWorld().spawnParticle(Particle.CRIT, hit.getHitBlock().getLocation().add(0.5,0.5,0.5), 20, 0.5, 0.5, 0.5, 0.5
                );
            }
        }

        if (hitEntity != null && hitEntity.getHitEntity() != null) { // Handle Damage!!
            Entity e = hitEntity.getHitEntity();
            if (e instanceof Player) {


//                main.broadcast("Acquired player");
//                if (e.getUniqueId() == p.getUniqueId()) {
//                    return;
//                }

                if (!main.game.sameTeam(
                        p.getUniqueId(),
                        e.getUniqueId()
                )){

//                    main.broadcast("Damaging player");
                    ((Player) e).damage(0.2);
                    ((Player) e).setNoDamageTicks(0);
                }
            }
        }
    }

    private class lumberjackProcess extends BukkitRunnable {

        int axeCount = 0;
        final int maxAxeCount = 7;

        public void run() {

            failsafe();

            if (axeCount < maxAxeCount) {
                axeCount++;
            } else {
                axeCount = 0;
                handleAxeRecharge();
            }
        }

        public void handleAxeRecharge() {
            if (isSawing) {
                isSawing = false;
            } else {
                if (getAxeMeta() != null) {
                    if (getAxeDamage() > 0) setAxeDamage(-1);
                }
            }
        }

        public void failsafe() {
            if (main.kit.kits.get(p.getUniqueId()) == null) {
                cancel();
            }
            if (!(main.kit.kits.get(p.getUniqueId()) instanceof Lumberjack)) {
                cancel();
            }
        }

    }

    private ItemStack getAxe() {
        if (p.getInventory().getItem(0).getType() == Material.GOLDEN_AXE) {
            return p.getInventory().getItem(0);
        } else {
            return null;
        }
    }

    private ItemMeta getAxeMeta() {
        if (p.getInventory().getItem(0).getType() == Material.GOLDEN_AXE) {
            return p.getInventory().getItem(0).getItemMeta();
        } else {
            return null;
        }
    }

    private void setAxeDamage(int damage) {
        if (getAxeMeta() != null) {
            if (getAxeMeta() instanceof Damageable) {
                Damageable dmg = (Damageable) getAxeMeta();

                int totalDamage = getAxeDamage() + damage;
                if (totalDamage > 32) totalDamage = 32;

                dmg.setDamage(totalDamage);
                getAxe().setItemMeta(dmg);
            }
        }
    }

    private int getAxeDamage() {
        if (getAxeMeta() != null) {
            if (getAxeMeta() instanceof Damageable) {
                return ((Damageable) getAxeMeta()).getDamage();
            }
        }
        return 0;
    }

}
