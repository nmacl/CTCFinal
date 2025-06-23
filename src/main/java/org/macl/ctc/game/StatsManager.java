package org.macl.ctc.game;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.UnaryOperator;

public class StatsManager {
    public record PlayerStats(
            int kills,
            int deaths,
            int wins,
            int captures,
            int coreCracks,
            int gamesPlayed
    ) {}

    private final ConcurrentMap<UUID,PlayerStats> stats = new ConcurrentHashMap<>();

    // ── Core recorders ───────────────────────────────────────────────────────
    public void recordKill(UUID id)      { modify(id, old -> new PlayerStats(old.kills()+1, old.deaths(), old.wins(), old.captures(), old.coreCracks(), old.gamesPlayed())); }
    public void recordDeath(UUID id)     { modify(id, old -> new PlayerStats(old.kills(), old.deaths()+1, old.wins(), old.captures(), old.coreCracks(), old.gamesPlayed())); }
    public void recordWin(UUID id)       { modify(id, old -> new PlayerStats(old.kills(), old.deaths(), old.wins()+1, old.captures(), old.coreCracks(), old.gamesPlayed())); }
    public void recordCapture(UUID id)   { modify(id, old -> new PlayerStats(old.kills(), old.deaths(), old.wins(), old.captures()+1, old.coreCracks(), old.gamesPlayed())); }
    public void recordCoreCrack(UUID id) { modify(id, old -> new PlayerStats(old.kills(), old.deaths(), old.wins(), old.captures(), old.coreCracks()+1, old.gamesPlayed())); }
    public void recordGamePlayed(UUID id){ modify(id, old -> new PlayerStats(old.kills(), old.deaths(), old.wins(), old.captures(), old.coreCracks(), old.gamesPlayed()+1)); }

    // ── Convenience overloads ────────────────────────────────────────────────
    public void recordKill(Player p)      { recordKill(p.getUniqueId()); }
    public void recordDeath(Player p)     { recordDeath(p.getUniqueId()); }
    public void recordWin(Player p)       { recordWin(p.getUniqueId()); }
    public void recordCapture(Player p)   { recordCapture(p.getUniqueId()); }
    public void recordCoreCrack(Player p) { recordCoreCrack(p.getUniqueId()); }
    public void recordGamePlayed(Player p){ recordGamePlayed(p.getUniqueId()); }

    /** Reset this player’s stats entirely */
    public void resetPlayer(UUID id)      { stats.remove(id); }
    public void resetPlayer(Player p)     { resetPlayer(p.getUniqueId()); }

    private void modify(UUID id, UnaryOperator<PlayerStats> op) {
        stats.compute(id, (__, old) -> {
            PlayerStats base = old==null
                    ? new PlayerStats(0,0,0,0,0,0)
                    : old;
            return op.apply(base);
        });
    }

    public PlayerStats get(UUID id) {
        return stats.getOrDefault(id, new PlayerStats(0,0,0,0,0,0));
    }

    // ── Persistence ─────────────────────────────────────────────────────────
    public void loadAll(FileConfiguration cfg) {
        if (!cfg.isConfigurationSection("stats")) return;
        for (String key : cfg.getConfigurationSection("stats").getKeys(false)) {
            UUID id = UUID.fromString(key);
            String base = "stats."+key+".";
            stats.put(id, new PlayerStats(
                    cfg.getInt(base+"kills",0),
                    cfg.getInt(base+"deaths",0),
                    cfg.getInt(base+"wins",0),
                    cfg.getInt(base+"captures",0),
                    cfg.getInt(base+"coreCracks",0),
                    cfg.getInt(base+"gamesPlayed",0)
            ));
        }
    }

    public void saveAll(FileConfiguration cfg) {
        for (var e : stats.entrySet()) {
            String base = "stats."+e.getKey()+".";
            PlayerStats ps = e.getValue();
            cfg.set(base+"kills",       ps.kills());
            cfg.set(base+"deaths",      ps.deaths());
            cfg.set(base+"wins",        ps.wins());
            cfg.set(base+"captures",    ps.captures());
            cfg.set(base+"coreCracks",  ps.coreCracks());
            cfg.set(base+"gamesPlayed", ps.gamesPlayed());
        }
    }
}
