package Yadro.DataStruct;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

@Setter
@Getter
public class Column {
    private ArrayList<String> data = new ArrayList<>();

    public Column(){}

    public void addData(int index, String content) { data.add(index, content); }
}