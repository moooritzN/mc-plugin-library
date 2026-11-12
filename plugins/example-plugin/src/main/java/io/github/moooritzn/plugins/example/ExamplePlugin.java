package io.github.moooritzn.plugins.example;

import io.github.moooritzn.mclib.plugin.BasePlugin;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

public final class ExamplePlugin extends BasePlugin {

    @Override
    public void onEnable() {
        registerListeners(new JoinListener());

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register("hello", "Sagt Hallo", new HelloCommand()));

        getSLF4JLogger().info("ExamplePlugin aktiviert");
    }
}
