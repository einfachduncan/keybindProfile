package dev.keybindprofile.mixin;

import dev.keybindprofile.Client;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Refresh cached vanilla labels after profile changes, including a hidden parent screen. */
@Mixin(KeyBindsScreen.class)
abstract class KeyBindsRefreshMixin {
    @Shadow private KeyBindsList keyBindsList;
    @Unique private long keybindprofile$seenRevision=-1;

    //? if >=26.1 {
    @Inject(method="extractRenderState",at=@At("HEAD"))
    //?} else {
    /*@Inject(method="render",at=@At("HEAD"))
    *///?}
    private void keybindprofile$refresh(CallbackInfo ci){
        long revision=Client.bindingRevision();
        if(keyBindsList!=null && keybindprofile$seenRevision!=revision){
            keyBindsList.refreshEntries();
            keybindprofile$seenRevision=revision;
        }
    }
}
