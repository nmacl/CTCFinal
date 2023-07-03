package org.macl.ctc.kits;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.macl.ctc.Main;
import org.macl.ctc.timers.cooldownTimer;

import java.util.ArrayList;

public class Tank extends Kit {
    
    ItemStack shield = (main.game.redHas(p)) ? newItem(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "Shield") : newItem(Material.BLUE_STAINED_GLASS_PANE, ChatColor.BLUE + "Shield");
    ItemStack glass = (main.game.redHas(p)) ? new ItemStack(Material.RED_STAINED_GLASS_PANE) : new ItemStack(Material.BLUE_STAINED_GLASS_PANE);

    ItemStack gun = newItem(Material.NETHERITE_SHOVEL, ChatColor.GOLD + "Gatling Gun");

    boolean setup = false;
    boolean gatling = false;

    public Tank(Main main, Player p, KitType type) {
        super(main, p, type);
        p.getInventory().addItem(gun);
        p.getInventory().addItem(newItem(Material.FLINT_AND_STEEL, ChatColor.RED + "Hellfire Missle", 1));
        p.getInventory().addItem(glass);
        e.setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        e.setLeggings(newItem(Material.IRON_LEGGINGS, "piss pants"));
        e.setBoots(new ItemStack(Material.NETHERITE_BOOTS));
        giveWool();
        giveWool();
        p.removePotionEffect(PotionEffectType.SPEED);
        cooldowns.put("hellfire", false);
        cooldowns.put("gatling", false);
    }
    ArrayList<Location> locs = new ArrayList<>();

    public BlockFace[] axis = { BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST };


    public Location getBlockCenter(Location location) {
        double x = location.getBlockX() + 0.5;
        double y = location.getBlockY();
        double z = location.getBlockZ() + 0.5;
        return new Location(location.getWorld(), x, y, z);
    }

