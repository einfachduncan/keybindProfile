package dev.keybindprofile;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import java.util.*;
final class GuideScreen extends Screen {
    final Screen parent;
    int page,capacity;
    final List<FormattedCharSequence> lines=new ArrayList<>();
    GuideScreen(Screen parent){super(Client.text(Titles.of("quick guide")));this.parent=parent;}
    @Override protected void init(){
        lines.clear();
        for(String paragraph:List.of(
            "A profile is a saved set of keys. For example: Building or PvP.",
            "1. SAVE: Set your keys in Minecraft. Open keybindProfile, type a name, then click Save keys.",
            "2. LOAD: Find a saved profile and click Load. This changes your keys. Undo puts them back.",
            "3. CHECK: Click a profile name to inspect it. Each row shows your current key -> the profile key. Check rows and use Load checked to change only those actions.",
            "4. SHARE: Open a profile and click Share, then Copy code. Send the entire code to your friend. They choose Add code, paste it, click Check code, then Save profile. Saving does not load it.",
            "Overwrite saves your CURRENT keys into an EXISTING profile. Load does the opposite: it uses the profile's saved keys.",
            "Star marks a favorite. Deleted contains removed profiles you can Restore. Permanently delete trash cannot be undone.",
            "Settings: ON means enabled, OFF means disabled. Hover over any button to see what it does. Profile shortcuts are blocked while playing by default."
        )){lines.addAll(font.split(Component.literal(paragraph),Math.max(80,width-40)));lines.add(FormattedCharSequence.EMPTY);}
        capacity=Math.max(1,(height-78)/12);page=Math.min(page,Math.max(0,(lines.size()-1)/capacity));
        addRenderableWidget(Button.builder(Client.text("Previous"),b->{page=Math.max(0,page-1);}).bounds(12,height-32,80,20).build());
        addRenderableWidget(Button.builder(Client.text("Next"),b->{page=Math.min((lines.size()-1)/capacity,page+1);}).bounds(width-92,height-32,80,20).build());
        addRenderableWidget(Button.builder(Client.text("Back"),b->onClose()).bounds(width/2-40,height-32,80,20).build());
    }
    //? if >=26.1 {
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
        g.fill(0,0,width,height,0xF0141317);super.extractRenderState(g,mx,my,dt);g.centeredText(font,title,width/2,12,0xFFFFD6A3);
        for(int i=page*capacity;i<Math.min(lines.size(),(page+1)*capacity);i++)g.text(font,lines.get(i),20,36+(i-page*capacity)*12,0xFFE8D7C7);
    }
    //?} else {
    /*@Override public void render(GuiGraphics g,int mx,int my,float dt){
        g.fill(0,0,width,height,0xF0141317);super.render(g,mx,my,dt);g.drawCenteredString(font,title,width/2,12,0xFFFFD6A3);
        for(int i=page*capacity;i<Math.min(lines.size(),(page+1)*capacity);i++)g.drawString(font,lines.get(i),20,36+(i-page*capacity)*12,0xFFE8D7C7);
    }
    *///?}
    @Override public void onClose(){Client.show(parent);}
}
