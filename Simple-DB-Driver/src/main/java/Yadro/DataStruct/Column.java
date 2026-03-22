package Yadro.DataStruct;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.ArrayList;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Column {
    private ArrayList<String> data = new ArrayList<>();

    public void addData(int index, String content) {
        data.add(index, content);
    }
}