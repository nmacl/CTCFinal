package org.macl.ctc.kits;

import net.minecraft.world.entity.Entity;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftEntity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.macl.ctc.Main;
import org.macl.ctc.timers.cooldownTimer;

public class Demolitionist extends Kit {

    public boolean canEgg = true;

    int eggTime = 0;
    int sheepTime = 0;

    public ItemStack sheepItem = newItem(Material.CARROT_ON_A_STICK, ChatColor.DARK_RED +"" + ChatColor.BOLD+ "Sheep Launcher");

    public Demolitionist(Main main, Player p, KitType type) {
        super(main, p, type);
        //CraftPlayer craft = (CraftPlayer) p;
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
        cooldowns.put("sheep", false);
    }

    public void launchSheep() {
        if(cooldowns.get("sheep"))
            return;
        Sheep g = (Sheep) p.getLocation().getWorld().spawnEntity(p.getLocation(), EntityType.SHEEP);
        g.setVelocity(p.getLocation().getDirection().multiply(1.75f));
        g.setBaby();
        g.setInvulnerable(true);
        new sheepLaunch(g).runTaskTimer(main, 0L, 1L);
        new cooldownTimer(this, 20*25, "sheep", sheepItem).runTaskTimer(main, 0L, 1L);
        cooldowns.put("sheep", true);
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

                    if(main.game.redHas(p1) && main.game.getBlue().hasEntry(p.getName()))
                        shep.getNavigation().moveTo(p1.getLocation().getX(), p1.getLocation().getY(), p1.getLocation().getZ(), 1.67f);
                    if(main.game.blueHas(p1) && main.game.getRed().hasEntry(p.getName()))
                        shep.getNavigation().moveTo(p1.getLocation().getX(), p1.getLocation().getY(), p1.getLocation().getZ(), 1.67f);
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
                if(!sheep.isDead())
                    sheep.getLocation().getWorld().createExplosion(sheep.getLocation().add(0,1,0), 4f);
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
            if(p == null || p.isDead() || main.getKits().get(p.getUniqueId()) == null || main.getKits().get(p.getUniqueId()) instanceof Demolitionist ) {
                this.cancel();
                return;
            }
            eggTime++;


            if(eggTime == 30) {
                int first = p.getInventory().first(Material.EGG);

                if(first == -1) {
                    addEgg();
                    eggTime = 0;
                    return;
                }

                ItemStack m = p.getInventory().getItem(first);

                if(m.getAmount() < 3)
                    addEgg();
                eggTime = 0;
            }
        }

        public void addEgg() {
            p.getInventory().addItem(newItem(Material.EGG, ChatColor.RED + "Egg Grenade"));
        }

    }
}