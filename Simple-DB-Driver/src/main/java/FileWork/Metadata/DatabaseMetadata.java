package FileWork.Metadata;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;

public class DatabaseMetadata {
    private String name;
    private String version;
    private String encoding;
    private String collation;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private Date createdDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private Date lastModified;

    private long totalSize;

    public DatabaseMetadata() {
    }

    public DatabaseMetadata(String name, String version, String encoding,
                            String collation, Date createdDate, Date lastModified,
                            long totalSize) {
        this.name = name;
        this.version = version;
        this.encoding = encoding;
        this.collation = collation;
        this.createdDate = createdDate;
        this.lastModified = lastModified;
        this.totalSize = totalSize;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getCollation() {
        return collation;
    }

    public void setCollation(String collation) {
        this.collation = collation;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }
}