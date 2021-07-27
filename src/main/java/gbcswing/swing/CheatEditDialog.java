package gbcswing.swing;

import gbc.Cheat;
import gbcswing.config.CheatItem;
import gbcswing.language.Language;
import gbcswing.tools.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class CheatEditDialog extends JDialog implements ActionListener {

    private final JLabel enableLabel, codeLabel, descriptionLabel;
    private final JCheckBox enableCheckBox;
    private final JTextArea codeField, descriptionField;
    private final JButton okButton, cancelButton;

    public CheatEditDialog(Window owner) {
        super(owner);
        setModal(true);
        setLocation(200, 200);
        enableLabel = new JLabel(Language.ENABLE);
        enableCheckBox = new JCheckBox();
        codeLabel = new JLabel(Language.CODE);
        codeField = new JTextArea();
        descriptionLabel = new JLabel(Language.DESCRIPTION);
        descriptionField = new JTextArea();
        cancelButton = new JButton(Language.CANCEL);
        okButton = new JButton(Language.OK);
        cancelButton.addActionListener(this);
        okButton.addActionListener(this);
        setLayout();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == okButton) {
            List<String> codes = new ArrayList<String>();
            for (String code : codeField.getText().split("\n")) {
                if (code.matches("^(\\s+)?$")) {
                    continue;
                }
                code = code.trim();
                if (null == Cheat.newCheat(code)) {
                    JOptionPane.showMessageDialog(CheatEditDialog.this, "ERROR");
                    return;
                }
                codes.add(code);
            }
            if (codes.isEmpty()) {
                JOptionPane.showMessageDialog(CheatEditDialog.this, "BLANK");
                return;
            }
            CheatItem cheatItem = new CheatItem();
            cheatItem.enabled = enableCheckBox.isSelected();
            cheatItem.codes = codes;
            cheatItem.description = descriptionField.getText();
            CheatEditDialog.this.cheatItem = cheatItem;
            CheatEditDialog.this.setVisible(false);
        } else if (e.getSource() == cancelButton) {
            CheatEditDialog.this.setVisible(false);
        }
    }

    private void setLayout() {
        enableLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        codeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        descriptionLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        JScrollPane jScrollPane1 = new JScrollPane();
        JScrollPane jScrollPane2 = new JScrollPane();
        jScrollPane1.setViewportView(descriptionField);
        jScrollPane2.setViewportView(codeField);
        //
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addGap(19, 19, 19)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addGroup(layout.createSequentialGroup()
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                        .addComponent(enableLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 77, Short.MAX_VALUE)
                                                        .addComponent(codeLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(descriptionLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                                .addGap(18, 18, 18)
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addGroup(layout.createSequentialGroup()
                                                                .addComponent(enableCheckBox)
                                                                .addGap(0, 0, Short.MAX_VALUE))
                                                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 516, Short.MAX_VALUE)
                                                        .addComponent(jScrollPane2)))
                                        .addGroup(layout.createSequentialGroup()
                                                .addGap(0, 0, Short.MAX_VALUE)
                                                .addComponent(cancelButton)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(okButton)))
                                .addGap(20, 20, 20))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGap(20, 20, 20)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(enableCheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(enableLabel))
                                .addGap(15, 15, 15)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 135, Short.MAX_VALUE)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(codeLabel)
                                                .addGap(0, 0, Short.MAX_VALUE)))
                                .addGap(15, 15, 15)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 104, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(descriptionLabel))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(okButton)
                                        .addComponent(cancelButton))
                                .addGap(20, 20, 20))
        );

        pack();
    }

    private CheatItem cheatItem;

    public CheatItem edit(CheatItem cheatItem) {
        this.cheatItem = null;
        if (cheatItem == null) {
            enableCheckBox.setSelected(false);
            codeField.setText("");
            descriptionField.setText("");
        } else {
            enableCheckBox.setSelected(cheatItem.enabled);
            codeField.setText(StringUtils.mLines(cheatItem.codes));
            descriptionField.setText(cheatItem.description);
        }
        setVisible(true);
        return this.cheatItem;
    }

}
