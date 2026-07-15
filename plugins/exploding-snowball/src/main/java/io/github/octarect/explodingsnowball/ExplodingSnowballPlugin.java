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
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.Component;

public class ExplodingSnowballPlugin extends JavaPlugin implements Listener {

    private static final int MAX_LEVEL = 8;
    private NamespacedKey markerKey;
    private NamespacedKey[] recipeKeys;

    @Override
    public void onEnable() {
        markerKey = new NamespacedKey(this, "exploding");
        recipeKeys = new NamespacedKey[MAX_LEVEL];

        ShapelessRecipe lv1Recipe = new ShapelessRecipe(
                new NamespacedKey(this, "exploding_snowball_lv1"), createCustomItem(1));
        lv1Recipe.addIngredient(4, Material.SNOWBALL);
        recipeKeys[0] = lv1Recipe.getKey();
        Bukkit.addRecipe(lv1Recipe);

        for (int lv = 2; lv <= MAX_LEVEL; lv++) {
            ShapelessRecipe recipe = new ShapelessRecipe(
                    new NamespacedKey(this, "exploding_snowball_lv" + lv), createCustomItem(lv));
            recipe.addIngredient(new RecipeChoice.ExactChoice(createCustomItem(lv - 1)));
            recipe.addIngredient(new RecipeChoice.ExactChoice(createCustomItem(lv - 1)));
            recipe.addIngredient(new RecipeChoice.ExactChoice(createCustomItem(lv - 1)));
            recipe.addIngredient(new RecipeChoice.ExactChoice(createCustomItem(lv - 1)));
            recipeKeys[lv - 1] = recipe.getKey();
            Bukkit.addRecipe(recipe);
        }

        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        for (NamespacedKey key : recipeKeys) {
            if (key != null) Bukkit.removeRecipe(key);
        }
    }

    private ItemStack createCustomItem(int level) {
        ItemStack item = new ItemStack(Material.SNOWBALL);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("爆発する雪玉Lv" + level));
        meta.setCustomModelData(level);
        item.setItemMeta(meta);
        return item;
    }

    private int getLevel(ItemStack item) {
        if (item == null || item.getType() != Material.SNOWBALL) return 0;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasCustomModelData()) return 0;
        int cmd = meta.getCustomModelData();
        return (cmd >= 1 && cmd <= MAX_LEVEL) ? cmd : 0;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        int level = getLevel(item);
        if (level == 0) return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        Snowball snowball = player.launchProjectile(Snowball.class);
        snowball.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, (byte) level);

        item.setAmount(item.getAmount() - 1);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball snowball)) return;
        Byte level = snowball.getPersistentDataContainer().get(markerKey, PersistentDataType.BYTE);
        if (level == null || level == 0) return;

        float power = (float) Math.pow(2.0, level);
        snowball.getWorld().createExplosion(snowball.getLocation(), power, false, true);
    }
}
