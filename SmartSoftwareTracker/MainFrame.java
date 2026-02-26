import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;

public class MainFrame extends JFrame {
    private SoftwareManager manager;
    private JTable table;
    private DefaultTableModel tableModel;

    public MainFrame() {
        manager = new SoftwareManager();
        setTitle("Smart Software Installation Tracker");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);

        createMenuBar();
        createTable();
        createButtonPanel();
        refreshTable();
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
    }

    private void createTable() {
        String[] columns = {"Name", "Version", "Installation Date", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        JButton addButton = new JButton("Add");
        JButton editButton = new JButton("Edit");
        JButton deleteButton = new JButton("Delete");
        JButton refreshButton = new JButton("Refresh");
        JButton scanButton = new JButton("Scan System");  // New button

        addButton.addActionListener(e -> addSoftware());
        editButton.addActionListener(e -> editSoftware());
        deleteButton.addActionListener(e -> deleteSoftware());
        refreshButton.addActionListener(e -> refreshTable());
        
        // Add action for scan button
        scanButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                "This will scan your system for installed software. Continue?",
                "Confirm System Scan",
                JOptionPane.YES_NO_OPTION);
                
            if (confirm == JOptionPane.YES_OPTION) {
                // Run scan in background thread to prevent UI freezing
                new Thread(() -> {
                    scanButton.setEnabled(false);
                    scanButton.setText("Scanning...");
                    manager.scanSystemForSoftware();
                    SwingUtilities.invokeLater(() -> {
                        refreshTable();
                        scanButton.setEnabled(true);
                        scanButton.setText("Scan System");
                        JOptionPane.showMessageDialog(this, 
                            "System scan complete! Total software: " + manager.getDetectedSoftwareCount());
                    });
                }).start();
            }
        });

        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(scanButton);  // Add the new button
        
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void addSoftware() {
        AddEditDialog dialog = new AddEditDialog(this, "Add Software", null);
        dialog.setVisible(true);
        
        if (dialog.isConfirmed()) {
            manager.addSoftware(
                dialog.getName(),
                dialog.getVersion(),
                dialog.getDate(),
                dialog.getStatus()
            );
            refreshTable();
        }
    }

    private void editSoftware() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a software to edit!");
            return;
        }

        Software software = manager.getAllSoftware().get(selectedRow);
        AddEditDialog dialog = new AddEditDialog(this, "Edit Software", software);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            manager.updateSoftware(
                selectedRow,
                dialog.getName(),
                dialog.getVersion(),
                dialog.getDate(),
                dialog.getStatus()
            );
            refreshTable();
        }
    }

    private void deleteSoftware() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a software to delete!");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to delete this software?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            manager.removeSoftware(selectedRow);
            refreshTable();
        }
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Software s : manager.getAllSoftware()) {
            tableModel.addRow(new Object[]{
                s.getName(),
                s.getVersion(),
                s.getInstallationDate(),
                s.getStatus()
            });
        }
    }
}