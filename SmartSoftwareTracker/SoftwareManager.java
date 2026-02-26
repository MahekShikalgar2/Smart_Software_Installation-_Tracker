import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SoftwareManager {
    private List<Software> softwareList;
    private static final String DATA_FILE = "software_data.txt";

    public SoftwareManager() {
        softwareList = new ArrayList<>();
        loadData();
    }

    public void addSoftware(String name, String version, LocalDate date, String status) {
        softwareList.add(new Software(name, version, date, status));
        saveData();
    }

    public void removeSoftware(int index) {
        if (index >= 0 && index < softwareList.size()) {
            softwareList.remove(index);
            saveData();
        }
    }

    public void updateSoftware(int index, String name, String version, LocalDate date, String status) {
        if (index >= 0 && index < softwareList.size()) {
            Software s = softwareList.get(index);
            s.setName(name);
            s.setVersion(version);
            s.setInstallationDate(date);
            s.setStatus(status);
            saveData();
        }
    }

    public List<Software> getAllSoftware() {
        return new ArrayList<>(softwareList);
    }

    public void scanSystemForSoftware() {
        try {
            System.out.println("Scanning for installed software using Registry...");
            
            // Try PowerShell first (with full path)
            if (!scanWithPowerShell()) {
                // If PowerShell fails, use direct registry query
                scanWithRegistryQuery();
            }
            
            // Save the updated list
            saveData();
            
            System.out.println("Scan complete! Total software in database: " + softwareList.size());
            
        } catch (Exception e) {
            System.err.println("Error scanning system: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private boolean scanWithPowerShell() {
        try {
            // Try different possible PowerShell locations
            String[] possiblePaths = {
                "powershell.exe",
                "C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe",
                "C:\\Windows\\SysWOW64\\WindowsPowerShell\\v1.0\\powershell.exe"
            };
            
            for (String powerShellPath : possiblePaths) {
                File psFile = new File(powerShellPath);
                if (psFile.exists()) {
                    System.out.println("Found PowerShell at: " + powerShellPath);
                    
                    String[] command = {
                        powerShellPath,
                        "-Command",
                        "Get-ItemProperty HKLM:\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\*, " +
                        "HKLM:\\Software\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\* | " +
                        "Where-Object { $_.DisplayName } | " +
                        "Select-Object DisplayName, DisplayVersion, InstallDate | " +
                        "ConvertTo-Csv -NoTypeInformation"
                    };
                    
                    ProcessBuilder processBuilder = new ProcessBuilder(command);
                    processBuilder.redirectErrorStream(true);
                    Process process = processBuilder.start();
                    
                    int count = parsePowerShellOutput(process);
                    
                    process.waitFor();
                    
                    if (count > 0) {
                        System.out.println("PowerShell scan added " + count + " software entries.");
                        return true;
                    }
                }
            }
            
        } catch (Exception e) {
            System.out.println("PowerShell scan failed: " + e.getMessage());
        }
        return false;
    }
    
    private int parsePowerShellOutput(Process process) throws IOException {
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            boolean firstLine = true;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                if (firstLine) {
                    firstLine = false;
                    continue; // Skip CSV header
                }
                
                // Parse CSV format
                String[] parts = line.split("\",\"");
                if (parts.length >= 1) {
                    String name = parts[0].replaceAll("^\"|\"$", "").trim();
                    
                    if (!name.isEmpty() && !name.equalsIgnoreCase("DisplayName")) {
                        String version = (parts.length >= 2) ? 
                            parts[1].replaceAll("^\"|\"$", "").trim() : "Unknown";
                        
                        // Check if already exists
                        boolean exists = false;
                        for (Software s : softwareList) {
                            if (s.getName().equalsIgnoreCase(name)) {
                                exists = true;
                                break;
                            }
                        }
                        
                        if (!exists) {
                            softwareList.add(new Software(name, version, LocalDate.now(), "Installed"));
                            count++;
                        }
                    }
                }
            }
        }
        return count;
    }

    private void scanWithRegistryQuery() {
        try {
            System.out.println("Using enhanced registry query method...");
            
            // Scan 64-bit software
            scanRegistryPath("HKLM\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall");
            
            // Scan 32-bit software on 64-bit Windows
            scanRegistryPath("HKLM\\Software\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall");
            
            // Scan per-user installations
            scanRegistryPath("HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall");
            
        } catch (Exception e) {
            System.err.println("Registry scan error: " + e.getMessage());
        }
    }
    
    private void scanRegistryPath(String regPath) {
        try {
            System.out.println("Scanning: " + regPath);
            
            // First, get all subkeys
            ProcessBuilder processBuilder = new ProcessBuilder(
                "reg", "query", regPath
            );
            
            Process process = processBuilder.start();
            List<String> subkeys = new ArrayList<>();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("HKEY")) {
                        subkeys.add(line);
                    }
                }
            }
            
            process.waitFor();
            
            // Now query each subkey for DisplayName and DisplayVersion
            for (String subkey : subkeys) {
                String displayName = getRegistryValue(subkey, "DisplayName");
                
                if (displayName != null && !displayName.isEmpty() && 
                    !displayName.contains("{")) { // Skip GUID-like names
                    
                    // Check if already exists
                    boolean exists = false;
                    for (Software s : softwareList) {
                        if (s.getName().equalsIgnoreCase(displayName)) {
                            exists = true;
                            break;
                        }
                    }
                    
                    if (!exists) {
                        String version = getRegistryValue(subkey, "DisplayVersion");
                        if (version == null) version = getRegistryValue(subkey, "Version");
                        if (version == null) version = "Unknown";
                        
                        String installDate = getRegistryValue(subkey, "InstallDate");
                        LocalDate date = LocalDate.now();
                        
                        if (installDate != null && installDate.length() == 8) {
                            try {
                                String formattedDate = installDate.substring(0, 4) + "-" +
                                                     installDate.substring(4, 6) + "-" +
                                                     installDate.substring(6, 8);
                                date = LocalDate.parse(formattedDate);
                            } catch (Exception e) {
                                // Use current date if parsing fails
                            }
                        }
                        
                        softwareList.add(new Software(displayName, version, date, "Installed"));
                        System.out.println("Found: " + displayName + " (" + version + ")");
                    }
                }
            }
            
        } catch (Exception e) {
            // Ignore errors for specific paths (may not exist)
        }
    }
    
    private String getRegistryValue(String keyPath, String valueName) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                "reg", "query", keyPath, "/v", valueName
            );
            Process process = processBuilder.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains(valueName)) {
                        // Registry output format: "    DisplayName    REG_SZ    Value"
                        String[] parts = line.split("REG_SZ");
                        if (parts.length >= 2) {
                            return parts[1].trim();
                        }
                    }
                }
            }
            
            process.waitFor();
        } catch (Exception e) {
            // Value not found
        }
        return null;
    }

    public int getDetectedSoftwareCount() {
        return softwareList.size();
    }

    private void saveData() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(DATA_FILE))) {
            for (Software s : softwareList) {
                writer.write(s.toString());
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadData() {
        File file = new File(DATA_FILE);
        if (!file.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    try {
                        softwareList.add(Software.fromString(line));
                    } catch (Exception e) {
                        System.err.println("Error parsing line: " + line);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}