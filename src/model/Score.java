package src.model;

public class Score {
    private int score;
    private int time ;
    private String difficulty;
    
    public Score(int score, int time,String difficulty) {
        this.score = score;
        this.time = time;
        this.difficulty=difficulty;
    }
    public int getScore() {
        return score;
    }
    public void setScore(int score) {
        this.score = score;
    }
    public int getTime() {
        return time;
    }
    public void setTime(int time) {
        this.time = time;
    }
    public String getDifficulty() {
        return difficulty;
    }
    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }
    
    
}
