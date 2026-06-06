package ui;
import database.ScoreDAO;
import game.Game;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.Border;
import model.*;

public class Minesweeper {

// c'est pour une particule visuelle utilisée dans l’animation d’explosion
    private static class Particle {
        float x, y, vx, vy, life, maxLife, size;
        Color color;
        boolean isSpark;

        Particle(float cx, float cy) {
            Random rng = new Random();
            double angle = rng.nextDouble() * Math.PI * 2;
            isSpark = rng.nextBoolean();
            float speed = isSpark ? 4f + rng.nextFloat() * 7f: 1f + rng.nextFloat() * 3f;
            vx = (float)(Math.cos(angle) * speed);
            vy = (float)(Math.sin(angle) * speed);
            x = cx; y = cy;

            if (isSpark) {
                life = maxLife = 8 + rng.nextInt(10);
                size = 1.5f + rng.nextFloat() * 2f;
                color = rng.nextBoolean() ? Color.WHITE : new Color(255, 230, 80);
            } 
            else {
                life = maxLife = 15 + rng.nextInt(15);
                size = 3f + rng.nextFloat() * 5f;
                int pick = rng.nextInt(3);
                color = pick == 0 ? new Color(255, 90, 0): pick == 1 ? new Color(255, 160, 20): new Color(200, 30, 0);
            }
        }
 // Met à jour la position de la particule et diminue sa durée de vie
        void update() { x += vx; y += vy; life--; }

        // Vérifie si la particule a terminé son animation
        boolean isDead() { return life <= 0; }

// Dessine la particule à l’écran avec une transparence selon sa durée de vie
        void draw(Graphics2D g2) {
            float alpha = life / maxLife;
            Color c = new Color(color.getRed(), color.getGreen(),color.getBlue(), (int)(alpha * 255));
            g2.setColor(c);
            if (isSpark) {
                g2.setStroke(new BasicStroke(Math.max(0.8f, size * alpha)));
                g2.drawLine((int)(x - vx * 2), (int)(y - vy * 2), (int)x, (int)y);
            }
             else {
                int s = Math.max(1, (int)(size * alpha));
                g2.fillOval((int)x, (int)y, s, s);
            }
        }
    }

    //Représente l’onde circulaire qui apparaît autour d’une explosion
    private static class Ring {
        float x, y, radius, life, maxLife;

        Ring(float cx, float cy) {
            x = cx; y = cy;
            radius = 5f;
            maxLife = life = 14;
        }

        void update()     { radius += 5f; life--; }
        boolean isDead()  { return life <= 0; }

        void draw(Graphics2D g2) {
            float alpha = life / maxLife;
            g2.setColor(new Color(255, 200, 60, (int)(alpha * 255)));
            g2.setStroke(new BasicStroke(3.5f * alpha));
            g2.drawOval((int)(x - radius), (int)(y - radius),
                        (int)(radius * 2), (int)(radius * 2));
        }
    }

    // Représente une couche graphique transparente utilisée pour afficher les animations d explosion au-dessus du plateau
    private class ExplosionLayer extends JComponent {
        private final List<Particle> particles = new ArrayList<>();
        private final List<Ring> rings  = new ArrayList<>();
        private  javax.swing.Timer timer;

        ExplosionLayer() {
            setOpaque(false);
            timer = new javax.swing.Timer(16, e -> {

                for (int i = particles.size() - 1; i >= 0; i--) {
                    particles.get(i).update();
                    if (particles.get(i).isDead()) particles.remove(i);
                }
                for (int i = rings.size() - 1; i >= 0; i--) {
                    rings.get(i).update();
                    if (rings.get(i).isDead()) rings.remove(i);
                }
                repaint();
                if (particles.isEmpty() && rings.isEmpty()) timer.stop();
            });
        }
// Lance l’animation d’explosion à la position de la case sélectionnée
        void explodeAt(Component cell) {
            Point p = SwingUtilities.convertPoint(cell.getParent(), cell.getX(), cell.getY(), this);
            float cx = p.x + cell.getWidth()  / 2f;
            float cy = p.y + cell.getHeight() / 2f;
            rings.add(new Ring(cx, cy));
            for (int i = 0; i < 65; i++) particles.add(new Particle(cx, cy));
            if (!timer.isRunning()) timer.start();
        }
        void clear() {
            particles.clear();
            rings.clear();
            timer.stop();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (particles.isEmpty() && rings.isEmpty()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            rings.forEach(r -> r.draw(g2));
            particles.forEach(p -> p.draw(g2));
            g2.dispose();
        }
    }
// Représente une cellule du plateau et gère son interaction avec le joueur
    private class Cell extends JButton {
        final int r, c;
        boolean revealed = false;

