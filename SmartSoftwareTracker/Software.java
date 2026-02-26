import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Software {
    private String name;
    private String version;
    private LocalDate installationDate;
    private String status;

    public Software(String name, String version, LocalDate installationDate, String status) {
        this.name = name;
        this.version = version;
        this.installationDate = installationDate;
        this.status = status;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public LocalDate getInstallationDate() { return installationDate; }
    public void setInstallationDate(LocalDate installationDate) { this.installationDate = installationDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return String.format("%s,%s,%s,%s", 
            name, version, installationDate.format(formatter), status);
    }

    public static Software fromString(String line) {
        String[] parts = line.split(",");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return new Software(
            parts[0],
            parts[1],
            LocalDate.parse(parts[2], formatter),
            parts[3]
        );
    }
}