package FileWork.Binary;

import Exceptions.*;
import FileWork.FileStorage;
import Yadro.DataStruct.Column;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BinaryFileStorage implements FileStorage {
    private final String basePath;

    // Magic bytes that identify the new columnar binary format ("DBCL")
    private static final byte[] COLUMN_MAGIC = {0x44, 0x42, 0x43, 0x4C};

    public BinaryFileStorage(String basePath) {
        this.basePath = basePath;
        File dir = new File(basePath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public BinaryFileStorage() {
        this("database");
    }

    @Override
    public boolean exists(String path) {
        return new File(path).exists();
    }

    @Override
    public <T> T readFile(String path, Class<T> type) throws FileStorageException {
        if (type == Column.class) {
            return type.cast(readColumn(path));
        }

        File file = new File(path);

        if (!file.exists()) {
            throw new NoFileException("File not found: " + path);
        }

        if (file.length() == 0) {
            throw new EmptyFileException("Empty file: " + path);
        }

        if (!file.canRead()) {
            throw new PermissionDeniedException("Permission denied: " + path);
        }

        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(file)))) {
            Object obj = ois.readObject();
            return type.cast(obj);
        } catch (ClassNotFoundException | ClassCastException e) {
            throw new SerializationStorageException("Invalid binary content in file: " + path, e);
        } catch (IOException e) {
            throw new FileStorageException("Failed to read file: " + path, e);
        }
    }

    @Override
    public <T> void writeFile(String path, T content) throws FileStorageException {
        if (content instanceof Column col) {
            writeColumn(path, col);
            return;
        }

        File file = new File(path);
        File parent = file.getParentFile();

        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new FileStorageException("Failed to create parent directory: " + parent.getAbsolutePath());
        }

        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(new FileOutputStream(file)))) {
            oos.writeObject(content);
            oos.flush();
        } catch (NotSerializableException e) {
            throw new SerializationStorageException("Object is not serializable for file: " + path, e);
        } catch (IOException e) {
            throw new SerializationStorageException("Could not write file: " + path, e);
        }

        if (!file.exists()) {
            throw new NoFileException("File not found: " + path);
        }

        if (!file.canWrite()) {
            throw new PermissionDeniedException("Permission denied: " + path);
        }
    }

    /**
     * Writes a Column using the custom slot-directory binary format.
     *
     * Format:
     *   [4 bytes] magic "DBCL"
     *   [4 bytes] row count N
     *   [N * 8 bytes] slot directory: each slot = (int offset_from_data_start, int byte_length)
     *                 NULL value: both fields = -1
     *   [variable] data section: concatenated UTF-8 string bytes
     */
    private void writeColumn(String path, Column col) throws FileStorageException {
        File file = new File(path);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new FileStorageException("Failed to create parent directory: " + parent.getAbsolutePath());
        }

        List<String> data = col.getData();
        int n = data.size();

        List<byte[]> rowBytesList = new ArrayList<>(n);
        for (String val : data) {
            rowBytesList.add((val == null || val.equals("NULL")) ? null : val.getBytes(StandardCharsets.UTF_8));
        }

        int[] offsets = new int[n];
        int[] lengths = new int[n];
        int currentOffset = 0;
        for (int i = 0; i < n; i++) {
            byte[] bytes = rowBytesList.get(i);
            if (bytes == null) {
                offsets[i] = -1;
                lengths[i] = -1;
            } else {
                offsets[i] = currentOffset;
                lengths[i] = bytes.length;
                currentOffset += bytes.length;
            }
        }

        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.setLength(0);

            raf.write(COLUMN_MAGIC);
            raf.writeInt(n);

            for (int i = 0; i < n; i++) {
                raf.writeInt(offsets[i]);
                raf.writeInt(lengths[i]);
            }

            for (byte[] bytes : rowBytesList) {
                if (bytes != null) {
                    raf.write(bytes);
                }
            }

        } catch (IOException e) {
            throw new FileStorageException("Failed to write column file: " + path, e);
        }
    }

    /**
     * Reads a Column written in the slot-directory format.
     * Falls back to Java serialization for files in the old format.
     */
    private Column readColumn(String path) throws FileStorageException {
        File file = new File(path);

        if (!file.exists()) throw new NoFileException("File not found: " + path);
        if (file.length() == 0) throw new EmptyFileException("Empty file: " + path);
        if (!file.canRead()) throw new PermissionDeniedException("Permission denied: " + path);

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            byte[] magic = new byte[4];
            raf.readFully(magic);

            if (!Arrays.equals(magic, COLUMN_MAGIC)) {
                return readColumnLegacy(path);
            }

            int n = raf.readInt();
            int dataStart = 8 + n * 8;

            int[] offsets = new int[n];
            int[] lengths = new int[n];
            for (int i = 0; i < n; i++) {
                offsets[i] = raf.readInt();
                lengths[i] = raf.readInt();
            }

            ArrayList<String> data = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                if (offsets[i] == -1) {
                    data.add("NULL");
                } else {
                    raf.seek(dataStart + offsets[i]);
                    byte[] bytes = new byte[lengths[i]];
                    raf.readFully(bytes);
                    data.add(new String(bytes, StandardCharsets.UTF_8));
                }
            }

            return new Column(data);

        } catch (IOException e) {
            throw new FileStorageException("Failed to read column file: " + path, e);
        }
    }

    private Column readColumnLegacy(String path) throws FileStorageException {
        File file = new File(path);
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(file)))) {
            return (Column) ois.readObject();
        } catch (ClassNotFoundException | ClassCastException e) {
            throw new SerializationStorageException("Invalid binary content in file: " + path, e);
        } catch (IOException e) {
            throw new FileStorageException("Failed to read file: " + path, e);
        }
    }

    /**
     * In-place update of a single row in a Column file.
     *
     * If the new value fits in the existing slot (newLen <= oldLen):
     *   writes the bytes directly at the old position (O(1) disk writes).
     * If the new value is larger:
     *   appends the bytes at the end of the file and updates the slot pointer.
     *   Old bytes become dead space (no compaction — acceptable for this project).
     */
    @Override
    public void writeRow(String path, int rowIndex, String newValue) throws FileStorageException {
        File file = new File(path);
        if (!file.exists()) throw new NoFileException("File not found: " + path);

        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            byte[] magic = new byte[4];
            raf.readFully(magic);

            if (!Arrays.equals(magic, COLUMN_MAGIC)) {
                // Old-format file: load, modify, rewrite in new format
                Column col = readColumnLegacy(path);
                col.getData().set(rowIndex, newValue != null ? newValue : "NULL");
                writeColumn(path, col);
                return;
            }

            int n = raf.readInt();
            if (rowIndex < 0 || rowIndex >= n) {
                throw new FileStorageException("Row index " + rowIndex + " out of bounds [0, " + n + ")");
            }

            int dataStart = 8 + n * 8;
            long slotOffset = 8L + (long) rowIndex * 8;

            raf.seek(slotOffset);
            int oldOffset = raf.readInt();
            int oldLen    = raf.readInt();

            boolean isNull = (newValue == null || newValue.equals("NULL"));

            if (isNull) {
                raf.seek(slotOffset);
                raf.writeInt(-1);
                raf.writeInt(-1);
                return;
            }

            byte[] newBytes = newValue.getBytes(StandardCharsets.UTF_8);
            int newLen = newBytes.length;

            if (oldOffset != -1 && newLen <= oldLen) {
                // True in-place: new value fits in the existing slot
                raf.seek(dataStart + oldOffset);
                raf.write(newBytes);
                raf.seek(slotOffset + 4);
                raf.writeInt(newLen);
            } else {
                // Append to end of file, update slot to point there
                long endPos = raf.length();
                raf.seek(endPos);
                raf.write(newBytes);

                int newOffset = (int) (endPos - dataStart);
                raf.seek(slotOffset);
                raf.writeInt(newOffset);
                raf.writeInt(newLen);
            }

        } catch (IOException e) {
            throw new FileStorageException("Failed to in-place update row " + rowIndex + " in: " + path, e);
        }
    }

    @Override
    public void deleteFile(String path) throws FileStorageException {
        File file = new File(path);

        if (!file.exists()) {
            throw new NoFileException("File not found: " + path);
        }

        if (!file.canWrite()) {
            throw new PermissionDeniedException("Permission denied: " + path);
        }

        if (!file.delete()) {
            throw new FileStorageException("Could not delete file: " + path);
        }
    }

    @Override
    public void renameFile(String path, String newName) throws FileStorageException {
        File file = new File(path);
        File target = new File(newName);

        if (!file.exists()) {
            throw new NoFileException("File not found: " + path);
        }

        if (target.exists()) {
            throw new AlreadyExistsException("File with new name already exists: " + newName);
        }

        if (!file.canWrite()) {
            throw new PermissionDeniedException("Permission denied: " + path);
        }

        if (!file.renameTo(target)) {
            throw new FileStorageException("Could not rename file: " + newName);
        }
    }

    @Override
    public void createDirectory(String path) throws FileStorageException {
        File folder = new File(path);

        if (folder.exists()) {
            throw new AlreadyExistsException("Directory already exists: " + path);
        }

        if (!folder.mkdirs()) {
            throw new FileStorageException("Failed to create directory: " + path);
        }
    }

    @Override
    public void deleteDirectory(String path) throws FileStorageException {
        File folder = new File(path);

        if (!folder.exists()) {
            throw new NoFileException("File not found: " + path);
        }

        if (!folder.isDirectory()) {
            throw new FileTypeException("Not a directory: " + path);
        }

        if (!folder.canWrite()) {
            throw new PermissionDeniedException("Cannot delete due to access rights: " + path);
        }

        if (!deleteFolder(folder)) {
            throw new FileStorageException("Could not delete directory: " + path);
        }
    }

    @Override
    public void renameDirectory(String path, String newPath) throws FileStorageException {
        File folder = new File(path);
        File target = new File(newPath);

        if (!folder.exists()) {
            throw new NoFileException("File not found: " + path);
        }

        if (target.exists()) {
            throw new AlreadyExistsException("A file with the same name already exists: " + newPath);
        }

        if (!folder.canWrite()) {
            throw new PermissionDeniedException("Permission denied: " + path);
        }

        if (!folder.renameTo(target)) {
            throw new FileStorageException("Could not rename directory: " + newPath);
        }
    }

    private boolean deleteFolder(File folder) {
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (!deleteFolder(file)) {
                        return false;
                    }
                }
            }
        }
        return folder.delete();
    }
}