        Cell(int r, int c) {
            this.r = r; this.c = c;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setOpaque(false);
            setBorderPainted(false);
            setMargin(new Insets(0, 0, 0, 0));
            setHorizontalAlignment(SwingConstants.CENTER);
            setVerticalAlignment(SwingConstants.CENTER);

     // Gère le clic gauche sur une case et empêche l’action si la partie est terminée ou si la case est déjà révélée
            addActionListener(e -> {
                if (gameEnded) return;
                if (revealed) return;
                if (!timerStarted) {
                    gameTimer.start();
                    timerStarted = true;
                }

                game.revealCell(r, c);
                score+=10;
                refreshBoard();
            });

            // clique droit → flag / unflag
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON3) {
                        if (gameEnded || revealed) return;
                        game.toggleFlage(r, c);
                        refreshBoard();
                    }
                }
            });
        }

        @Override
        // Redessine graphiquement la case selon son état : cachée, marquée par un drapeau ou révélée
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            int w = getWidth(), h = getHeight();

            Cellule cell = game.getCellule(r, c);

            if (!revealed) {
                if (cell.isFlagged()) {
                    // Flagged tile: raised grey + flag emoji
                    g2.setColor(new Color(192, 192, 192));
                    g2.fillRect(0, 0, w, h);
                    g2.setColor(Color.WHITE);
                    g2.fillRect(0, 0, w, 3);
                    g2.fillRect(0, 0, 3, h);
                    g2.setColor(new Color(100, 100, 100));
                    g2.fillRect(0, h - 3, w, 3);
                    g2.fillRect(w - 3, 0, 3, h);
                    g2.setColor(new Color(135, 135, 135));
                    g2.fillRect(3, 3, w - 6, h - 6);
                    g2.dispose();
                    super.paintComponent(g);
                } 
                else {
                    g2.setColor(new Color(192, 192, 192));
                    g2.fillRect(0, 0, w, h);
                    g2.setColor(Color.WHITE);
                    g2.fillRect(0, 0, w, 3);
                    g2.fillRect(0, 0, 3, h);
                    g2.setColor(new Color(100, 100, 100));
                    g2.fillRect(0, h - 3, w, 3);
                    g2.fillRect(w - 3, 0, 3, h);
                    g2.setColor(new Color(135, 135, 135));
                    g2.fillRect(3, 3, w - 6, h - 6);
                    g2.dispose();
                    super.paintComponent(g);
                }
            }
             else {
    
                g2.setColor(new Color(30, 15, 5));
                g2.fillRect(0, 0, w, h);
                float cx = w / 2f, cy = h / 2f;
                RadialGradientPaint scorch = new RadialGradientPaint(
                    cx, cy, Math.min(w, h) / 2f,
                    new float[]{0f, 0.5f, 1f},
                    new Color[]{new Color(80, 40, 10), new Color(35, 18,  5),new Color(12,  6,  2)});
                g2.setPaint(scorch);
                g2.fillRect(0, 0, w, h);
                g2.dispose();
                super.paintComponent(g);
            }
        }
    }

   //les variables principales;
    private Game    game  = new Game();
    private boolean gameEnded = false;
    private boolean timerStarted = false;
    private int seconds = 0;
    private int minesLeft  = 6;
    private int score = 0;

    // Board dimensions 
    int tileSize = 70;
    int rowNum = 8;
    int colNum  = 8;
    int boardWidth  = tileSize * colNum;
    int boardHeight = tileSize * rowNum;

    // Swing components
    JFrame frame = new JFrame("Minesweeper");
    JPanel boardPanel = new JPanel();
    JPanel titlePanel = new JPanel();
    JPanel  content;
    Cell[][] cells = new Cell[rowNum][colNum];

    ExplosionLayer explosionLayer = new ExplosionLayer();

    JComboBox<String> difficulty =new JComboBox<>(new String[]{"Easy", "Medium", "Hard"});
    JLabel mineCountLabel = new JLabel("", SwingConstants.CENTER);
    JLabel timerLabel     = new JLabel("00:00", SwingConstants.CENTER);

    javax.swing.Timer gameTimer;

    // Charge une police personnalisée pour les titres et le timer, avec une police de secours en cas d’erreur de chargement
