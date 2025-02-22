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
import org.bukkit.craftbukkit.v1_20_R3.CraftWorld;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowman;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.macl.ctc.Main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class Engineer extends Kit {
    private Location loc1 = null;
    private Location loc2 = null;
    private boolean canPlaceTeleport = true;
    private boolean canTeleport = true;

    public Engineer(Main main, Player p, KitType type) {
        super(main, p, type);
        setupInventory(p);
    }

    private void setupInventory(Player p) {
        PlayerInventory e = p.getInventory();
        ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta meta = (LeatherArmorMeta) chestplate.getItemMeta();
        if (meta != null) {
            meta.setColor(main.game.redHas(p) ? Color.RED : Color.BLUE);
            chestplate.setItemMeta(meta);
        }
        p.getInventory().setChestplate(chestplate);
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
        regenItem("turret", newItem(Material.DISPENSER, ChatColor.BOLD + "Snowman Turret"), 40, 2, 1);
        giveWool();
        giveWool();
    }

    public void turret(Location l) {
        CraftWorld w = (CraftWorld) p.getLocation().getWorld();
        SnowmanTurret s = new SnowmanTurret(EntityType.SNOW_GOLEM, w.getHandle(), p.getLocation());
        Snowman s1 = (Snowman) s.getBukkitEntity();
        s1.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 99999999, 3));
        s1.setDerp(true);
    }

    public void overload() {
        new BukkitRunnable() {

            int timer = 0;
            @Override
            public void run() {
                Location l = p.getLocation().add(0,2,0);
                World w = l.getWorld();
                Vector dir = p.getLocation().getDirection().multiply(1.8);
                timer++;
                if(timer > 120) {
                    this.cancel();
                }
                FallingBlock fire = p.getWorld().spawnFallingBlock(l, Material.FIRE.createBlockData());
                dir.multiply(Math.random()*0.4);
                FallingBlock web = p.getWorld().spawnFallingBlock(l, Material.COBWEB.createBlockData());
                fire.setVelocity(dir);
                web.setVelocity(dir);
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, p.getLocation().add(dir),3, 0.1,0.1,0.1);
                w.spawnParticle(Particle.FLAME, p.getLocation().add(dir),3, 0.1,0.1,0.1);
                p.playSound(p.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 3f, 3f);
            }
        }.runTaskTimer(main, 0L, 1L);
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
            Snowball entitysnowball = new Snowball(this.level(), (LivingEntity)this);
            double d0 = entityliving.getEyeY() - 1.100000023841858D;
            double d1 = entityliving.getX() - getX();
            double d2 = d0 - entitysnowball.getY();
            double d3 = entityliving.getZ() - getZ();
            double d4 = Math.sqrt(d1 * d1 + d3 * d3) * 0.20000000298023224D;
            entitysnowball.shoot(d1, d2 + d4, d3, 1.6F, 12.0F);
            playSound(SoundEvents.SNOW_GOLEM_SHOOT, 1.0F, 0.4F / (getRandom().nextFloat() * 0.4F + 0.8F));
            this.level().addFreshEntity((net.minecraft.world.entity.Entity) entitysnowball);
        }

        public void aiStep() {
            super.aiStep();
            Snowman s = (Snowman) this.getBukkitEntity();
            HashMap<Double, Player> dubs = new HashMap<Double, Player>();
            for(org.bukkit.entity.Entity e : s.getNearbyEntities(20, 20, 20)) {
                if(e instanceof Player) {
                    Player p1 = (Player) e;
                    if(p1.getGameMode() == GameMode.SPECTATOR)
                        continue;
                    /*for(Spy spy : main.getKits().getSpies())
                        if(spy.has(p))
                            if(!p.hasPotionEffect(PotionEffectType.GLOWING)) {
                                dubs.put(s.getLocation().distance(p.getLocation()), p);
                                continue;
                            }*/
                    if(main.game.redHas(p1) && main.game.redHas(p))
                        continue;
                    if(main.game.blueHas(p1) && main.game.blueHas(p))
                        continue;
                    dubs.put(s.getLocation().distance(p1.getLocation()), p1);
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

