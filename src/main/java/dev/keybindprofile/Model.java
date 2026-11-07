package dev.keybindprofile;

import java.util.*;

public final class Model {
    public static final int FORMAT = 1, MAX_BINDS = 8192, MAX_PROFILES = 256;
    public static final class Profile {
        public int format = FORMAT;
        public String id = UUID.randomUUID().toString(), name = "New profile", gameVersion = "";
        public boolean favorite, deleted;
        public int order, shortcut = -1;
        public Map<String,String> binds = new TreeMap<>();
        public void validate() {
            if (format != FORMAT) throw new IllegalArgumentException("Unsupported profile format");
            UUID.fromString(id);
            if (name == null || name.isBlank() || name.length() > 64) throw new IllegalArgumentException("Name must contain 1-64 characters");
            if (shortcut != -1 && (shortcut < 32 || shortcut > 348)) throw new IllegalArgumentException("Invalid shortcut");
            validateBinds(binds);
        }
    }
    public static final class Library {
        public int format = FORMAT;
        public List<Profile> profiles = new ArrayList<>();
        public void validate() {
            if (format != FORMAT || profiles == null || profiles.size() > MAX_PROFILES) throw new IllegalArgumentException("Invalid library format or size");
            Set<String> ids = new HashSet<>(), names = new HashSet<>();
            for (Profile p : profiles) { p.validate(); if (!ids.add(p.id)) throw new IllegalArgumentException("Duplicate profile ID"); if(!p.deleted && !names.add(p.name.strip().toLowerCase(Locale.ROOT)))throw new IllegalArgumentException("Profile name already exists"); }
        }
    }
    public static final class Settings {
        public boolean confirm = true, protectInWorld = true, resetMissing, controlsButton = true, bindsButton = true, favoritesOnly;
        public String autoApply = "", lastApplied = "";
        public List<Map<String,String>> undo = new ArrayList<>(), redo = new ArrayList<>();
    }
    public static void validateBinds(Map<String,String> binds) {
        if (binds == null || binds.size() > MAX_BINDS) throw new IllegalArgumentException("Invalid binding count");
        binds.forEach((id,key) -> {
            if (id == null || id.isBlank() || id.length()>256 || key==null || key.isBlank() || key.length()>128)
                throw new IllegalArgumentException("Invalid binding entry");
        });
    }
    public static Map<String,String> plan(Map<String,String> current, Map<String,String> defaults, Map<String,String> profile, Set<String> selected, boolean resetMissing) {
        validateBinds(profile);
        Map<String,String> result = new TreeMap<>(current);
        current.forEach((id,key) -> {
            if (selected != null && !selected.contains(id)) return;
            if (profile.containsKey(id)) result.put(id, profile.get(id));
            else if (resetMissing && defaults.containsKey(id)) result.put(id,defaults.get(id));
        });
        return result;
    }
    public record Conflict(String key, List<String> actions) {}
    public static Set<Conflict> conflicts(Map<String,String> binds) {
        Map<String,List<String>> keys=new HashMap<>();
        binds.forEach((id,key) -> { if (!key.equals("key.keyboard.unknown") && !id.startsWith("key.debug.")) keys.computeIfAbsent(key,k->new ArrayList<>()).add(id); });
        Set<Conflict> result=new HashSet<>();
        keys.forEach((key,ids)->{if(ids.size()>1){Collections.sort(ids);result.add(new Conflict(key,List.copyOf(ids)));}});
        return result;
    }
    public static Set<Conflict> addedConflicts(Map<String,String> before,Map<String,String> after) {
        Set<Conflict> result=conflicts(after);result.removeAll(conflicts(before));return result;
    }
    public static void push(List<Map<String,String>> history,Map<String,String> snapshot) {
        history.add(new TreeMap<>(snapshot));while(history.size()>20) history.remove(0);
    }
    public static String activeLabel(Library library,String lastApplied,Map<String,String> current){
        Profile p=library.profiles.stream().filter(v->!v.deleted && v.id.equals(lastApplied)).findFirst().orElse(null);
        if(p==null)return "Active profile: None";
        boolean changed=p.binds.entrySet().stream().anyMatch(e->current.containsKey(e.getKey())&&!Objects.equals(current.get(e.getKey()),e.getValue()));
        return "Active profile: "+p.name+(changed?" (modified)":"");
    }
    public static String importedName(String original,Library library) {
        Set<String> names=new HashSet<>();library.profiles.stream().filter(p->!p.deleted).forEach(p->names.add(p.name.toLowerCase(Locale.ROOT)));
        if(!names.contains(original.toLowerCase(Locale.ROOT)))return original;
        for(int i=1;i<=MAX_PROFILES+1;i++){
            String suffix=" (import "+i+")";
            String candidate=original.substring(0,Math.min(original.length(),64-suffix.length()))+suffix;
            if(!names.contains(candidate.toLowerCase(Locale.ROOT)))return candidate;
        }
        throw new IllegalArgumentException("No free import name");
    }
    public static boolean hasFavorites(Library library) {
        return library.profiles.stream().anyMatch(p->!p.deleted && p.favorite);
    }
    public static List<Profile> visibleProfiles(Library library,boolean trash,boolean favorites,String query) {
        boolean onlyFavorites=favorites && hasFavorites(library);
        String search=query.strip().toLowerCase(Locale.ROOT);
        return library.profiles.stream().filter(p->p.deleted==trash && (trash||!onlyFavorites||p.favorite)
            && p.name.toLowerCase(Locale.ROOT).contains(search))
            .sorted(Comparator.comparingInt((Profile p)->p.order).thenComparing(p->p.name)).toList();
    }
    public static List<Profile> ordered(Library library,boolean favorites) {
        return library.profiles.stream().filter(p->!p.deleted && (!favorites || p.favorite)).sorted(Comparator.comparingInt((Profile p)->p.order).thenComparing(p->p.name,String.CASE_INSENSITIVE_ORDER).thenComparing(p->p.id)).toList();
    }
}
