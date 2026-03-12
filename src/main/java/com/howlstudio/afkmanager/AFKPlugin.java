package com.howlstudio.afkmanager;

import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

public final class AFKPlugin extends JavaPlugin {
    private AFKManager manager;
    public AFKPlugin(JavaPluginInit init) { super(init); }

    @Override
    protected void setup() {
        System.out.println("[AFKManager] Loading...");
        manager = new AFKManager();
        new AFKListener(manager).register();
        CommandManager cmd = CommandManager.get();
        cmd.register(manager.getAFKCommand());
        cmd.register(manager.getAFKListCommand());
        manager.startChecker();
        System.out.println("[AFKManager] Ready. AFK after 5m idle.");
    }

    @Override
    protected void shutdown() {
        if (manager != null) manager.stopChecker();
        System.out.println("[AFKManager] Stopped.");
    }
}
