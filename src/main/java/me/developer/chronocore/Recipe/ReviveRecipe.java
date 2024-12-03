package me.developer.chronocore.Recipe;

import me.developer.chronocore.ChronoCore;
import me.developer.chronocore.Utils.ColorUtils;
import me.developer.chronocore.Utils.PlayerDataManager;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

@SuppressWarnings("deprecation")
public class ReviveRecipe implements Listener {

    private final ChronoCore plugin;
    private final PlayerDataManager playerDataManager;
    private final Map<UUID, UUID> reviverTargets = new HashMap<>();

    public ReviveRecipe(ChronoCore plugin, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
    }

    public void loadRecipe() {
        FileConfiguration config = plugin.getConfig();
        String path = "Revive_Recipe";

        String itemName = ChatColor.translateAlternateColorCodes('&', config.getString(path + ".Item-Name", "Unnamed Item"));
        String itemMaterial = config.getString(path + ".Item-Material", "STONE");
        List<String> itemLore = config.getStringList(path + ".Item-Lore");

        Material material = Material.matchMaterial(itemMaterial);
        if (material == null) {
            plugin.getLogger().warning("Invalid material for Revive Recipe: " + itemMaterial);
            return;
        }

        ItemStack outputItem = new ItemStack(material);
        ItemMeta outputMeta = outputItem.getItemMeta();
        if (outputMeta != null) {
            outputMeta.setDisplayName(itemName);
            if (!itemLore.isEmpty()) {
                List<String> lore = new ArrayList<>();
                for (String line : itemLore) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', line));
                }
                outputMeta.setLore(lore);
            }
            outputItem.setItemMeta(outputMeta);
        }

        List<String> shape = config.getStringList(path + ".Recipe");
        if (shape.size() != 3) {
            plugin.getLogger().warning("Invalid recipe shape for Revive Recipe. It must have 3 rows.");
            return;
        }

        Map<String, Object> recipeItems = config.getConfigurationSection(path + ".Recipe-Items").getValues(false);
        Map<Character, RecipeChoice> ingredientMap = new HashMap<>();

        for (Map.Entry<String, Object> entry : recipeItems.entrySet()) {
            char keyChar = entry.getKey().charAt(0);
            String materialName = entry.getValue().toString();

            if (materialName.equalsIgnoreCase("BEEF")) {
                ItemStack customBeef = new ItemStack(Material.BEEF);
                ItemMeta beefMeta = customBeef.getItemMeta();
                if (beefMeta != null) {
                    beefMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&e&lTime &6&lMeat"));
                    customBeef.setItemMeta(beefMeta);
                }
                ingredientMap.put(keyChar, new RecipeChoice.ExactChoice(customBeef));
            } else {
                Material mat = Material.matchMaterial(materialName);
                if (mat == null) {
                    plugin.getLogger().warning("Invalid material for key " + keyChar + ": " + materialName);
                    continue;
                }
                ingredientMap.put(keyChar, new RecipeChoice.MaterialChoice(mat));
            }
        }

        NamespacedKey key = new NamespacedKey(plugin, "revive_recipe");
        ShapedRecipe recipe = new ShapedRecipe(key, outputItem);
        recipe.shape(shape.get(0), shape.get(1), shape.get(2));

        for (Map.Entry<Character, RecipeChoice> entry : ingredientMap.entrySet()) {
            recipe.setIngredient(entry.getKey(), entry.getValue());
        }

        Bukkit.addRecipe(recipe);
        plugin.getLogger().info("Revive Recipe loaded successfully with custom ingredients!");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getItem() == null || !event.getItem().hasItemMeta()) return;
        String path = "Revive_Recipe";
        FileConfiguration config = plugin.getConfig();
        String itemName = ChatColor.translateAlternateColorCodes('&', config.getString(path + ".Item-Name", "Unnamed Item"));

        ItemMeta meta = event.getItem().getItemMeta();
        if (meta.getDisplayName().equals(ChatColor.translateAlternateColorCodes('&', itemName))) {
            event.setCancelled(true);
            player.sendMessage(ColorUtils.translateColors(plugin.getConfig().getString("Revive_Item.Type_Player_Name_Message").replace("%prefix%", ChronoCore.getInstance().prefix)));
            reviverTargets.put(player.getUniqueId(), null);
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!reviverTargets.containsKey(player.getUniqueId())) return;

        event.setCancelled(true);
        String targetName = event.getMessage();
        Player target = Bukkit.getPlayerExact(targetName);

        if (target == null || !target.isOnline() || target.getGameMode() != GameMode.SPECTATOR) {
            player.sendMessage(ColorUtils.translateColors(plugin.getConfig().getString("Revive_Item.Invalid_Player").replace("%prefix%", ChronoCore.getInstance().prefix)));
            reviverTargets.remove(player.getUniqueId());
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            target.setGameMode(GameMode.SURVIVAL);
            target.teleport(player.getLocation());
            player.sendMessage(ColorUtils.translateColors(plugin.getConfig().getString("Revive_Item.Revived_Successfully")
                    .replace("%prefix%", ChronoCore.getInstance().prefix)
                    .replace("%target%", target.getName())));

            target.sendMessage(ColorUtils.translateColors(plugin.getConfig().getString("Revive_Item.Revived_Successfully_by_Player")
                    .replace("%prefix%", ChronoCore.getInstance().prefix)
                    .replace("%player%", player.getName())));

            playerDataManager.setPlayerNeededHours(target.getUniqueId(), 2);
            removeReviveItem(player);
        });

        reviverTargets.remove(player.getUniqueId());
    }

    private void removeReviveItem(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
    }
}