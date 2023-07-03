package org.macl.ctc.kits;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.level.Level;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowman;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.macl.ctc.Main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class Engineer extends Kit {
    public Engineer(Main main, Player p, KitType type) {
        super(main, p, type);
    }
}

    /*public Location loc1 = null;
    public Location loc2 = null;

    public boolean canTeleport = true;

    public Location enemyTracker = null;

    public Engineer(Main main, Player p, KitType type) {
        super(main, p, type);
        PlayerInventory e = p.getInventory();
        ItemStack Flare = new ItemStack(Material.FIREWORK_ROCKET, 64);
        FireworkMeta fwm = (FireworkMeta)Flare.getItemMeta();
        FireworkEffect effect;
        if(main.game.redHas(p)) {
            effect = FireworkEffect.builder().withColor(Color.RED).with(FireworkEffect.Type.BALL_LARGE).withFlicker().trail(false).build();
        } else {
            effect = FireworkEffect.builder().withColor(Color.BLUE).with(FireworkEffect.Type.BALL_LARGE).withFlicker().trail(false).build();
        }
        fwm.addEffects(effect);
        fwm.setPower(1);
        Flare.setItemMeta(fwm);
        e.setItemInOffHand(Flare);
        ArrayList<Enchantment> es = new ArrayList<Enchantment>();
        es.add(Enchantment.MULTISHOT);
        es.add(Enchantment.QUICK_CHARGE);
        ItemStack crossbow = newItemEnchants(Material.CROSSBOW, "Firework Shotgun", es, 1);
        e.addItem(crossbow);

        Color col = ((main.game.redHas(p)) ? Color.RED : Color.BLUE;
        ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta chestplatemeta = (LeatherArmorMeta) chestplate.getItemMeta();
        chestplatemeta.setColor(col);
        chestplate.setItemMeta(chestplatemeta);
        p.getInventory().setChestplate(chestplate);
        p.getInventory().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
        p.getInventory().setLeggings(new ItemStack(Material.CHAINMAIL_LEGGINGS));

        e.addItem(newItem(Material.BEACON, ChatColor.DARK_GREEN + "Teleporter", 2));
        e.addItem(newItem(Material.SHULKER_BOX, ChatColor.RED + "Enemy Tracker"));
        e.addItem(newItem(Material.DISPENSER, ChatColor.BOLD + "Snowman Turret"));
        giveWool();
        giveWool();

    }

    public void placeTracker(Block b) {
        if(main.getAbilityManager().onCooldown(p, ChatColor.RED + "Enemy Tracker"))
            return;
        enemyTracker = b.getLocation();
        new Ability(main, Material.SHULKER_BOX, ChatColor.RED + "Enemy Tracker", 20*20, p, 2) {
            @Override
            public void run() {
                if(ticks >= dur) {
                    this.cancel();
                    p.getInventory().remove(Material.SHULKER_BOX);
                    p.getInventory().addItem(newItem(Material.SHULKER_BOX, ChatColor.RED + "Enemy Tracker"));
                }
                for(org.bukkit.entity.Entity e1 : enemyTracker.getWorld().getNearbyEntities(enemyTracker, 10, 10, 10)) {
                    if(e1 instanceof Player) {
                        Player p1 = (Player) e1;
                        p1.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 100000, 1));
                    }
                }
            }
        }.runTaskTimer(main, 0, 1);
    }

    public void breakTracker(Block b) {

    }

    public void teleportDestroy(Block b) {
        if(b.equals(loc1.getBlock()) || b.equals(loc2.getBlock())) {
            loc1.getBlock().getLocation().clone().add(0,1,0).getBlock().setType(Material.AIR);
            loc2.getBlock().getLocation().clone().add(0,1,0).getBlock().setType(Material.AIR);
            loc1.getBlock().setType(Material.AIR);
            loc2.getBlock().setType(Material.AIR);
            for(int i = -1; i < 2; i++) {
                for(int j = -1; j < 2; j++) {
                    loc1.getWorld().getBlockAt(loc1).getLocation().add(i,-1,j).getBlock().setType(Material.WHITE_WOOL);
                    loc2.getWorld().getBlockAt(loc2).getLocation().add(i,-1,j).getBlock().setType(Material.WHITE_WOOL);
                }
            }
            loc1 = null;
            loc2 = null;
            p.getInventory().addItem(newItem(Material.BEACON, ChatColor.GREEN + "Teleporter", 1));
            new Ability(main, Material.BEACON, ChatColor.GREEN + "Teleporter", 20*20, p, 1) {
                @Override
                public void run() {
                    if(ticks >= dur) {
                        this.cancel();
                        p.getInventory().remove(Material.BEACON);
                        p.getInventory().addItem(newItem(Material.BEACON, ChatColor.DARK_GREEN + "Teleporter", 2));
                    }
                }
            }.runTaskTimer(main, 0, 1);
            p.sendMessage(main.getPrefix() + ChatColor.RED + "Your teleporter has been destroyed!");
        }
    }

    public void teleport(Player p, Location to) {
        if(canTeleport == true && (loc1 != null) && (loc2 != null) && to.getBlock().getLocation().clone().add(0,-1,0).getBlock().getType() == Material.BEACON) {
            Location l = to.clone().add(0,-1,0);
            if(l.getBlock().equals(loc1.getBlock()))
                p.teleport(loc2.clone().add(0,2,0));
            if(l.getBlock().equals(loc2.getBlock()))
                p.teleport(loc1.clone().add(0,2,0));
            canTeleport = false;
            p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
            new BukkitRunnable() {
                @Override
                public void run() {
                    canTeleport = true;
                }
            }.runTaskLater(main, 80L);

        }
    }

    public void placeTeleport(Location loc) {
        if(main.getAbilityManager().onCooldown(p, ChatColor.GREEN + "Teleporter"))
            return;
        Material wool = (main.getTeams().blueHas(p)) ? Material.BLUE_WOOL : Material.RED_WOOL;
        if(loc1 == null) {
            loc1 = loc;
            p.sendMessage(main.getPrefix() + "Teleporter 1 placed!");
            Bukkit.broadcastMessage(loc1.toString());
        } else if(loc2 == null) {
            loc2 = loc;
            p.sendMessage(main.getPrefix() + "Teleporter 2 placed!");
            Bukkit.broadcastMessage(loc2.toString());
        }
        for(int i = -1; i < 2; i++) {
            for(int j = -1; j < 2; j++) {
                loc.getWorld().getBlockAt(loc).getLocation().add(i,-1,j).getBlock().setType(Material.IRON_BLOCK);
            }
        }
        if(team)
            loc.getWorld().getBlockAt(loc).getLocation().clone().add(0,1,0).getBlock().setType(Material.RED_STAINED_GLASS_PANE);
        else
            loc.getWorld().getBlockAt(loc).getLocation().clone().add(0,1,0).getBlock().setType(Material.BLUE_STAINED_GLASS_PANE);
    }

    public void turret() {
        CraftWorld w = (CraftWorld) p.getLocation().getWorld();
        SnowmanTurret s = new SnowmanTurret(EntityType.SNOW_GOLEM, w.getHandle(), p.getLocation());
        Snowman s1 = (Snowman) s.getBukkitEntity();
        s1.setDerp(true);
    }


    public class SnowmanTurret extends SnowGolem {

        public SnowmanTurret(EntityType<? extends SnowGolem> entitytypes, Level world, Location loc) {
            super((EntityType)entitytypes, world);

            this.setPos(loc.getX(), loc.getY(), loc.getZ());
            world.addFreshEntity(this);
            Snowman s = (Snowman) this.getBukkitEntity();
            s.setHealth(s.getMaxHealth());
            s.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 99999999, 2));
            Bukkit.broadcastMessage("hi");
        }

        protected void registerGoals() {
            this.goalSelector.addGoal(1, (Goal)new RangedAttackGoal(this, 1.25D, 1, 30.0F));
            this.goalSelector.addGoal(2, (Goal)new WaterAvoidingRandomStrollGoal(this, 1.0D, 1.0000001E-5F));
            this.goalSelector.addGoal(3, (Goal)new LookAtPlayerGoal((Mob)this, net.minecraft.world.entity.animal.Pig.class, 6.0F));
            this.goalSelector.addGoal(4, (Goal)new RandomLookAroundGoal((Mob)this));
            this.targetSelector.addGoal(1, (Goal)new NearestAttackableTargetGoal((Mob)this, net.minecraft.world.entity.player.Player.class, 30, true, false, entityliving -> entityliving instanceof net.minecraft.world.entity.player.Player));
            Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0D).add(Attributes.MOVEMENT_SPEED, 0D);
        }


        public void performRangedAttack(LivingEntity entityliving, float f) {
            Snowball entitysnowball = new Snowball(this.level, (LivingEntity)this);
            double d0 = entityliving.getEyeY() - 1.100000023841858D;
            double d1 = entityliving.getX() - getX();
            double d2 = d0 - entitysnowball.getY();
            double d3 = entityliving.getZ() - getZ();
            double d4 = Math.sqrt(d1 * d1 + d3 * d3) * 0.20000000298023224D;
            entitysnowball.shoot(d1, d2 + d4, d3, 1.6F, 12.0F);
            playSound(SoundEvents.SNOW_GOLEM_SHOOT, 1.0F, 0.4F / (getRandom().nextFloat() * 0.4F + 0.8F));
            this.level.addFreshEntity((net.minecraft.world.entity.Entity) entitysnowball);
        }

        public void aiStep() {
            super.aiStep();
            Snowman s = (Snowman) this.getBukkitEntity();
            HashMap<Double, Player> dubs = new HashMap<Double, Player>();
            for(org.bukkit.entity.Entity e : s.getNearbyEntities(20, 20, 20)) {
                if(e instanceof Player) {
                    Player p = (Player) e;
                    if(p.getGameMode() == GameMode.SPECTATOR)
                        continue;
                    for(Spy spy : main.getKits().getSpies())
                        if(spy.has(p))
                            if(!p.hasPotionEffect(PotionEffectType.GLOWING)) {
                                dubs.put(s.getLocation().distance(p.getLocation()), p);
                                continue;
                            }
                    if(main.getTeams().redHas(p) && team)
                        continue;
                    if(main.getTeams().blueHas(p) && !team)
                        continue;
                    dubs.put(s.getLocation().distance(p.getLocation()), p);
                }
            }
            if(dubs.keySet().isEmpty())
                return;
            double min = Collections.min(dubs.keySet());
            Player target = dubs.get(min);
            s.setTarget(target);
        }

    }

}

*/