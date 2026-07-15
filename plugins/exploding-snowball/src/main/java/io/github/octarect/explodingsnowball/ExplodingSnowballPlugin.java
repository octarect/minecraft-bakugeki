package io.github.octarect.explodingsnowball;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.Component;

public class ExplodingSnowballPlugin extends JavaPlugin implements Listener {

    private static final String ITEM_NAME = "爆発する雪玉";
    private NamespacedKey markerKey;
    private NamespacedKey recipeKey;

    @Override
    public void onEnable() {
        markerKey = new NamespacedKey(this, "exploding");
        recipeKey = new NamespacedKey(this, "exploding_snowball");

        ShapelessRecipe recipe = new ShapelessRecipe(recipeKey, createCustomItem());
        recipe.addIngredient(4, Material.SNOWBALL);
        Bukkit.addRecipe(recipe);

        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        Bukkit.removeRecipe(recipeKey);
    }

    private ItemStack createCustomItem() {
        ItemStack item = new ItemStack(Material.SNOWBALL);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(ITEM_NAME));
        meta.setCustomModelData(1);
        item.setItemMeta(meta);
        return item;
    }

    private boolean isCustomItem(ItemStack item) {
        if (item == null || item.getType() != Material.SNOWBALL) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        Component name = meta.displayName();
        return meta.hasCustomModelData()
            && meta.getCustomModelData() == 1
            && name != null
            && Component.text(ITEM_NAME).equals(name);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (!isCustomItem(item)) return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        Snowball snowball = player.launchProjectile(Snowball.class);
        snowball.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, (byte) 1);

        item.setAmount(item.getAmount() - 1);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball snowball)) return;
        if (!snowball.getPersistentDataContainer().has(markerKey, PersistentDataType.BYTE)) return;

        snowball.getWorld().createExplosion(snowball.getLocation(), 4.0f, false, true);
    }
}
