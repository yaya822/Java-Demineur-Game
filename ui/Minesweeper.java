package ui;
import game.Game;
import java.awt.*;
import java.io.File;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.Border;
import database.ScoreDAO;
import model.*;

public class Minesweeper {

    // ══════════════════════════════════════════════════════════════════
    //  PARTICLE  (spark or ember flying out of an explosion)
    // ══════════════════════════════════════════════════════════════════
    private static class Particle {
        float x, y, vx, vy, life, maxLife, size;
        Color color;
        boolean isSpark;

        Particle(float cx, float cy) {
            Random rng = new Random();
            double angle = rng.nextDouble() * Math.PI * 2;
            isSpark = rng.nextBoolean();
            float speed = isSpark ? 4f + rng.nextFloat() * 7f
                                  : 1f + rng.nextFloat() * 3f;
            vx = (float)(Math.cos(angle) * speed);
            vy = (float)(Math.sin(angle) * speed);
            x = cx; y = cy;

            if (isSpark) {
                life = maxLife = 8 + rng.nextInt(10);
                size = 1.5f + rng.nextFloat() * 2f;
                color = rng.nextBoolean() ? Color.WHITE : new Color(255, 230, 80);
            } else {
                life = maxLife = 15 + rng.nextInt(15);
                size = 3f + rng.nextFloat() * 5f;
                int pick = rng.nextInt(3);
                color = pick == 0 ? new Color(255, 90, 0)
                      : pick == 1 ? new Color(255, 160, 20)
                                  : new Color(200, 30, 0);
            }
        }

        void update() { x += vx; y += vy; life--; }
        boolean isDead() { return life <= 0; }

        void draw(Graphics2D g2) {
            float alpha = life / maxLife;
            Color c = new Color(color.getRed(), color.getGreen(),
                                color.getBlue(), (int)(alpha * 255));
            g2.setColor(c);
            if (isSpark) {
                g2.setStroke(new BasicStroke(Math.max(0.8f, size * alpha)));
                g2.drawLine((int)(x - vx * 2), (int)(y - vy * 2), (int)x, (int)y);
            } else {
                int s = Math.max(1, (int)(size * alpha));
                g2.fillOval((int)x, (int)y, s, s);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  RING  (shockwave circle expanding from explosion centre)
    // ══════════════════════════════════════════════════════════════════
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

    // ══════════════════════════════════════════════════════════════════
    //  EXPLOSION LAYER  (transparent overlay, sits on top of everything)
    // ══════════════════════════════════════════════════════════════════
    private class ExplosionLayer extends JComponent {
        private final List<Particle> particles = new ArrayList<>();
        private final List<Ring>     rings     = new ArrayList<>();
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

        void explodeAt(Component cell) {
            Point p  = SwingUtilities.convertPoint(
                           cell.getParent(), cell.getX(), cell.getY(), this);
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

    // ══════════════════════════════════════════════════════════════════
    //  CELL  (visual button + hooks into Game logic)
    // ══════════════════════════════════════════════════════════════════
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

            // Left-click → reveal
            addActionListener(e -> {
                if (gameEnded) return;
                if (revealed) return;

                // Start timer on first click
                if (!timerStarted) {
                    gameTimer.start();
                    timerStarted = true;
                }

                // Ask game logic to reveal; this may flood-fill neighbours
                game.revealCell(r, c);
                score+=10;

                // Sync every cell's visual state with the model
                refreshBoard();
            });

            // Right-click → flag / unflag
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
                    // Draw flag emoji centred
                    super.paintComponent(g);
                } else {
                    // Normal unrevealed tile
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
            } else {
                // Scorched crater tile
                g2.setColor(new Color(30, 15, 5));
                g2.fillRect(0, 0, w, h);
                float cx = w / 2f, cy = h / 2f;
                RadialGradientPaint scorch = new RadialGradientPaint(
                    cx, cy, Math.min(w, h) / 2f,
                    new float[]{0f, 0.5f, 1f},
                    new Color[]{new Color(80, 40, 10),
                                new Color(35, 18,  5),
                                new Color(12,  6,  2)});
                g2.setPaint(scorch);
                g2.fillRect(0, 0, w, h);
                g2.dispose();
                // Draw number / mine emoji on top
                super.paintComponent(g);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  FIELDS
    // ══════════════════════════════════════════════════════════════════
    private Game    game         = new Game();
    private boolean gameEnded    = false;
    private boolean timerStarted = false;
    private int     seconds      = 0;
    private int     minesLeft    = 6;
    private int score = 0;

    // Board dimensions (recalculated on difficulty change)
    int tileSize    = 70;
    int rowNum      = 8;
    int colNum      = 8;
    int boardWidth  = tileSize * colNum;
    int boardHeight = tileSize * rowNum;

    // Swing components
    JFrame   frame      = new JFrame("Minesweeper");
    JPanel   boardPanel = new JPanel();
    JPanel   titlePanel = new JPanel();
    JPanel   content;
    Cell[][] cells      = new Cell[rowNum][colNum];

    ExplosionLayer explosionLayer = new ExplosionLayer();

    JComboBox<String> difficulty =
        new JComboBox<>(new String[]{"Easy", "Medium", "Hard"});
    JLabel mineCountLabel = new JLabel("", SwingConstants.CENTER);
    JLabel timerLabel     = new JLabel("00:00", SwingConstants.CENTER);

    javax.swing.Timer gameTimer;

private Font loadDigitalFont(float size) {
    try {
        Font font = Font.createFont(
            Font.TRUETYPE_FONT,
          new File("fonts/digital-dream/DIGITALDREAM.ttf")
        );

        GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);

        return font.deriveFont(Font.PLAIN, size);
    } catch (Exception e) {
        e.printStackTrace();
        return new Font("Monospaced", Font.BOLD, Math.round(size));
    }
}

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
    // ══════════════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ══════════════════════════════════════════════════════════════════
    public Minesweeper() {
        // Start Easy game
        game.start(rowNum, colNum, minesLeft);

        Color  gray         = new Color(192, 192, 192);
        Font   retro        = new Font("Courier New", Font.BOLD, 18);
        Border raisedBorder = BorderFactory.createBevelBorder(
                                  javax.swing.border.BevelBorder.RAISED);
        Border loweredBorder = BorderFactory.createBevelBorder(
                                  javax.swing.border.BevelBorder.LOWERED);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setLayout(new BorderLayout());

        // ── Title bar ───────────────────────────────────────────────
        titlePanel.setLayout(new GridLayout(1, 3));
        titlePanel.setPreferredSize(new Dimension(boardWidth, 50));
        titlePanel.setBackground(gray);
        titlePanel.setBorder(raisedBorder);
        JButton rankingBtn = new JButton(" Top 10");
        rankingBtn.addActionListener(e->showRankingDialog());
        difficulty.setFont(retro);
        difficulty.setBackground(gray);
        difficulty.setBorder(raisedBorder);
        JPanel dp = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 8));
        dp.add(rankingBtn);
        dp.setBackground(gray);
        dp.add(difficulty);

        mineCountLabel.setFont(new Font("Monospaced", Font.BOLD, 22));
        mineCountLabel.setOpaque(true);
        mineCountLabel.setBackground(Color.BLACK);
        mineCountLabel.setForeground(Color.RED);
        mineCountLabel.setPreferredSize(new Dimension(150, 30));
        mineCountLabel.setBorder(loweredBorder);
        mineCountLabel.setText("\uD83D\uDCA3 " + minesLeft);
        JPanel mp = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 8));
        mp.setBackground(gray);
        mp.add(mineCountLabel);

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

