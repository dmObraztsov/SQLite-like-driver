package FileWork.JSON;

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
    public <T> T readFile(String path, Class<T> type) {
        try {
            File file = new File(path);

            if (!file.exists()) {
                System.out.println("File does not exist: " + file.getAbsolutePath());
                return null;
            }

            if (file.length() == 0) {
                System.out.println("File is empty: " + file.getAbsolutePath());
                return null;
            }

            System.out.println("File successfully read: " + file.getAbsolutePath());
            return mapper.readValue(file, type);
        } catch (IOException e) {
            System.err.println("Error reading file '" + path + "': " + e.getMessage());
            return null;
        }
    }

    @Override
    public <T> boolean writeFile(String path, T content) {
        try {
            File file = new File(path);
            mapper.writeValue(file, content);
            System.out.println("File successfully written: " + file.getAbsolutePath());
            return true;
        } catch (IOException e) {
            System.err.println("Error writing file '" + path + "': " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean deleteFile(String path) {
        File file = new File(path);
        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                System.out.println("File deleted: " + file.getAbsolutePath());
            } else {
                System.out.println("Failed to delete directory: " + file.getAbsolutePath());
            }
            return deleted;
        }

        System.out.println("File does not exist: " + file.getAbsolutePath());
        return false;
    }

    @Override
    public boolean renameFile(String path, String newName)
    {
        File file = new File(path);
        if (file.exists()) {
            boolean renamed = file.renameTo(new File(newName));
            if (renamed) {
                System.out.println("File renamed: " + file.getAbsolutePath());
            } else {
                System.out.println("Failed to rename directory: " + file.getAbsolutePath());
            }
            return renamed;
        }

        System.out.println("File does not exist: " + file.getAbsolutePath());
        return false;
    }

    @Override
    public boolean createDirectory(String path) {
        File folder = new File(path);
        if (!folder.exists()) {
            boolean created = folder.mkdirs();
            if (created) {
                System.out.println("Directory created: " + folder.getAbsolutePath());
            } else {
                System.out.println("Failed to create directory: " + folder.getAbsolutePath());
            }
            return created;
        }
        System.out.println("Directory already exists: " + folder.getAbsolutePath());
        return false;
    }

    @Override
    public boolean deleteDirectory(String path) {
        File folder = new File(path);
        if (!folder.exists()) {
            System.out.println("Directory does not exist: " + folder.getAbsolutePath());
            return false;
        }

        boolean deleted = deleteFolder(folder);
        if (deleted) {
            System.out.println("Directory deleted: " + folder.getAbsolutePath());
        } else {
            System.out.println("Failed to delete directory: " + folder.getAbsolutePath());
        }
        return deleted;
    }

    @Override
    public boolean renameDirectory(String path, String newName)
    {
        File folder = new File(path);
        if(!folder.exists()) {
            System.out.println("Directory does not exist: " + folder.getAbsolutePath());
            return false;
        }

        System.out.println("Directory successful renamed: " + folder.getAbsolutePath());
        return folder.renameTo(new File(newName));
    }

    private boolean deleteFolder(File folder) {
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