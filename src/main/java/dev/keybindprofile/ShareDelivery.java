package dev.keybindprofile;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Supplier;
public final class ShareDelivery {
    private ShareDelivery() {}
    public static void copy(String code,Consumer<String> write,Supplier<String> read) throws IOException {
        try {write.accept(code);if(!code.equals(read.get()))throw new IOException("Clipboard did not retain the code. Select the field and press Ctrl+C.");}
        catch(RuntimeException e){throw new IOException("Clipboard unavailable. Select the field and press Ctrl+C.",e);}
    }
}
