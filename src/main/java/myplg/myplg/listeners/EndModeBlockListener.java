package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.DragonFireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.EnderDragonChangePhaseEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;

/**
 * ENDモード中のブロック破壊制御とエンダードラゴン攻撃無効化
 * ENDモード発動中は黒曜石のみ破壊不可能
 * エンダードラゴンはプレイヤーを攻撃・吹っ飛ばししない
 */
public class EndModeBlockListener implements Listener {
    private final PvPGame plugin;

    public EndModeBlockListener(PvPGame plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        // ENDモードがアクティブでない場合は何もしない
        if (!plugin.getEndModeManager().isEndModeActive()) {
            return;
        }

        Block block = event.getBlock();
        Player player = event.getPlayer();

        // ENDモード中は黒曜石のみ破壊不可
        if (block.getType() == Material.OBSIDIAN) {
            event.setCancelled(true);
            player.sendMessage("§5§l[ENDモード] §c黒曜石はENDモード中は破壊できません！");
        }
    }

    /**
     * エンダードラゴンからのダメージを無効化
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // ダメージを与えた側がエンダードラゴンの場合
        if (event.getDamager() instanceof EnderDragon) {
            // プレイヤーへのダメージをキャンセル
            if (event.getEntity() instanceof Player) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * エンダードラゴン関連のダメージ（接触ダメージなど）を無効化
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        // ENDモードがアクティブでない場合は何もしない
        if (!plugin.getEndModeManager().isEndModeActive()) {
            return;
        }

        // ドラゴン関連のダメージタイプを無効化
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.DRAGON_BREATH) {
            event.setCancelled(true);
        }
    }

    /**
     * ドラゴンのブレス攻撃フェーズを防止
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDragonPhaseChange(EnderDragonChangePhaseEvent event) {
        // ENDモードがアクティブでない場合は何もしない
        if (!plugin.getEndModeManager().isEndModeActive()) {
            return;
        }

        // ブレス攻撃フェーズへの移行をキャンセル
        EnderDragon.Phase newPhase = event.getNewPhase();
        if (newPhase == EnderDragon.Phase.BREATH_ATTACK ||
            newPhase == EnderDragon.Phase.STRAFING) {
            event.setCancelled(true);
        }
    }

    /**
     * ドラゴンファイアボール（ブレスの弾）のスポーンを防止
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        // ENDモードがアクティブでない場合は何もしない
        if (!plugin.getEndModeManager().isEndModeActive()) {
            return;
        }

        // ドラゴンファイアボールをキャンセル
        if (event.getEntity() instanceof DragonFireball) {
            event.setCancelled(true);
        }
    }

    /**
     * AreaEffectCloud（ドラゴンブレスのエリア効果）のスポーンを防止
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onAreaEffectCloudSpawn(EntitySpawnEvent event) {
        // ENDモードがアクティブでない場合は何もしない
        if (!plugin.getEndModeManager().isEndModeActive()) {
            return;
        }

        // ドラゴンブレスのエリア効果雲をキャンセル
        if (event.getEntity() instanceof AreaEffectCloud) {
            AreaEffectCloud cloud = (AreaEffectCloud) event.getEntity();
            // ドラゴンが発生源の場合のみキャンセル
            if (cloud.getSource() instanceof EnderDragon) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * AreaEffectCloudの効果適用を防止（念のため）
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onAreaEffectCloudApply(AreaEffectCloudApplyEvent event) {
        // ENDモードがアクティブでない場合は何もしない
        if (!plugin.getEndModeManager().isEndModeActive()) {
            return;
        }

        // ドラゴンが発生源のエリア効果をキャンセル
        AreaEffectCloud cloud = event.getEntity();
        if (cloud.getSource() instanceof EnderDragon) {
            event.setCancelled(true);
        }
    }
}
