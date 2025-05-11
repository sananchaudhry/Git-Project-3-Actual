import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

public class BTree {
    private static final int BLOCK_SIZE = 512;
    private static final String MAGIC = "4348PRJ3";

    private RandomAccessFile file;
    private long rootBlock;
    private long nextBlockId;

    private final LinkedHashMap<Long, BTreeNode> cache = new LinkedHashMap<>() {
        protected boolean removeEldestEntry(Map.Entry<Long, BTreeNode> eldest) {
            if (size() > 3) {
                writeNode(eldest.getValue());
                return true;
            }
            return false;
        }
    };

    public BTree(RandomAccessFile raf) throws IOException {
        this.file = raf;
        file.seek(0);
        byte[] block = new byte[BLOCK_SIZE];
        file.read(block);
        ByteBuffer buf = ByteBuffer.wrap(block).order(ByteOrder.BIG_ENDIAN);

        byte[] magicBytes = new byte[8];
        buf.get(magicBytes);
        String magic = new String(magicBytes);
        if (!magic.equals(MAGIC)) throw new IOException("Invalid index file");

        rootBlock = buf.getLong();
        nextBlockId = buf.getLong();
    }

    public void insert(long key, long value) {
        if (rootBlock == 0) {
            BTreeNode root = newNode();
            root.keys[0] = key;
            root.values[0] = value;
            root.numKeys = 1;
            rootBlock = root.id;
            writeHeader();
            writeNode(root);
        } else {
            BTreeNode root = readNode(rootBlock);
            if (root.numKeys == BTreeNode.MAX_KEYS) {
                BTreeNode newRoot = newNode();
                newRoot.children[0] = root.id;
                root.parent = newRoot.id;
                splitChild(newRoot, 0, root);
                insertNonFull(newRoot, key, value);
                rootBlock = newRoot.id;
                writeHeader();
            } else {
                insertNonFull(root, key, value);
            }
        }
    }

    private void insertNonFull(BTreeNode node, long key, long value) {
        int i = node.numKeys - 1;
        if (node.isLeaf()) {
            while (i >= 0 && key < node.keys[i]) {
                node.keys[i + 1] = node.keys[i];
                node.values[i + 1] = node.values[i];
                i--;
            }
            node.keys[i + 1] = key;
            node.values[i + 1] = value;
            node.numKeys++;
            writeNode(node);
        } else {
            while (i >= 0 && key < node.keys[i]) i--;
            i++;
            BTreeNode child = readNode(node.children[i]);
            if (child.numKeys == BTreeNode.MAX_KEYS) {
                splitChild(node, i, child);
                if (key > node.keys[i]) i++;
            }
            insertNonFull(readNode(node.children[i]), key, value);
        }
    }

    private void splitChild(BTreeNode parent, int i, BTreeNode full) {
        BTreeNode z = newNode();
        BTreeNode y = full;

        z.parent = parent.id;
        z.numKeys = BTreeNode.DEGREE - 1;

        for (int j = 0; j < BTreeNode.DEGREE - 1; j++) {
            z.keys[j] = y.keys[j + BTreeNode.DEGREE];
            z.values[j] = y.values[j + BTreeNode.DEGREE];
        }
        if (!y.isLeaf()) {
            for (int j = 0; j < BTreeNode.DEGREE; j++) {
                z.children[j] = y.children[j + BTreeNode.DEGREE];
            }
        }

        y.numKeys = BTreeNode.DEGREE - 1;

        for (int j = parent.numKeys; j >= i + 1; j--) {
            parent.children[j + 1] = parent.children[j];
        }
        parent.children[i + 1] = z.id;

        for (int j = parent.numKeys - 1; j >= i; j--) {
            parent.keys[j + 1] = parent.keys[j];
            parent.values[j + 1] = parent.values[j];
        }
        parent.keys[i] = y.keys[BTreeNode.DEGREE - 1];
        parent.values[i] = y.values[BTreeNode.DEGREE - 1];
        parent.numKeys++;

        writeNode(y);
        writeNode(z);
        writeNode(parent);
    }

    public Long search(long key) {
        return searchNode(readNode(rootBlock), key);
    }

    private Long searchNode(BTreeNode node, long key) {
        int i = 0;
        while (i < node.numKeys && key > node.keys[i]) i++;
        if (i < node.numKeys && key == node.keys[i]) return node.values[i];
        if (node.isLeaf()) return null;
        return searchNode(readNode(node.children[i]), key);
    }

    public List<Map.Entry<Long, Long>> getAll() {
        List<Map.Entry<Long, Long>> out = new ArrayList<>();
        collect(readNode(rootBlock), out);
        out.sort(Comparator.comparing(Map.Entry::getKey));
        return out;
    }

    private void collect(BTreeNode node, List<Map.Entry<Long, Long>> list) {
        for (int i = 0; i < node.numKeys; i++) {
            if (!node.isLeaf()) collect(readNode(node.children[i]), list);
            list.add(Map.entry(node.keys[i], node.values[i]));
        }
        if (!node.isLeaf()) collect(readNode(node.children[node.numKeys]), list);
    }

    private BTreeNode readNode(long id) {
        if (cache.containsKey(id)) return cache.get(id);
        try {
            file.seek(id * BLOCK_SIZE);
            byte[] buf = new byte[BLOCK_SIZE];
            file.read(buf);
            BTreeNode node = BTreeNode.fromBytes(buf);
            cache.put(id, node);
            return node;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read node");
        }
    }

    private void writeNode(BTreeNode node) {
        try {
            file.seek(node.id * BLOCK_SIZE);
            file.write(node.toBytes());
        } catch (IOException e) {
            throw new RuntimeException("Failed to write node");
        }
    }

    private void writeHeader() {
        try {
            file.seek(0);
            ByteBuffer buf = ByteBuffer.allocate(BLOCK_SIZE).order(ByteOrder.BIG_ENDIAN);
            buf.put(MAGIC.getBytes());
            buf.putLong(rootBlock);
            buf.putLong(nextBlockId);
            file.write(buf.array());
        } catch (IOException e) {
            throw new RuntimeException("Failed to write header");
        }
    }

    private BTreeNode newNode() {
        BTreeNode node = new BTreeNode(nextBlockId++);
        cache.put(node.id, node);
        return node;
    }

    public void close() {
        for (BTreeNode node : cache.values()) {
            writeNode(node);
        }
        writeHeader();
    }
}
