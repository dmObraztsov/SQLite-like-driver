package FileWork.WAL;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WalEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    private String txId;
    private String status;
    private List<WalColumnEntry> columns;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WalColumnEntry {
        private String tableName;
        private String columnName;
        private List<String> originalData;
    }
}
