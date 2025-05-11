import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

public class IndexFile {
    private static final int BLOCK_SIZE = 512;
    private static final String MAGIC = "4348PRJ3";

    public static void create(String filename) throws IOException {
        File f = new File(filename);
        if (f.exists()) {
            System.err.println("Error: file already exists.");
            return;
        }

        try (RandomAccessFile raf = new RandomAccessFile(f, "rw")) {
            byte[] block = new byte[BLOCK_SIZE];
            ByteBuffer buffer = ByteBuffer.wrap(block);
            buffer.order(ByteOrder.BIG_ENDIAN);

            buffer.put(MAGIC.getBytes());
            buffer.putLong(0); // root block id
            buffer.putLong(1); // next available block id

            raf.write(block);
        }

        System.out.println("Index file created.");
    }

    public static void insert(String filename, long key, long value) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(filename, "rw")) {
            BTree tree = new BTree(raf);
            tree.insert(key, value);
            tree.close();
        }
    }

    public static void search(String filename, long key) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(filename, "r")) {
            BTree tree = new BTree(raf);
            Long result = tree.search(key);
            if (result != null) {
                System.out.println(key + " => " + result);
            } else {
                System.err.println("Key not found.");
            }
        }
    }

    public static void load(String indexFile, String csvFile) throws IOException {
        File file = new File(csvFile);
        if (!file.exists()) {
            System.err.println("CSV file does not exist.");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.trim().split(",");
                if (parts.length != 2) continue;

                long key = Long.parseLong(parts[0]);
                long value = Long.parseLong(parts[1]);
                insert(indexFile, key, value);
            }
        }
    }

    public static void print(String filename) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(filename, "r")) {
            BTree tree = new BTree(raf);
            List<Map.Entry<Long, Long>> all = tree.getAll();
            for (Map.Entry<Long, Long> entry : all) {
                System.out.println(entry.getKey() + " => " + entry.getValue());
            }
        }
    }

    public static void extract(String indexFile, String outputFile) throws IOException {
        File out = new File(outputFile);
        if (out.exists()) {
            System.err.println("Output file already exists.");
            return;
        }

        try (RandomAccessFile raf = new RandomAccessFile(indexFile, "r");
             BufferedWriter writer = new BufferedWriter(new FileWriter(out))) {
            BTree tree = new BTree(raf);
            List<Map.Entry<Long, Long>> all = tree.getAll();
            for (Map.Entry<Long, Long> entry : all) {
                writer.write(entry.getKey() + "," + entry.getValue());
                writer.newLine();
            }
        }
    }
}

