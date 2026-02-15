package SqlParser.QueriesStruct;

import Yadro.DataStruct.Row;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public class ExecutionResult {
    @Getter
    @Setter
    private boolean success;

    @Getter
    @Setter
    private String message;

    @Getter
    @Setter
    List<Row> rows;

    public ExecutionResult(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.rows = new ArrayList<>();
    }

    public ExecutionResult(boolean success, String message, List<Row> rows) {
        this(success, message);
        this.rows.addAll(rows);
    }
}
