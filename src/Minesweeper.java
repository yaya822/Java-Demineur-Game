 import java.awt.*;
import javax.swing.*;
import javax.swing.border.Border;

public class Minesweeper {
    private Game game=new Game();
    private Boolean gameEnded=false;
    private int seconds=0;
    private Boolean timerStarted=false;
    private int minesLeft=10;

    private class Cell extends JButton {
        final int r , c;
        public Cell(int r, int c) {
            this.r = r;
            this.c = c;
            addActionListener(e->{
                if(gameEnded) return;
                if(!timerStarted){
                    gameTimer.start();
                    timerStarted=true;

                }
                game.revealCell(r, c);
                refreshBoard();
            });
        }
    }
         public void refreshBoard(){
            for(int i=0;i<8;i++){
                for(int j=0;j<8;j++){

                    Cellule c= game.getCellule(i, j);
                    Cell  btn = cells[i][j];

                    if(c.isReveald()){
                        btn.setEnabled(false);
                        if(c.isMine()){
                            btn.setText("💣");
                            btn.setOpaque(true);
                        }
                        else{
                            int adj=game.getAdjCells(i, j);
                            if(adj!=0){
                                 btn.setText(String.valueOf(adj));

                                    switch(adj) {
                                        case 1 -> btn.setForeground(Color.BLUE);
                                        case 2 -> btn.setForeground(new Color(0,128,0));
                                        case 3 -> btn.setForeground(Color.RED);
                                        case 4 -> btn.setForeground(new Color(0,0,128));
                                        default -> btn.setForeground(Color.BLACK);
                                    }
                            }
                        }
                    }
                }
            }
            if(game.isGameover() && !gameEnded){
                gameEnded=true;
                gameTimer.stop();
                revealAllMines();
                JOptionPane.showMessageDialog(frame, "Game Over!");
            }
           
        }

        private   void revealAllMines(){
            for(int i=0;i<8;i++){
                for(int j=0;j<8;j++){
                    if(game.getCellule(i, j).isMine()){
                        cells[i][j].setText("💣");
                        cells[i][j].setEnabled(false);
                    }
                }
            }
        }

         private void resetGame() {
        int mines = switch (difficulty.getSelectedIndex()) {
            case 1 -> 20;   // Medium
            case 2 -> 35;   // Hard
            default -> 10;  // Easy
        };
        game= new Game();
        game.getBoard().generateMins(mines);       
        gameEnded    = false;
        timerStarted = false;
        seconds      = 0;
        minesLeft    = mines;
        timerLabel.setText("00:00");
        mineCountLabel.setText("💣 " + minesLeft);
        gameTimer.stop();

        // Réinitialise les boutons
        Color classicGray = new Color(192, 192, 192);
        Border raised = BorderFactory.createBevelBorder(
            javax.swing.border.BevelBorder.RAISED);
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                cells[r][c].setText("");
                cells[r][c].setEnabled(true);
                cells[r][c].setBackground(classicGray);
                cells[r][c].setForeground(Color.BLACK);
                cells[r][c].setBorder(raised);
            }
        }
    }

    int tileSize    = 70;
    int rowNum      = 8;
    int colNum      = rowNum;
    int boardWidth  = tileSize * colNum;  
    int boardHeight = tileSize * rowNum;  

    JFrame  frame          = new JFrame("Minesweeper");
    JPanel  boardPanel     = new JPanel();
    JPanel  titlePanel     = new JPanel();
    Cell[][]cells          = new Cell[rowNum][colNum];

    
    JComboBox<String> difficulty =
        new JComboBox<>(new String[]{"Easy", "Medium", "Hard"});

    JLabel timerLabel     = new JLabel("00:00", SwingConstants.CENTER);
    JLabel mineCountLabel = new JLabel("", SwingConstants.CENTER);

    // Timer state
    javax.swing.Timer gameTimer;

    public Minesweeper() {
        game.start(8,8,10);
        Color  classicGray   = new Color(192, 192, 192);
        Font   retroFont     = new Font("Courier New", Font.BOLD, 18);
        Border raisedBorder  = BorderFactory.createBevelBorder(
                                   javax.swing.border.BevelBorder.RAISED);
        Border loweredBorder = BorderFactory.createBevelBorder(
                                   javax.swing.border.BevelBorder.LOWERED);
            
       // Frames and panels
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(boardWidth, boardHeight + 50);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.getContentPane().setBackground(classicGray);
        frame.setLayout(new BorderLayout());
    
        titlePanel.setLayout(new GridLayout(1, 3));
        titlePanel.setPreferredSize(new Dimension(boardWidth, 50));
        titlePanel.setBackground(classicGray);
        titlePanel.setBorder(raisedBorder);

        // Difficulty
        difficulty.setFont(retroFont);
        difficulty.setBackground(classicGray);
        difficulty.setBorder(raisedBorder);
        JPanel difficultyPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 8));
        difficultyPanel.setBackground(classicGray);
        difficultyPanel.add(difficulty);

        // Mines left
        mineCountLabel.setFont(new Font("Monospaced", Font.BOLD, 22));
        mineCountLabel.setOpaque(true);
        mineCountLabel.setBackground(Color.BLACK);
        mineCountLabel.setForeground(Color.RED);
        mineCountLabel.setPreferredSize(new Dimension(150, 30));
        mineCountLabel.setBorder(loweredBorder);
        mineCountLabel.setText("\uD83D\uDCA3 " + minesLeft);
        JPanel mineCountPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 8));
        mineCountPanel.setBackground(classicGray);
        mineCountPanel.add(mineCountLabel);

        //Timer
        timerLabel.setFont(new Font("Monospaced", Font.BOLD, 25));
        timerLabel.setOpaque(true);
        timerLabel.setBackground(Color.BLACK);
        timerLabel.setForeground(Color.RED);
        timerLabel.setPreferredSize(new Dimension(90, 30));
        timerLabel.setBorder(loweredBorder);
        JPanel timerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 8));
        timerPanel.setBackground(classicGray);
        timerPanel.add(timerLabel);

    
        titlePanel.add(difficultyPanel);
        titlePanel.add(mineCountPanel);
        titlePanel.add(timerPanel);

        // Board 
        boardPanel.setLayout(new GridLayout(rowNum, colNum));
        for (int r = 0; r < rowNum; r++) {
            for (int c = 0; c < colNum; c++) {
                Cell cell = new Cell(r, c);
                cell.setBorder(raisedBorder);
                cells[r][c] = cell;
                boardPanel.add(cell);
            }
        }

        frame.add(titlePanel, BorderLayout.NORTH);
        frame.add(boardPanel, BorderLayout.CENTER);
        frame.setVisible(true);

        //Count-up timer (MM:SS) 
        gameTimer = new javax.swing.Timer(1000, e -> {
            seconds++;
            if (seconds > 5999) seconds = 5999;   // cap at 99:59
            int mm = seconds / 60;
            int ss = seconds % 60;
            timerLabel.setText(String.format("%02d:%02d", mm, ss));
        });
        
    }

}