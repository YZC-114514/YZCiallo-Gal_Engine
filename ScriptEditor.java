import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * Visual Novel Script Editor (Integrated with Smart VAR Editor)
 */
public class ScriptEditor extends JFrame {

    private static final String[] COLUMN_NAMES = {"#", "Type", "Character", "Mood", "Content/Parameter"};
    private static final int[] COLUMN_WIDTHS = {40, 100, 100, 80, 500};
    
    private JComboBox<String> fileSelector;
    private JTable scriptTable;
    private DefaultTableModel tableModel;
    private JButton saveButton, addButton, deleteButton, moveUpButton, moveDownButton;
    private JLabel statusLabel;
    
    private ArrayList<ScriptData> currentScript = new ArrayList<>();
    private String currentFile = null;
    private boolean hasUnsavedChanges = false;
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public ScriptEditor() {
        setTitle("Visual Novel Script Editor");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Handle window closing with unsaved changes check
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (confirmDiscardChanges()) {
                    dispose();
                }
            }
        });

        initComponents();
        loadFileList();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        
        // Top panel - File selection
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        topPanel.setBorder(new EmptyBorder(5, 10, 5, 10));
        
        topPanel.add(new JLabel("Select Chapter:"));
        fileSelector = new JComboBox<>();
        fileSelector.setPreferredSize(new Dimension(200, 30));
        fileSelector.addActionListener(e -> onFileSelected());
        topPanel.add(fileSelector);
        
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> loadFileList());
        topPanel.add(refreshButton);
        
        JButton newFileButton = new JButton("New Chapter");
        newFileButton.addActionListener(e -> createNewChapter());
        topPanel.add(newFileButton);
        
        add(topPanel, BorderLayout.NORTH);
        
        // Center - Table
        tableModel = new DefaultTableModel(COLUMN_NAMES, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column > 0; // Row number not editable
            }
        };
        
        scriptTable = new JTable(tableModel);
        scriptTable.setRowHeight(30);
        scriptTable.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        scriptTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 14));
        scriptTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Set column widths
        for (int i = 0; i < COLUMN_WIDTHS.length; i++) {
            scriptTable.getColumnModel().getColumn(i).setPreferredWidth(COLUMN_WIDTHS[i]);
        }
        
        // 1. 設定 Type 欄位 (Index 1) 的下拉選單
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{
            "DIALOGUE", "BG", "CHOICE", "LABEL", "GOTO", "NEXT_CHAPTER", "VAR"
        });
        scriptTable.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(typeCombo));
        
        SmartCellEditor smartEditor = new SmartCellEditor(scriptTable, 1);
        scriptTable.getColumnModel().getColumn(4).setCellEditor(smartEditor);

        // Listen for table changes
        tableModel.addTableModelListener(e -> {
            if (e.getType() == javax.swing.event.TableModelEvent.UPDATE) {
                hasUnsavedChanges = true;
                updateTitle();
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(scriptTable);
        scrollPane.setBorder(new EmptyBorder(0, 10, 0, 10));
        add(scrollPane, BorderLayout.CENTER);
        
        // Right panel - Action buttons
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBorder(new EmptyBorder(10, 5, 10, 10));
        
        addButton = new JButton("Add Line");
        addButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        addButton.addActionListener(e -> addNewLine());
        rightPanel.add(addButton);
        rightPanel.add(Box.createVerticalStrut(10));
        
        deleteButton = new JButton("Delete Line");
        deleteButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        deleteButton.addActionListener(e -> deleteSelectedLine());
        rightPanel.add(deleteButton);
        rightPanel.add(Box.createVerticalStrut(20));
        
        moveUpButton = new JButton("Move Up");
        moveUpButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        moveUpButton.addActionListener(e -> moveLineUp());
        rightPanel.add(moveUpButton);
        rightPanel.add(Box.createVerticalStrut(10));
        
        moveDownButton = new JButton("Move Down");
        moveDownButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        moveDownButton.addActionListener(e -> moveLineDown());
        rightPanel.add(moveDownButton);
        
        add(rightPanel, BorderLayout.EAST);
        
        // Bottom panel - Save and status
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        statusLabel = new JLabel("Ready. Select a chapter file to edit.");
        bottomPanel.add(statusLabel, BorderLayout.WEST);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        saveButton = new JButton("Save");
        saveButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        saveButton.setPreferredSize(new Dimension(100, 35));
        saveButton.addActionListener(e -> saveScript());
        buttonPanel.add(saveButton);
        
        JButton saveAsButton = new JButton("Save As...");
        saveAsButton.addActionListener(e -> saveScriptAs());
        buttonPanel.add(saveAsButton);
        
        bottomPanel.add(buttonPanel, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void loadFileList() {
        fileSelector.removeAllItems();
        fileSelector.addItem("-- Select a file --");
        
        File dir = new File(".");
        File[] files = dir.listFiles((d, name) -> name.matches("Chapter.*\\.json"));
        
        if (files != null) {
            for (File file : files) {
                fileSelector.addItem(file.getName());
            }
        }
    }

    private void onFileSelected() {
        String selected = (String) fileSelector.getSelectedItem();
        if (selected == null || selected.startsWith("--")) return;
        
        if (hasUnsavedChanges && !confirmDiscardChanges()) {
            fileSelector.setSelectedItem(currentFile != null ? currentFile : "-- Select a file --");
            return;
        }
        
        loadScript(selected);
    }

    private void loadScript(String filename) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(filename), StandardCharsets.UTF_8))) {
            
            java.lang.reflect.Type listType = new TypeToken<ArrayList<ScriptData>>(){}.getType();
            currentScript = GSON.fromJson(reader, listType);
            currentFile = filename;
            hasUnsavedChanges = false;
            
            refreshTable();
            updateTitle();
            statusLabel.setText("Loaded: " + filename + " (" + currentScript.size() + " lines)");
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Error loading file: " + e.getMessage(), 
                "Load Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        
        for (int i = 0; i < currentScript.size(); i++) {
            ScriptData data = currentScript.get(i);
            tableModel.addRow(new Object[]{
                i + 1,
                data.type != null ? data.type : "",
                data.name != null ? data.name : "",
                data.mood != null ? data.mood : "",
                getDisplayContent(data)
            });
        }
    }

    private String getDisplayContent(ScriptData data) {
        if (data.type == null) return "";
        
        switch (data.type.toUpperCase()) {
            case "DIALOGUE":
                return data.text != null ? data.text : "";
            case "BG":
            case "LABEL":
            case "GOTO":
            case "NEXT_CHAPTER":
            case "CHOICE":
            case "VAR":
                return data.text != null && !data.text.isEmpty() ? data.text : (data.param != null ? data.param : "");
            default:
                return data.text != null ? data.text : (data.param != null ? data.param : "");
        }
    }

    private void addNewLine() {
        int selectedRow = scriptTable.getSelectedRow();
        int insertIndex = selectedRow >= 0 ? selectedRow + 1 : currentScript.size();
        
        ScriptData newData = new ScriptData();
        newData.type = "DIALOGUE";
        newData.name = "";
        newData.mood = "";
        newData.text = "";
        
        currentScript.add(insertIndex, newData);
        hasUnsavedChanges = true;
        refreshTable();
        updateTitle();
        
        scriptTable.setRowSelectionInterval(insertIndex, insertIndex);
        statusLabel.setText("Added new line at position " + (insertIndex + 1));
    }

    private void deleteSelectedLine() {
        int selectedRow = scriptTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a line to delete.");
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete line " + (selectedRow + 1) + "?",
            "Confirm Delete", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            currentScript.remove(selectedRow);
            hasUnsavedChanges = true;
            refreshTable();
            updateTitle();
            statusLabel.setText("Deleted line " + (selectedRow + 1));
        }
    }

    private void moveLineUp() {
        int selectedRow = scriptTable.getSelectedRow();
        if (selectedRow <= 0) return;
        
        ScriptData temp = currentScript.get(selectedRow);
        currentScript.set(selectedRow, currentScript.get(selectedRow - 1));
        currentScript.set(selectedRow - 1, temp);
        
        hasUnsavedChanges = true;
        refreshTable();
        updateTitle();
        scriptTable.setRowSelectionInterval(selectedRow - 1, selectedRow - 1);
    }

    private void moveLineDown() {
        int selectedRow = scriptTable.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= currentScript.size() - 1) return;
        
        ScriptData temp = currentScript.get(selectedRow);
        currentScript.set(selectedRow, currentScript.get(selectedRow + 1));
        currentScript.set(selectedRow + 1, temp);
        
        hasUnsavedChanges = true;
        refreshTable();
        updateTitle();
        scriptTable.setRowSelectionInterval(selectedRow + 1, selectedRow + 1);
    }

    private void syncTableToScript() {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            ScriptData data = currentScript.get(i);
            
            String type = (String) tableModel.getValueAt(i, 1);
            String name = (String) tableModel.getValueAt(i, 2);
            String mood = (String) tableModel.getValueAt(i, 3);
            String content = (String) tableModel.getValueAt(i, 4);
            
            data.type = type;
            data.name = name.isEmpty() ? null : name;
            data.mood = mood.isEmpty() ? null : mood;
            
            if (type != null) {
                switch (type.toUpperCase()) {
                    case "DIALOGUE":
                        data.text = content;
                        data.param = null;
                        break;
                    case "BG":
                    case "LABEL":
                    case "GOTO":
                    case "NEXT_CHAPTER":
                    case "CHOICE":
                        data.param = content;
                        data.text = "";
                        break;
                    case "VAR":
                        data.text = content;
                        data.param = null;
                        break;
                    default:
                        data.text = content;
                }
            }
        }
    }

    private void saveScript() {
        if (currentFile == null) {
            saveScriptAs();
            return;
        }
        
        syncTableToScript();
        
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(currentFile), StandardCharsets.UTF_8))) {
            
            GSON.toJson(currentScript, writer);
            hasUnsavedChanges = false;
            updateTitle();
            statusLabel.setText("Saved: " + currentFile);
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error saving file: " + e.getMessage(),
                "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveScriptAs() {
        String newName = JOptionPane.showInputDialog(this,
            "Enter filename (e.g., Chapter2_1.json):",
            "Save As", JOptionPane.PLAIN_MESSAGE);
        
        if (newName == null || newName.trim().isEmpty()) return;
        
        if (!newName.endsWith(".json")) {
            newName += ".json";
        }
        
        currentFile = newName;
        saveScript();
        loadFileList();
        fileSelector.setSelectedItem(currentFile);
    }

    private void createNewChapter() {
        if (hasUnsavedChanges && !confirmDiscardChanges()) {
            return;
        }
        
        String newName = JOptionPane.showInputDialog(this,
            "Enter new chapter filename (e.g., Chapter2_1.json):",
            "New Chapter", JOptionPane.PLAIN_MESSAGE);
        
        if (newName == null || newName.trim().isEmpty()) return;
        
        if (!newName.endsWith(".json")) {
            newName += ".json";
        }
        
        currentScript = new ArrayList<>();
        currentFile = newName;
        hasUnsavedChanges = true;
        
        ScriptData bgData = new ScriptData();
        bgData.type = "BG";
        bgData.param = "Background.jpg";
        bgData.text = "";
        currentScript.add(bgData);
        
        refreshTable();
        updateTitle();
        statusLabel.setText("Created new chapter: " + newName + " (unsaved)");
    }

    private boolean confirmDiscardChanges() {
        if (!hasUnsavedChanges) return true;
        
        int result = JOptionPane.showConfirmDialog(this,
            "You have unsaved changes. Do you want to save before continuing?",
            "Unsaved Changes",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (result == JOptionPane.YES_OPTION) {
            saveScript();
            return true;
        } else if (result == JOptionPane.NO_OPTION) {
            return true;
        }
        return false;
    }

    private void updateTitle() {
        String title = "Visual Novel Script Editor";
        if (currentFile != null) {
            title += " - " + currentFile;
        }
        if (hasUnsavedChanges) {
            title += " *";
        }
        setTitle(title);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            
            new ScriptEditor().setVisible(true);
        });
    }

    // ==========================================
    // [NEW] 智慧型單元格編輯器 (攔截 VAR)
    // ==========================================
    static class SmartCellEditor extends DefaultCellEditor {

        private JTable table;
        private int typeColumnIndex;

        public SmartCellEditor(JTable table, int typeColumnIndex) {
            super(new JTextField());
            this.table = table;
            this.typeColumnIndex = typeColumnIndex;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            
            // 檢查這一行的 Type
            Object typeObj = table.getValueAt(row, typeColumnIndex);
            String type = (typeObj != null) ? typeObj.toString() : "";

            // 如果是 VAR，彈出專用視窗
            if ("VAR".equals(type)) {
                String currentVal = (value != null) ? value.toString() : "";
                // 停止當前編輯狀態，避免視窗卡住
                SwingUtilities.invokeLater(() -> {
                    String generated = VarEditorHelper.showVarDialog(table, currentVal);
                    if (generated != null) {
                        table.setValueAt(generated, row, column);
                    }
                });
                // 這裡暫時回傳空或原本的字串
                return super.getTableCellEditorComponent(table, currentVal, isSelected, row, column);
            } else if ("CHOICE".equals(type)){
                String currentVal = (value != null) ? value.toString() : "";
                SwingUtilities.invokeLater(() -> {
                    String generated = ChoiceEditorHelper.showChoiceDialog(table, currentVal);
                    if (generated != null) {
                        table.setValueAt(generated, row, column);
                    }
                });
                return super.getTableCellEditorComponent(table, currentVal, isSelected, row, column);
            }

            // 其他類型正常顯示文字框
            return super.getTableCellEditorComponent(table, value, isSelected, row, column);
        }
    }

    // ==========================================
    // [NEW] 變數編輯器 UI 輔助類別
    // ==========================================
    static class VarEditorHelper {
        public static String showVarDialog(Component parent, String currentValue) {
            JPanel panel = new JPanel(new GridLayout(4, 2, 5, 5));

            JComboBox<String> opCombo = new JComboBox<>(new String[]{"ADD", "SET", "VIEW"});
            JTextField keyField = new JTextField();
            JTextField valField = new JTextField();
            JTextField labelField = new JTextField();

            panel.add(new JLabel("操作 (Operation):"));
            panel.add(opCombo);
            panel.add(new JLabel("變數名稱 (Key):"));
            panel.add(keyField);
            panel.add(new JLabel("數值 (Value):"));
            panel.add(valField);
            panel.add(new JLabel("跳轉標籤 (View only):"));
            panel.add(labelField);

            // 嘗試還原舊值
            if (currentValue != null && !currentValue.isEmpty()) {
                String[] parts = currentValue.split(",");
                if (parts.length >= 1) opCombo.setSelectedItem(parts[0].trim());
                if (parts.length >= 2) keyField.setText(parts[1].trim());
                if (parts.length >= 3) valField.setText(parts[2].trim());
                if (parts.length >= 4) labelField.setText(parts[3].trim());
            }

            // 連動：非 VIEW 模式禁用標籤欄位
            opCombo.addActionListener(e -> {
                String selected = (String) opCombo.getSelectedItem();
                boolean isView = "VIEW".equals(selected) || "CHECK".equals(selected);
                labelField.setEnabled(isView);
                if (!isView) labelField.setText("");
            });
            // 觸發一次初始化
            if(opCombo.getItemCount() > 0) opCombo.setSelectedIndex(opCombo.getSelectedIndex());

            int result = JOptionPane.showConfirmDialog(parent, panel, "VAR 指令生成器", 
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (result == JOptionPane.OK_OPTION) {
                String op = (String) opCombo.getSelectedItem();
                String key = keyField.getText().trim();
                String val = valField.getText().trim();
                String lbl = labelField.getText().trim();

                if (key.isEmpty()) return null;

                StringBuilder sb = new StringBuilder();
                sb.append(op).append(", ").append(key).append(", ").append(val);
                
                if (labelField.isEnabled() && !lbl.isEmpty()) {
                    sb.append(", ").append(lbl);
                }
                return sb.toString();
            }
            return null;
        }
    }
    static class ChoiceEditorHelper {
        
        public static String showChoiceDialog(Component parent, String currentValue) {
            // 建立一個對話框面板
            JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
            mainPanel.setPreferredSize(new Dimension(500, 300));

            // 表格模型：三欄 (選項文字, 跳轉標籤, 顯示條件)
            String[] columnNames = {"選項文字 (Text)", "跳轉標籤 (Label)", "顯示條件 (可選)"};
            DefaultTableModel model = new DefaultTableModel(columnNames, 0);
            JTable table = new JTable(model);
            
            // 設定表格高度與字體
            table.setRowHeight(25);
            table.getTableHeader().setReorderingAllowed(false);
            
            // --- 解析舊資料並填入表格 ---
            // 格式範例: "Yes:LabelA, No:LabelB|Var>10"
            if (currentValue != null && !currentValue.trim().isEmpty()) {
                String[] options = currentValue.split(",");
                for (String opt : options) {
                    String text = "";
                    String label = "";
                    String condition = "";

                    // 先處理條件 (分隔符號 |)
                    String[] condParts = opt.split("\\|");
                    if (condParts.length > 1) {
                        condition = condParts[1].trim();
                        opt = condParts[0]; // 剩下 Text:Label
                    }
                    
                    // 再處理標籤 (分隔符號 :)
                    String[] labelParts = opt.split(":");
                    if (labelParts.length > 0) text = labelParts[0].trim();
                    if (labelParts.length > 1) label = labelParts[1].trim();

                    model.addRow(new Object[]{text, label, condition});
                }
            } else {
                // 預設加一行空的方便編輯
                model.addRow(new Object[]{"", "", ""});
            }

            // --- 按鈕區 (新增/刪除) ---
            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JButton addBtn = new JButton("＋ 新增選項");
            JButton delBtn = new JButton("－ 刪除選取");

            addBtn.addActionListener(e -> model.addRow(new Object[]{"", "", ""}));
            delBtn.addActionListener(e -> {
                int selected = table.getSelectedRow();
                if (selected != -1) {
                    model.removeRow(selected);
                }
            });

            btnPanel.add(addBtn);
            btnPanel.add(delBtn);
            btnPanel.add(new JLabel("<html><font color='gray'>提示: 條件可填如 'Girl_Love>10'，留空則無條件</font></html>"));

            mainPanel.add(new JScrollPane(table), BorderLayout.CENTER);
            mainPanel.add(btnPanel, BorderLayout.SOUTH);

            // --- 顯示視窗 ---
            int result = JOptionPane.showConfirmDialog(parent, mainPanel, "編輯選項 (Choice Editor)", 
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (result == JOptionPane.OK_OPTION) {
                // --- 組裝回字串 ---
                // 停止編輯以確保最後輸入的資料被保存
                if (table.isEditing()) table.getCellEditor().stopCellEditing();

                StringBuilder sb = new StringBuilder();
                int rows = model.getRowCount();
                boolean first = true;

                for (int i = 0; i < rows; i++) {
                    String text = (String) model.getValueAt(i, 0);
                    String label = (String) model.getValueAt(i, 1);
                    String cond = (String) model.getValueAt(i, 2);

                    if (text == null || text.trim().isEmpty()) continue; // 跳過沒文字的

                    if (!first) sb.append(", ");
                    
                    // 格式: Text:Label
                    sb.append(text.trim());
                    
                    if (label != null && !label.trim().isEmpty()) {
                        sb.append(":").append(label.trim());
                    }
                    
                    // 格式: |Condition
                    if (cond != null && !cond.trim().isEmpty()) {
                        sb.append("|").append(cond.trim());
                    }
                    
                    first = false;
                }
                return sb.toString();
            }
            return null;
        }
    }
}
