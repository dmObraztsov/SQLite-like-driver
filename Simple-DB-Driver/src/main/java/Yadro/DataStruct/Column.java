package Yadro.DataStruct;

import java.util.ArrayList;

public class Column {
    private ArrayList<String> data = new ArrayList<>();

    public Column(){}

    public ArrayList<String> getData() {
        return data;
    }

    public void setData(ArrayList<String> data) {
        this.data = data;
    }

    public boolean addData(String content) {
        data.add(content);
        return true;
    }
}