        // Difficulty listener
        difficulty.addActionListener(e ->
            applyDifficulty((String) difficulty.getSelectedItem()));

        // ── Board panel ─────────────────────────────────────────────
        buildBoard();

        // ── Layer stack (content + explosion overlay) ───────────────
        content = new JPanel(new BorderLayout());
        content.add(titlePanel, BorderLayout.NORTH);
        content.add(boardPanel, BorderLayout.CENTER);

        JLayeredPane lp = frame.getLayeredPane();
        content.setBounds(0, 0, boardWidth, boardHeight + 50);
        lp.add(content, JLayeredPane.DEFAULT_LAYER);

        explosionLayer.setBounds(0, 0, boardWidth, boardHeight + 50);
        lp.add(explosionLayer, JLayeredPane.DRAG_LAYER);

        frame.setVisible(true);

        // Fix window size to account for OS title-bar insets
        Insets ins = frame.getInsets();
        frame.setSize(boardWidth  + ins.left + ins.right,
                      boardHeight + 50 + ins.top + ins.bottom);
        content.setBounds(0, 0, boardWidth, boardHeight + 50);
        explosionLayer.setBounds(0, 0, boardWidth, boardHeight + 50);
        frame.setLocationRelativeTo(null);

        // ── Count-up game timer ─────────────────────────────────────
        gameTimer = new javax.swing.Timer(1000, e -> {
            if (++seconds > 5999) seconds = 5999;
            timerLabel.setText(String.format("%02d:%02d",
                seconds / 60, seconds % 60));
        });
    }

