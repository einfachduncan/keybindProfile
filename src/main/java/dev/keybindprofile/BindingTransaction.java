package dev.keybindprofile;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class BindingTransaction {
    private BindingTransaction() {}
    public static void commit(Map<String,String> before,Map<String,String> target,Supplier<Map<String,String>> current,Consumer<Map<String,String>> write) {
        if(!current.get().equals(before))throw new IllegalStateException("Bindings changed while saving; apply cancelled");
        try {write.accept(target);}
        catch(RuntimeException failure){
            try{write.accept(before);}catch(RuntimeException rollback){failure.addSuppressed(rollback);}
            throw failure;
        }
    }
}
