package dev.keybindprofile;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
//? if >=26.1 {
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
//?} else {
/*import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
*///?}
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.gui.screens.options.controls.ControlsScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.slf4j.LoggerFactory;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public final class Client implements ClientModInitializer {
    public static Client INSTANCE;
    final ExecutorService io=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"keybindProfile-storage");t.setDaemon(true);return t;});
    Model.Library library=new Model.Library();
    Model.Settings settings=new Model.Settings();
    Storage store;
    Path settingsFile;
    boolean ready,busy;
    String status="Loading profiles...";
    private String activeLabel="Loading profiles...";
    public static String activeProfileLabel(){return INSTANCE==null?"Loading profiles...":INSTANCE.activeLabel;}
    void updateActiveLabel(){if(ready)activeLabel=Model.activeLabel(library,settings.lastApplied,capture());}
    KeyMapping open,cycle;
    final Set<Integer> held=new HashSet<>();
    final Map<String,String> applied=new TreeMap<>();
    static Component text(String s){return Component.literal(s);}
    static Screen screen(){
        //? if >=26.2 {
        return Minecraft.getInstance().gui.screen();
        //?} else {
        /*return Minecraft.getInstance().screen;
        *///?}
    }
    static void show(Screen screen){
        //? if >=26.2 {
        Minecraft.getInstance().gui.setScreen(screen);
        //?} else {
        /*Minecraft.getInstance().setScreen(screen);
        *///?}
    }
    public static boolean headerEnabled(Screen screen) {
        Client app=INSTANCE;
        return app!=null && ((screen instanceof KeyBindsScreen && app.settings.bindsButton)
            || (screen instanceof ControlsScreen && app.settings.controlsButton));
    }
    public static void openProfiles(Screen parent) {show(new ProfileScreen(parent));}
    @Override public void onInitializeClient() {
        INSTANCE=this;
        String override=System.getProperty("keybindprofile.dir");
        Path root=override!=null?Path.of(override):Path.of(System.getProperty("user.home"),".keybindProfile");
        store=new Storage(root);settingsFile=FabricLoader.getInstance().getConfigDir().resolve("keybindprofile.json");
        //? if >=26.1 {
        open=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.keybindprofile.open",InputConstants.KEY_F8,KeyMapping.Category.MISC));
        cycle=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.keybindprofile.cycle",-1,KeyMapping.Category.MISC));
        //?} else {
        /*open=KeyBindingHelper.registerKeyBinding(new KeyMapping("key.keybindprofile.open",InputConstants.KEY_F8,KeyMapping.Category.MISC));
        cycle=KeyBindingHelper.registerKeyBinding(new KeyMapping("key.keybindprofile.cycle",-1,KeyMapping.Category.MISC));
        *///?}
        ClientLifecycleEvents.CLIENT_STARTED.register(mc->load(true));
        ClientLifecycleEvents.CLIENT_STOPPING.register(mc->io.shutdown());
        ClientTickEvents.END_CLIENT_TICK.register(mc->{
            if(screen() instanceof ProfileScreen || screen() instanceof KeyBindsScreen || screen() instanceof ControlsScreen)updateActiveLabel();
            boolean requested=false;while(open.consumeClick())requested=true;
            if(requested && !(screen() instanceof ProfileScreen))show(new ProfileScreen(screen()));
            boolean next=false;while(cycle.consumeClick())next=true;
            if(next && screen()==null && ready && !busy && !(mc.level!=null && settings.protectInWorld)) {
                var list=Model.ordered(library,settings.favoritesOnly);
                if(!list.isEmpty()){int index=-1;for(int i=0;i<list.size();i++)if(list.get(i).id.equals(settings.lastApplied))index=i;request(list.get((index+1)%list.size()),null,null);}
            }
            Set<Integer> down=new HashSet<>();
            if(ready) for(var p:library.profiles)if(!p.deleted && p.shortcut>=32) {
                //? if >=26.3 {
                /*boolean pressed=InputConstants.isKeyDown(p.shortcut);
                *///?} elif >=26.1 {
                boolean pressed=InputConstants.isKeyDown(mc.getWindow(),p.shortcut);
                //?} else {
                /*boolean pressed=InputConstants.isKeyDown(mc.getWindow(),p.shortcut);
                *///?}
                if(pressed)down.add(p.shortcut);
                if(pressed && !held.contains(p.shortcut) && mc.isWindowActive() && screen()==null && !busy && !(mc.level!=null && settings.protectInWorld)) {
                    long count=library.profiles.stream().filter(q->!q.deleted && q.shortcut==p.shortcut).count();
                    if(count==1){held.add(p.shortcut);request(p,null,null);}else status="Duplicate profile shortcut: change it in Settings";
                }
            }
            held.clear();held.addAll(down);
        });
    }
    void load(boolean startup) {
        if(busy)return;busy=true;
        io.execute(()->{try{
            var lib=store.read();var cfg=Storage.read(settingsFile,Model.Settings.class,new Model.Settings());validateSettings(cfg);
            Minecraft.getInstance().execute(()->{library=lib;settings=cfg;ready=true;busy=false;status="Ready";refresh();
                if(startup && !cfg.autoApply.isEmpty())lib.profiles.stream().filter(p->!p.deleted && p.id.equals(cfg.autoApply)).findFirst().ifPresent(p->apply(p,null,false,0));
            });
        }catch(Exception e){fail(e);}});
    }
    static void validateSettings(Model.Settings s) {
        if(s.undo==null||s.redo==null||s.undo.size()>20||s.redo.size()>20||s.autoApply==null||s.lastApplied==null)throw new IllegalArgumentException("Invalid instance settings");
        s.undo.forEach(Model::validateBinds);s.redo.forEach(Model::validateBinds);
    }
    void fail(Exception e){LoggerFactory.getLogger("keybindProfile").error("Profile operation failed",e);Minecraft.getInstance().execute(()->{busy=false;status="Could not finish: "+HelpText.error(e);});}
    void change(Consumer<Model.Library> action) { change(action,()->{}); }
    void change(Consumer<Model.Library> action,Runnable saved) {
        if(!ready||busy)return;busy=true;io.execute(()->{try{var lib=store.change(action);Minecraft.getInstance().execute(()->{library=lib;busy=false;status="Saved";saved.run();refresh();});}catch(Exception e){fail(e);}});
    }
    void edit(Model.Profile profile,Consumer<Model.Profile> operation) {
        String expected=Storage.JSON.toJson(profile);
        change(lib->{var found=lib.profiles.stream().filter(p->p.id.equals(profile.id)).findFirst().orElseThrow(()->new IllegalArgumentException("Profile removed; reload first"));
            if(!Storage.JSON.toJson(found).equals(expected))throw new IllegalArgumentException("Profile changed in another instance; reload first");operation.accept(found);
        });
    }
    void configure(Consumer<Model.Settings> change) {
        if(!ready||busy)return;var cfg=Storage.copy(settings);change.accept(cfg);busy=true;
        io.execute(()->{try{Storage.write(settingsFile,cfg);Minecraft.getInstance().execute(()->{settings=cfg;busy=false;status="Settings saved";refresh();});}catch(Exception e){fail(e);}});
    }
    private static long bindingRevision;
    public static long bindingRevision(){return bindingRevision;}
    void refresh(){updateActiveLabel();if(screen() instanceof ProfileScreen s)s.refresh();}
    Map<String,KeyMapping> mappings() {
        Map<String,KeyMapping> map=new TreeMap<>();
        for(KeyMapping key:Minecraft.getInstance().options.keyMappings) if(map.putIfAbsent(key.getName(),key)!=null)throw new IllegalArgumentException("Duplicate keybinding ID: "+key.getName());
        return map;
    }
    Map<String,String> capture(){Map<String,String> map=new TreeMap<>();mappings().forEach((id,key)->map.put(id,key.saveString()));return map;}
    Map<String,String> defaults(){Map<String,String> map=new TreeMap<>();mappings().forEach((id,key)->map.put(id,key.getDefaultKey().getName()));return map;}
    String version(){return FabricLoader.getInstance().getModContainer("minecraft").orElseThrow().getMetadata().getVersion().getFriendlyString();}
    void create(String name,Model.Profile source) {
        var p=new Model.Profile();p.name=name.strip();p.gameVersion=version();p.binds=source==null?capture():new TreeMap<>(source.binds);
        change(lib->{if(lib.profiles.stream().anyMatch(q->!q.deleted&&q.name.equalsIgnoreCase(p.name)))throw new IllegalArgumentException("Name already exists");p.order=lib.profiles.stream().mapToInt(q->q.order).max().orElse(-1)+1;lib.profiles.add(p);});
    }
    void request(Model.Profile p,Set<String> selected,Screen parent) {
        if(!ready||busy)return;
        Model.Profile snapshot=Storage.copy(p);Set<String> chosen=selected==null?null:Set.copyOf(selected);
        Map<String,String> before=capture(),target=Model.plan(before,defaults(),snapshot.binds,chosen,settings.resetMissing);
        int conflicts=Model.addedConflicts(before,target).size();
        if(settings.confirm || conflicts>0) {
            show(new ConfirmScreen(ok->{show(parent);if(ok)apply(snapshot,chosen,false,0);},text(Titles.of("load profile")),text("Load "+snapshot.name+"? "+(conflicts==0?"Your current keys will change. You can Undo this.":conflicts+" keys will control more than one action. Check the Conflicts filter if unsure."))));
        }else apply(snapshot,chosen,false,0);
    }
    void history(boolean redo){if(busy||!ready)return;var list=redo?settings.redo:settings.undo;if(list.isEmpty()){status="There is no earlier change to restore.";return;}
        var p=new Model.Profile();p.name=redo?"Redo":"Undo";p.binds=new TreeMap<>(list.get(list.size()-1));apply(p,null,true,redo?2:1);
    }
    void reset(Screen parent){show(new ConfirmScreen(ok->{show(parent);if(ok){var p=new Model.Profile();p.name="Default bindings";p.binds=defaults();apply(p,null,true,0);}},text(Titles.of("reset bindings")),text("Reset all installed bindings? Undo remains available.")));}
    void apply(Model.Profile profile,Set<String> selected,boolean special,int history) {
        if(busy||!ready)return;
        try {
            var keys=mappings();var before=capture();
            var target=Model.plan(before,defaults(),profile.binds,selected,!special&&settings.resetMissing);
            Map<String,InputConstants.Key> validated=new TreeMap<>();
            target.forEach((id,key)->{var parsed=InputConstants.getKey(key);if(!parsed.getName().equals(key))throw new IllegalArgumentException("Unsupported key: "+key);validated.put(id,parsed);});
            if(target.equals(before)){if(!special)configure(c->c.lastApplied=profile.id);status="These keys are already in use. Nothing changed.";return;}
            var previous=Storage.copy(settings);
            var cfg=Storage.copy(settings);
            if(history==1){cfg.undo.remove(cfg.undo.size()-1);Model.push(cfg.redo,before);}
            else if(history==2){cfg.redo.remove(cfg.redo.size()-1);Model.push(cfg.undo,before);}
            else {Model.push(cfg.undo,before);cfg.redo.clear();}
            if(!special)cfg.lastApplied=profile.id;
            busy=true;status="Saving recovery history...";
            io.execute(()->{try {
                Storage.write(settingsFile,cfg);
                Minecraft.getInstance().execute(()->{
                    try {
                        if(!mappings().equals(keys))throw new IllegalStateException("Binding registry changed; apply cancelled");
                        BindingTransaction.commit(before,target,this::capture,values->{
                            KeyMapping.releaseAll();values.forEach((id,key)->keys.get(id).setKey(InputConstants.getKey(key)));KeyMapping.resetMapping();bindingRevision++;Minecraft.getInstance().options.save();
                        });
                        settings=cfg;applied.clear();applied.putAll(target);status="Loaded "+profile.name+". Undo restores your previous keys.";busy=false;refresh();
                    }catch(Exception e){
                        io.execute(()->{try{Storage.write(settingsFile,previous);}catch(Exception recovery){e.addSuppressed(recovery);}fail(e);});
                    }

                });
            }catch(Exception e){fail(e);}});
        }catch(Exception e){fail(e);}
    }
}