    public BlockFace yawToFace(float yaw) {
        return axis[Math.round(yaw / 90f) & 0x3];
    }
    public void gatlingSetup() {
        BlockFace face = yawToFace(p.getLocation().getYaw());

        Location bLoc = getBlockCenter(p.getLocation());

        bLoc.setPitch(p.getLocation().getPitch());
        bLoc.setYaw(p.getLocation().getYaw());



        new BukkitRunnable() {
            @Override
            public void run() {
                main.broadcast(face.toString());

                if (face == BlockFace.EAST || face == BlockFace.WEST) {
                    // z + -
                    locs.add(bLoc.clone().add(0, 1, 1));
                    locs.add(bLoc.clone().add(0, 1, -1));
                } else if (face == BlockFace.SOUTH || face == BlockFace.NORTH) {
                    // x + -
                    locs.add(bLoc.clone().add(1, 1, 0));
                    locs.add(bLoc.clone().add(-1, 1, 0));
                }
                locs.add(bLoc.clone().add(0,-1,0));

                locs.add(bLoc.clone().add(-1, 0, 0));
                locs.add(bLoc.clone().add(1, 0, 0));
                locs.add(bLoc.clone().add(0, 0, 1));
                locs.add(bLoc.clone().add(0, 0, -1));
                locs.add(bLoc.clone().add(0, 2, 0));

                for (Location loc : locs) {
                    loc.getBlock().setType(Material.IRON_BLOCK);
                }

                p.teleport(bLoc);
            }
        }.runTaskLater(main, 3L);



        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 999999999, 9999));
        e.clear();
        e.setItem(0, gun);
        e.setItem(1, newItem(Material.FLINT, ChatColor.RED + "EXIT"));
        e.setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        e.setLeggings(newItem(Material.IRON_LEGGINGS, "piss pants"));
        e.setBoots(new ItemStack(Material.NETHERITE_BOOTS));
        setup = true;
    }

    BukkitTask task = null;

    int usage = 0;

    public void gatling(BlockFace face) {
        if(cooldowns.get("gatling"))
            return;
        if(!setup) {
            if(locs != null) {
                for (Location loc : locs)
                    loc.getBlock().setType(Material.AIR);
                locs.clear();
            }
            gatlingSetup();
            // cool down
            // in gatling setup change inventory
        } else {
            if(gatling) {
                // gattling is already on
                task.cancel();
                gatling = false;
                return;
            } else {
                // gattling turn on
                gatling = true;



                task = new BukkitRunnable() {
                    int timer = 0;
                    @Override
                    public void run() {
                        usage++;
                        ItemStack item = e.getItem(0);
                        int dmg = usage*20;

                        if(dmg >= item.getType().getMaxDurability()) {
                            usage = 0;
                            exit();
                            this.cancel();
                            return;
                        }

                        if(gatling == false || item == null ) {
                            this.cancel();
                            return;
                        }

                        ItemMeta itemMeta = item.getItemMeta();
                        Damageable damage = (Damageable) itemMeta;


                        if (itemMeta instanceof Damageable){
                            if(dmg != item.getType().getMaxDurability())
                                damage.setDamage(dmg);
                        }


                        item.setItemMeta(itemMeta);



                        p.launchProjectile(Arrow.class);
                        // if over heat cancel
                        p.getWorld().playSound(p.getLocation(), Sound.ITEM_FLINTANDSTEEL_USE, 1f,0.1f);
                        timer++;
                    }
                }.runTaskTimer(main, 0L, 1L);
            }
        }

    }

    public boolean shieldOn = false;


    public void shield(Block placedBlock, BlockFace playerFacing) {
        if(shieldOn == true) return;
        shieldOn = true;



        // cool down
        ArrayList<Block> blocks = new ArrayList<>();



        if(playerFacing == BlockFace.NORTH || playerFacing == BlockFace.SOUTH) {
            // Place east to west (+x -x)
            for(int x = -3; x < 4; x++) {
                for(int y = 0; y < 3; y++) {
                    Block b = placedBlock.getWorld().getBlockAt(placedBlock.getX() + x, placedBlock.getY() + y, placedBlock.getZ());
                    if(b.getType() == Material.AIR)
                        b.setType(glass.getType());
                    blocks.add(b);
                }
            }
        } else if(playerFacing == BlockFace.EAST || playerFacing == BlockFace.WEST) {
            for(int z = -3; z < 4; z++) {
                for(int y = 0; y < 3; y++) {
                    Block b = placedBlock.getWorld().getBlockAt(placedBlock.getX(), placedBlock.getY() + y, placedBlock.getZ() + z);
                    if(b.getType() == Material.AIR)
                        b.setType(glass.getType());
                    blocks.add(b);
                }
            }
        }

        p.getWorld().playSound(placedBlock.getLocation(), Sound.ENTITY_IRON_GOLEM_REPAIR, 1f, 1f);

        // kinda icky but it works. 10 second shield 20 second give back

        new BukkitRunnable() {
            @Override
            public void run() {
                for(Block b : blocks)
                    b.setType(Material.AIR);
                p.playSound(p.getLocation(), Sound.BLOCK_METAL_PLACE, 1f, 1f);
            }
        }.runTaskLater(main, 20*10);

        new BukkitRunnable() {
            @Override
            public void run() {
                e.setItem(2, shield);
                shieldOn = false;
            }
        }.runTaskLater(main, 20*20);

    }

    int time = 0;

    boolean inHellfire = false;
    Location previousLoc = null;

    int count = 0;
    public void hellfire() {
        if(inHellfire || cooldowns.get("hellfire"))
            return;
        new BukkitRunnable() {
            @Override
            public void run() {
                count++;
                if(count == 15) {
                    this.cancel();
                    count = 0;
                }
                p.getWorld().spawnParticle(Particle.DRIP_LAVA, p.getLocation(), 20);
            }
        }.runTaskTimer(main, 0L, 1L);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 5f, 1f);
        new cooldownTimer(this, 20*20, "hellfire", p.getInventory().getItem(1)).runTaskTimer(main, 0L, 1L);
        inHellfire = true;
        previousLoc = p.getLocation();

        Location ps = new Location(p.getWorld(), p.getLocation().getX(), p.getLocation().getY() + 150, p.getLocation().getZ());

        new BukkitRunnable() {
            @Override
            public void run() {
                p.teleport(ps, PlayerTeleportEvent.TeleportCause.PLUGIN);
                p.setRotation(0, 90);
                p.setInvulnerable(true);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        //add checks
                        if(p.isDead()) {
                            this.cancel();
                            return;
                        }
                        time++;
                        if(time > 20*25 || (p.getFallDistance() == 0 && time > 80) ) {
                            p.getWorld().createExplosion(p.getLocation(), 2f, true);
                            p.setInvulnerable(false);
                            p.teleport(previousLoc);
                            previousLoc = null;
                            this.cancel();
                            time = 0;
                            inHellfire = false;
                            return;
                        }
                        p.getWorld().spawnParticle(Particle.FLAME, p.getLocation(), 10);
                        p.setVelocity(p.getLocation().getDirection().multiply(1.9));
                        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_SHOOT, 5f, 0.5f);
                    }
                }.runTaskTimer(main, 0L, 1L);
            }
        }.runTaskLater(main, 8L);

    }

    public void exit() {
        new cooldownTimer(this, 20*10, "gatling", p.getInventory().getItem(0)).runTaskTimer(main, 0L, 1L);

        new BukkitRunnable() {
            @Override
            public void run() {
                p.removePotionEffect(PotionEffectType.SLOW);
                p.playSound(p.getLocation(), Sound.ENTITY_IRON_GOLEM_DEATH, 1f, 1f);
                e.clear();
                for(Location loc : locs)
                    loc.getBlock().setType(Material.AIR);
                p.getLocation().add(0,-1,0).getBlock().setType(Material.STONE);
                p.getInventory().addItem(gun);
                p.getInventory().addItem(newItem(Material.FLINT_AND_STEEL, ChatColor.RED + "Hellfire Missle", 1));
                if(!shieldOn)
                    p.getInventory().addItem(glass);
                e.setItem(3, new ItemStack(wool, 64));
                e.setItem(4, new ItemStack(wool, 64));
                e.setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
                e.setLeggings(newItem(Material.IRON_LEGGINGS, "piss pants"));
                e.setBoots(new ItemStack(Material.NETHERITE_BOOTS));
                setup = false;
                gatling = false;
                usage = 0;


            }
        }.runTaskLater(main, 2L);
    }
}