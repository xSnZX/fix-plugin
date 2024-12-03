package me.developer.chronocore;

import lombok.Getter;
import me.developer.chronocore.Commands.InfoCommand;
import me.developer.chronocore.Commands.ReloadConfigCommand;
import me.developer.chronocore.Commands.TimeLeftCommand;
import me.developer.chronocore.Events.*;
import me.developer.chronocore.Recipe.ReviveRecipe;
import me.developer.chronocore.Utils.PlayerDataManager;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ChronoCore extends JavaPlugin {

    @Getter
    private static ChronoCore instance;
    private @Getter Map<UUID, Long> joinTimes = new HashMap<>();
    private @Getter Map<UUID, Long> joinTimesFixed = new HashMap<>();
    public String prefix = this.getConfig().getString("prefix");
    private @Getter PlayerDataManager playerDataManager;

    @Override
    public void onEnable() {
        getLogger().info("Plugin has been Enabled.");

        getConfig().options().copyDefaults(true);
        saveDefaultConfig();
        reloadConfig();

        this.playerDataManager = new PlayerDataManager(this);
        ReviveRecipe recipe = new ReviveRecipe(this, playerDataManager);

        recipe.loadRecipe();
        registerCMDS();
        registerEvents();
        instance = this;
    }

    private void registerCMDS(){

        // Players Commands [HERE]
        getCommand("timeleft").setExecutor(new TimeLeftCommand(this));
        getCommand("info").setExecutor(new InfoCommand(this));
        // Operators Commands [HERE]
        getCommand("cc").setExecutor(new ReloadConfigCommand(this));
    }

    private void registerEvents(){
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(playerDataManager, this), this);
        getServer().getPluginManager().registerEvents(new FoodsItemsListener(playerDataManager, this), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(playerDataManager, this), this);
        getServer().getPluginManager().registerEvents(new DisableSpectatorsTeleportListener(), this);
        getServer().getPluginManager().registerEvents(new MobsDropsListener(this), this);
        getServer().getPluginManager().registerEvents(new SmeltMeatListener(this), this);
        getServer().getPluginManager().registerEvents(new ReviveRecipe(this, playerDataManager), this);
    }

    @Override
    public void onDisable() {
        getLogger().info("Plugin has been Disabled.");
        saveConfig();
    }
}