package dev.keybindprofile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.zip.*;

public final class Storage {
    public static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    public static final int LIMIT=2*1024*1024, CODE_LIMIT=1500000;
    private final Path root;
    public Storage(Path root) { this.root=root; }
    public Path root() { return root; }
    public Model.Library read() throws IOException {
        Model.Library lib=read(root.resolve("profiles.json"),Model.Library.class,new Model.Library());
        try {lib.validate();} catch(RuntimeException e) {throw new IOException("Profile library is invalid; original preserved",e);}return lib;
    }
    public Model.Library change(Consumer<Model.Library> change) throws IOException {
        Files.createDirectories(root);
        try(FileChannel channel=FileChannel.open(root.resolve("profiles.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE)) {
            java.nio.channels.FileLock lock;
            try { lock=channel.tryLock(); } catch(java.nio.channels.OverlappingFileLockException e){throw new IOException("Another profile operation is running",e);}
            if(lock==null) throw new IOException("Another instance is updating profiles; try again");
            try(lock) { Model.Library lib=read();change.accept(lib);lib.validate();write(root.resolve("profiles.json"),lib);return lib; }
        }
    }
    public static <T> T read(Path path,Class<T> type,T fallback) throws IOException {
        if(!Files.exists(path))return fallback;
        if(Files.size(path)>LIMIT)throw new IOException("Configuration exceeds size limit");
        try(var in=Files.newInputStream(path)) {
            byte[] bytes=in.readNBytes(LIMIT+1);if(bytes.length>LIMIT)throw new IOException("Configuration exceeds size limit");
            T value=JSON.fromJson(new String(bytes,StandardCharsets.UTF_8),type);
            if(value==null)throw new IOException("Empty configuration; original preserved");return value;
        } catch(com.google.gson.JsonParseException e){throw new IOException("Invalid configuration; original preserved",e);}
    }
    public static void write(Path path,Object value) throws IOException {
        byte[] data=JSON.toJson(value).getBytes(StandardCharsets.UTF_8);
        if(data.length>LIMIT)throw new IOException("Configuration exceeds size limit");
        Files.createDirectories(path.toAbsolutePath().getParent());
        Path temp=Files.createTempFile(path.toAbsolutePath().getParent(),".keybindprofile-",".tmp");
        try {
            try(FileChannel out=FileChannel.open(temp,StandardOpenOption.WRITE)) { ByteBuffer buffer=ByteBuffer.wrap(data);while(buffer.hasRemaining())out.write(buffer);out.force(true); }
            try {Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException e){throw new IOException("Filesystem does not support atomic saves; original preserved",e);}
        } finally {Files.deleteIfExists(temp);}
    }
    public static Model.Profile copy(Model.Profile p) {return JSON.fromJson(JSON.toJson(p),Model.Profile.class);}
    public static Model.Settings copy(Model.Settings p) {return JSON.fromJson(JSON.toJson(p),Model.Settings.class);}
    public static String encode(Model.Profile p) throws IOException {
        p.validate();
        ByteArrayOutputStream raw=new ByteArrayOutputStream();
        try(DataOutputStream data=new DataOutputStream(raw)) {
            data.writeUTF(p.name);data.writeUTF(p.gameVersion==null?"":p.gameVersion);
            List<String> keys=new ArrayList<>(new TreeSet<>(p.binds.values()));
            Map<String,Integer> indexes=new HashMap<>();
            data.writeShort(keys.size());
            for(int i=0;i<keys.size();i++){data.writeUTF(keys.get(i));indexes.put(keys.get(i),i);}
            data.writeShort(p.binds.size());String previous="";
            for(var entry:new TreeMap<>(p.binds).entrySet()){
                String id=entry.getKey();int prefix=0;
                while(prefix<previous.length() && prefix<id.length() && previous.charAt(prefix)==id.charAt(prefix))prefix++;
                data.writeShort(prefix);data.writeUTF(id.substring(prefix));data.writeShort(indexes.get(entry.getValue()));previous=id;
            }
        }
        if(raw.size()>LIMIT)throw new IOException("Profile too large to share");
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        try(GZIPOutputStream gzip=new GZIPOutputStream(out)){gzip.write(raw.toByteArray());}
        String code="KBP2."+Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray());
        if(code.length()>CODE_LIMIT)throw new IOException("Profile too large to share");return code;
    }
    public static Model.Profile decode(String code) throws IOException {
        if(code==null || code.length()>CODE_LIMIT)throw new IOException("Share code exceeds size limit");
        code=code.replaceAll("\\s+", "");if(!code.startsWith("KBP1.")&&!code.startsWith("KBP2."))throw new IOException("Expected a KBP1 or KBP2 share code");
        try {
            byte[] compressed=Base64.getUrlDecoder().decode(code.substring(5));
            try(var gzip=new GZIPInputStream(new ByteArrayInputStream(compressed))) {
                byte[] bytes=gzip.readNBytes(LIMIT+1);if(bytes.length>LIMIT)throw new IOException("Unpacked profile exceeds size limit");
                Model.Profile p=code.startsWith("KBP2.")?decodeCompact(bytes):JSON.fromJson(new String(bytes,StandardCharsets.UTF_8),Model.Profile.class);
                if(p==null)throw new IOException("Empty profile");p.validate();p.id=UUID.randomUUID().toString();p.deleted=false;p.shortcut=-1;return p;
            }
        }catch(RuntimeException e){throw new IOException("Invalid or unsupported share code",e);}
    }
    private static Model.Profile decodeCompact(byte[] bytes)throws IOException {
        try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes))){
            Model.Profile p=new Model.Profile();p.name=in.readUTF();p.gameVersion=in.readUTF();
            int count=in.readUnsignedShort();if(count>Model.MAX_BINDS)throw new IOException("Invalid key count");
            List<String> keys=new ArrayList<>();
            for(int i=0;i<count;i++){String key=in.readUTF();if(key.isBlank()||key.length()>128)throw new IOException("Invalid key");keys.add(key);}
            int entries=in.readUnsignedShort();if(entries>Model.MAX_BINDS)throw new IOException("Invalid binding count");
            String previous="";
            for(int i=0;i<entries;i++){
                int prefix=in.readUnsignedShort();if(prefix>previous.length())throw new IOException("Invalid action prefix");
                String id=previous.substring(0,prefix)+in.readUTF();int index=in.readUnsignedShort();
                if(id.isBlank()||id.length()>256||index>=keys.size()||p.binds.containsKey(id))throw new IOException("Invalid binding entry");
                p.binds.put(id,keys.get(index));previous=id;
            }
            if(in.read()!=-1)throw new IOException("Unexpected data after profile");
            p.validate();return p;
        }
    }
}
