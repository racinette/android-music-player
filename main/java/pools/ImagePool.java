package pools;

import android.graphics.Bitmap;
import android.util.SparseArray;

import java.util.LinkedList;

public class ImagePool {

    private LinkedList<Integer> unusedKeys;

    private SparseArray <Bitmap> pool;

    private static final ImagePool instance = new ImagePool();

    private ImagePool(){
        pool = new SparseArray<>();
        unusedKeys = new LinkedList<>();
        unusedKeys.push(0);
    }

    private int getNextKey(){
        int key;

        if (unusedKeys.size() == 1){
            key = unusedKeys.getFirst();
            unusedKeys.set(0, key + 1);
        } else {
            key = unusedKeys.pop();
        }

        return key;
    }

    public static synchronized Bitmap getImage(int key){
        if (key < 0)
            return null;
        return instance.pool.get(key);
    }

    public static synchronized void removeImage(int key){
        instance.pool.remove(key);
        instance.unusedKeys.push(key);
    }

    // returns a key of the added image
    public static synchronized int addImage(Bitmap image){
        int imageKey = instance.getNextKey();
        instance.pool.put(imageKey, image);
        return imageKey;
    }
}
