package dev.keybindprofile.mixin;

import dev.keybindprofile.Client;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.options.controls.ControlsScreen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Participate in vanilla layout so lists, clipping and resize use the enlarged header. */
@Mixin(OptionsSubScreen.class)
abstract class ProfileHeaderMixin extends Screen {
    @Shadow @Final public HeaderAndFooterLayout layout;
    protected ProfileHeaderMixin(Component title) { super(title); }

    @Inject(method="addTitle", at=@At("HEAD"), cancellable=true)
    private void keybindprofile$header(CallbackInfo callback) {
        if(!Client.headerEnabled(this))return;
        LinearLayout header=LinearLayout.vertical().spacing(6);
        header.defaultCellSetting().alignHorizontallyCenter();
        header.addChild(new StringWidget(title,font));
        header.addChild(Button.builder(Component.literal("keybindProfile"),button->Client.openProfiles(this))
                .width(Math.min(150,Math.max(60,width-32))).build());
        header.addChild(new dev.keybindprofile.ActiveProfileWidget(Math.min(320,Math.max(60,width-32)),font));
        header.arrangeElements();
        layout.setHeaderHeight(Math.max(layout.getHeaderHeight(),header.getHeight()+16));
        layout.addToHeader(header);
        callback.cancel();
    }
}
