package dev.keybindprofile;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.network.chat.Component;
public final class ActiveProfileWidget extends StringWidget {
    private final Font labelFont;
    public ActiveProfileWidget(int width,Font font){super(width,12,Component.literal(""),font);labelFont=font;}
    //? if >=26.1 {
    @Override public void extractWidgetRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
        setMessage(Component.literal(labelFont.plainSubstrByWidth(Client.activeProfileLabel(),getWidth())));
        super.extractWidgetRenderState(g,mx,my,dt);
    }
    //?} else {
    /*@Override public void renderWidget(GuiGraphics g,int mx,int my,float dt){
        setMessage(Component.literal(labelFont.plainSubstrByWidth(Client.activeProfileLabel(),getWidth())));
        super.renderWidget(g,mx,my,dt);
    }
    *///?}
}
