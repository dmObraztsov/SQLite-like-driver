package FileWork.JSON;

import FileWork.FileStorage;
import Yadro.DataStruct.Column;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

public class JsonFileStorage implements FileStorage {
    private static final String BASE_PATH = "src/main/data/";
    private final ObjectMapper mapper = JacksonConfig.createConfiguredMapper();

    private String getFullPath(String path) {
        return BASE_PATH + path;
    }

    private String getFilePath(String path) {
        return getFullPath(path + ".json");
    }

    @Override
    public boolean exists(String path) {
        File dir = new File(getFullPath(path));
        return dir.exists();
    }

    @Override
    public Column readFile(String path) {
        try {
            File file = new File(getFilePath(path));

            if (!file.exists()) {
                System.out.println("File does not exist: " + file.getAbsolutePath());
                return null;
            }

            if (file.length() == 0) {
                System.out.println("File is empty: " + file.getAbsolutePath());
                return null;
            }

            return mapper.readValue(file, Column.class);
        } catch (IOException e) {
            System.err.println("Error reading file '" + path + "': " + e.getMessage());
            return null;
        }
    }

    @Override
    public boolean writeFile(String path, Column content) {
        try {
            File file = new File(getFilePath(path));
            mapper.writeValue(file, content);
            System.out.println("File successfully written: " + file.getAbsolutePath());
            return true;
        } catch (IOException e) {
            System.err.println("Error writing file '" + path + "': " + e.getMessage());
            return false;
        }
    }



    @Override
    public boolean createDirectory(String path) {
        File folder = new File(getFullPath(path));
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
        File folder = new File(getFullPath(path));
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
        File folder = new File(getFullPath(path));
        if(!folder.exists()) {
            System.out.println("Directory does not exist: " + folder.getAbsolutePath());
            return false;
        }

        return folder.renameTo(new File(getFullPath(newName)));
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