import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

/** Generates the tiny structure template used by the dedicated GameTest run. */
public final class GenerateEmptyStructure {
    private static final int TAG_END = 0;
    private static final int TAG_INT = 3;
    private static final int TAG_STRING = 8;
    private static final int TAG_LIST = 9;
    private static final int TAG_COMPOUND = 10;

    public static void main(String[] args) throws Exception {
        Path output = Path.of(args[0]);
        Files.createDirectories(output.getParent());
        try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(output.toFile())))) {
            out.writeByte(TAG_COMPOUND);
            out.writeUTF("");
            intTag(out, "DataVersion", 3955);
            intList(out, "size", 3, 3, 3);

            out.writeByte(TAG_LIST);
            out.writeUTF("palette");
            out.writeByte(TAG_COMPOUND);
            out.writeInt(1);
            stringTag(out, "Name", "minecraft:air");
            out.writeByte(TAG_END);

            emptyCompoundList(out, "blocks");
            emptyCompoundList(out, "entities");
            out.writeByte(TAG_END);
        }
    }

    private static void intTag(DataOutputStream out, String name, int value) throws Exception {
        out.writeByte(TAG_INT);
        out.writeUTF(name);
        out.writeInt(value);
    }

    private static void stringTag(DataOutputStream out, String name, String value) throws Exception {
        out.writeByte(TAG_STRING);
        out.writeUTF(name);
        out.writeUTF(value);
    }

    private static void intList(DataOutputStream out, String name, int... values) throws Exception {
        out.writeByte(TAG_LIST);
        out.writeUTF(name);
        out.writeByte(TAG_INT);
        out.writeInt(values.length);
        for (int value : values) out.writeInt(value);
    }

    private static void emptyCompoundList(DataOutputStream out, String name) throws Exception {
        out.writeByte(TAG_LIST);
        out.writeUTF(name);
        out.writeByte(TAG_COMPOUND);
        out.writeInt(0);
    }
}
