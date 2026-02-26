import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;

public class AddEditDialog extends JDialog {
    private JTextField nameField;
    private JTextField versionField;
    private JTextField dateField;
    private JComboBox<String> statusCombo;
    private boolean confirmed = false;

    public AddEditDialog(JFrame parent, String title, Software software) {
        super(parent, title, true);
        setSize(300, 250);
        setLocationRelativeTo(parent);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Name
        gbc.gridx = 0; gbc.gridy = 0;
        add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        nameField = new JTextField(15);
        add(nameField, gbc);

        // Version
        gbc.gridx = 0; gbc.gridy = 1;
        add(new JLabel("Version:"), gbc);
        gbc.gridx = 1;
        versionField = new JTextField(15);
        add(versionField, gbc);

        // Installation Date (YYYY-MM-DD)
        gbc.gridx = 0; gbc.gridy = 2;
        add(new JLabel("Date (YYYY-MM-DD):"), gbc);
        gbc.gridx = 1;
        dateField = new JTextField(15);
        dateField.setText(LocalDate.now().toString());
        add(dateField, gbc);

        // Status
        gbc.gridx = 0; gbc.gridy = 3;
        add(new JLabel("Status:"), gbc);
        gbc.gridx = 1;
        statusCombo = new JComboBox<>(new String[]{"Installed", "Trial", "Expired", "Not Installed"});
        add(statusCombo, gbc);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");

        okButton.addActionListener(e -> {
            if (validateInput()) {
                confirmed = true;
                setVisible(false);
            }
        });

        cancelButton.addActionListener(e -> setVisible(false));

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        gbc.gridx = 0; gbc.gridy = 4;
        gbc.gridwidth = 2;
        add(buttonPanel, gbc);

        // If editing, populate fields
        if (software != null) {
            nameField.setText(software.getName());
            versionField.setText(software.getVersion());
            dateField.setText(software.getInstallationDate().toString());
            statusCombo.setSelectedItem(software.getStatus());
        }
    }

    private boolean validateInput() {
        if (nameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name cannot be empty!");
            return false;
        }
        try {
            LocalDate.parse(dateField.getText().trim());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Invalid date format! Use YYYY-MM-DD");
            return false;
        }
        return true;
    }

    public boolean isConfirmed() { return confirmed; }
    public String getName() { return nameField.getText().trim(); }
    public String getVersion() { return versionField.getText().trim(); }
    public LocalDate getDate() { return LocalDate.parse(dateField.getText().trim()); }
    public String getStatus() { return (String) statusCombo.getSelectedItem(); }
}