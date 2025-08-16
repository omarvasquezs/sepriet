/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package Forms;

import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.JOptionPane;

/**
 *
 * @author omarv
 */
public class frmLogin extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(frmLogin.class.getName());

    /**
     * Creates new form Login
     */
    public frmLogin() {
        initComponents();
        getContentPane().setBackground(Color.WHITE);
        // Center window on screen
        setLocationRelativeTo(null);
        // Disable maximize button
        setResizable(false);
        // Add placeholders
        addPlaceholders();
        // Mouse pointer on button hover
        btnLogin.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnConfigurar.setUI(new javax.swing.plaf.basic.BasicButtonUI()); // strips LAF effects
        btnLogin.setUI(new javax.swing.plaf.basic.BasicButtonUI()); // strips LAF effects
        btnConfigurar.setBackground(Color.WHITE);
        btnConfigurar.setOpaque(true);
        btnConfigurar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        // Tooltip
        btnConfigurar.setToolTipText("Configurar conexión");
        // Set window icon
        setIconImage(new javax.swing.ImageIcon(getClass().getResource("/Forms/icon.png")).getImage());
    }

    private void addPlaceholders() {
        // Placeholder for txtUsuario
        txtUsuario.setText("INGRESE USUARIO");
        txtUsuario.setForeground(Color.GRAY);
        txtUsuario.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (txtUsuario.getText().equals("INGRESE USUARIO")) {
                    txtUsuario.setText("");
                    txtUsuario.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (txtUsuario.getText().isEmpty()) {
                    txtUsuario.setText("INGRESE USUARIO");
                    txtUsuario.setForeground(Color.GRAY);
                }
            }
        });

        // Placeholder for txtPassword
        txtPassword.setText("********");
        txtPassword.setForeground(Color.GRAY);
        txtPassword.setEchoChar((char) 0); // Disable echo char for placeholder
        txtPassword.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (txtPassword.getText().equals("********")) {
                    txtPassword.setText("");
                    txtPassword.setForeground(Color.BLACK);
                    txtPassword.setEchoChar('*'); // Enable echo char for password
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (txtPassword.getText().isEmpty()) {
                    txtPassword.setText("********");
                    txtPassword.setForeground(Color.GRAY);
                    txtPassword.setEchoChar((char) 0); // Disable echo char for placeholder
                }
            }
        });
        txtUsuario.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                txtUsuario.setText(txtUsuario.getText().toUpperCase());
            }
        });
        txtPassword.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    btnLogin.doClick(); // Simulate a button click
                }
            }
        });
    }

    private boolean verifyPassword(String plainPassword,
            String encryptedPassword) {
        try {
            // Step 1: Hash the password using SHA-1
            java.security.MessageDigest sha1 = java.security.MessageDigest.getInstance("SHA-1");
            byte[] sha1Hash = sha1.digest(plainPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sha1Hex = new StringBuilder();
            for (byte b : sha1Hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    sha1Hex.append('0');
                }
                sha1Hex.append(hex);
            }

            // Step 2: Hash the SHA-1 result using MD5
            java.security.MessageDigest md5 = java.security.MessageDigest.getInstance("MD5");
            byte[] md5Hash = md5.digest(sha1Hex.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder md5Hex = new StringBuilder();
            for (byte b : md5Hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    md5Hex.append('0');
                }
                md5Hex.append(hex);
            }

            // Compare the final MD5 hash with the encrypted password from the database
            return md5Hex.toString().equals(encryptedPassword);
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new RuntimeException("Error verifying password", ex);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        txtUsuario = new javax.swing.JTextField();
        txtPassword = new javax.swing.JPasswordField();
        btnLogin = new javax.swing.JButton();
        lblTitulo1 = new javax.swing.JLabel();
        btnConfigurar = new javax.swing.JButton();
        lblTitulo3 = new javax.swing.JLabel();
        lblTitulo2 = new javax.swing.JLabel();
        imgLogin = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("SISTEMA DE LAVANDERIA 1.0");
        setBackground(new java.awt.Color(255, 255, 255));

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));

        txtUsuario.setFont(new java.awt.Font("Segoe UI", 0, 16)); // NOI18N

        txtPassword.setFont(new java.awt.Font("Segoe UI", 0, 16)); // NOI18N

        btnLogin.setBackground(new java.awt.Color(0, 123, 255));
        btnLogin.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        btnLogin.setForeground(new java.awt.Color(255, 255, 255));
        btnLogin.setText("INGRESAR");
        btnLogin.setOpaque(true);
        btnLogin.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLoginActionPerformed(evt);
            }
        });

        lblTitulo1.setFont(new java.awt.Font("Segoe UI", 1, 28)); // NOI18N
        lblTitulo1.setText("Lavandería Seprie't");

        btnConfigurar.setForeground(new java.awt.Color(255, 255, 255));
        btnConfigurar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Forms/icon_gear.png"))); // NOI18N
        btnConfigurar.setToolTipText("");
        btnConfigurar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnConfigurarActionPerformed(evt);
            }
        });

        lblTitulo3.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lblTitulo3.setText("CONTRASEÑA:");

        lblTitulo2.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lblTitulo2.setText("USUARIO:");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnConfigurar))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(15, 15, 15)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                            .addComponent(txtPassword, javax.swing.GroupLayout.PREFERRED_SIZE, 266, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnLogin, javax.swing.GroupLayout.PREFERRED_SIZE, 266, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblTitulo1)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(lblTitulo3)
                                .addComponent(txtUsuario, javax.swing.GroupLayout.PREFERRED_SIZE, 266, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(lblTitulo2)))
                        .addGap(0, 8, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addComponent(lblTitulo1, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(28, 28, 28)
                .addComponent(lblTitulo2, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtUsuario, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblTitulo3, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtPassword, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(btnLogin, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnConfigurar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        imgLogin.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Forms/logo_yurivanny.jpg"))); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(imgLogin, javax.swing.GroupLayout.PREFERRED_SIZE, 335, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(imgLogin, javax.swing.GroupLayout.PREFERRED_SIZE, 347, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnConfigurarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnConfigurarActionPerformed
        
        this.setVisible(false); // Hide frmLogin
        frmDB dbForm = new frmDB(); // Create an instance of frmDB
        dbForm.setVisible(true); // Show frmDB
    }//GEN-LAST:event_btnConfigurarActionPerformed

    private void btnLoginActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLoginActionPerformed
        
        String username = txtUsuario.getText();
        String password = new String(txtPassword.getPassword());

        // Obtain a live DB connection using DatabaseConfig
        try (Connection conn = DatabaseConfig.getConnection()) {
            String query = "SELECT password FROM users WHERE username = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, username);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String encrypted = rs.getString("password");
                        if (verifyPassword(password, encrypted)) {
                            this.setVisible(false);
                            new frmMain().setVisible(true);
                        } else {
                            JOptionPane.showMessageDialog(this,
                                    "Usuario o Clave inválidos!",
                                    "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } else {
                        JOptionPane.showMessageDialog(this,
                                "Usuario o Clave inválidos!",
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        } catch (IllegalStateException ex) {
            // Missing or bad DB settings
            JOptionPane.showMessageDialog(this,
                    ex.getMessage(),
                    "Error de configuración", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Database Error: " + ex.getMessage(),
                    "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnLoginActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Windows".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new frmLogin().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnConfigurar;
    private javax.swing.JButton btnLogin;
    private javax.swing.JLabel imgLogin;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JLabel lblTitulo1;
    private javax.swing.JLabel lblTitulo2;
    private javax.swing.JLabel lblTitulo3;
    private javax.swing.JPasswordField txtPassword;
    private javax.swing.JTextField txtUsuario;
    // End of variables declaration//GEN-END:variables
}
