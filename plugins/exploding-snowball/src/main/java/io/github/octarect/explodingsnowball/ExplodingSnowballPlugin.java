package io.github.octarect.explodingsnowball;

import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public class ExplodingSnowballPlugin extends JavaPlugin implements Listener {

    private static final int MAX_LEVEL = 8;
    private static final int TYPE_EXPLODING = 0;
    private static final int TYPE_INCENDIARY = 1;
    private static final int TYPE_THERMOBARIC = 2;
    private static final int LAUNCHER_CMD = 31;

    private NamespacedKey markerKey;
    private NamespacedKey[] recipeKeys;
    private final Map<Location, Float> incendiaryExplosions = Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<UUID, Long> chargingMap = new HashMap<>();
    private final Map<UUID, BukkitTask> chargingTasks = new HashMap<>();

    @Override
    public void onEnable() {
        markerKey = new NamespacedKey(this, "exploding");
        recipeKeys = new NamespacedKey[MAX_LEVEL * 5 - 1];

        ShapelessRecipe lv1Recipe = new ShapelessRecipe(
                new NamespacedKey(this, "exploding_snowball_lv1"), createItem(TYPE_EXPLODING, 1));
        lv1Recipe.addIngredient(4, Material.SNOWBALL);
        recipeKeys[0] = lv1Recipe.getKey();
        Bukkit.addRecipe(lv1Recipe);

        for (int lv = 2; lv <= MAX_LEVEL; lv++) {
            ShapelessRecipe recipe = new ShapelessRecipe(
                    new NamespacedKey(this, "exploding_snowball_lv" + lv), createItem(TYPE_EXPLODING, lv));
            recipe.addIngredient(new RecipeChoice.ExactChoice(createItem(TYPE_EXPLODING, lv - 1)));
            recipe.addIngredient(new RecipeChoice.ExactChoice(createItem(TYPE_EXPLODING, lv - 1)));
            recipeKeys[lv - 1] = recipe.getKey();
            Bukkit.addRecipe(recipe);
        }

        for (int lv = 1; lv <= MAX_LEVEL; lv++) {
            ShapelessRecipe recipe = new ShapelessRecipe(
                    new NamespacedKey(this, "incendiary_snowball_lv" + lv), createItem(TYPE_INCENDIARY, lv));
            recipe.addIngredient(new RecipeChoice.ExactChoice(createItem(TYPE_EXPLODING, lv)));
            recipe.addIngredient(Material.GUNPOWDER);
            recipeKeys[MAX_LEVEL + lv - 1] = recipe.getKey();
            Bukkit.addRecipe(recipe);
        }

        for (int lv = 2; lv <= MAX_LEVEL; lv++) {
            ShapelessRecipe recipe = new ShapelessRecipe(
                    new NamespacedKey(this, "incendiary_snowball_lv" + lv + "_upgrade"), createItem(TYPE_INCENDIARY, lv));
            recipe.addIngredient(new RecipeChoice.ExactChoice(createItem(TYPE_INCENDIARY, lv - 1)));
            recipe.addIngredient(new RecipeChoice.ExactChoice(createItem(TYPE_INCENDIARY, lv - 1)));
            recipeKeys[MAX_LEVEL * 2 + lv - 2] = recipe.getKey();
            Bukkit.addRecipe(recipe);
        }

        for (int lv = 1; lv <= MAX_LEVEL; lv++) {
            ShapelessRecipe recipe = new ShapelessRecipe(
                    new NamespacedKey(this, "thermobaric_snowball_lv" + lv), createItem(TYPE_THERMOBARIC, lv));
            recipe.addIngredient(new RecipeChoice.ExactChoice(createItem(TYPE_EXPLODING, lv)));
            recipe.addIngredient(Material.GUNPOWDER);
            recipe.addIngredient(Material.GUNPOWDER);
            recipeKeys[MAX_LEVEL * 3 - 1 + lv - 1] = recipe.getKey();
            Bukkit.addRecipe(recipe);
        }

        for (int lv = 2; lv <= MAX_LEVEL; lv++) {
            ShapelessRecipe recipe = new ShapelessRecipe(
                    new NamespacedKey(this, "thermobaric_snowball_lv" + lv + "_upgrade"), createItem(TYPE_THERMOBARIC, lv));
            recipe.addIngredient(new RecipeChoice.ExactChoice(createItem(TYPE_THERMOBARIC, lv - 1)));
            recipe.addIngredient(new RecipeChoice.ExactChoice(createItem(TYPE_THERMOBARIC, lv - 1)));
            recipeKeys[MAX_LEVEL * 4 - 1 + lv - 2] = recipe.getKey();
            Bukkit.addRecipe(recipe);
        }

        ShapedRecipe launcherRecipe = new ShapedRecipe(
                new NamespacedKey(this, "snowball_launcher"), createLauncher());
        launcherRecipe.shape("SSS", "SBS", "SSS");
        launcherRecipe.setIngredient('S', Material.STONE);
        launcherRecipe.setIngredient('B', Material.BOW);
        recipeKeys[MAX_LEVEL * 5 - 2] = launcherRecipe.getKey();
        Bukkit.addRecipe(launcherRecipe);

        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        for (NamespacedKey key : recipeKeys) {
            if (key != null) Bukkit.removeRecipe(key);
        }
        for (BukkitTask task : chargingTasks.values()) task.cancel();
        chargingTasks.clear();
        chargingMap.clear();
    }

    private ItemStack createItem(int type, int level) {
        ItemStack item = new ItemStack(Material.SNOWBALL);
        ItemMeta meta = item.getItemMeta();
        String[] names = {"爆発する雪玉", "焼夷雪玉", "燃料気化雪玉"};
        int[] cmdBase = {0, 10, 20};
        meta.displayName(Component.text(names[type] + "Lv" + level));
        meta.setCustomModelData(cmdBase[type] + level);
        meta.setMaxStackSize(64);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createLauncher() {
        ItemStack item = new ItemStack(Material.BOW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("爆発する雪玉ランチャー"));
        meta.setCustomModelData(LAUNCHER_CMD);
        meta.addEnchant(Enchantment.INFINITY, 1, true);
        item.setItemMeta(meta);
        return item;
    }

    private boolean isLauncher(ItemStack item) {
        if (item == null || item.getType() != Material.BOW) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == LAUNCHER_CMD;
    }

    private int[] getTypeAndLevel(ItemStack item) {
        if (item == null || item.getType() != Material.SNOWBALL) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasCustomModelData()) return null;
        int cmd = meta.getCustomModelData();
        if (cmd >= 1 && cmd <= MAX_LEVEL) return new int[]{TYPE_EXPLODING, cmd};
        if (cmd >= 11 && cmd <= 10 + MAX_LEVEL) return new int[]{TYPE_INCENDIARY, cmd - 10};
        if (cmd >= 21 && cmd <= 20 + MAX_LEVEL) return new int[]{TYPE_THERMOBARIC, cmd - 20};
        return null;
    }

    private ItemStack findSnowball(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (getTypeAndLevel(item) != null) return item;
        }
        return null;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();

        if (isLauncher(item)) {
            if (findSnowball(event.getPlayer()) == null) {
                event.setCancelled(true);
            }
            return;
        }

        int[] typeAndLevel = getTypeAndLevel(item);
        if (typeAndLevel == null) return;

        event.setCancelled(true);

        int type = typeAndLevel[0];
        int level = typeAndLevel[1];
        byte encoded = (byte) (type * 10 + level);

        Player player = event.getPlayer();
        Snowball snowball = player.launchProjectile(Snowball.class);
        snowball.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, encoded);

        item.setAmount(item.getAmount() - 1);
    }

    @EventHandler
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isLauncher(event.getBow())) return;

        event.setCancelled(true);
        event.setConsumeItem(false);

        ItemStack ammo = findSnowball(player);
        if (ammo == null) return;

        int[] typeAndLevel = getTypeAndLevel(ammo);
        if (typeAndLevel == null) return;

        double speed = 1.0 + event.getForce() * 7.0;

        Snowball snowball = player.launchProjectile(Snowball.class);
        Vector velocity = snowball.getVelocity().normalize().multiply(speed);
        snowball.setVelocity(velocity);

        byte encoded = (byte) (typeAndLevel[0] * 10 + typeAndLevel[1]);
        snowball.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, encoded);

        ammo.setAmount(ammo.getAmount() - 1);

        UUID id = player.getUniqueId();
        BukkitTask task = chargingTasks.remove(id);
        if (task != null) task.cancel();
        chargingMap.remove(id);
        player.sendActionBar(Component.empty());
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack current = player.getInventory().getItem(event.getPreviousSlot());
        if (!isLauncher(current)) return;
        UUID id = player.getUniqueId();
        BukkitTask task = chargingTasks.remove(id);
        if (task != null) task.cancel();
        chargingMap.remove(id);
        player.sendActionBar(Component.empty());
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball snowball)) return;
        Byte encoded = snowball.getPersistentDataContainer().get(markerKey, PersistentDataType.BYTE);
        if (encoded == null || encoded == 0) return;

        int type = encoded / 10;
        int level = encoded % 10;
        float power = (float) Math.pow(2.0, level);

        boolean fire = type == TYPE_INCENDIARY || type == TYPE_THERMOBARIC;
        boolean breakBlocks = type == TYPE_EXPLODING || type == TYPE_THERMOBARIC;
        Location loc = snowball.getLocation();
        if (type == TYPE_INCENDIARY) {
            incendiaryExplosions.put(loc, power);
            Bukkit.getScheduler().runTask(this, () -> incendiaryExplosions.remove(loc));
        }
        snowball.getWorld().createExplosion(loc, power, fire, breakBlocks);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) return;
        Location loc = event.getEntity().getLocation();
        for (Map.Entry<Location, Float> entry : incendiaryExplosions.entrySet()) {
            Location explosion = entry.getKey();
            float power = entry.getValue();
            if (explosion.getWorld().equals(loc.getWorld()) && explosion.distanceSquared(loc) <= power * power) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