private Font loadDigitalFont(float size) {
    try (java.io.InputStream fontStream = Minesweeper.class.getResourceAsStream("/fonts/digital-dream/DIGITALDREAM.ttf")) {
        if (fontStream == null) {
            throw new java.io.FileNotFoundException("fonts/digital-dream/DIGITALDREAM.ttf");
        }
        Font font = Font.createFont(Font.TRUETYPE_FONT, fontStream);
        GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
        return font.deriveFont(Font.PLAIN, size);

    } catch (Exception e) {
        e.printStackTrace();
        return new Font("Monospaced", Font.BOLD, Math.round(size));
    }
}
// Crée le tableau des meilleurs scores
    private JScrollPane createRankingTable(String level) {
    String[] cols = {"#","Score", "Time"};
    ScoreDAO scoreDao = new ScoreDAO();
    List<Score> scores = scoreDao.getTop10Scores(level);
    Object[][] rows = new Object[scores.size()][4];
    for (int i = 0; i < scores.size(); i++) {
        rows[i][0] = i + 1;
        rows[i][1] = scores.get(i).getScore();
        rows[i][2] = scores.get(i).getTime();
    }
    JTable table = new JTable(rows, cols);
    table.setEnabled(false);
    return new JScrollPane(table);
}
//afiiche le ranking
   private void showRankingDialog() {
    JDialog dialog = new JDialog();
    dialog.setTitle("Top 10 Rankings");
    dialog.setModal(true);
    dialog.setSize(500, 450);
    dialog.setLocationRelativeTo(null);  // centre l'écran

    JTabbedPane tabs = new JTabbedPane();
    tabs.addTab("Easy",   createRankingTable("easy"));
    tabs.addTab("Medium", createRankingTable("medium"));
    tabs.addTab("Hard",   createRankingTable("hard"));

    dialog.add(tabs);
    dialog.setVisible(true);
}
   // Initialise la fenêtre principale, démarre la partie et prépare tous les composants graphiques du jeu
    public Minesweeper() {
        game.start(rowNum, colNum, minesLeft);
        Color  gray         = new Color(192, 192, 192);
        Font   retro        = new Font("Courier New", Font.BOLD, 18);
        Border raisedBorder = BorderFactory.createBevelBorder( javax.swing.border.BevelBorder.RAISED);
        Border loweredBorder = BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED);
        
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        
        frame.setLayout(new BorderLayout());
        //titre panel
        titlePanel.setLayout(new GridLayout(1, 3));
        titlePanel.setPreferredSize(new Dimension(boardWidth, 70));
        titlePanel.setBackground(gray);
        titlePanel.setBorder(raisedBorder);

        // Bouton pour afficher le classement des meilleurs scores
        JButton rankingBtn = new JButton(" Top 10");
        rankingBtn.setText("RANKING");
        rankingBtn.setFont(new Font("Courier New", Font.BOLD, 13));
        rankingBtn.setForeground(Color.BLACK);
        rankingBtn.setBackground(new Color(230, 230, 230));
        rankingBtn.setFocusPainted(false);
        rankingBtn.setBorder(raisedBorder);
        rankingBtn.setPreferredSize(new Dimension(95, 24));
        rankingBtn.addActionListener(e->showRankingDialog());
       // Menu déroulant pour sélectionner la difficulté du jeu
        difficulty.setFont(retro);
        difficulty.setBackground(gray);
        difficulty.setBorder(raisedBorder);
        JPanel dp = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 8));
        dp.setBackground(gray);
        dp.add(difficulty);
