package Yadro.DataStruct;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Column {
    private ArrayList<Object> data = new ArrayList<>();

    public Column(){}

    public ArrayList<Object> getData() {
        return data;
    }

    public void setData(ArrayList<Object> data) {
        this.data = data;
    }

    public boolean addData(Object content) {
        data.add(content);
        return true;
    }
}