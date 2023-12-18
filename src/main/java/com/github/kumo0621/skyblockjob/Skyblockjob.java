package com.github.kumo0621.skyblockjob;

import io.papermc.paper.event.player.ChatEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;

import java.util.*;

import static org.bukkit.Material.*;

public final class Skyblockjob extends JavaPlugin implements Listener {
    private Random random = new Random();
    private final Map<UUID, Integer> lapisCount = new HashMap<>();
    private final Map<UUID, Set<UUID>> playerHorns = new HashMap<>();

    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof TNTPrimed) {
            Location loc = entity.getLocation();
            // TNTが水中で爆発したかどうかをチェック
            if (loc.getBlock().getType() == Material.WATER) {
                Entity source = ((TNTPrimed) entity).getSource();
                if (source instanceof Player) {
                    Player player = (Player) source;
                    Team team = player.getScoreboard().getEntryTeam(player.getName());
                    if (team != null && team.getName().equals("漁師")) {
                        // 魚をランダムに生成
                        dropFish(loc);
                        // TNTの近くにあるトライデントを探す
                        for (Entity nearbyEntity : entity.getNearbyEntities(3, 3, 3)) {
                            if (nearbyEntity instanceof Trident) {
                                Trident trident = (Trident) nearbyEntity;
                                if (trident.getShooter() instanceof Player) {
                                    // 金鉱石と鉄鉱石をドロップ
                                    dropOres(loc);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void dropOres(Location loc) {
        World world = loc.getWorld();
        if (world == null) return;

        // 金鉱石を10個ドロップ
        for (int i = 0; i < 10; i++) {
            world.dropItemNaturally(loc, new ItemStack(Material.RAW_GOLD));
        }

        // 鉄鉱石を3個ドロップ
        for (int i = 0; i < 3; i++) {
            world.dropItemNaturally(loc, new ItemStack(Material.RAW_IRON));
        }
        for (int i = 0; i < 1; i++) {
            world.dropItemNaturally(loc, new ItemStack(Material.DIAMOND));
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        ItemStack tool = player.getInventory().getItemInMainHand();
        Material material = block.getType();
        // プレイヤーが「電気工事士」チームに属しているかどうかをチェック
        Team team = player.getScoreboard().getTeam("鍛冶屋");
        if (team != null && team.hasEntry(player.getName())) {
            // ブロックがレッドストーン鉱石かどうかをチェック
            if (event.getBlock().getType() == Material.REDSTONE_ORE) {
                // レッドストーンがパワーを受けているかどうかをチェック
                if (event.getBlock().isBlockPowered() || event.getBlock().isBlockIndirectlyPowered()) {
                    // シルクタッチエンチャントをチェック
                    if (tool.containsEnchantment(Enchantment.SILK_TOUCH)) {
                        event.setDropItems(false);
                        event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(Material.REDSTONE, 1));
                    } else {
                        // ラピスラズリをドロップ
                        int amountLapis = 1 + random.nextInt(2);
                        event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(Material.LAPIS_LAZULI, amountLapis));
                        // 石炭をドロップ
                        int amountCoal = 1 + random.nextInt(2);
                        event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(Material.LAPIS_LAZULI, amountCoal));
                    }
                }
            }
        }
        if (player.getScoreboard().getEntryTeam(player.getName()).getName().equals("石工")) {
            // 鉱石ブロックのリスト
            Material[] ores = {
                    COAL_ORE,
                    IRON_ORE,
                    GOLD_ORE,
                    Material.DIAMOND_ORE,
                    Material.LAPIS_ORE,
                    Material.REDSTONE_ORE,
                    Material.EMERALD_ORE,
                    Material.NETHER_QUARTZ_ORE,
                    Material.NETHER_GOLD_ORE,
                    Material.ANCIENT_DEBRIS,
                    Material.COPPER_ORE,
                    Material.DEEPSLATE_IRON_ORE,
                    Material.DEEPSLATE_GOLD_ORE,
                    Material.DEEPSLATE_DIAMOND_ORE,
                    Material.DEEPSLATE_LAPIS_ORE,
                    Material.DEEPSLATE_REDSTONE_ORE,
                    Material.DEEPSLATE_EMERALD_ORE,
                    Material.DEEPSLATE_COPPER_ORE,
            };

            // ブロックが鉱石かチェック
            for (Material ore : ores) {
                if (event.getBlock().getType() == ore) {
                    // レコードの破片をドロップ
                    ItemStack recordFragment = new ItemStack(Material.DISC_FRAGMENT_5, 1); // レコードの破片のアイテムを選択
                    player.getWorld().dropItemNaturally(event.getBlock().getLocation(), recordFragment);
                    break;
                }
            }
        }
        if (player.getScoreboard().getEntryTeam(player.getName()).getName().equals("石工")) {
            switch (material) {
                case COAL_ORE:
                case COPPER_ORE:
                case GOLD_ORE:
                case IRON_ORE:
                    onUPItem(event);
                    break;
                default:
                    break;
            }
        }
    }

    private void onUPItem(BlockBreakEvent event) {
        dropItem(event, Material.COAL, random.nextInt(3));
        dropItem(event, Material.IRON_INGOT, random.nextInt(2));
        dropItem(event, Material.GOLD_INGOT, random.nextInt(2));
        dropItem(event, Material.COPPER_INGOT, random.nextInt(3));
    }

    private void dropItem(BlockBreakEvent event, Material item, int quantity) {
        event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(item, quantity));
    }


    private void dropFish(Location loc) {
        Random random = new Random();
        int numberOfFish = random.nextInt(5) + 15; // 5から9までのランダムな数の魚
        int numberOfSeaweed = random.nextInt(3) + 5; // 1から3までのランダムな数の海藻
        int numberOfInkSacs = random.nextInt(2); // 0または1のイカスミ
        // 魚のドロップ
        for (int i = 0; i < numberOfFish; i++) {
            loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.COD));
        }
        for (int i = 0; i < numberOfFish; i++) {
            loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.SALMON));
        }
        // 海藻のドロップ
        for (int i = 0; i < numberOfSeaweed; i++) {
            loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.KELP));
        }
        //ふぐ
        for (int i = 0; i < numberOfSeaweed; i++) {
            loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.PUFFERFISH));
        }
        //クマノミ
        for (int i = 0; i < numberOfSeaweed; i++) {
            loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.TROPICAL_FISH));
        }
        //皮
        for (int i = 0; i < numberOfSeaweed; i++) {
            loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.LEATHER));
        }
        // イカスミのドロップ
        for (int i = 0; i < numberOfInkSacs; i++) {
            loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.INK_SAC));
        }
        //貝殻
        for (int i = 0; i < numberOfInkSacs; i++) {
            loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.NAUTILUS_SHELL));
        }
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        // プレイヤーがアイテムを持っているか確認
        if (event.getItem() == null || event.getItem().getType() != Material.PAPER) return;

        ItemStack item = event.getItem();
        ItemMeta meta = item.getItemMeta();

        // カスタムモデルデータを確認
        if (meta == null || !meta.hasCustomModelData()) return;

        int customModelData = meta.getCustomModelData();

        // それぞれの紙に対する特定の座標
        Location adventurerLocation = new Location(getServer().getWorld("world_the_end"), 91, 97, 104); // 冒険者の座標
        Location blacksmithLocation = new Location(getServer().getWorld("world_the_end"), 115, 35, 11); // 石工の座標
        Location bakeryLocation = new Location(getServer().getWorld("world_the_end"), 33, 60, -55); // パン屋の座標
        Location ryousiLocation = new Location(getServer().getWorld("world_the_end"), 80, 140, 52); // パン屋の座標

        boolean teleported = false;
        switch (customModelData) {
            case 101:
                event.getPlayer().teleport(adventurerLocation);
                teleported = true;
                break;
            case 102:
                event.getPlayer().teleport(blacksmithLocation);
                teleported = true;
                break;
            case 103:
                event.getPlayer().teleport(bakeryLocation);
                teleported = true;
                break;
            case 104:
                event.getPlayer().teleport(ryousiLocation);
                teleported = true;
        }

        // テレポートした場合、アイテムを1つ減らす
        if (teleported) {
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                event.getPlayer().getInventory().removeItem(item);
            }
        }
    }

    @EventHandler
    public void onAnvilLand(EntityChangeBlockEvent event) {
        if (event.getEntity().getType() == EntityType.FALLING_BLOCK &&
                event.getTo() == Material.ANVIL) {

            int ironNuggetCount = 0, goldNuggetCount = 0;
            for (Entity entity : event.getEntity().getNearbyEntities(1, 1, 1)) {
                if (entity instanceof Item) {
                    Item item = (Item) entity;
                    switch (item.getItemStack().getType()) {
                        case IRON_NUGGET:
                            ironNuggetCount += item.getItemStack().getAmount();
                            break;
                        case GOLD_NUGGET:
                            goldNuggetCount += item.getItemStack().getAmount();
                            break;
                    }
                    // 下にあるアイテムをすべて削除
                    item.remove();
                }
            }

            if (ironNuggetCount >= 64 && goldNuggetCount >= 64) {
                // カスタムモデルデータ1を持つ鉄の原石をドロップ
                ItemStack customIronOre = new ItemStack(Material.RAW_IRON);
                ItemMeta meta = customIronOre.getItemMeta();
                meta.setCustomModelData(3);
                meta.setDisplayName("銅の原石");
                customIronOre.setItemMeta(meta);
                event.getEntity().getWorld().dropItemNaturally(event.getEntity().getLocation(), customIronOre);
            }
        }
    }

    @EventHandler
    public void onExpBottleThrow(EntitySpawnEvent event) {
        if (event.getEntity() instanceof ThrownExpBottle) {
            ThrownExpBottle thrownExpBottle = (ThrownExpBottle) event.getEntity();
            if (thrownExpBottle.getShooter() instanceof Player) {
                Player player = (Player) thrownExpBottle.getShooter();
                Team team = player.getScoreboard().getTeam("パン屋");
                if (team != null && team.hasEntry(player.getName())) {

                    for (Item item : thrownExpBottle.getWorld().getEntitiesByClass(Item.class)) {
                        if (item.getItemStack().getType() == Material.LAPIS_LAZULI && item.getItemStack().getAmount() == 1) {
                            if (item.getLocation().distance(thrownExpBottle.getLocation()) < 5.0) { // 5ブロック以内を検出範囲とする
                                UUID itemId = item.getUniqueId();
                                int count = lapisCount.getOrDefault(itemId, 0);
                                count++;

                                if (count >= 64) {
                                    // ダメージ軽減のエンチャント本を生成
                                    ItemStack enchantedBook = new ItemStack(Material.ENCHANTED_BOOK);

                                    // ItemStackのメタデータを取得 (EnchantmentStorageMetaにキャスト)
                                    EnchantmentStorageMeta meta = (EnchantmentStorageMeta) enchantedBook.getItemMeta();

                                    // エンチャントを追加
                                    meta.addStoredEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);

                                    // 変更したメタデータをItemStackに適用
                                    enchantedBook.setItemMeta(meta);
                                    // エンチャント本をドロップ
                                    item.getWorld().dropItemNaturally(item.getLocation(), enchantedBook);

                                    // ラピスラズリを消去
                                    item.remove();

                                    // カウントをリセット
                                    lapisCount.remove(itemId);
                                } else {
                                    lapisCount.put(itemId, count);
                                }

                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item != null && item.getType() == Material.GOAT_HORN) { // 適切なアイテムタイプを設定
            UUID playerUuid = player.getUniqueId();

            if (player.isSneaking()) {
                // シフトを押しながら右クリックで視線にあるプレイヤーをリストに追加
                Player target = getTargetPlayer(player);
                if (target != null) {
                    playerHorns.putIfAbsent(playerUuid, new HashSet<>());
                    Set<UUID> registeredPlayers = playerHorns.get(playerUuid);

                    if (target.isSneaking()) {
                        if (registeredPlayers.size() < 2) {
                            registeredPlayers.add(target.getUniqueId());
                            player.sendMessage(target.getName() + "さんを招集の角笛に入れたよ");
                            target.sendMessage(player.getName() + "さんがあなたを招集の角笛に入れたよ");
                        } else {
                            player.sendMessage("招集の角笛が満杯だよ");
                        }
                    } else {
                        player.sendMessage("招集するためには相手がシフトを押している必要があるよ");
                        target.sendMessage(player.getName() + "さんがあなたを招集の角笛に入れようとしています。招集されるにはシフトを押してください");
                    }
                } else {
                    player.sendMessage("プレイヤーが見つからないよ。招集したいプレイヤーを右クリックしてね");
                }
            } else {
                // 通常の右クリックでリスト内のプレイヤーを自分にテレポート
                if (playerHorns.containsKey(playerUuid)) {
                    // 禁止エリアにいるかどうかをチェック
                    BoundingBox box = new BoundingBox(
                            295, 319, 45,
                            224, 315, 24
                    );
                    if (player.getLocation().getWorld().getWorldType() == WorldType.NORMAL
                            && box.contains(player.getLocation().toVector())) {
                        player.sendMessage("禁止エリアにいるため、招集できません");
                        return;
                    }

                    for (UUID targetPlayerUuid : playerHorns.get(playerUuid)) {
                        Player targetPlayer = player.getServer().getPlayer(targetPlayerUuid);
                        if (targetPlayer != null) {
                            targetPlayer.teleport(player.getLocation());
                            targetPlayer.sendMessage("招集が完了しました。");
                        }
                    }
                    item.setAmount(0); // 角笛を消滅させる
                    player.sendMessage("召喚したため、角笛が消えました");
                    playerHorns.remove(playerUuid);
                }
            }
        }
    }

    private Player getTargetPlayer(Player player) {
        RayTraceResult result = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getLocation().getDirection(),
                10, // 視線のトレース範囲を10ブロックに設定
                1.0,
                entity -> entity instanceof Player && entity != player
        );

        if (result != null && result.getHitEntity() instanceof Player) {
            return (Player) result.getHitEntity();
        }
        return null;
    }

}


