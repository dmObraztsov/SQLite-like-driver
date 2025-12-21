package FileWork.Metadata;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Setter
@Getter
public class DatabaseMetadata {
    private String name;
    private String version;
    private String encoding;
    private String collation;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private Date createdDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private Date lastModified;

    //TODO private long totalSize;

    public DatabaseMetadata() {
    }

    public DatabaseMetadata(String name, String version, String encoding,
                            String collation, Date createdDate, Date lastModified) {
        this.name = name;
        this.version = version;
        this.encoding = encoding;
        this.collation = collation;
        this.createdDate = createdDate;
        this.lastModified = lastModified;
    }

}