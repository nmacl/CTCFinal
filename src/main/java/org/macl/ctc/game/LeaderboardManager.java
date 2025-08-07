package org.macl.ctc.game;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.macl.ctc.Main;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LeaderboardManager {
    private final Main plugin;
    private Hologram holo;

    public LeaderboardManager(Main plugin) {
        this.plugin = plugin;
    }

    public void init() {
        // 1) Grab your spawn location + an offset
        Location spawn = new Location(Bukkit.getWorld("world"), 51, 110, 38);

        // 2) Don't recreate on reload
        if (DHAPI.getHologram("ctc_leaderboard") != null) {
            holo = DHAPI.getHologram("ctc_leaderboard");
        } else {
            holo = DHAPI.createHologram("ctc_leaderboard", spawn);
        }

        update();
    }

    public void update() {
        var sm = plugin.getStats();
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());

        // 1) Compute top values and who holds them
        int bestKills  = online.stream().mapToInt(p -> sm.get(p.getUniqueId()).kills()).max().orElse(5);
        int bestDeaths = online.stream().mapToInt(p -> sm.get(p.getUniqueId()).deaths()).max().orElse(0);
        int bestCaps   = online.stream().mapToInt(p -> sm.get(p.getUniqueId()).captures()).max().orElse(0);
        int bestCracks = online.stream().mapToInt(p -> sm.get(p.getUniqueId()).coreCracks()).max().orElse(0);

        String topKillsNames  = online.stream()
                .filter(p -> sm.get(p.getUniqueId()).kills()   == bestKills)
                .map(Player::getName).collect(Collectors.joining(", "));
        String topDeathNames  = online.stream()
                .filter(p -> sm.get(p.getUniqueId()).deaths()  == bestDeaths)
                .map(Player::getName).collect(Collectors.joining(", "));
        String topCapsNames   = online.stream()
                .filter(p -> sm.get(p.getUniqueId()).captures()== bestCaps)
                .map(Player::getName).collect(Collectors.joining(", "));
        String topCracksNames = online.stream()
                .filter(p -> sm.get(p.getUniqueId()).coreCracks()== bestCracks)
                .map(Player::getName).collect(Collectors.joining(", "));

        // 2) Build nicely spaced & bolded lines
        List<String> lines = List.of(
                "&6&l╔══════════════════════╗",
                "&6&l║  CTC &e&lLeaderboard  &6&l║",
                "&6&l╚══════════════════════╝",
                "",

                "&c&lOffense &8| &f" + topKillsNames + " &7(" + bestKills  + ")",
                "",

                "&a&lDefense &8| &f" + topCapsNames   + " &7(" + bestCaps   + ")",
                "",

                "&5&lCore MVP &8| &f" + topCracksNames + " &7(" + bestCracks + ")",
                "",

                "&4&lLVP &8| &f" + topDeathNames      + " &7(" + bestDeaths + ")",
                "",

                "&7&lLegend:",
                "&7Offense = Kills   Defense = Captures",
                "&7Core MVP = Core Cracks   LVP = Deaths"
        );

        // 3) Clear existing lines
        int oldCount = holo.getPage(0).getLines().size();
        for (int i = 0; i < oldCount; i++) {
            DHAPI.removeHologramLine(holo, 0);
        }

        // 4) Add our new lines
        for (String raw : lines) {
            DHAPI.addHologramLine(holo, 0, raw);
        }
    }
}