// Label qui affiche le nombre de mines restantes à trouver, avec un style rétro et une bordure en relief
        mineCountLabel.setFont(new Font("Monospaced", Font.BOLD, 22));
        mineCountLabel.setOpaque(true);
        mineCountLabel.setBackground(Color.BLACK);
        mineCountLabel.setForeground(Color.RED);
        mineCountLabel.setPreferredSize(new Dimension(150, 30));
        mineCountLabel.setBorder(loweredBorder);
        mineCountLabel.setText("\uD83D\uDCA3 " + minesLeft);
      JPanel mp = new JPanel();
mp.setLayout(new BoxLayout(mp, BoxLayout.Y_AXIS));
mp.setBackground(gray);

JPanel rankingPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 2));
rankingPanel.setBackground(gray);
rankingPanel.add(rankingBtn);

JPanel minesPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
minesPanel.setBackground(gray);
minesPanel.add(mineCountLabel);

mp.add(rankingPanel);
mp.add(minesPanel);
// Label qui affiche le temps écoulé depuis le début de la partie, avec une police numérique et une bordure en relief
       timerLabel.setFont(loadDigitalFont(28f));
        timerLabel.setOpaque(true);
        timerLabel.setBackground(Color.BLACK);
        timerLabel.setForeground(Color.RED);
      timerLabel.setPreferredSize(new Dimension(115, 35));
        timerLabel.setBorder(loweredBorder);
        JPanel tp = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 8));
        tp.setBackground(gray);
        tp.add(timerLabel);

        titlePanel.add(dp);
        titlePanel.add(mp);
        titlePanel.add(tp);

        // Détecte le changement de difficulté choisi par le joueur et réinitialise la partie avec ce niveau
        difficulty.addActionListener(e ->applyDifficulty((String) difficulty.getSelectedItem()));

        // Construit le plateau
        buildBoard();

        // Organise les composants graphiques dans la fenêtre principale en utilisant un JLayeredPane pour superposer la couche d’explosion au-dessus du plateau de jeu
        content = new JPanel(new BorderLayout());
        content.add(titlePanel, BorderLayout.NORTH);
        content.add(boardPanel, BorderLayout.CENTER);

        JLayeredPane lp = frame.getLayeredPane();
        content.setBounds(0, 0, boardWidth, boardHeight + 50);
        lp.add(content, JLayeredPane.DEFAULT_LAYER);

        explosionLayer.setBounds(0, 0, boardWidth, boardHeight + 50);
        lp.add(explosionLayer, JLayeredPane.DRAG_LAYER);

        frame.setVisible(true);

        // Ajuste la taille de la fenêtre pour s’adapter au plateau de jeu et à la barre de titre, en tenant compte des bordures et de la barre de titre du système d’exploitation
        Insets ins = frame.getInsets();
        frame.setSize(boardWidth  + ins.left + ins.right,boardHeight + 50 + ins.top + ins.bottom);
        content.setBounds(0, 0, boardWidth, boardHeight + 50);
        explosionLayer.setBounds(0, 0, boardWidth, boardHeight + 50);
        frame.setLocationRelativeTo(null);

        // Initialise le timer de jeu qui met à jour le label du temps écoulé chaque seconde et s’arrête lorsque le temps atteint 99:59
        gameTimer = new javax.swing.Timer(1000, e -> {
            if (++seconds > 5999) seconds = 5999;
            timerLabel.setText(String.format("%02d:%02d",
                seconds / 60, seconds % 60));
        });
    }

   // Construit le plateau de jeu en créant une grille de boutons personnalisés (Cell) qui représentent les cases du jeu, et les ajoute au panneau du plateau
    private void buildBoard() {
        boardPanel.removeAll();
        boardPanel.setLayout(new GridLayout(rowNum, colNum));
        cells = new Cell[rowNum][colNum];
        for (int r = 0; r < rowNum; r++)
            for (int c = 0; c < colNum; c++) {
                cells[r][c] = new Cell(r, c);
                boardPanel.add(cells[r][c]);
            }
        boardPanel.revalidate();
        boardPanel.repaint();
    }
