package org.macl.ctc.kits;

import net.minecraft.world.entity.Entity;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.macl.ctc.Main;

public class Demolitionist extends Kit {

    public boolean canEgg = true;

    int eggTime = 0;
    int sheepTime = 0;

    public ItemStack sheepItem = newItem(Material.CARROT_ON_A_STICK, ChatColor.DARK_RED +"" + ChatColor.BOLD+ "Sheep Launcher");

    public Demolitionist(Main main, Player p, KitType type) {
        super(main, p, type);
        CraftPlayer craft = (CraftPlayer) p;
        PlayerInventory e = p.getInventory();
        e.addItem(newItem(Material.STONE_SHOVEL, ChatColor.MAGIC + "OEIHRIOQW"));
        e.setHelmet(newItem(Material.CHAINMAIL_HELMET, ""));
        e.setChestplate(newItem(Material.CHAINMAIL_CHESTPLATE, ""));
        e.setBoots(newItem(Material.CHAINMAIL_BOOTS, ""));
        e.addItem(newItem(Material.EGG, ChatColor.RED + "Egg Grenade", 3));
        e.addItem(newItem(Material.STONE_PRESSURE_PLATE, ChatColor.GRAY + "Mine", 3));
        e.addItem(sheepItem);
        new eggReplenish(this, p, main).runTaskTimer(main, 0L, 20L);
        giveWool();

        setHearts(24);
    }

    public void launchSheep() {
        if(isOnCooldown("sheep"))
            return;
        Sheep g = (Sheep) p.getLocation().getWorld().spawnEntity(p.getLocation(), EntityType.SHEEP);
        g.setVelocity(p.getLocation().getDirection().multiply(1.35f));
        g.setBaby();
        g.setInvulnerable(true);
        new sheepLaunch(g).runTaskTimer(main, 0L, 1L);
        setCooldown("sheep", 25, Sound.ENTITY_TNT_PRIMED, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
    }

    public class sheepLaunch extends BukkitRunnable {
        int timer = 0;

        Sheep sheep;

        net.minecraft.world.entity.animal.Sheep shep;

        public sheepLaunch(Sheep sheep) {
            this.sheep = sheep;
            Entity entitySheep = ((CraftEntity)sheep).getHandle();
            shep = (net.minecraft.world.entity.animal.Sheep) entitySheep;
        }

        public void run() {
            timer++;
            for(org.bukkit.entity.Entity e : sheep.getNearbyEntities(50, 50, 50)) {
                if(e instanceof Player) {
                    Player p1 = (Player) e;

                    if(main.game.redHas(p1) && main.game.blueHas(p))
                        shep.getNavigation().moveTo(p1.getLocation().getX(), p1.getLocation().getY(), p1.getLocation().getZ(), 1.75f);
                    if(main.game.blueHas(p1) && main.game.redHas(p))
                        shep.getNavigation().moveTo(p1.getLocation().getX(), p1.getLocation().getY(), p1.getLocation().getZ(), 1.75f);
                }
            }

            if(timer == 20)
                sheep.setColor(DyeColor.YELLOW);
            if(timer == 40)
                sheep.setColor(DyeColor.ORANGE);
            if(timer == 60) {
                sheep.setColor(DyeColor.RED);
                sheep.setAdult();
            }
            sheep.getLocation().getWorld().playSound(sheep.getLocation(), Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 1, (0.02f*timer));
            if(timer == 80)
                sheep.setColor(DyeColor.BLACK);
            if(timer == 100) {
                main.fakeExplode(sheep.getLocation(), 20, 8);
                sheep.setHealth(0);
                this.cancel();
            }
        }
    }


    public class eggReplenish extends BukkitRunnable {


        Main main;

        Demolitionist d;

        Player p;

        public eggReplenish(Demolitionist d, Player p, Main main) {
            this.p = p;
            this.main = main;
            this.d = d;
        }

        @Override
        public void run() {
            if(p == null || p.isDead() || main.getKits().get(p.getUniqueId()) == null || !(main.getKits().get(p.getUniqueId()) instanceof Demolitionist)) {
                this.cancel();
                return;
            }
            eggTime++;

            int m = p.getInventory().first(Material.EGG);
            ItemStack stack = p.getInventory().getItem(m);
            if(eggTime == 30) {
                assert stack != null;
                if (stack.getAmount() < 4) {
                    addEgg();
                    eggTime = 0;
                }
            }
        }

        public void addEgg() {
            p.getInventory().addItem(newItem(Material.EGG, ChatColor.RED + "Egg Grenade"));
        }

    }
}