package gbcswing.swing;

import gbc.Cheat;
import gbcswing.config.CheatItem;
import gbcswing.language.Language;
import gbcswing.tools.StringUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public abstract class CheatListDialog extends JDialog implements ActionListener {

    abstract void onChange(List<CheatItem> cheatCodes);

    //
    private CheatEditDialog inputDialog = new CheatEditDialog(this);
    //
    private DefaultTableModel cheatTableModel;
    private JTable cheatTable;
    private JButton addButton, removeButton, editButton, switchButton;

    private void initComponents() {
        //
        cheatTableModel = new DefaultTableModel(new Object[]{Language.ENABLE, Language.CODE, Language.DESCRIPTION}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        cheatTable = new JTable(cheatTableModel);
        cheatTable.getTableHeader().setReorderingAllowed(false);
        cheatTable.setRowHeight(30);
        cheatTable.getColumnModel().getColumn(0).setCellRenderer(cheatTable.getDefaultRenderer(Boolean.class));
        cheatTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        //
        addButton = new JButton(Language.ADD);
        removeButton = new JButton(Language.REMOVE_SELECTED);
        editButton = new JButton(Language.EDIT_SELECTED);
        switchButton = new JButton(Language.SWITCH_SELECTED);
        addButton.addActionListener(this);
        removeButton.addActionListener(this);
        editButton.addActionListener(this);
        switchButton.addActionListener(this);
    }

    private void setLayout() {
        JScrollPane jScrollPane = new JScrollPane(cheatTable);
        JPanel south = new JPanel();
        add(jScrollPane, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
        south.setLayout(new FlowLayout(FlowLayout.CENTER));
        south.add(addButton);
        south.add(removeButton);
        south.add(editButton);
        south.add(switchButton);
    }

    public CheatListDialog(Window owner) {
        super(owner);
        setModal(true);
        setTitle(Language.CHEATS);
        setLocation(200, 200);
        setSize(600, 300);
        setResizable(false);
        initComponents();
        setLayout();
    }

    public void resetModel(List<CheatItem> cheatCodes) {
        this.cheatTableModel.setRowCount(0);
        for (int i = 0; i < cheatCodes.size(); i++) {
            insertCheatToTable(cheatCodes.get(i));
        }
    }

    private void notifyChange() {
        List<CheatItem> list = new ArrayList<CheatItem>();
        for (int i = 0; i < cheatTableModel.getRowCount(); i++) {
            list.add(getCheatCodes(i));
        }
        onChange(list);
    }

    protected CheatItem getCheatCodes(int i) {
        CheatItem newCheatItem = new CheatItem();
        newCheatItem.enabled = Boolean.parseBoolean(cheatTableModel.getValueAt(i, 0).toString());
        newCheatItem.codes = StringUtils.toList(cheatTableModel.getValueAt(i, 1).toString().split("\n"));
        newCheatItem.description = cheatTableModel.getValueAt(i, 2).toString();
        return newCheatItem;
    }

    private void insertCheatToTable(CheatItem cheatItem) {
        cheatTableModel.addRow(new Object[]{cheatItem.enabled, StringUtils.mLines(cheatItem.codes), cheatItem.description});
    }

    private void updateCheatToTable(CheatItem cheatItem, int row) {
        cheatTableModel.removeRow(row);
        cheatTableModel.insertRow(row, new Object[]{cheatItem.enabled, StringUtils.mLines(cheatItem.codes), cheatItem.description});
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == addButton) {
            CheatItem cheatItem = inputDialog.edit(null);
            if (cheatItem != null) {
                List<CheatItem> vfs = new ArrayList<CheatItem>();
                vfs.add(cheatItem);
                for (int i = 0; i < cheatTableModel.getRowCount(); i++) {
                    vfs.add(getCheatCodes(i));
                }
                if (testAddressDuplicate(vfs)) {
                    JOptionPane.showMessageDialog(CheatListDialog.this, "ADDRESS DUPLICATE");
                    return;
                }
                insertCheatToTable(cheatItem);
                notifyChange();
            }
        } else if (e.getSource() == removeButton) {
            int row = cheatTable.getSelectedRow();
            if (row != -1) {
                cheatTableModel.removeRow(row);
                notifyChange();
            }
        } else if (e.getSource() == editButton) {
            int row = cheatTable.getSelectedRow();
            if (row != -1) {
                CheatItem cheatItem = inputDialog.edit(getCheatCodes(row));
                List<CheatItem> vfs = new ArrayList<CheatItem>();
                vfs.add(cheatItem);
                for (int i = 0; i < cheatTableModel.getRowCount(); i++) {
                    if (i == row) {
                        continue;
                    }
                    vfs.add(getCheatCodes(i));
                }
                if (testAddressDuplicate(vfs)) {
                    JOptionPane.showMessageDialog(CheatListDialog.this, "ADDRESS DUPLICATE");
                    return;
                }
                updateCheatToTable(cheatItem, row);
                notifyChange();
            }
        } else if (e.getSource() == switchButton) {
            int row = cheatTable.getSelectedRow();
            if (row != -1) {
                Boolean b = (Boolean) cheatTableModel.getValueAt(row, 0);
                cheatTableModel.setValueAt(!b, row, 0);
                notifyChange();
            }
        }
    }

    private boolean testAddressDuplicate(List<CheatItem> vfs) {
        Set<Integer> addresses = new TreeSet<Integer>();
        for (CheatItem vf : vfs) {
            for (String code : vf.codes) {
                Integer address = Cheat.newCheat(code).address;
                if (!addresses.add(address)) {
                    return true;
                }
            }
        }
        return false;
    }
}
