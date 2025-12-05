package org.macl.ctc.commands;

import net.md_5.bungee.api.chat.*;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.macl.ctc.Main;
import org.macl.ctc.game.StatsManager;

public class StatsCommand implements CommandExecutor {

    private final Main plugin;
    private final String serverIp = "playctc.us"; // ← your actual IP

    public StatsCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Only players can run that.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reset")) {
            plugin.getStats().resetPlayer(p);
            p.sendMessage(ChatColor.GREEN + "Your stats have been reset!");
            return true;
        }

        StatsManager sm = this.getStats();
        StatsManager.PlayerStats ps = sm.get(p.getUniqueId());

        double kdRatio = ps.deaths() == 0
                ? ps.kills()
                : ((double) ps.kills() / ps.deaths());

        // Header
        p.sendMessage(ChatColor.GOLD + "====== "
                + ChatColor.YELLOW + "[ CTC Stats ]"
                + ChatColor.GOLD + " ======");

        // Stats lines
        p.sendMessage(formatLine("Kills",        ps.kills()));
        p.sendMessage(formatLine("Deaths",       ps.deaths()));
        p.sendMessage(formatLine("K/D Ratio",    String.format("%.2f", kdRatio)));
        p.sendMessage(formatLine("Wins",         ps.wins()));
        p.sendMessage(formatLine("Captures",     ps.captures()));
        p.sendMessage(formatLine("Core Cracks", ps.coreCracks()));
        p.sendMessage(formatLine("Games Played", ps.gamesPlayed()));
        p.sendMessage(formatLine("Damage Dealt", ps.damageDealt()));
        p.sendMessage(formatLine("Damage Taken", ps.damageTaken()));

        // Footer
        p.sendMessage(ChatColor.GOLD + "==========================");

        // Clickable IP
        TextComponent ipLine = new TextComponent("▶ ");
        ipLine.setColor(net.md_5.bungee.api.ChatColor.AQUA);

        TextComponent ipText = new TextComponent(serverIp);
        ipText.setColor(net.md_5.bungee.api.ChatColor.WHITE);
        ipText.setBold(true);
        ipText.setHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("Click to copy server IP")
                        .color(net.md_5.bungee.api.ChatColor.GRAY).create()
        ));
        ipText.setClickEvent(new ClickEvent(
                ClickEvent.Action.COPY_TO_CLIPBOARD,
                serverIp
        ));

        ipLine.addExtra(ipText);
        p.spigot().sendMessage(ipLine);

        return true;
    }

    private StatsManager getStats() {
        return plugin.stats;
    }

    private String formatLine(String label, int value) {
        return ChatColor.YELLOW
                + String.format("%-14s", label + ":")
                + ChatColor.WHITE
                + value;
    }
    private String formatLine(String label, String value) {
        return ChatColor.YELLOW
                + String.format("%-14s", label + ":")
                + ChatColor.WHITE
                + value;
    }
}
