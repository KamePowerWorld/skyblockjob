package com.github.kumo0621.skyblockjob;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;

import java.util.*;

import static org.bukkit.Material.*;

public final class Skyblockjob extends JavaPlugin implements Listener {
    private Random random = new Random();
    private final Map<UUID, Integer> lapisCount = new HashMap<>();
    private final Map<UUID, Set<UUID>> playerHorns = new HashMap<>();

    private static class DenyArea {
        private final World world;
        private final BoundingBox boundingBox;

        public DenyArea(World world, BoundingBox boundingBox) {
            this.world = world;
            this.boundingBox = boundingBox;
        }

        public boolean contains(Location location) {
            return world.equals(location.getWorld()) && boundingBox.contains(location.toVector());
        }
    }

    private List<DenyArea> denyAreaList;

    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(this, this);

        // コンフィグ
        saveDefaultConfig();

        // 禁止エリアの範囲を設定
        denyAreaList = new ArrayList<>();
        Objects.requireNonNull(getConfig().getList("denyAreaList")).forEach(obj -> {
            Map<?, ?> area = (Map<?, ?>) obj;
            World world = getServer().getWorld(Objects.requireNonNull((String) area.get("world")));
            Map<?, ?> min = (Map<?, ?>) area.get("min");
            Map<?, ?> max = (Map<?, ?>) area.get("max");
            BoundingBox boundingBox = new BoundingBox(
                    (int) min.get("x"),
                    (int) min.get("y"),
                    (int) min.get("z"),
                    (int) max.get("x"),
                    (int) max.get("y"),
                    (int) max.get("z")
            );
            denyAreaList.add(new DenyArea(world, boundingBox));
        });
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
                    if (team != null && team.getName().equals("ryousi")) {
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

    @EventHandler
    public void onAnvilPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Team team = player.getScoreboard().getEntryTeam(player.getName());
        if (team != null && team.getName().equals("nougyou")) {
            if (event.getBlock().getType() == OBSIDIAN) {
                Location anvilLocation = event.getBlock().getLocation();
                checkAndActivateHarvest(anvilLocation, player);
            }
        }
    }

