package magick;

/**
 * The sole purpouse of this class is to cause the native
 * library to be loaded in SYSTEM classloader whenever a
 * concrete class is used.
 *
 * @author Max Kollegov &lt;Virtual_Max@geocities.com&gt;
 */
public class MagickLoader {
    
    static {      
        // System.load("/data/data/com.mageventory.image_magic_plugin/lib/libndk1.so");
    }
    
}
