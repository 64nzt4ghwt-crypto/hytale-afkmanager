package com.howlstudio.afkmanager;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public class AFKListener {
    private final AFKManager manager;
    public AFKListener(AFKManager manager) { this.manager = manager; }

    public void register() {
        HytaleServer.get().getEventBus().registerGlobal(PlayerReadyEvent.class, e -> {
            Player p = e.getPlayer();
            if (p == null) return;
            PlayerRef ref = p.getPlayerRef();
            if (ref == null) return;
            manager.onJoin(ref.getUuid(), ref.getUsername());
        });
        HytaleServer.get().getEventBus().registerGlobal(PlayerDisconnectEvent.class, e -> {
            PlayerRef ref = e.getPlayerRef();
            if (ref != null) manager.onLeave(ref.getUuid());
        });
    }
}
