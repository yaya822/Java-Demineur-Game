package database;

import model.Score;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ScoreDAO {
    private static Connection conn=Connexion.getConn();

    // methode pour enregistrer le score apres la victoire 
    public void saveScore(Score score){

        String sql ="INSERT INTO scores(score, time,difficulty) VALUES( ?, ?,?)";

        try (
            PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {

            pstmt.setInt(1, score.getScore());
            pstmt.setInt(2, score.getTime());
            pstmt.setString(3, score.getDifficulty());
            pstmt.executeUpdate();
            System.out.println("Score saved !!!!");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // methode qui retourne les 10 premier score 
    public List<Score> getTop10Scores(String difficulty ) {

        List<Score> scores = new ArrayList<>();

        String sql ="SELECT * FROM Scores"+"where difficulty=?"+ "ORDER BY score DESC"+"LIMIT 10";

        try (
            PreparedStatement stmt =conn.prepareStatement(sql);
        ) {
            stmt.setString(1, difficulty);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {

                scores.add(
                    new Score(
                        rs.getInt("score"),
                        rs.getInt("time"),
                        rs.getString("difficulty")
                    )
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return scores;
    }
}