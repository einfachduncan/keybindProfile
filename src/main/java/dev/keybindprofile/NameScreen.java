package dev.keybindprofile;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import java.util.function.Consumer;
final class NameScreen extends Screen {
    final Screen parent;final String initial;final Consumer<String> save;
    NameScreen(Screen parent,String initial,Consumer<String> save){super(Client.text(Titles.of("profile name")));this.parent=parent;this.initial=initial;this.save=save;}
    @Override protected void init(){
        var field=addRenderableWidget(new EditBox(font,width/2-140,height/2-30,280,20,Client.text("Profile name")));field.setMaxLength(64);field.setValue(initial);
        addRenderableWidget(Button.builder(Client.text("Save"),b->{if(!field.getValue().isBlank()){Client.show(parent);save.accept(field.getValue().strip());}}).bounds(width/2-140,height/2,136,20).build());
        addRenderableWidget(Button.builder(Client.text("Cancel"),b->onClose()).bounds(width/2+4,height/2,136,20).build());setInitialFocus(field);
    }
    //? if >=26.1 {
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
        g.fill(0,0,width,height,0xF0141317);super.extractRenderState(g,mx,my,dt);
        g.centeredText(font,title,width/2,12,0xFFFFD6A3);
        g.centeredText(font,"Choose a name, for example Building or PvP.",width/2,height/2-50,0xFFE8D7C7);
    }
    //?} else {
    /*@Override public void render(GuiGraphics g,int mx,int my,float dt){
        g.fill(0,0,width,height,0xF0141317);super.render(g,mx,my,dt);
        g.drawCenteredString(font,title,width/2,12,0xFFFFD6A3);
        g.drawCenteredString(font,"Choose a name, for example Building or PvP.",width/2,height/2-50,0xFFE8D7C7);
    }
    *///?}
    @Override public void onClose(){Client.show(parent);}
}
