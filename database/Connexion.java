package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.swing.JOptionPane;

public class Connexion {
    private static Connection conn=null;
    static{
        try {
            Class.forName("com.mysql.cj.jdbc.Driver"); 
            String url = "jdbc:mysql://localhost:3306/Minesweeper?useSSL=false&serverTimezone=UTC";
            conn = DriverManager.getConnection(url, "root", "1234");
            }
            catch (ClassNotFoundException ex) { // si le driver n’est pas charger par forName
            JOptionPane.showMessageDialog(null, "Classe introuvable" + ex.getMessage());}
            catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "Connexion Impossible" + ex.getMessage());

            }
    }
    public static Connection getConn() {
        return conn;
    }
}
