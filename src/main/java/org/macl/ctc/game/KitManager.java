package org.macl.ctc.game;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.macl.ctc.Main;
import org.macl.ctc.events.DefaultListener;
import org.macl.ctc.kits.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class KitManager implements Listener {
    Main main;
    //public ArrayList<Kit> kits = new ArrayList<>();
    public HashMap<UUID, Kit> kits = new HashMap<UUID, Kit>();
    public KitManager(Main main) {
        this.main = main;
        main.listens.add(this);
    }

    public KitMenu getMenu() {
        KitMenu menu = new KitMenu(main.prefix + "Kit Menu", 18);
        List<String> gLore = createLore(ChatColor.DARK_PURPLE + "Cane: Knockback 10",
                ChatColor.LIGHT_PURPLE + "Cookies: Right click player to heal",
                ChatColor.RED + "No speed");

        menu.setItem(0, ChatColor.LIGHT_PURPLE + "GRANDMA", Enchantment.KNOCKBACK, gLore, Material.STICK);

        List<String> snowLore = createLore(ChatColor.BLUE + "Rocket Jump: Right click to fly",
                ChatColor.DARK_BLUE + "Snowball Launcher: Right click for snowballs",
                ChatColor.WHITE + "Speed: II");

        menu.setItem(1, ChatColor.AQUA + "SNOWBALLER", Enchantment.PROTECTION_FALL, snowLore, Material.SNOWBALL);


        List<String> demoLore = createLore(ChatColor.RED + "Egg Grenade",
                ChatColor.GRAY + "Mine",
                ChatColor.BOLD +"" + ChatColor.DARK_RED + "Sheep Bomb: Heat seeking sheep");

        menu.setItem(2, ChatColor.DARK_RED + "DEMOLITIONIST", Enchantment.PROTECTION_EXPLOSIONS, demoLore, Material.TNT);

        List<String> buildLore = createLore(ChatColor.DARK_BLUE + "Hammer: Right click build menu",
                ChatColor.DARK_GRAY + "Shears",
                ChatColor.GREEN + "Extra Wool!");

        menu.setItem(3, ChatColor.DARK_AQUA + "BUILDER", Enchantment.DIG_SPEED, buildLore, Material.GRASS_BLOCK);

        List<String> spyLore = createLore(ChatColor.GREEN + "Poison Dagger",
                ChatColor.WHITE + "Invisibility",
                ChatColor.RED + "Can't mine core");

        menu.setItem(4, ChatColor.WHITE + "SPY", Enchantment.ARROW_INFINITE, spyLore, Material.IRON_HOE);

        List<String> runnerLore = createLore(ChatColor.BLUE + "Runner",
                ChatColor.GOLD + "Block Run",
                ChatColor.WHITE + "Polar Deflection Field");

        menu.setItem(5, ChatColor.BLUE + "Runner", Enchantment.FROST_WALKER, runnerLore, Material.LEATHER_BOOTS);

        List<String> tankLore = createLore(ChatColor.DARK_GREEN + "Gatling Mode",
                ChatColor.RED + "Hellfire Missile",
                ChatColor.DARK_AQUA + "Shield");

        menu.setItem(6, ChatColor.GRAY + "Tank", Enchantment.PROTECTION_ENVIRONMENTAL, tankLore, Material.IRON_BLOCK);

        List<String> fishLore = createLore(ChatColor.GOLD + "Fishing rod",
                ChatColor.YELLOW + "Pufferfish Bomb",
                ChatColor.LIGHT_PURPLE + "Cod Sniper");

        menu.setItem(7, ChatColor.DARK_AQUA + "Fisherman", Enchantment.LUCK, fishLore, Material.FISHING_ROD);

        List<String> grandLore = createLore(ChatColor.GRAY + "Double barreled slug shotgun",
                ChatColor.YELLOW + "Booze: Increased damage and speed at a cost",
                ChatColor.RED + "No speed");

        menu.setItem(8, ChatColor.DARK_GRAY + "Grandpa", Enchantment.VANISHING_CURSE, grandLore, Material.HONEY_BOTTLE);


        return menu;
    }

    public void openMenu(Player p) {
        p.openInventory(getMenu().getKitMenu());
    }

    public List<String> createLore(String s1, String s2, String s3) {
        List<String> lore = new ArrayList<String>();
        lore.add(s1);
        lore.add(s2);
        lore.add(s3);
        return lore;
    }

    @EventHandler
    public void close(InventoryCloseEvent event) {
        Player p = (Player) event.getPlayer();
        if(kits.get(p.getUniqueId()) == null && main.game.started && (main.game.redHas(p) || main.game.blueHas(p)) && event.getView().getTitle().equalsIgnoreCase(main.prefix + "Kit Menu"))
            kits.put(p.getUniqueId(), new Snowballer(main, p, KitType.SNOWBALLER));
    }

    @EventHandler
    public void click(InventoryClickEvent event) {
        Player p = (Player) event.getWhoClicked();
        InventoryView view = event.getView();
        ItemStack click = event.getCurrentItem();
        //if(main.game.started != true)
          //  return;

        if(main.game.started)
            event.setCancelled(true);
        if(view.getTitle().equals(main.prefix + "Kit Menu")) {
            if(click == null)
                return;
            switch(click.getType()) {
                case SNOWBALL:
                    kits.put(p.getUniqueId(), new Snowballer(main, p, KitType.SNOWBALLER));
                    break;
                case STICK:
                    kits.put(p.getUniqueId(), new Grandma(main, p, KitType.GRANDMA));
                    break;
                case IRON_HOE:
                    kits.put(p.getUniqueId(), new Spy(main, p, KitType.SPY));
                    break;
                case TNT:
                    kits.put(p.getUniqueId(), new Demolitionist(main, p, KitType.DEMOLITIONIST));
                    break;
                case GRASS_BLOCK:
                    kits.put(p.getUniqueId(), new Builder(main, p, KitType.BUILDER));
                    break;
                case LEATHER_BOOTS:
                    kits.put(p.getUniqueId(), new Runner(main, p, KitType.RUNNER));
                    break;
                case IRON_BLOCK:
                    kits.put(p.getUniqueId(), new Tank(main, p, KitType.TANK));
                    break;
                case FISHING_ROD:
                    kits.put(p.getUniqueId(), new Fisherman(main, p, KitType.FISHERMAN));
                    break;
                case HONEY_BOTTLE:
                    kits.put(p.getUniqueId(), new Grandpa(main, p, KitType.GRANDPA));
                    break;
                default:
                    break;
            }
            event.setCancelled(true);
            p.closeInventory();
        }
        if(view.getTitle().equalsIgnoreCase(main.prefix + "BuildTools")) {
            if(main.getKits().get(p.getUniqueId()) != null && main.getKits().get(p.getUniqueId()) instanceof Builder) {
                Builder b = (Builder) main.getKits().get(p.getUniqueId());
                switch (click.getType()) {
                    case OAK_SLAB:
                        b.bridge();
                        break;
                    case OAK_STAIRS:
                        b.stairs();
                        break;
                    case LADDER:
                        b.tower();
                        break;
                    default:
                        event.setCancelled(true);
                        p.closeInventory();
                        break;
                }
                event.setCancelled(true);
                p.closeInventory();
            }
        }
        /*if(getMain().getGames().getState() == GameState.STARTED)
            event.setCancelled(true);
        if(click == null)
            return;
        if(view.getTitle().equalsIgnoreCase(getMain().getPrefix() + "Kit Menu")) {
            if(kitHas(p)) {
                p.closeInventory();
                event.setCancelled(true);
                return;
            }
            switch(click.getType()) {
                case SNOWBALL:
                    Snowballer b = new Snowballer(getMain(), p);
                    snowballers.add(b);
                    Bukkit.broadcastMessage("snowball");
                    break;
                case STICK:
                    Grandma g = new Grandma(getMain(), p);
                    grandmas.add(g);
                    Bukkit.broadcastMessage("grandma");
                    break;
                case TNT:
                    Demolitionist demo = new Demolitionist(getMain(), p);
                    demos.add(demo);
                    break;
                case GRASS_BLOCK:
                    Builder build = new Builder(getMain(), p);
                    builders.add(build);
                    break;
                case IRON_HOE:
                    Spy spy = new Spy(getMain(), p);
                    spies.add(spy);
                    break;
                default:
                    event.setCancelled(true);
                    p.closeInventory();
                    break;
            }
            p.closeInventory();
            event.setCancelled(true);
        }*/
    }

    public void remove(Player p) {
        if(kits.get(p.getUniqueId()) != null) {
            main.broadcast("remove");
            kits.remove(p.getUniqueId());
        }
    }

    class KitMenu {
        Inventory e;

        public KitMenu(String name, int size) {
            e = Bukkit.createInventory(null, size, name);
        }

        public void setItem(int slot, String name, Enchantment enchantment, List<String> lore, Material m) {
            ItemStack st = new ItemStack(m);
            ItemMeta stmeta = st.getItemMeta();
            stmeta.setLore(lore);
            stmeta.setDisplayName(name);
            st.setItemMeta(stmeta);
            st.addUnsafeEnchantment(enchantment, 1);
            e.setItem(slot, st);
        }

        public Inventory getKitMenu() {
            return e;
        }
    }

    //COMBINE 2 METHOD
}
