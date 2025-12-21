package FileWork.Metadata;

import Yadro.DataStruct.Collate;
import Yadro.DataStruct.Constraints;
import Yadro.DataStruct.DataType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

@Setter
@Getter
public class ColumnMetadata {
    private String name;
    private DataType type;
    private int size;
    private ArrayList<Constraints> constraints = new ArrayList<>();
    private Collate collate;

    public ColumnMetadata() {}

    public ColumnMetadata(String name, DataType type, int size, ArrayList<Constraints> constraints, Collate collate) {
        this.name = name;
        this.type = type;
        this.size = size;
        this.constraints = constraints;
        this.collate = collate;
    }

    @JsonIgnore
    public void incrementSize() {
        this.size++;
    }
}