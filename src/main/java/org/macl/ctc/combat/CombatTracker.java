package org.macl.ctc.combat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.macl.ctc.Main;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Map.entry;

/**
 * Centralized combat attribution + custom death messages.
 *
 * Usage:
 *  combatTracker.setHealth(victim, newHealth, attacker, "Flamethrower");
 *  // Call combatTracker.onDeath(event) inside PlayerDeathEvent.
 */
public class CombatTracker {
    private final long hitExpiryMs;
    private final Map<UUID, CombatState> states = new ConcurrentHashMap<>();
    private final Set<UUID> alreadyHandled = ConcurrentHashMap.newKeySet();
    private final Main main;

    private final Map<String,String> ABILITY_MESSAGES = Map.ofEntries(
            entry("grenade", "%killer% blew up %victim% with a grenade"),
            entry("direct hit grenade", "%killer% nailed %victim% with a point-blank grenade"),
            entry("mine", "%killer%'s mine turned %victim% into confetti"),
            entry("hellfire", "%killer%'s hellfire missile erased %victim%"),
            entry("spy", "%killer% remotely detonated %victim%"),
            entry("sheep", "%killer%'s sheep bomb claimed %victim%"),
            entry("log", "%killer% splintered %victim% with a flying log"),
            entry("cane", "%killer% whacked %victim% into oblivion"),
            entry("scooter", "%victim% got run over by %killer%'s scooter"),
            entry("flamethrower", "%killer% roasted %victim% with a flamethrower"),
            entry("void bomb", "%killer% dropped a void nuke on %victim% "),
            entry("cod sniper", "%killer% sniped %victim% with a fish"),
            entry("Fireball", "%victim% ate a fireball from %killer%"),
            entry("Shulker Bullet", "%killer%'s pepper'd %victim% in the face"),
            entry("Snowball", "%killer% pummeled %victim% with snowballs"),
            entry("Spectral Arrow", "%victim% got frostbite from %killer%'s frozen arrow"),
            entry("Sword", "%victim% got slashed by %killer%"),
            entry("Shovel", "%killer% dug up %victim%"),
            entry("Hoe", "%killer% poisoned %victim% with a hoe")
    );

    private record Hit(UUID attackerId, String attackerName, String ability, double amount, long ts) {}

    private class CombatState {
        final Deque<Hit> hits = new ArrayDeque<>();
    }

    public CombatTracker(Main main) {
        this(main, 9_000L);
    }

    public CombatTracker(Main main, long hitExpiryMs) {
        this.main = main;
        this.hitExpiryMs = hitExpiryMs;
    }

    /**
     * Tag normal Bukkit damage (melee, arrows, etc.).
     * Call this from an EntityDamageByEntityEvent listener.
     */
    public void tagDamage(Player victim, double amount, @Nullable Player attacker, @Nullable String ability) {
        if (victim.isDead()) return;
        if (amount <= 0) return;
        tagHit(victim, attacker, ability == null ? "" : ability, amount);
    }


    public void setHealth(Player victim, double newHealth, @Nullable Player attacker, String ability) {
        if (victim.isDead()) return;

        double delta = victim.getHealth() - newHealth; // >0 damage, <0 heal
        if (delta > 0) {
            tagHit(victim, attacker, ability, delta);
        }

        if (newHealth <= 0) {
            victim.setHealth(0);   // This will fire PlayerDeathEvent
        } else {
            victim.setHealth(newHealth);
        }

        victim.setHealth(newHealth);
    }

    /** Call from PlayerDeathEvent listener. */
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();

        CombatState state = states.remove(victim.getUniqueId());
        Hit killerHit = resolveKiller(state, victim);

        recordStats(victim, killerHit);
        String message = buildDeathMessage(victim, killerHit,
                victim.getLastDamageCause() == null ? null : victim.getLastDamageCause().getCause());

