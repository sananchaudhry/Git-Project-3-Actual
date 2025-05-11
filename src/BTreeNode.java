import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

public class BTreeNode {
    public static final int DEGREE = 10;
    public static final int MAX_KEYS = 2 * DEGREE - 1;
    public static final int MAX_CHILDREN = 2 * DEGREE;

    public long id;
    public long parent;
    public int numKeys;
    public long[] keys = new long[MAX_KEYS];
    public long[] values = new long[MAX_KEYS];
    public long[] children = new long[MAX_CHILDREN];

    public BTreeNode(long id) {
        this.id = id;
        this.parent = 0;
        this.numKeys = 0;
    }

    public byte[] toBytes() {
        ByteBuffer buf = ByteBuffer.allocate(512);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putLong(id);
        buf.putLong(parent);
        buf.putLong(numKeys);
        for (int i = 0; i < MAX_KEYS; i++) buf.putLong(keys[i]);
        for (int i = 0; i < MAX_KEYS; i++) buf.putLong(values[i]);
        for (int i = 0; i < MAX_CHILDREN; i++) buf.putLong(children[i]);
        return buf.array();
    }

    public static BTreeNode fromBytes(byte[] data) {
        ByteBuffer buf = ByteBuffer.wrap(data);
        buf.order(ByteOrder.BIG_ENDIAN);
        BTreeNode node = new BTreeNode(buf.getLong());
        node.parent = buf.getLong();
        node.numKeys = (int) buf.getLong();
        for (int i = 0; i < MAX_KEYS; i++) node.keys[i] = buf.getLong();
        for (int i = 0; i < MAX_KEYS; i++) node.values[i] = buf.getLong();
        for (int i = 0; i < MAX_CHILDREN; i++) node.children[i] = buf.getLong();
        return node;
    }

    public boolean isLeaf() {
        for (int i = 0; i < MAX_CHILDREN; i++) {
            if (children[i] != 0) return false;
        }
        return true;
    }
}

