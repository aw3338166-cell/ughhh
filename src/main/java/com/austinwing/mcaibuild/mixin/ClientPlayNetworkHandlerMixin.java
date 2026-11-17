package com.austinwing.mcaibuild.mixin;

import com.austinwing.mcaibuild.RedstoneSoundDetector;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onPlaySound", at = @At("HEAD"))
    private void minecraftAiBuild$detectRedstoneSound(PlaySoundS2CPacket packet, CallbackInfo ci) {
        Identifier id = Registries.SOUND_EVENT.getId(packet.getSound().value());
        if (id != null) {
            String soundId = id.toString();
            MinecraftClient client = MinecraftClient.getInstance();
            client.execute(() -> RedstoneSoundDetector.record(soundId, packet.getX(), packet.getY(), packet.getZ()));
        }
    }
}
