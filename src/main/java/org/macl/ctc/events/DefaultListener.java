package org.macl.ctc.events;

import org.bukkit.event.Listener;
import org.macl.ctc.Main;
import org.macl.ctc.game.GameManager;
import org.macl.ctc.game.KitManager;
import org.macl.ctc.game.WorldManager;

public class DefaultListener implements Listener {
    Main main;
    GameManager game;
    WorldManager world;
    KitManager kit;
    public DefaultListener(Main main) {
        this.main = main;
        this.game = main.game;
        this.world = main.worldManager;
        this.kit = main.kit;
        main.listens.add(this);
    }
}
