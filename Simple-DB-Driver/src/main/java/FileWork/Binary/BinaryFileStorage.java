package FileWork.Binary;

import Exceptions.*;
import FileWork.FileStorage;

import java.io.*;

public class BinaryFileStorage implements FileStorage {
    private final String basePath;

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