package FileWork.JSON;

import Exceptions.*;
import FileWork.FileStorage;
import Yadro.DataStruct.Column;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

public class JsonFileStorage implements FileStorage {
    private final ObjectMapper mapper = JacksonConfig.createConfiguredMapper();
    private final String basePath;

    public JsonFileStorage(String basePath) {
        this.basePath = basePath;
        File dir = new File(basePath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public JsonFileStorage() {
        this("database");
    }

    @Override
    public boolean exists(String path) {
        File dir = new File(path);
        return dir.exists();
    }

    @Override
    public <T> T readFile(String path, Class<T> type) throws FileStorageException {
        try {
            File file = new File(path);

            if (!file.exists()) {
                throw new NoFileException("File not found: " + path);
            }

            if (file.length() == 0) {
                throw new EmptyFileException("Empty File: " + path);
            }

            if (!file.canRead()) {
                throw new PermissionDeniedException("Permission denied: " + path);
            }

            return mapper.readValue(file, type);

        } catch (IOException e) {
            throw new FileStorageException("Failed to read file: " + path, e);
        }
    }

    @Override
    public <T> void writeFile(String path, T content) throws FileStorageException {
        try {
            File file = new File(path);

            mapper.writeValue(file, content);

            if (!file.exists()) {
                throw new NoFileException("File not found:" + path);
            }

            if (!file.canWrite()) {
                throw new PermissionDeniedException("Permission denied:" + path);
            }

            System.out.println("File successfully written: " + file.getAbsolutePath());
        } catch (IOException e) {
            throw new SerializationStorageException("Could not write file: " + path, e);
        }
    }

    @Override
    public void deleteFile(String path) throws FileStorageException {
        File file = new File(path);

        if (!file.exists()) {
            throw new NoFileException("File not found:" + path);
        }

        if (!file.canWrite()) {
            throw new PermissionDeniedException("Permission denied:" + path);
        }

        boolean deleted = file.delete();
        if (!deleted) {
            throw new FileStorageException("Could not delete file: " + path);
        }
    }

    @Override
    public void renameFile(String path, String newName) throws FileStorageException {
        File file = new File(path);
        File target = new File(newName);

        if (!file.canWrite()) {
            throw new PermissionDeniedException("Permission denied:" + path);
        }

        if (target.exists()) {
            throw new AlreadyExistsException("File with new name already exists:" + newName);
        }

        if (!file.exists()) {
            throw new NoFileException("File Not Found: " + path);
        }

        boolean renamed = file.renameTo(new File(newName));
        if (!renamed) {
            throw new FileStorageException("Could not rename file: " + newName);
        }
    }

    @Override
    public void createDirectory(String path) throws FileStorageException {
        File folder = new File(path);

        if (folder.exists()) {
            throw new AlreadyExistsException("Directory already exists: " + path);
        }

        boolean created = folder.mkdirs();
        if (!created) {
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
            throw new PermissionDeniedException("Cannot delete due to accession rights: " + path);
        }

        boolean deleted = deleteFolder(folder);
        if (!deleted) {
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
            throw new PermissionDeniedException("Permission denied:" + path);
        }

        boolean renamed = folder.renameTo(target);
        if (!renamed) {
            throw new FileStorageException("Could not rename directory: " + newPath);
        }
    }

    @Override
    public void writeRow(String path, int rowIndex, String newValue) throws FileStorageException {
        Column col = readFile(path, Column.class);
        col.getData().set(rowIndex, newValue != null ? newValue : "NULL");
        writeFile(path, col);
    }

    private boolean deleteFolder(File folder) {
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (!deleteFolder(file)) {
                        System.err.println("Failed to delete: " + file.getAbsolutePath());
                        return false;
                    }
                }
            }
        }
        return folder.delete();
    }
}
