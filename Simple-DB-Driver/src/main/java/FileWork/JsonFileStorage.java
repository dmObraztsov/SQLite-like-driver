package FileWork;

import java.io.File;

public class JsonFileStorage implements FileStorage {
    @Override
    public boolean exists(String path) {
        File file = new File("src/main/data/" + path);
        return file.exists();
    }

    @Override
    public String readFile(String path) {
        return "";
    }

    @Override
    public void writeFile(String path, String content) {

    }

    @Override
    public void createDirectory(String path) {
        if(!exists(path))
        {
            File folder = new File("src/main/data/" + path);
            folder.mkdir(); //TODO handle errors
        }

        else
        {
            System.out.println("Directory already exist");
        }
    }

    @Override
    public void deleteDirectory(String path)
    {
        //TODO func
    }
}