// Met à jour le label du nombre de mines restantes en comptant les cases marquées d’un drapeau et en soustrayant ce nombre du total de mines à trouver
    private void updateMinesLeftLabel() {
    int flaggedCells = 0;

    for (int i = 0; i < rowNum; i++) {
        for (int j = 0; j < colNum; j++) {
            Cellule c = game.getCellule(i, j);
            if (c.isFlagged() && !c.isReveald()) {
                flaggedCells++;
            }
        }
    }
    if (minesLeft > flaggedCells)
        mineCountLabel.setText("\uD83D\uDCA3 " + (minesLeft - flaggedCells));
    else
        mineCountLabel.setText("\uD83D\uDCA3 " + 0);
}

// Applique les paramètres de la difficulté sélectionnée par le joueur, réinitialise la partie et ajuste la taille de la fenêtre en conséquence
    private void applyDifficulty(String diff) {
        switch (diff) {

     case "Easy" -> { rowNum = 8;  colNum = 8;  tileSize = 70; minesLeft = 6; }
     case "Medium" -> { rowNum = 12; colNum = 12; tileSize = 46; minesLeft = 16; }
     default   -> { rowNum = 14; colNum = 14; tileSize = 40; minesLeft = 25; }
        }
        boardWidth = tileSize * colNum;
        boardHeight = tileSize * rowNum;

        // Redémarre le jeu avec les nouveaux paramètres
        game = new Game();
        game.start(rowNum, colNum, minesLeft);
        gameEnded = false;
        timerStarted = false;
        seconds = 0;
        gameTimer.stop();

        // Met à jour les labels et la taille de la fenêtre
        updateMinesLeftLabel();
        timerLabel.setText("00:00");
        titlePanel.setPreferredSize(new Dimension(boardWidth, 70));

        // Reconstruit le plateau avec la nouvelle configuration de taille et de nombre de mines
        buildBoard();

        // Ajuste la taille de la fenêtre pour s’adapter au nouveau plateau et à la barre de titre, en tenant compte des bordures et de la barre de titre du système d’exploitation
        Insets ins = frame.getInsets();
        frame.setSize(boardWidth  + ins.left + ins.right,boardHeight + 50 + ins.top + ins.bottom);
        content.setBounds(0, 0, boardWidth, boardHeight + 50);
        explosionLayer.setBounds(0, 0, boardWidth, boardHeight + 50);
        explosionLayer.clear();
        frame.setLocationRelativeTo(null);
    }
