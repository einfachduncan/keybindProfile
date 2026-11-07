package dev.keybindprofile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;

class CoreTest {
    @TempDir Path dir;
    Model.Profile profile(){var p=new Model.Profile();p.name="Combat";p.binds.put("key.jump","key.keyboard.space");return p;}
    @Test void keepsMissingBindings(){assertEquals(Map.of("a","x","b","z"),Model.plan(Map.of("a","old","b","z"),Map.of("b","default"),Map.of("a","x"),null,false));}
    @Test void resetsMissingOnlyWhenRequested(){assertEquals("default",Model.plan(Map.of("a","old","b","z"),Map.of("b","default"),Map.of("a","x"),null,true).get("b"));}
    @Test void selectedBindingsOnly(){assertEquals(Map.of("a","x","b","old"),Model.plan(Map.of("a","old","b","old"),Map.of(),Map.of("a","x","b","y"),Set.of("a"),false));}
    @Test void missingModsNeverAddedToRuntime(){assertEquals(Map.of("a","x"),Model.plan(Map.of("a","old"),Map.of(),Map.of("a","x","absent","y"),null,false));}
    @Test void detectsReplacedConflictAtSameCount(){var before=Map.of("a","x","b","x","c","y");var after=Map.of("a","x","b","y","c","y");assertEquals(1,Model.addedConflicts(before,after).size());}
    @Test void unknownKeysAreNotConflicts(){assertTrue(Model.conflicts(Map.of("a","key.keyboard.unknown","b","key.keyboard.unknown")).isEmpty());}
    @Test void boundedConflictMemory(){Map<String,String> binds=new HashMap<>();for(int i=0;i<8192;i++)binds.put("key."+i,"x");assertEquals(1,Model.conflicts(binds).size());}
    @Test void immutableHistoryAndLimit(){var history=new ArrayList<Map<String,String>>();var source=new HashMap<>(Map.of("a","x"));Model.push(history,source);source.put("a","y");assertEquals("x",history.get(0).get("a"));for(int i=0;i<30;i++)Model.push(history,source);assertEquals(20,history.size());}
    @Test void staleApplyDoesNotTouchBindings(){var writes=new AtomicInteger();assertThrows(IllegalStateException.class,()->BindingTransaction.commit(Map.of("a","old"),Map.of("a","new"),()->Map.of("a","user-change"),x->writes.incrementAndGet()));assertEquals(0,writes.get());}
    @Test void partialFailureRollsBack(){Map<String,String> state=new HashMap<>(Map.of("a","old"));assertThrows(IllegalArgumentException.class,()->BindingTransaction.commit(Map.of("a","old"),Map.of("a","new"),()->new HashMap<>(state),x->{state.putAll(x);if(x.get("a").equals("new"))throw new IllegalArgumentException();}));assertEquals("old",state.get("a"));}
    @Test void profileRoundTrip()throws Exception{var s=new Storage(dir);var p=profile();s.change(l->l.profiles.add(p));assertEquals(p.binds,s.read().profiles.get(0).binds);}
    @Test void changesMergeFreshDiskState()throws Exception{var a=new Storage(dir);var b=new Storage(dir);a.change(l->l.profiles.add(profile()));b.change(l->{var p=profile();p.name="Second";l.profiles.add(p);});assertEquals(2,a.read().profiles.size());}
    @Test void lockRejectsConcurrentWriter()throws Exception{var s=new Storage(dir);try(var channel=FileChannel.open(dir.resolve("profiles.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);var lock=channel.lock()){assertThrows(IOException.class,()->s.change(l->l.profiles.add(profile())));}assertFalse(Files.exists(dir.resolve("profiles.json")));}
    @Test void corruptFilePreserved()throws Exception{var file=dir.resolve("profiles.json");Files.writeString(file,"broken{");assertThrows(IOException.class,()->new Storage(dir).change(l->l.profiles.add(profile())));assertEquals("broken{",Files.readString(file));}
    @Test void badMutationDoesNotReplaceFile()throws Exception{var s=new Storage(dir);s.change(l->l.profiles.add(profile()));String before=Files.readString(dir.resolve("profiles.json"));assertThrows(IllegalArgumentException.class,()->s.change(l->l.profiles.get(0).name=""));assertEquals(before,Files.readString(dir.resolve("profiles.json")));}
    @Test void shareRoundTripUsesNewIdentity()throws Exception{var p=profile();p.shortcut=65;var q=Storage.decode(Storage.encode(p));assertEquals(p.binds,q.binds);assertNotEquals(p.id,q.id);assertEquals(-1,q.shortcut);}
    @Test void rejectsOversizedEncodedInput(){assertThrows(IOException.class,()->Storage.decode("X".repeat(Storage.CODE_LIMIT+1)));}
    @Test void rejectsExpandedBomb()throws Exception{var out=new ByteArrayOutputStream();try(var gzip=new GZIPOutputStream(out)){gzip.write(new byte[Storage.LIMIT+1]);}String code="KBP1."+Base64.getUrlEncoder().encodeToString(out.toByteArray());assertThrows(IOException.class,()->Storage.decode(code));}
    @Test void rejectsUnknownSchema()throws Exception{var s=new Storage(dir);Files.writeString(dir.resolve("profiles.json"),"{\"format\":99,\"profiles\":[]}");assertThrows(IOException.class,s::read);}
    @Test void rejectsUnknownShareSchema()throws Exception{var p=profile();p.format=2;assertThrows(IllegalArgumentException.class,()->Storage.encode(p));}
    @Test void favoritesAndTrashAreExcluded(){var lib=new Model.Library();var p=profile();var q=profile();q.favorite=true;var r=profile();r.favorite=true;r.deleted=true;lib.profiles.addAll(List.of(p,q,r));assertEquals(List.of(q),Model.ordered(lib,true));}
    @Test void keepsNoTemporaryFiles()throws Exception{Storage.write(dir.resolve("a.json"),profile());try(var files=Files.list(dir)){assertFalse(files.anyMatch(p->p.toString().endsWith(".tmp")));}}
    @Test void duplicateIdsRejected(){var lib=new Model.Library();var p=profile();lib.profiles.add(p);lib.profiles.add(p);assertThrows(IllegalArgumentException.class,lib::validate);}
    @Test void namesAreUniqueIgnoringCase(){var lib=new Model.Library();var a=profile();var b=profile();b.name="combat";lib.profiles.addAll(List.of(a,b));assertThrows(IllegalArgumentException.class,lib::validate);}
    @Test void trashCanRetainOldName(){var lib=new Model.Library();var a=profile();var b=profile();b.deleted=true;lib.profiles.addAll(List.of(a,b));assertDoesNotThrow(lib::validate);}
    @Test void maliciousNameNeverBecomesPath()throws Exception{var s=new Storage(dir);var p=profile();p.name="../outside";s.change(l->l.profiles.add(p));assertTrue(Files.exists(dir.resolve("profiles.json")));assertEquals("../outside",s.read().profiles.get(0).name);}
    @Test void titleUsesSmallCaps(){assertNotEquals("profile",Titles.of("Profile"));assertFalse(Titles.of("Profile").contains("?"));}
    @Test void noFavoritesStillShowsSavedProfiles(){var lib=new Model.Library();var p=profile();lib.profiles.add(p);assertEquals(List.of(p),Model.visibleProfiles(lib,false,true,""));}
    @Test void favoritesFilterWorksWhenFavoritesExist(){var lib=new Model.Library();var p=profile();var q=profile();q.name="Other";q.favorite=true;lib.profiles.addAll(List.of(p,q));assertEquals(List.of(q),Model.visibleProfiles(lib,false,true,""));}
    @Test void trashedFavoritesDoNotHideActiveProfiles(){var lib=new Model.Library();var p=profile();var q=profile();q.favorite=true;q.deleted=true;lib.profiles.addAll(List.of(p,q));assertEquals(List.of(p),Model.visibleProfiles(lib,false,true,""));}
    @Test void searchIsCaseInsensitiveAndTrimmed(){var lib=new Model.Library();var p=profile();lib.profiles.add(p);assertEquals(List.of(p),Model.visibleProfiles(lib,false,false," COM "));assertTrue(Model.visibleProfiles(lib,false,false,"missing").isEmpty());}

    @Test void shareCopyVerifiesClipboard()throws Exception{var clipboard=new java.util.concurrent.atomic.AtomicReference<String>();ShareDelivery.copy("KBP1.example",clipboard::set,clipboard::get);assertEquals("KBP1.example",clipboard.get());}
    @Test void shareCopyRejectsMismatch(){assertThrows(IOException.class,()->ShareDelivery.copy("code",x->{},()->"old"));}
    @Test void shareCopyReportsUnavailableClipboard(){assertThrows(IOException.class,()->ShareDelivery.copy("code",x->{throw new IllegalStateException();},()->""));}
    @Test void wrappedShareCodeImports()throws Exception{String code=Storage.encode(profile());String wrapped=" \n"+code.substring(0,10)+"\r\n"+code.substring(10)+" ";assertEquals(profile().binds,Storage.decode(wrapped).binds);}
    @Test void importedNamesAvoidCaseInsensitiveCollisions(){var lib=new Model.Library();lib.profiles.add(profile());assertNotEquals("combat",Model.importedName("combat",lib).toLowerCase(Locale.ROOT));}
    @Test void importedNamesStayWithinLengthLimit(){var lib=new Model.Library();var p=profile();p.name="x".repeat(64);lib.profiles.add(p);String name=Model.importedName(p.name,lib);assertTrue(name.length()<=64);assertNotEquals(p.name,name);}
    @Test void brokenCodeExplainsNextStep(){assertTrue(HelpText.error(new IOException("Expected a KBP1 share code")).contains("Share > Copy code"));}
    @Test void activeProfileReportsNameAndChanges(){var l=new Model.Library();var p=profile();l.profiles.add(p);assertEquals("Active profile: Combat",Model.activeLabel(l,p.id,p.binds));assertEquals("Active profile: Combat (modified)",Model.activeLabel(l,p.id,Map.of("key.jump","key.keyboard.a")));}
    @Test void deletedProfileIsNotShownAsActive(){var l=new Model.Library();var p=profile();p.deleted=true;l.profiles.add(p);assertEquals("Active profile: None",Model.activeLabel(l,p.id,p.binds));}
    @Test void absentModDoesNotMarkActiveProfileModified(){var l=new Model.Library();var p=profile();l.profiles.add(p);assertEquals("Active profile: Combat",Model.activeLabel(l,p.id,Map.of()));}
    String legacyCode(Model.Profile p)throws Exception{return compressed("KBP1.",Storage.JSON.toJson(p).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    String compressed(String prefix,byte[] bytes)throws Exception{var out=new ByteArrayOutputStream();try(var zip=new GZIPOutputStream(out)){zip.write(bytes);}return prefix+Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray());}
    @Test void oldShareCodesRemainImportable()throws Exception{var p=profile();assertEquals(p.binds,Storage.decode(legacyCode(p)).binds);}
    @Test void compactShareKeepsUnicodeAndMouseBindings()throws Exception{var p=profile();p.name="Bau \u00e4 \u0416";p.gameVersion="26.2";p.binds.put("mod.action","key.mouse.left");var q=Storage.decode(Storage.encode(p));assertEquals(p.name,q.name);assertEquals(p.gameVersion,q.gameVersion);assertEquals(p.binds,q.binds);}
    @Test void compactSharingOmitsLocalSettings()throws Exception{var p=profile();p.favorite=true;p.deleted=true;p.order=12;p.shortcut=65;var q=Storage.decode(Storage.encode(p));assertFalse(q.favorite);assertFalse(q.deleted);assertEquals(0,q.order);assertEquals(-1,q.shortcut);}
    @Test void compactSharingHandlesEmptyProfile()throws Exception{var p=profile();p.binds.clear();assertTrue(Storage.decode(Storage.encode(p)).binds.isEmpty());}
    @Test void compactSharingIsShorterForModdedProfile()throws Exception{var p=profile();for(int i=0;i<100;i++)p.binds.put("key.examplemod.action."+i,i%4==0?"key.keyboard.space":"key.keyboard.unknown");String old=legacyCode(p),now=Storage.encode(p);assertEquals(p.binds,Storage.decode(now).binds);assertTrue(now.length()<old.length());System.out.println("Share comparison (synthetic 101 bindings): KBP1="+old.length()+", KBP2="+now.length());}
    @Test void compactSharingRejectsTruncatedPayload()throws Exception{assertThrows(IOException.class,()->Storage.decode(compressed("KBP2.",new byte[]{0,9,1})));}
    @Test void compactSharingRejectsInvalidPrefix()throws Exception{var out=new ByteArrayOutputStream();try(var d=new DataOutputStream(out)){d.writeUTF("Name");d.writeUTF("26.2");d.writeShort(1);d.writeUTF("key.keyboard.a");d.writeShort(1);d.writeShort(2);d.writeUTF("x");d.writeShort(0);}assertThrows(IOException.class,()->Storage.decode(compressed("KBP2.",out.toByteArray())));}
    @Test void compactSharingRejectsExpandedBomb()throws Exception{assertThrows(IOException.class,()->Storage.decode(compressed("KBP2.",new byte[Storage.LIMIT+1])));}
}