        event.setDeathMessage("");
        victim.getWorld().getPlayers().forEach(p -> p.sendMessage(message));
        event.getDrops().clear();
    }

    // --- internals ---

    private void tagHit(Player victim, @Nullable Player attacker, String ability, double amount) {
        if (attacker == null || attacker.getUniqueId().equals(victim.getUniqueId())) return;
        CombatState cs = states.computeIfAbsent(victim.getUniqueId(), __ -> new CombatState());
        prune(cs);
        cs.hits.addLast(new Hit(
                attacker.getUniqueId(),
                attacker.getName(),
                ability,
                amount,
                System.currentTimeMillis()
        ));

        // Record damage stats
        int damageAmount = (int) Math.ceil(amount);
        main.getStats().recordDamage(attacker.getUniqueId(), damageAmount);
        main.getStats().recordDamageTaken(victim.getUniqueId(), damageAmount);
    }

    private void prune(CombatState cs) {
        if (cs == null) return;
        long now = System.currentTimeMillis();
        while (!cs.hits.isEmpty() && now - cs.hits.peekFirst().ts > hitExpiryMs) {
            cs.hits.removeFirst();
        }
    }

    /** Pick the latest valid hit; you can change this to "most damage in window" later if you want. */
    private Hit resolveKiller(CombatState cs, Player victim) {
        if (cs == null) return null;
        prune(cs);
        return cs.hits.peekLast();
    }

    private String colorName(Player p) {
        String teamColor = String.valueOf(main.game.redHas(p) ? ChatColor.RED : ChatColor.BLUE);
        return teamColor + p.getName() + ChatColor.GRAY;
    }


    private String buildDeathMessage(Player victim, Hit hit, EntityDamageEvent.DamageCause cause) {
        String v = colorName(victim);
        if (hit != null) {
            String k = colorName(Bukkit.getPlayer(hit.attackerName()));
            String template = ABILITY_MESSAGES.get(hit.ability());
            if (template != null) {
                return ChatColor.GRAY + template
                        .replace("%killer%", k)
                        .replace("%victim%", v);
            }
            String ability = hit.ability();
            if (ability != null && !ability.isEmpty()) {
                return k + " eliminated " + v + ChatColor.GRAY + " with " + ChatColor.AQUA + ability;
            }
            return k + ChatColor.GRAY + " eliminated " + v;
        }

        if (cause != null) {
            return switch (cause) {
                case VOID      -> ChatColor.YELLOW + v + ChatColor.GRAY + " fell into the void";
                case FALL      -> ChatColor.YELLOW + v + ChatColor.GRAY + " hit the ground too hard";
                case LAVA      -> ChatColor.YELLOW + v + ChatColor.GRAY + " tried to swim in lava";
                case DROWNING  -> ChatColor.YELLOW + v + ChatColor.GRAY + " forgot how to swim";
                case FIRE, FIRE_TICK -> ChatColor.YELLOW + v + ChatColor.GRAY + " was burned to a crisp";
                case BLOCK_EXPLOSION, ENTITY_EXPLOSION -> ChatColor.YELLOW + v + ChatColor.GRAY + " was blown up";
                case LIGHTNING -> ChatColor.YELLOW + v + ChatColor.GRAY + " was struck by lightning";
                default -> ChatColor.YELLOW + v + ChatColor.GRAY + " died";
            };
        }
        return ChatColor.YELLOW + v + ChatColor.GRAY + " died";
    }

    private void recordStats(Player victim, @Nullable Hit killerHit) {
        if (killerHit != null) {
            main.getStats().recordKill(killerHit.attackerId());

            // If you add a damage-tracking API, you can hook it here:
            // main.getStats().recordDamage(killerHit.attackerId(), victim.getUniqueId(), killerHit.amount());
        }
        main.getStats().recordDeath(victim);
    }

    private void broadcastDeath(Player victim, @Nullable Hit killerHit) {
        String msg = buildDeathMessage(victim, killerHit,
                victim.getLastDamageCause() == null ? null : victim.getLastDamageCause().getCause());
        victim.getWorld().getPlayers().forEach(p -> p.sendMessage(msg));
    }
}
