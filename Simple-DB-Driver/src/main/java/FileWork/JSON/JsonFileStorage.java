package FileWork.JSON;

import Exceptions.*;
import FileWork.FileStorage;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

public class JsonFileStorage implements FileStorage {
    private final ObjectMapper mapper = JacksonConfig.createConfiguredMapper();

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
                throw new NoFileException("File not found:" + path);
            }

            if (file.length() == 0) {
                throw new EmptyFileException("Empty File:" + path);
            }

            if (!file.canRead()) {
                throw new PermissionDeniedException("Permission denied:" + path);
            }

            return mapper.readValue(file, type);

        } catch (IOException e) {
            throw new SerializationStorageException("Could not read file: " + path, e);
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

        boolean deleted =  file.delete();
        if (!deleted) {
            throw new FileStorageException("Could not delete file: " + path);
        }
    }

    @Override
    public void renameFile(String path, String newName) throws FileStorageException {
        File file = new File(path);
        File target = new File(newName);

        if(!file.canWrite()) {
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
            if (folder.isDirectory()) {
                return;
            } else {
                throw new AlreadyExistsException("A file with the same name already exists: " + path);
            }
        }

//        File parent = folder.getParentFile();
//        if (parent != null && !parent.canWrite()) {
//            throw new PermissionDeniedException("Cannot write to parent directory: " + parent.getAbsolutePath());
//        }

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

        if  (!folder.canWrite()) {
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

        if(!folder.exists()) {
            throw new NoFileException("File not found: " + path);
        }

        if (target.exists()) {
            throw new AlreadyExistsException("A file with the same name already exists: " + newPath);
        }

        if(!folder.canWrite()) {
            throw new PermissionDeniedException("Permission denied:" + path);
        }

        boolean renamed = folder.renameTo(target);
        if (!renamed) {
            throw new FileStorageException("Could not rename directory: " + newPath);
        }
    }

    private boolean deleteFolder(File folder) { //TODO можно добавить вывод тру или фолз после каждого удаления, чтобы можно было понять, какой именно файл не смогли удалить
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteFolder(file);
                }
            }
        }
        return folder.delete();
    }
}