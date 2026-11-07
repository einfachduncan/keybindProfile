package dev.keybindprofile;
import java.util.Map;
final class HelpText {
    private static final Map<String,String> TIPS=Map.ofEntries(
        Map.entry("Profiles","Your saved sets of keys. Click a name to see its keys, or Load to use it."),
        Map.entry("Settings","Choose when profiles load and whether Minecraft asks first."),
        Map.entry("Undo","Put your keys back the way they were before the last change."),
        Map.entry("Redo","Repeat a change you just undid."),
        Map.entry("Help","Read a short step-by-step guide."),
        Map.entry("Close","Return to the previous Minecraft menu."),
        Map.entry("Load","Use this profile's keys. Your current keys are saved so you can undo."),
        Map.entry("Save keys","Create a new profile from the keys you use right now. Enter a name first."),
        Map.entry("Add code","Add a profile someone shared with you. Paste their code, check it, then save it."),
        Map.entry("Refresh","Read profiles saved by your other Minecraft instances."),
        Map.entry("Open folder","Open the folder where your profiles are saved."),
        Map.entry("Defaults","Reset all your keys to Minecraft and mod defaults. A confirmation appears first."),
        Map.entry("Deleted","Show profiles you deleted. You can restore them here."),
        Map.entry("Show saved","Return to your saved profiles."),
        Map.entry("Favorites","Show your favorite profiles. If you have none, all profiles stay visible."),
        Map.entry("Show all","Show every saved profile, including those not marked as favorites."),
        Map.entry("Select results","Select every action matching the search and filter, across all pages."),
        Map.entry("Select none","Clear the check marks. Your keys do not change."),
        Map.entry("Load checked","Change only the actions you checked. Leave the other keys alone."),
        Map.entry("Load all","Use all installed keys from this profile."),
        Map.entry("Save profile","Keep this imported profile. This does not change your current keys."),
        Map.entry("Star","Mark this profile as a favorite."),
        Map.entry("Unstar","Remove this profile from your favorites."),
        Map.entry("Rename","Change the profile's name. Its keys stay the same."),
        Map.entry("Copy","Create another profile with the same keys and a new name."),
        Map.entry("Overwrite","Replace the keys SAVED IN THIS PROFILE with the keys you use now. This does not load the profile."),
        Map.entry("Share","Show a code you can copy and send to someone. They can use Add code to import it."),
        Map.entry("Delete","Move this profile to Deleted. You can restore it later."),
        Map.entry("Restore","Put this deleted profile back into your saved profiles."),
        Map.entry("Up","Move this profile one place up in the list and shortcut cycle."),
        Map.entry("Down","Move this profile one place down in the list and shortcut cycle."),
        Map.entry("<","Previous page."),Map.entry(">","Next page.")
    );
    static String tip(String label){
        if(TIPS.containsKey(label))return TIPS.get(label);
        if(label.startsWith("Ask before"))return "ON: show a confirmation before changing your keys. New conflicts always ask.";
        if(label.startsWith("Allow profile"))return "OFF prevents accidental profile switches while playing. You can still load profiles from this menu.";
        if(label.startsWith("Keys not saved"))return "Keep current is recommended. Reset to default also changes actions that are not saved in the profile.";
        if(label.startsWith("Button in"))return "Show or hide the keybindProfile button in this Minecraft menu.";
        if(label.startsWith("Load at startup"))return "Choose a profile to load automatically each time this Minecraft instance starts. Click to cycle; Off disables it.";
        if(label.startsWith("Shortcut")||label.startsWith("Choose a profile"))return "Open a profile first, return to Settings, then click here and press the key you want. Escape removes its shortcut.";
        if(label.startsWith("Permanently"))return "Delete every profile in Deleted forever. Saved profiles are not affected. You must confirm first.";
        return label;
    }
    static String on(boolean value){return value?"ON":"OFF";}
    static String error(Exception e){
        String raw=e.getMessage()==null?"Unknown error":e.getMessage();
        if(raw.contains("Expected a KBP1"))return "That is not a profile code. Ask the sender to use Share > Copy code.";
        if(raw.contains("Invalid or unsupported share")||e instanceof java.util.zip.ZipException)return "This code is incomplete or damaged. Copy the whole code and try again.";
        if(raw.contains("already exists"))return "That name is already used. Enter a different profile name.";
        if(raw.contains("Clipboard"))return "Could not use the clipboard. Select the code and use Ctrl+C or Ctrl+V.";
        if(raw.contains("exceeds")||raw.contains("too large"))return "This code or file is too large to load safely.";
        return raw;
    }
}