// Affiche une boîte de dialogue personnalisée lorsque le joueur perd la partie en cliquant sur une mine, avec un titre animé et un bouton pour recommencer
private void showGameOverDialog() {
    JDialog dialog = new JDialog(frame, "Game Over", true);

    dialog.setSize(420, 240);
    dialog.setLayout(new BorderLayout());
    dialog.getContentPane().setBackground(Color.BLACK);

    JLabel title = new JLabel("", SwingConstants.CENTER);
title.setFont(loadDigitalFont(42f));
    title.setForeground(Color.RED);

    JLabel msg = new JLabel("YOU STEPPED ON A MINE!", SwingConstants.CENTER);
   msg.setFont(new Font("Arial", Font.PLAIN, 18));
    msg.setForeground(Color.WHITE);

    JButton restart = new JButton("PLAY AGAIN");
  restart.setFont(new Font("Arial", Font.BOLD, 18));
    restart.setBorderPainted(false);
      restart.setFocusPainted(false);
    restart.addActionListener(e -> {
        dialog.dispose();
        applyDifficulty((String)difficulty.getSelectedItem());
    });

    JPanel center = new JPanel(new GridLayout(2, 1));
    center.setBackground(Color.BLACK);
    center.add(title);
    center.add(msg);

    JPanel south = new JPanel();
    south.setBackground(Color.BLACK);
    south.add(restart);

    dialog.add(center, BorderLayout.CENTER);
    dialog.add(south, BorderLayout.SOUTH);

    String text = "GAME OVER";
    final int[] index = {0};

    javax.swing.Timer textTimer = new javax.swing.Timer(120, e -> {
        title.setText(text.substring(0, index[0] + 1));
        index[0]++;

        if (index[0] >= text.length()) {
            ((javax.swing.Timer)e.getSource()).stop();
        }
    });

    dialog.setLocationRelativeTo(frame);
    textTimer.start();
    dialog.setVisible(true);
}
// Met à jour l’affichage du plateau de jeu en fonction de l’état actuel de chaque cellule, et vérifie si le joueur a gagné ou perdu après chaque action
    private void refreshBoard() {
          updateMinesLeftLabel();
        boolean wonNow = false;
        for (int i = 0; i < rowNum; i++) {
            for (int j = 0; j < colNum; j++) {
                Cellule c = game.getCellule(i, j);
                Cell btn = cells[i][j];

                if (c.isFlagged() && !c.isReveald()) {
    btn.setText("\uD83D\uDEA9"); // 🚩
   btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, Math.min(24, tileSize - 14)));
    btn.setForeground(Color.BLACK);
    btn.repaint();
    continue;
}

if (!c.isFlagged() && !c.isReveald() && !btn.revealed) {
    btn.setText("");   // efface le 🚩 si flag retiré
    btn.repaint();
    continue;
}
     if (c.isReveald() && !btn.revealed) {
         btn.revealed = true;
         btn.setEnabled(false);
        btn.setText("");

            if (c.isMine()) {
                       
btn.setText("\uD83D\uDCA3"); // 💣
btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, Math.min(24, tileSize - 14))); 
 explosionLayer.explodeAt(btn);
} 
else {
         int adj = c.getAdjacentMines();
          if (adj > 0) {
        btn.setText(String.valueOf(adj));
// Couleur du texte selon le nombre de mines adjacentes
btn.setFont(new Font("Courier New", Font.BOLD, Math.min(22, tileSize - 16)));
                    btn.setForeground(switch (adj) {
                     case 1 -> Color.BLUE;
                     case 2 -> new Color(0, 128, 0);
                     case 3 -> Color.RED;
                     case 4 -> new Color(0, 0, 128);
                     case 5 -> new Color(128, 0, 0);
                    case 6 -> new Color(0, 128, 128);
                    case 7 -> Color.BLACK;
                    default -> Color.DARK_GRAY;
                            });
                        }
                        
                    }
                    btn.repaint();
                }
            }
        }

        // Check win condition 
    String selectedDifficulty=difficulty.getSelectedItem().toString();
        if (!gameEnded && game.checkWin()) {
            gameEnded = true;
            gameTimer.stop();
            showWinDialog(seconds);
            int finalScore=score-seconds;
            int finaltime=seconds;
            Score scoreObj=new Score(finalScore, finaltime,selectedDifficulty);
            ScoreDAO dao=new ScoreDAO();
            dao.saveScore(scoreObj);
        }

        // Check game over
        if (!gameEnded && game.isGameover()) {
            gameEnded = true;
            gameTimer.stop();
            revealAllMinesOneByOne();
        }
    }

  // Affiche une animation d’explosion pour chaque mine révélée lorsque le joueur perd la partie, puis affiche la boîte de dialogue de fin de jeu
  private void revealAllMinesOneByOne() {
    List<Cell> minesToReveal = new ArrayList<>();

    for (int i = 0; i < rowNum; i++) {
        for (int j = 0; j < colNum; j++) {
            Cellule cellule = game.getCellule(i, j);

            if (cellule.isMine() && !cellule.isReveald()) {
                minesToReveal.add(cells[i][j]);
            }
        }
    }

    Collections.shuffle(minesToReveal);

    if (minesToReveal.isEmpty()) {
        showGameOverDialog();
        return;
    }

    final int[] index = {0};
    final javax.swing.Timer[] revealTimer = new javax.swing.Timer[1];

    revealTimer[0] = new javax.swing.Timer(250, e -> {
        Cell cell = minesToReveal.get(index[0]);

        cell.revealed = true;
        cell.setText("\uD83D\uDCA3");
        cell.setFont(new Font("Segoe UI Emoji", Font.PLAIN, Math.min(24, tileSize - 14)));
        cell.setEnabled(false);
        cell.repaint();
        explosionLayer.explodeAt(cell);
        index[0]++;

        if (index[0] >= minesToReveal.size()) {
            revealTimer[0].stop();
            showGameOverDialog();
        }
    });

    revealTimer[0].start();
}
// Affiche une boîte de dialogue personnalisée lorsque le joueur gagne la partie, avec un titre animé, le temps écoulé et un bouton pour recommencer
private void showWinDialog(int time) {
    JDialog dialog = new JDialog(frame, "You Win!", true);
    dialog.setSize(480, 300);
    dialog.setLayout(new BorderLayout(0, 10));
    dialog.getContentPane().setBackground(Color.BLACK);

    JLabel title = new JLabel("", SwingConstants.CENTER);
    title.setFont(loadDigitalFont(46f));
    title.setForeground(new Color(255, 215, 0));   // gold

    JLabel trophy = new JLabel("\uD83C\uDFC6 CONGRATULATIONS! \uD83C\uDFC6",SwingConstants.CENTER);
    trophy.setFont(new Font("Segoe UI Emoji", Font.BOLD, 17));
    trophy.setForeground(new Color(255, 215, 0));

    String timeStr = String.format("%02d:%02d", time / 60, time % 60);
    JLabel timeDisplay = new JLabel(" TIME : " + timeStr,SwingConstants.CENTER);
                                    
    timeDisplay.setFont(new Font("Courier New", Font.BOLD, 20));
    timeDisplay.setForeground(new Color(0, 220, 80));   // green

    JPanel center = new JPanel(new GridLayout(3, 1, 0, 6));
    center.setBackground(Color.BLACK);
    center.setBorder(BorderFactory.createEmptyBorder(15, 20, 5, 20));
    center.add(title);
    center.add(trophy);
    center.add(timeDisplay);

    // PLAY AGAIN button 
    JButton restart = new JButton("PLAY AGAIN");
    restart.setFont(new Font("Arial", Font.BOLD, 16));
    restart.setOpaque(true);
    restart.setBackground(new Color(255, 215, 0));  
    restart.setForeground(Color.BLACK);
    restart.setBorderPainted(false);
    restart.setFocusPainted(false);
    restart.setPreferredSize(new Dimension(160, 38));
    restart.addActionListener(e -> {
        dialog.dispose();
        applyDifficulty((String) difficulty.getSelectedItem());
    });

    JPanel south = new JPanel();
    south.setBackground(Color.BLACK);
    south.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
    south.add(restart);

    dialog.add(center, BorderLayout.CENTER);
    dialog.add(south,  BorderLayout.SOUTH);
    dialog.setLocationRelativeTo(frame);

    // Animation du titre "YOU WIN!" avec un effet de machine à écrire, suivi d’un clignotement entre le doré et le blanc
    String text = "YOU WIN!";
    final int[] idx = {0};
    javax.swing.Timer typewriter = new javax.swing.Timer(90, e -> {
        title.setText(text.substring(0, idx[0] + 1));
        idx[0]++;
        if (idx[0] >= text.length()) {
            ((javax.swing.Timer) e.getSource()).stop();

            final boolean[] toggle = {false};
            new javax.swing.Timer(450, blink -> {
                title.setForeground(toggle[0] ? new Color(255, 215, 0) : Color.WHITE);
                toggle[0] = !toggle[0];
            }).start();
        }
    });

    typewriter.start();
    dialog.setVisible(true);
}
}