    // ══════════════════════════════════════════════════════════════════
    //  BUILD / REBUILD the cell grid
    // ══════════════════════════════════════════════════════════════════
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
    // ══════════════════════════════════════════════════════════════════
    //  APPLY DIFFICULTY  (called when combo-box changes)
    // ══════════════════════════════════════════════════════════════════
    private void applyDifficulty(String diff) {
        switch (diff) {
         case "Easy"   -> { rowNum = 8;  colNum = 8;  tileSize = 70; minesLeft = 6; }
case "Medium" -> { rowNum = 12; colNum = 12; tileSize = 46; minesLeft = 16; }
default       -> { rowNum = 14; colNum = 14; tileSize = 40; minesLeft = 25; }
        }
        boardWidth  = tileSize * colNum;
        boardHeight = tileSize * rowNum;

        // Reset game logic
        game = new Game();
        game.start(rowNum, colNum, minesLeft);
        gameEnded    = false;
        timerStarted = false;
        seconds      = 0;
        gameTimer.stop();

        // Update header labels
        updateMinesLeftLabel();
        timerLabel.setText("00:00");
        titlePanel.setPreferredSize(new Dimension(boardWidth, 50));

        // Rebuild cells
        buildBoard();

        // Resize window (including OS chrome)
        Insets ins = frame.getInsets();
        frame.setSize(boardWidth  + ins.left + ins.right,
                      boardHeight + 50 + ins.top + ins.bottom);
        content.setBounds(0, 0, boardWidth, boardHeight + 50);
        explosionLayer.setBounds(0, 0, boardWidth, boardHeight + 50);
        explosionLayer.clear();
        frame.setLocationRelativeTo(null);
    }
//     private void showGameOverDialog() {

//     JDialog dialog = new JDialog(frame, "Game Over", true);

//     dialog.setSize(350, 220);
//     dialog.setLayout(new BorderLayout());
//     dialog.getContentPane().setBackground(new Color(40, 40, 40));

//     // ===== TITLE =====
//     JLabel title = new JLabel("💥 GAME OVER 💥", SwingConstants.CENTER);

//     title.setFont(new Font("Arial", Font.BOLD, 28));
//     title.setForeground(Color.RED);

//     // ===== MESSAGE =====
//     JLabel msg = new JLabel(
//         "You stepped on a mine!",
//         SwingConstants.CENTER
//     );

//     msg.setFont(new Font("Arial", Font.PLAIN, 18));
//     msg.setForeground(Color.WHITE);

//     // ===== BUTTON =====
//     JButton restart = new JButton("Play Again");

//     restart.setFont(new Font("Arial", Font.BOLD, 18));

//     restart.addActionListener(e -> {
//         dialog.dispose();
//         applyDifficulty((String)difficulty.getSelectedItem());
//     });

//     JPanel center = new JPanel(new GridLayout(2,1));
//     center.setBackground(new Color(40,40,40));

//     center.add(title);
//     center.add(msg);

//     JPanel south = new JPanel();
//     south.setBackground(new Color(40,40,40));
//     south.add(restart);

//     dialog.add(center, BorderLayout.CENTER);
//     dialog.add(south, BorderLayout.SOUTH);

//     dialog.setLocationRelativeTo(frame);
//     dialog.setVisible(true);
// }
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

    // ══════════════════════════════════════════════════════════════════
    //  REFRESH BOARD  (sync visuals with game model after every move)
    // ══════════════════════════════════════════════════════════════════
    private void refreshBoard() {
          updateMinesLeftLabel();
        boolean wonNow = false;

        for (int i = 0; i < rowNum; i++) {
            for (int j = 0; j < colNum; j++) {
                Cellule c   = game.getCellule(i, j);
                Cell    btn = cells[i][j];

                if (c.isFlagged() && !c.isReveald()) {
    btn.setText("\uD83D\uDEA9"); // 🚩
btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, Math.min(24, tileSize - 14)));
    btn.setForeground(Color.BLACK);
    btn.repaint();
    continue;
}

// ET ajouter le cas unflag (quand le flag est retiré) :
if (!c.isFlagged() && !c.isReveald() && !btn.revealed) {
    btn.setText("");   // ← efface le 🚩 si flag retiré
    btn.repaint();
    continue;
}

                if (c.isReveald() && !btn.revealed) {
                    btn.revealed = true;
                    btn.setEnabled(false);
                    btn.setText("");

                    if (c.isMine()) {
                        // PAR :
btn.setText("\uD83D\uDCA3"); // 💣
btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, Math.min(24, tileSize - 14))); 
 explosionLayer.explodeAt(btn);
} else {
                        int adj = c.getAdjacentMines();
                        if (adj > 0) {
                            btn.setText(String.valueOf(adj));
                            // PAR :
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
        String selectedDifficulty=difficulty.getSelectedItem().toString();
        // ── Check win ──────────────────────────────────────────────
        if (!gameEnded && game.checkWin()) {
            gameEnded = true;
            gameTimer.stop();

            JOptionPane.showMessageDialog(frame,
                "🎉 You win! Time: " + timerLabel.getText());
            int finalScore=score-seconds;
            int finaltime=seconds;

            Score scoreObj=new Score(finalScore, finaltime,selectedDifficulty);
            ScoreDAO dao=new ScoreDAO();
            dao.saveScore(scoreObj);
        }

        // ── Check game over (mine hit) ─────────────────────────────
        if (!gameEnded && game.isGameover()) {
            gameEnded = true;
            gameTimer.stop();
            revealAllMinesOneByOne();
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  REVEAL ALL MINES  (called when player loses)
    // ══════════════════════════════════════════════════════════════════
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
}
