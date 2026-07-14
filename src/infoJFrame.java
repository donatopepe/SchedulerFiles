/*
 * Copyright (C) 2020 Donato
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

public class infoJFrame extends javax.swing.JFrame {

    public infoJFrame() {
        initComponents();
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setLocationByPlatform(true);
        setName("infoFrame");
        setResizable(false);

        jTextArea1.setEditable(false);
        jTextArea1.setColumns(20);
        jTextArea1.setLineWrap(true);
        jTextArea1.setRows(5);
        jTextArea1.setText("\t           Files Scheduler\n\n"
                + "This software allows copying or moving from a source directory\n"
                + "to a destination directory.\n\n"
                + "You can choose to respect the folder hierarchy of the source\n"
                + "directory or create a hierarchy based on year, month and file\n"
                + "extension.\n\n"
                + "Empty folders are not created.\n\n"
                + "Drag & drop folders onto the text fields or use the Browse\n"
                + "buttons to select them.\n\n"
                + "Copyright (C) 2020 Donato Pepe\n"
                + "License: GNU GPL v3 or later");
        jScrollPane1.setViewportView(jTextArea1);

        getContentPane().add(jScrollPane1, java.awt.BorderLayout.CENTER);

        pack();
    }

    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea jTextArea1;
}
