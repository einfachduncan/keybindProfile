package dev.keybindprofile;

//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import java.util.*;

final class ProfileScreen extends Screen {
    private final Screen parent;
    private final Client app=Client.INSTANCE;
    private String mode="Profiles",query="",filter="All";
    private int page,rows;
    private Model.Profile selected,imported;
    private final Set<String> checked=new HashSet<>();
    private final List<Button> listButtons=new ArrayList<>();
    private boolean trash,waitingKey;
    private EditBox search,name;
    private int left,wide;
    ProfileScreen(Screen parent){super(Client.text(Titles.of("keybind profile")));this.parent=parent;}
    Button button(String label,int x,int y,int w,Runnable action){
        var button=addRenderableWidget(Button.builder(Client.text(label),b->{if(!app.busy)try{action.run();}catch(Exception e){app.fail(e);}}).bounds(x,y,w,20).build());
        button.setTooltip(Tooltip.create(Client.text(HelpText.tip(label))));return button;
    }
    void refresh(){if(selected!=null&&!selected.id.equals("import"))selected=app.library.profiles.stream().filter(p->p.id.equals(selected.id)).findFirst().orElse(selected);rebuildWidgets();}
    void tab(String next){mode=next;query="";filter="All";page=0;waitingKey=false;rebuildWidgets();}
    @Override protected void init(){
        wide=Math.min(700,width-16);left=(width-wide)/2;rows=Math.max(1,(height-184)/23);
        int nav=(wide-20)/6;
        button("Profiles",left,30,nav,()->tab("Profiles"));button("Settings",left+nav+4,30,nav,()->tab("Settings"));
        button("Undo",left+2*(nav+4),30,nav,()->app.history(false));button("Redo",left+3*(nav+4),30,nav,()->app.history(true));
        button("Help",left+4*(nav+4),30,nav,()->Client.show(new GuideScreen(this)));
        button("Close",left+5*(nav+4),30,nav,this::onClose);
        if(!app.ready)return;
        if(mode.equals("Settings")){settings();return;}
        int filters=wide/3;
        search=addRenderableWidget(new EditBox(font,left,58,wide-filters-6,20,Client.text("Search")));
        search.setMaxLength(128);search.setValue(query);search.setHint(Client.text(mode.equals("Profiles")?"Find a saved profile...":"Find an action or key..."));
        search.setResponder(v->{query=v;page=0;list();});
        if(mode.equals("Profiles")) {
            button(trash?"Show saved":"Deleted",left+wide-filters,58,(filters-4)/2,()->{trash=!trash;page=0;rebuildWidgets();});
            button(app.settings.favoritesOnly?"Show all":"Favorites",left+wide-(filters-4)/2,58,(filters-4)/2,()->app.configure(c->c.favoritesOnly=!c.favoritesOnly));
            name=addRenderableWidget(new EditBox(font,left,height-84,wide-110,20,Client.text("Profile name")));name.setMaxLength(64);name.setHint(Client.text("Name your profile, e.g. Building"));
            button("Save keys",left+wide-104,height-84,104,()->{if(!name.getValue().isBlank())app.create(name.getValue(),null);else app.status="Enter a profile name";});
            int footer=(wide-12)/4;
            button("Add code",left,height-58,footer,this::paste);
            button("Refresh",left+footer+4,height-58,footer,()->app.load(false));
            button("Open folder",left+2*(footer+4),height-58,footer,this::openFolder);
            button("Defaults",left+3*(footer+4),height-58,footer,()->app.reset(this));
        } else {
            button(filter,left+wide-filters,58,filters,()->{var choices=List.of("All","Changed","Conflicts","Unbound","Missing mods","Selected");filter=choices.get((choices.indexOf(filter)+1)%choices.size());page=0;rebuildWidgets();});
            int aw=(wide-12)/4;
            button("Select results",left,height-84,aw,()->{checked.addAll(visibleIds());list();});
            button("Select none",left+aw+4,height-84,aw,()->{checked.clear();list();});
            button("Load checked",left+2*(aw+4),height-84,aw,()->{if(checked.isEmpty())app.status="Click the actions you want, then choose Load checked";else app.request(selected,checked,this);});
            button(imported!=null?"Save profile":"Load all",left+3*(aw+4),height-84,aw,()->{if(imported!=null){var p=Storage.copy(imported);app.change(lib->{if(lib.profiles.stream().anyMatch(q->q.id.equals(p.id)))throw new IllegalArgumentException("Already imported");p.name=Model.importedName(p.name,lib);p.order=lib.profiles.size();lib.profiles.add(p);},()->{imported=null;tab("Profiles");});}else app.request(selected,null,this);});
            if(imported==null) {
                int bw=(wide-20)/6;
                button(selected.favorite?"Unstar":"Star",left+0*(bw+4),height-58,bw,()->app.edit(selected,p->p.favorite=!p.favorite));
                button("Rename",left+1*(bw+4),height-58,bw,()->prompt(selected.name,value->app.edit(selected,p->p.name=value)));
                button("Copy",left+2*(bw+4),height-58,bw,()->prompt(selected.name+" copy",value->app.create(value,selected)));
                button("Overwrite",left+3*(bw+4),height-58,bw,()->confirm("Save your CURRENT keys over this profile? Its old saved keys will be replaced.",()->{var current=app.capture();app.edit(selected,p->p.binds=new TreeMap<>(current));}));
                button("Share",left+4*(bw+4),height-58,bw,this::share);
                button("Delete",left+5*(bw+4),height-58,bw,()->confirm("Move this profile to Trash?",()->{app.edit(selected,p->p.deleted=true);tab("Profiles");}));
            }
        }
        button("<",left+wide-64,height-112,28,()->{page=Math.max(0,page-1);list();});
        button(">",left+wide-30,height-112,28,()->{page++;list();});
        list();
    }
    List<String> visibleIds(){
        var current=app.capture();var target=Model.plan(current,app.defaults(),selected.binds,null,app.settings.resetMissing);
        Set<String> all=new TreeSet<>(selected.binds.keySet());all.addAll(current.keySet());
        Set<String> conflicts=new HashSet<>();Model.conflicts(target).forEach(c->{conflicts.addAll(c.actions());});
        String q=query.toLowerCase(Locale.ROOT);
        return all.stream().filter(id->{String now=current.get(id),next=target.getOrDefault(id,selected.binds.get(id));String label=Component.translatable(id).getString();
            return (id+" "+label+" "+now+" "+next).toLowerCase(Locale.ROOT).contains(q) && switch(filter){case "Changed"->now!=null&&!Objects.equals(now,next);case "Conflicts"->conflicts.contains(id);case "Unbound"->"key.keyboard.unknown".equals(next);case "Missing mods"->now==null;case "Selected"->checked.contains(id);default->true;};
        }).toList();
    }
    void list(){
        for(var b:listButtons)removeWidget(b);listButtons.clear();
        if(!app.ready||mode.equals("Settings"))return;
        if(mode.equals("Profiles")){
            var current=app.capture();
            var profiles=Model.visibleProfiles(app.library,trash,app.settings.favoritesOnly,query);
            if(!trash && app.settings.favoritesOnly && !Model.hasFavorites(app.library))app.status="No favorites yet - showing all profiles";
            if(profiles.isEmpty()) {
                String message=trash?"Trash is empty":!query.isBlank()?"No matches - clear search":"No profiles yet - save your current bindings below";
                listButtons.add(button(message,left,86,wide,()->{query="";if(search!=null)search.setValue("");}));
            }
            page=Math.max(0,Math.min(page,Math.max(0,(profiles.size()-1)/rows)));
            for(int i=page*rows;i<Math.min(profiles.size(),(page+1)*rows);i++){
                var p=profiles.get(i);int y=86+(i-page*rows)*23;
                boolean differs=p.binds.entrySet().stream().anyMatch(e->current.containsKey(e.getKey())&&!Objects.equals(current.get(e.getKey()),e.getValue()));
                String marker=p.id.equals(app.settings.lastApplied)?(differs?"[modified] ":"[active] "):"";
                listButtons.add(button((p.favorite?"* ":"")+marker+p.name+" ("+p.binds.size()+")",left,y,wide-(trash?85:150),()->{if(trash)app.edit(p,v->v.deleted=false);else {selected=p;imported=null;checked.clear();tab("Details");}}));
                if(trash)listButtons.add(button("Restore",left+wide-80,y,80,()->app.edit(p,v->v.deleted=false)));
                else {
                    listButtons.add(button("Load",left+wide-146,y,70,()->app.request(p,null,this)));
                    listButtons.add(button("Up",left+wide-72,y,34,()->move(p,-1)));
                    listButtons.add(button("Down",left+wide-34,y,34,()->move(p,1)));
                }
            }
        } else if(selected!=null){
            var ids=visibleIds();var current=app.capture();var target=Model.plan(current,app.defaults(),selected.binds,null,app.settings.resetMissing);
            page=Math.max(0,Math.min(page,Math.max(0,(ids.size()-1)/rows)));
            for(int i=page*rows;i<Math.min(ids.size(),(page+1)*rows);i++){
                String id=ids.get(i),old=current.get(id),next=target.getOrDefault(id,selected.binds.get(id));
                String label=(checked.contains(id)?"[x] ":"[ ] ")+Component.translatable(id).getString()+": "+keyName(old)+" -> "+keyName(next);
                var b=button(font.plainSubstrByWidth(label,wide-12),left,86+(i-page*rows)*23,wide,()->{if(!checked.remove(id))checked.add(id);list();});
                b.setTooltip(Tooltip.create(Client.text("Current key -> saved key\n"+label+"\nClick to select or deselect.\n"+id)));listButtons.add(b);
            }
        }
    }
    static String keyName(String key){
        if(key==null)return "Mod not installed";
        if(key.equals("key.keyboard.unknown"))return "Not assigned";
        try{return com.mojang.blaze3d.platform.InputConstants.getKey(key).getDisplayName().getString();}catch(RuntimeException e){return "Unknown key";}
    }
    static String shortcutName(int key){
        if(key<0)return "Not assigned";
        //? if >=26.3 {
        /*return keyName(com.mojang.blaze3d.platform.InputConstants.Type.KEYBOARD.getOrCreate(key).getName());
        *///?} else {
        return keyName(com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(key).getName());
        //?}
    }
    void move(Model.Profile p,int delta){app.change(lib->{var list=new ArrayList<>(Model.ordered(lib,false));int i=-1;for(int n=0;n<list.size();n++)if(list.get(n).id.equals(p.id))i=n;int other=i+delta;if(i>=0&&other>=0&&other<list.size())Collections.swap(list,i,other);for(int n=0;n<list.size();n++)list.get(n).order=n;});}
    void settings(){
        String auto=app.library.profiles.stream().filter(p->p.id.equals(app.settings.autoApply)).map(p->p.name).findFirst().orElse("Off");
        var labels=List.of("Ask before loading: "+HelpText.on(app.settings.confirm),"Allow profile hotkeys while playing: "+HelpText.on(!app.settings.protectInWorld),
            "Keys not saved in profile: "+(app.settings.resetMissing?"Reset to default":"Keep current"),"Button in Controls: "+HelpText.on(app.settings.controlsButton),
            "Button in Key Binds: "+HelpText.on(app.settings.bindsButton),"Load at startup: "+auto,
            selected==null?"Choose a profile first to set its shortcut":(waitingKey?"Press a key; Escape clears":"Shortcut: "+selected.name+" / "+shortcutName(selected.shortcut)),"Permanently delete trash");
        List<Runnable> actions=List.of(
            ()->app.configure(c->c.confirm=!c.confirm),()->app.configure(c->c.protectInWorld=!c.protectInWorld),
            ()->app.configure(c->c.resetMissing=!c.resetMissing),()->app.configure(c->c.controlsButton=!c.controlsButton),
            ()->app.configure(c->c.bindsButton=!c.bindsButton),
            ()->{var list=Model.ordered(app.library,false);var ids=new ArrayList<String>();ids.add("");list.forEach(p->ids.add(p.id));app.configure(c->c.autoApply=ids.get((ids.indexOf(c.autoApply)+1)%ids.size()));},
            ()->{if(selected!=null){waitingKey=true;rebuildWidgets();}},
            ()->confirm("Permanently remove all trashed profiles?",()->app.change(lib->lib.profiles.removeIf(p->p.deleted)))
        );
        int capacity=Math.max(1,(height-124)/24);page=Math.min(page,(labels.size()-1)/capacity);
        for(int i=page*capacity;i<Math.min(labels.size(),(page+1)*capacity);i++)button(labels.get(i),left,62+(i-page*capacity)*24,wide,actions.get(i));
        button("Previous",left,height-58,100,()->{page=Math.max(0,page-1);rebuildWidgets();});
        button("Next",left+wide-100,height-58,100,()->{page++;rebuildWidgets();});
    }
    @Override public boolean keyPressed(KeyEvent event){
        if(waitingKey && selected!=null){waitingKey=false;int key=event.key()==256?-1:event.key();if(key>=32||key==-1)app.edit(selected,p->{if(key!=-1&&app.library.profiles.stream().anyMatch(q->!q.deleted&&!q.id.equals(p.id)&&q.shortcut==key))throw new IllegalArgumentException("Shortcut already assigned");p.shortcut=key;});return true;}return super.keyPressed(event);
    }
    void confirm(String message,Runnable action){Client.show(new ConfirmScreen(ok->{Client.show(this);if(ok)action.run();},Client.text(Titles.of("confirm")),Client.text(message)));}
    void prompt(String initial,java.util.function.Consumer<String> save){Client.show(new NameScreen(this,initial,save));}
    void paste(){
        String code="";try{code=minecraft.keyboardHandler.getClipboard();}catch(RuntimeException e){app.status="Clipboard unavailable; paste the code manually";}
        if(code.length()>Storage.CODE_LIMIT)code="";
        Client.show(new CodeScreen(this,code,this::importCode));
    }
    void importCode(String code){
        app.busy=true;app.io.execute(()->{try{var p=Storage.decode(code);minecraft.execute(()->{
            try{p.name=Model.importedName(p.name,app.library);imported=p;selected=p;checked.clear();tab("Details");app.status="Check the keys below, then click Save profile. Your keys will not change.";}
            catch(Exception e){app.fail(e);}finally{app.busy=false;}
        });}catch(Exception e){app.fail(e);}});
    }
    void openFolder(){app.io.execute(()->{try{java.nio.file.Files.createDirectories(app.store.root());String os=System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            String opener=os.contains("win")?"explorer.exe":os.contains("mac")?"open":"xdg-open";
            new ProcessBuilder(opener,app.store.root().toAbsolutePath().toString()).start();}catch(Exception e){app.fail(e);}});}
    void share(){
        var p=Storage.copy(selected);app.busy=true;
        app.io.execute(()->{try{String code=Storage.encode(p);minecraft.execute(()->{
            try{Client.show(new CodeScreen(this,code,null));app.status="Share code ready";}
            catch(Exception e){app.fail(e);}finally{app.busy=false;}
        });}catch(Exception e){app.fail(e);}});
    }
    //? if >=26.1 {
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
        g.fill(0,0,width,height,0xF0141317);g.fill(left,23,left+wide,25,0xFFC98250);
        super.extractRenderState(g,mx,my,dt);
        g.centeredText(font,font.plainSubstrByWidth(title.getString()+" | "+Client.activeProfileLabel(),wide),width/2,10,0xFFFFD6A3);
        g.text(font,font.plainSubstrByWidth(app.status,wide),left,height-22,0xFFE8D7C7);
        if(!mode.equals("Settings"))g.text(font,mode+" | Page "+(page+1)+(selected!=null&&mode.equals("Details")?" | "+selected.name+" | "+selected.gameVersion:""),left,height-106,0xFFB8A79B);
    }
    //?} else {
    /*@Override public void render(GuiGraphics g,int mx,int my,float dt){
        g.fill(0,0,width,height,0xF0141317);g.fill(left,23,left+wide,25,0xFFC98250);
        super.render(g,mx,my,dt);
        g.drawCenteredString(font,font.plainSubstrByWidth(title.getString()+" | "+Client.activeProfileLabel(),wide),width/2,10,0xFFFFD6A3);
        g.drawString(font,font.plainSubstrByWidth(app.status,wide),left,height-22,0xFFE8D7C7);
        if(!mode.equals("Settings"))g.drawString(font,mode+" | Page "+(page+1)+(selected!=null&&mode.equals("Details")?" | "+selected.name+" | "+selected.gameVersion:""),left,height-106,0xFFB8A79B);
    }
    *///?}
    @Override public boolean isPauseScreen(){return true;}
    @Override public void onClose(){Client.show(parent);}
}