    private void checkAndActivateHarvest(Location location, Player player) {
        // Check in a radius of 3 for potatoes and carrots
        int potatoesCount = 0, carrotsCount = 0;
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                Block block = location.clone().add(x, 0, z).getBlock();
                if (block.getType() == Material.POTATOES) {
                    potatoesCount++;
                } else if (block.getType() == Material.CARROTS) {
                    carrotsCount++;
                }
            }
        }

        // If conditions are met, drop items immediately
        if (potatoesCount >= 5 && carrotsCount >= 5) {
            int cropsCount = 0;
            for (int x = -5; x <= 5; x++) {
                for (int z = -5; z <= 5; z++) {
                    Block block = location.clone().add(x, 0, z).getBlock();
                    if (block.getType() == Material.WHEAT || block.getType() == Material.POTATOES || block.getType() == Material.CARROTS) {
                        cropsCount++;
                    }
                }
            }
            if (cropsCount > 0) {
                ItemStack drop = new ItemStack(Material.WHEAT, cropsCount - 10);
                location.getWorld().dropItemNaturally(location.add(0.5, 1, 0.5), drop);
            }
            player.sendMessage(ChatColor.GREEN + "設置が完了しました。");
        } else {
            player.sendMessage(ChatColor.RED + "3種類の作物を（人参・じゃがいも・種）を半径5マスに設置してください。");
        }
    }

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        Team team = player.getScoreboard().getEntryTeam(player.getName());
        if (team != null && team.getName().equals("ryousi")) {
            if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
                Random random = new Random();
                if (random.nextDouble() < 0.5) {
                    ItemStack specialFish1 = new ItemStack(Material.COD, 1);
                    ItemMeta meta1 = specialFish1.getItemMeta();
                    meta1.setDisplayName(ChatColor.GOLD + "Golden Fish");
                    specialFish1.setItemMeta(meta1);

                    ItemStack specialFish2 = new ItemStack(Material.SALMON, 1);
                    ItemMeta meta2 = specialFish2.getItemMeta();
                    meta2.setDisplayName(ChatColor.BLUE + "Mystic Fish");
                    specialFish2.setItemMeta(meta2);

                    event.getPlayer().getInventory().addItem(specialFish1);
                    event.getPlayer().getInventory().addItem(specialFish2);
                }
            }
        }
    }

    @EventHandler
    public void onItemBreak(PlayerItemBreakEvent event) {
        Player player = event.getPlayer();
        Team team = player.getScoreboard().getEntryTeam(player.getName());
        if (team != null && team.getName().equals("isiku")) {


            // アイテムが壊れたときの処理
            ItemStack brokenItem = event.getBrokenItem();

            // 壊れたアイテムが鉄のピッケルかどうかを確認
            if (brokenItem.getType() == Material.IRON_PICKAXE) {
                // 新しい木のピッケルを作成
                ItemStack newItem = new ItemStack(Material.WOODEN_PICKAXE, 1);

                // 耐久値を設定 (最大耐久値から58減らす)
                newItem.setDurability((short) (Material.WOODEN_PICKAXE.getMaxDurability() - 1));

                ItemMeta meta = newItem.getItemMeta();

                // カスタムモデルデータを設定
                meta.setCustomModelData(1);

                // アイテムの表示名を設定
                meta.setDisplayName("鍛冶屋に売りつけると高い");

                newItem.setItemMeta(meta);

                // アイテムをプレイヤーの足元にドロップ
                event.getPlayer().getWorld().dropItemNaturally(event.getPlayer().getLocation(), newItem);
            }

        }
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        Team team = player.getScoreboard().getEntryTeam(player.getName());
        if (team != null && team.getName().equals("kaziya")) {
            if (event.isSneaking()) {  // プレイヤーがしゃがんだ時
                Random random1 = new Random();
                int i = random1.nextInt(10);
                if (i == 1) {
                    ItemStack itemInHand = event.getPlayer().getInventory().getItemInMainHand();  // 手に持っているアイテムを取得

                    if (itemInHand != null && itemInHand.getType() != Material.AIR && itemInHand.getType().getMaxDurability() > 0) {
                        // アイテムが空気ではなく、耐久値を持つアイテムの場合
                        if (itemInHand.getDurability() > 0) {  // 耐久値が0より大きい場合
                            itemInHand.setDurability((short) (itemInHand.getDurability() - 1));  // 耐久値を1回復
                            event.getPlayer().sendMessage("アイテムの耐久値を回復しました。");  // プレイヤーに通知
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
        for (int i = 0; i < 1; i++) {
            world.dropItemNaturally(loc, new ItemStack(Material.RAW_GOLD));
        }
        for (int i = 0; i < 2; i++) {
            world.dropItemNaturally(loc, new ItemStack(IRON_NUGGET));
        }
        for (int i = 0; i < 2; i++) {
            world.dropItemNaturally(loc, new ItemStack(GOLD_NUGGET));
        }
        // 鉄鉱石を3個ドロップ
        for (int i = 0; i < 1; i++) {
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
        if (player.getScoreboard().getEntryTeam(player.getName()).getName().equals("kikori")) {
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
                        int amountLapis = 1;
                        event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(Material.LAPIS_LAZULI, amountLapis));
                        // 石炭をドロップ
                        int amountCoal = 1 + random.nextInt(1);
                        event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(COAL, amountCoal));
                    }
                }
            }
        } else if (player.getScoreboard().getEntryTeam(player.getName()).getName().equals("isiku")) {
            if (event.getBlock().getType() == Material.STONE) {
                if (tool.containsEnchantment(Enchantment.SILK_TOUCH)) {
                    event.setDropItems(false);
                    int amountLapis = 1 + random.nextInt(1);
                    event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(IRON_ORE, amountLapis));
                } else {
                    // 鉄鉱石をドロップ
                    event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(IRON_ORE, 1));

                }
            }
        } else if (player.getScoreboard().getEntryTeam(player.getName()).getName().equals("kikori")) {
            if (event.getBlock().getType() == JUNGLE_LOG) {

                if (tool.containsEnchantment(Enchantment.SILK_TOUCH)) {
                    event.setDropItems(false);
                    // 鉄鉱石をドロップ
                    int amountLapis = 1 + random.nextInt(2);
                    event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(GUNPOWDER, amountLapis));
                } else {
                    event.setDropItems(false);
                    event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(GUNPOWDER, 1));

                }
            }
        }
    }


    private void dropFish(Location loc) {
        Random random = new Random();
        int numberOfFish = random.nextInt(5); // 5から9までのランダムな数の魚
        int numberOfSeaweed = random.nextInt(3); // 1から3までのランダムな数の海藻
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

        // 紙のテレポートをコンフィグから取得
        boolean teleported = false;
        List<Map<?, ?>> ticketTeleport = getConfig().getMapList("ticketTeleport");
        for (Map<?, ?> map : ticketTeleport) {
            if (map.get("id").equals(customModelData)) {
                Location location = new Location(
                        getServer().getWorld((String) map.get("world")),
                        (int) map.get("x"),
                        (int) map.get("y"),
                        (int) map.get("z")
                );
                event.getPlayer().teleport(location);
                teleported = true;
                break;
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
                Team team = player.getScoreboard().getTeam("nougyou");
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
                                }
                            }
                        }
                        if (item.getItemStack().getType() == IRON_INGOT && item.getItemStack().getAmount() == 1) {
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
                                    meta.addStoredEnchant(Enchantment.DAMAGE_ALL, 1, true);

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
                    if (denyAreaList.stream().anyMatch(denyArea -> denyArea.contains(player.getLocation()))) {
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

    @EventHandler
    public void onPistonBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        if (event.getBlock().getType() == PISTON) {
            // レッドストーンがパワーを受けているかどうかをチェック
            if (event.getBlock().isBlockPowered() || event.getBlock().isBlockIndirectlyPowered()) {
                Random rand = new Random();
                int chance = rand.nextInt(100);

                // 鎖、レッドストーン、鉄をドロップする確率
                if (chance < 50) {
                    block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.CHAIN));
                } else if (chance < 60) {
                    block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.REDSTONE));
                } else if (chance < 90) {
                    block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(IRON_INGOT));

                }
                // ピストンを消す
                event.setDropItems(false);
            }
        }
    }

    @EventHandler
    public void onEntityBreed(EntityBreedEvent event) {
        // エンティティが交配されたときのイベント
        Random random = new Random();
        int amountToDrop = random.nextInt(2); // 1から3のランダムな数

        // レッドストーンダストをドロップ
        event.getEntity().getWorld().dropItemNaturally(event.getEntity().getLocation(), new ItemStack(Material.REDSTONE, amountToDrop));
    }
}


