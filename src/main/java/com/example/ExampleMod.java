package com.example.examplemod; // ВАЖНО: Оставь тот package, который был в файле изначально!

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public class ExampleModClient implements ClientModInitializer {

    // === НАСТРОЙКИ ===
    private static final double RANGE = 3.0;          // Дистанция удара (макс 3.0)
    private static final int DELAY_MS = 50;           // Фиксированная задержка (50мс)
    private static final boolean ONLY_PLAYERS = true; // Бить только игроков
    private static final boolean WEAPON_ONLY = true;  // Бить только мечом/топором
    
    private long lastAttackTime = 0;

    @Override
    public void onInitializeClient() {
        // Слушаем конец каждого игрового тика
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;
            if (client.currentScreen != null) return; // Не бьем, если открыт чат/инвентарь
            
            checkAndAttack(client);
        });
    }

    private void checkAndAttack(MinecraftClient mc) {
        // 1. Проверка оружия в руке
        if (WEAPON_ONLY) {
            String itemName = mc.player.getMainHandStack().getItem().toString();
            if (!itemName.contains("sword") && !itemName.contains("axe")) return;
        }

        // 2. Кулдаун атаки (чтобы крит сработал на полную силу)
        if (mc.player.getAttackCooldownProgress(0.0f) < 0.9f) return;

        // 3. Проверка на крит (Падение)
        // Бить можно только если мы падаем (velocityY < 0) и мы не на земле
        boolean isFalling = mc.player.getVelocity().y < 0 && !mc.player.isOnGround();
        boolean canCrit = isFalling && !mc.player.isTouchingWater() && !mc.player.isInLava();
        
        if (!canCrit) return;

        // 4. Raycast (наводим прицел)
        HitResult hit = mc.crosshairTarget;
        if (hit == null || hit.getType() != HitResult.Type.ENTITY) return;

        Entity target = ((EntityHitResult) hit).getEntity();

        // 5. Фильтры цели
        if (!(target instanceof LivingEntity)) return;
        if (target == mc.player) return;
        if (ONLY_PLAYERS && !(target instanceof PlayerEntity)) return;
        if (mc.player.distanceTo(target) > RANGE) return;

        // 6. Фиксированная задержка
        long now = System.currentTimeMillis();
        if (now - lastAttackTime < DELAY_MS) return;
        lastAttackTime = now;

        // 7. Атака (крит)
        if (mc.interactionManager != null) {
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }
}
