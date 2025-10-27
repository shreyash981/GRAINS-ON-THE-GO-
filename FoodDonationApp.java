import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class FoodDonationApp extends JFrame {

    private Connection conn;
    private JTextField donorNameField, locationField, foodTypeField, quantityField, pickupTimeField;
    private JTable foodTable;
    private DefaultTableModel tableModel;

    public FoodDonationApp() {
        setTitle("Food Donation Management");
        setSize(800, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Connect and setup DB
        connectDatabase();
        createTables();

        // Input Panel
        JPanel inputPanel = new JPanel(new GridLayout(6, 2, 5, 5));
        donorNameField = new JTextField();
        locationField = new JTextField();
        foodTypeField = new JTextField();
        quantityField = new JTextField();
        pickupTimeField = new JTextField();

        inputPanel.add(new JLabel("Donor Name / Organization:"));
        inputPanel.add(donorNameField);
        inputPanel.add(new JLabel("Location:"));
        inputPanel.add(locationField);
        inputPanel.add(new JLabel("Food Type:"));
        inputPanel.add(foodTypeField);
        inputPanel.add(new JLabel("Quantity (servings):"));
        inputPanel.add(quantityField);
        inputPanel.add(new JLabel("Pickup Time (dd-mm-yyyy hh:mm):"));
        inputPanel.add(pickupTimeField);

        JButton addButton = new JButton("Add Food Entry");
        JButton viewButton = new JButton("View Available Food");

        inputPanel.add(addButton);
        inputPanel.add(viewButton);
        add(inputPanel, BorderLayout.NORTH);

        // Table Section
        tableModel = new DefaultTableModel(new String[]{
                "Food ID", "Donor", "Location", "Food Type", "Quantity", "Pickup Time"
        }, 0);
        foodTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(foodTable);
        add(scrollPane, BorderLayout.CENTER);

        // Button Actions
        addButton.addActionListener(e -> insertFoodData());
        viewButton.addActionListener(e -> loadAvailableFood());
    }

    // Connect to SQLite DB
    private void connectDatabase() {
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:food_donation.db");
            System.out.println("✅ Database connected (food_donation.db created if not present).");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database connection failed: " + e.getMessage());
        }
    }

    // Create both tables
    private void createTables() {
        String donorTable = """
                CREATE TABLE IF NOT EXISTS donor (
                    donor_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    location TEXT
                );
                """;

        String foodTable = """
                CREATE TABLE IF NOT EXISTS available_food (
                    food_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    donor_id INTEGER,
                    food_type TEXT,
                    quantity INTEGER,
                    pickup_time TEXT,
                    FOREIGN KEY (donor_id) REFERENCES donor(donor_id)
                );
                """;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(donorTable);
            stmt.execute(foodTable);
            System.out.println("✅ Tables created successfully (if not existing).");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error creating tables: " + e.getMessage());
        }
    }

    // Insert food + donor info
    private void insertFoodData() {
        String donorName = donorNameField.getText().trim();
        String location = locationField.getText().trim();
        String foodType = foodTypeField.getText().trim();
        String quantityText = quantityField.getText().trim();
        String pickupTime = pickupTimeField.getText().trim();

        if (donorName.isEmpty() || foodType.isEmpty() || quantityText.isEmpty() || pickupTime.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill all required fields!");
            return;
        }

        try {
            int quantity = Integer.parseInt(quantityText);

            // Insert donor
            String donorQuery = "INSERT INTO donor (name, location) VALUES (?, ?)";
            PreparedStatement donorStmt = conn.prepareStatement(donorQuery, Statement.RETURN_GENERATED_KEYS);
            donorStmt.setString(1, donorName);
            donorStmt.setString(2, location);
            donorStmt.executeUpdate();

            ResultSet keys = donorStmt.getGeneratedKeys();
            int donorId = keys.next() ? keys.getInt(1) : -1;

            // Insert food
            String foodQuery = "INSERT INTO available_food (donor_id, food_type, quantity, pickup_time) VALUES (?, ?, ?, ?)";
            PreparedStatement foodStmt = conn.prepareStatement(foodQuery);
            foodStmt.setInt(1, donorId);
            foodStmt.setString(2, foodType);
            foodStmt.setInt(3, quantity);
            foodStmt.setString(4, pickupTime);
            foodStmt.executeUpdate();

            JOptionPane.showMessageDialog(this, "✅ Food entry added successfully!");
            clearFields();

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Quantity must be a number!");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error adding data: " + e.getMessage());
        }
    }

    // Load data into table
    private void loadAvailableFood() {
        tableModel.setRowCount(0);
        String query = """
                SELECT f.food_id, d.name, d.location, f.food_type, f.quantity, f.pickup_time
                FROM available_food f
                JOIN donor d ON f.donor_id = d.donor_id
                """;
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getInt("food_id"),
                        rs.getString("name"),
                        rs.getString("location"),
                        rs.getString("food_type"),
                        rs.getInt("quantity"),
                        rs.getString("pickup_time")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading data: " + e.getMessage());
        }
    }

    private void clearFields() {
        donorNameField.setText("");
        locationField.setText("");
        foodTypeField.setText("");
        quantityField.setText("");
        pickupTimeField.setText("");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FoodDonationApp().setVisible(true));
    }
}