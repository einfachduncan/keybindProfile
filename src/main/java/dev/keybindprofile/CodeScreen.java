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

final class CodeScreen extends Screen {
    private final Screen parent;
    private String value,message="";
    private final Consumer<String> accept;
    private EditBox field;
    CodeScreen(Screen parent,String value,Consumer<String> accept){super(Client.text(Titles.of(accept==null?"share profile":"import profile")));this.parent=parent;this.value=value;this.accept=accept;}
    @Override protected void init(){
        int w=Math.min(600,width-24),x=(width-w)/2,y=Math.max(32,height/2-32);
        field=addRenderableWidget(new EditBox(font,x,y,w,20,Client.text("Share code")));
        field.setMaxLength(Storage.CODE_LIMIT);field.setValue(value);field.setResponder(v->value=v);field.setEditable(accept!=null);
        int half=(w-4)/2;
        addRenderableWidget(Button.builder(Client.text(accept==null?"Copy code":"Check code"),b->{
            if(accept==null) {
                try{ShareDelivery.copy(field.getValue(),minecraft.keyboardHandler::setClipboard,minecraft.keyboardHandler::getClipboard);message="Copied. Send the entire code to the other player.";}
                catch(Exception e){message=HelpText.error(e);}
            }else {String code=field.getValue();if(code.isBlank()){message="Paste a profile share code first.";return;}Client.show(parent);accept.accept(code);}
        }).bounds(x,y+28,half,20).build());
        addRenderableWidget(Button.builder(Client.text("Back"),b->onClose()).bounds(x+half+4,y+28,half,20).build());
        setInitialFocus(field);field.setCursorPosition(0);field.setHighlightPos(value.length());
    }
    //? if >=26.1 {
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
        g.fill(0,0,width,height,0xF0141317);super.extractRenderState(g,mx,my,dt);
        g.centeredText(font,title,width/2,12,0xFFFFD6A3);
        g.centeredText(font,accept==null?"Select all: Ctrl+A | Copy: Ctrl+C":"Paste: Ctrl+V | Check code, then Save profile",width/2,28,0xFFE8D7C7);
        g.centeredText(font,font.plainSubstrByWidth(message,width-24),width/2,height-24,0xFFE8D7C7);
    }
    //?} else {
    /*@Override public void render(GuiGraphics g,int mx,int my,float dt){
        g.fill(0,0,width,height,0xF0141317);super.render(g,mx,my,dt);
        g.drawCenteredString(font,title,width/2,12,0xFFFFD6A3);
        g.drawCenteredString(font,accept==null?"Select all: Ctrl+A | Copy: Ctrl+C":"Paste: Ctrl+V | Check code, then Save profile",width/2,28,0xFFE8D7C7);
        g.drawCenteredString(font,font.plainSubstrByWidth(message,width-24),width/2,height-24,0xFFE8D7C7);
    }
    *///?}
    @Override public void onClose(){Client.show(parent);}
}
