import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import javax.swing.*;
// Adicionado para Anti-aliasing (texto mais suave)
import java.awt.RenderingHints;

/**
 * Classe principal que inicia a aplicação Tetris.
 */
public class Main {
    public static void main(String[] args) {
        // Garante que a GUI é construída na thread de eventos do Swing (EDT)
        SwingUtilities.invokeLater(() -> {
            GameEngine engine = new GameEngine();
            engine.startGame();
        });
    }
}

/**
 * CLASSE DE CONFIGURAÇÃO DO BANCO DE DADOS
 * Altere as constantes abaixo para rodar em outra máquina.
 */
class DatabaseConfig {
    // URL de Conexão:
    // "localhost" = roda na máquina local. Se for rede, coloque o IP (ex:
    // 192.168.0.10).
    // "1433" = porta padrão do SQL Server.
    // "encrypt=true;trustServerCertificate=true" = necessário para drivers novos
    // não bloquearem conexão SSL.
    public static final String URL = "jdbc:sqlserver://localhost:1433;databaseName=TetrisDB;encrypt=true;trustServerCertificate=true;";

    // Usuário do SQL Server (Geralmente 'sa' ou seu usuário do Windows se
    // configurado mixed mode)
    public static final String USER = "sa";

    // Senha do banco de dados
    public static final String PASSWORD = "zecamalandraria";
}

// --- ENUMS E MODELOS ADICIONAIS ---

enum GameState {
    SPLASH, MENU, PLAYING, PAUSED, GAME_OVER, HIGH_SCORES
}

class HighscoreEntry {
    private final String name;
    private final int score;

    public HighscoreEntry(String name, int score) {
        this.name = name;
        this.score = score;
    }

    public String getName() {
        return name;
    }

    public int getScore() {
        return score;
    }
}

/**
 * Gerencia a lista de melhores pontuações (começa vazia).
 */
/**
 * Gerencia as pontuações salvando e lendo do SQL Server.
 */
/**
 * Gerencia as pontuações salvando e lendo do SQL Server.
 * Atualizado para as colunas: idPontuacao, nome, pontuacao, dthRegistro
 */
class HighscoreManager {

    public HighscoreManager() {
    }

    public void addScore(String playerName, int scoreValue) {
        // MUDANÇA AQUI: Nomes das colunas atualizados no INSERT
        String sql = "INSERT INTO Highscores (nome, pontuacao) VALUES (?, ?)";

        try (Connection conn = DriverManager.getConnection(DatabaseConfig.URL, DatabaseConfig.USER,
                DatabaseConfig.PASSWORD);
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, playerName);
            pstmt.setInt(2, scoreValue);
            pstmt.executeUpdate();
            System.out.println("Pontuação salva no SQL Server com sucesso!");

        } catch (SQLException e) {
            System.err.println("ERRO AO SALVAR NO BANCO: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Erro de conexão com o Banco de Dados!\n" + e.getMessage());
        }
    }

    public List<HighscoreEntry> getTopScores() {
        List<HighscoreEntry> scores = new ArrayList<>();

        // MUDANÇA AQUI: Nomes das colunas atualizados no SELECT
        // 'dthRegistro' não precisa vir se não for exibir, mas está na tabela.
        String sql = "SELECT TOP 10 nome, pontuacao FROM Highscores ORDER BY pontuacao DESC";

        try (Connection conn = DriverManager.getConnection(DatabaseConfig.URL, DatabaseConfig.USER,
                DatabaseConfig.PASSWORD);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                // MUDANÇA AQUI: Lendo as colunas com os novos nomes
                String name = rs.getString("nome");
                int s = rs.getInt("pontuacao");

                scores.add(new HighscoreEntry(name, s));
            }

        } catch (SQLException e) {
            System.err.println("ERRO AO LER DO BANCO: " + e.getMessage());
            scores.add(new HighscoreEntry("Erro BD", 0));
        }

        return scores;
    }
}

// --- 1. MODELO (Lógica do Jogo) ---------------------------------------------
// Nenhuma alteração nas classes Tetromino, Peças (I,J,L,O,S,T,Z), Board, Score

abstract class Tetromino {
    protected int[][] shape;
    protected int x, y;
    protected Color color;

    public Tetromino() {
        this.x = Board.WIDTH / 2 - 2;
        this.y = 0;
        initializeShape();
    }

    public abstract void initializeShape();

    public void rotate() {
        int rows = shape.length;
        int cols = shape[0].length;
        int[][] rotatedShape = new int[cols][rows];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                rotatedShape[j][rows - 1 - i] = shape[i][j];
            }
        }
        shape = rotatedShape;
    }

    public Tetromino clone() {
        try {
            Tetromino clonedPiece = (Tetromino) super.clone();
            clonedPiece.shape = Arrays.stream(this.shape).map(int[]::clone).toArray(int[][]::new);
            return clonedPiece;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    public int[][] getShape() {
        return shape;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Color getColor() {
        return color;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }
}

class IPiece extends Tetromino implements Cloneable {
    public IPiece() {
        super();
        color = Color.CYAN;
    }

    @Override
    public void initializeShape() {
        shape = new int[][] { { 1, 1, 1, 1 } };
        this.x = Board.WIDTH / 2 - 2;
        this.y = 0;
    }
}

class JPiece extends Tetromino implements Cloneable {
    public JPiece() {
        super();
        color = Color.BLUE;
    }

    @Override
    public void initializeShape() {
        shape = new int[][] { { 1, 0, 0 }, { 1, 1, 1 } };
    }
}

class LPiece extends Tetromino implements Cloneable {
    public LPiece() {
        super();
        color = Color.ORANGE;
    }

    @Override
    public void initializeShape() {
        shape = new int[][] { { 0, 0, 1 }, { 1, 1, 1 } };
    }
}

class OPiece extends Tetromino implements Cloneable {
    public OPiece() {
        super();
        color = Color.YELLOW;
    }

    @Override
    public void initializeShape() {
        shape = new int[][] { { 1, 1 }, { 1, 1 } };
    }

    @Override
    public void rotate() {
        /* Não faz nada */ }
}

class SPiece extends Tetromino implements Cloneable {
    public SPiece() {
        super();
        color = Color.GREEN;
    }

    @Override
    public void initializeShape() {
        shape = new int[][] { { 0, 1, 1 }, { 1, 1, 0 } };
    }
}

class TPiece extends Tetromino implements Cloneable {
    public TPiece() {
        super();
        color = Color.MAGENTA;
    }

    @Override
    public void initializeShape() {
        shape = new int[][] { { 0, 1, 0 }, { 1, 1, 1 } };
    }
}

class ZPiece extends Tetromino implements Cloneable {
    public ZPiece() {
        super();
        color = Color.RED;
    }

    @Override
    public void initializeShape() {
        shape = new int[][] { { 1, 1, 0 }, { 0, 1, 1 } };
    }
}

/**
 * Fábrica (Double 7-Bag Randomizer)
 */
class TetrominoFactory {
    private static final Random random = new Random();
    private static final Class<? extends Tetromino>[] PIECES = new Class[] {
            IPiece.class, JPiece.class, LPiece.class, OPiece.class,
            SPiece.class, TPiece.class, ZPiece.class
    };
    private static List<Class<? extends Tetromino>> bag;
    static {
        bag = new ArrayList<>();
        fillBag();
    }

    private static void fillBag() {
        bag.clear();
        bag.addAll(Arrays.asList(PIECES)); // Sacola 1
        bag.addAll(Arrays.asList(PIECES)); // Sacola 2
        Collections.shuffle(bag, random);
    }

    public static Tetromino createRandomPiece() {
        if (bag.isEmpty()) {
            fillBag();
        }
        try {
            return bag.remove(0).getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

class Board {
    public static final int WIDTH = 10;
    public static final int HEIGHT = 20;
    private Color[][] grid;

    public Board() {
        grid = new Color[HEIGHT][WIDTH];
    }

    public boolean isValidPosition(Tetromino piece, int newX, int newY, int[][] shape) {
        for (int row = 0; row < shape.length; row++) {
            for (int col = 0; col < shape[0].length; col++) {
                if (shape[row][col] != 0) {
                    int boardX = newX + col;
                    int boardY = newY + row;
                    if (boardX < 0 || boardX >= WIDTH || boardY >= HEIGHT)
                        return false;
                    if (boardY >= 0 && grid[boardY][boardX] != null)
                        return false;
                }
            }
        }
        return true;
    }

    public void solidifyPiece(Tetromino piece) {
        int[][] shape = piece.getShape();
        for (int row = 0; row < shape.length; row++) {
            for (int col = 0; col < shape[0].length; col++) {
                if (shape[row][col] != 0) {
                    int boardX = piece.getX() + col;
                    int boardY = piece.getY() + row;
                    if (boardY >= 0)
                        grid[boardY][boardX] = piece.getColor();
                }
            }
        }
    }

    public int clearLines() {
        int linesCleared = 0;
        for (int r = HEIGHT - 1; r >= 0; r--) {
            if (isLineComplete(r)) {
                removeLine(r);
                linesCleared++;
                r++;
            }
        }
        return linesCleared;
    }

    private boolean isLineComplete(int row) {
        for (int c = 0; c < WIDTH; c++) {
            if (grid[row][c] == null)
                return false;
        }
        return true;
    }

    private void removeLine(int row) {
        for (int r = row; r > 0; r--) {
            grid[r] = grid[r - 1];
        }
        grid[0] = new Color[WIDTH];
    }

    public Color getCellColor(int row, int col) {
        return grid[row][col];
    }
}

class Score {
    private int score;
    private int level;
    private int linesCleared;
    private final int LINES_PER_LEVEL = 10;

    public Score() {
        this.score = 0;
        this.level = 1;
        this.linesCleared = 0;
    }

    public void addScore(int numLines) {
        int points;
        switch (numLines) {
            case 1:
                points = 40;
                break;
            case 2:
                points = 100;
                break;
            case 3:
                points = 300;
                break;
            case 4:
                points = 1200;
                break;
            default:
                points = 0;
        }
        this.score += points * this.level;
        this.linesCleared += numLines;
        if (this.linesCleared >= this.level * LINES_PER_LEVEL) {
            level++;
        }
    }

    public int getScore() {
        return score;
    }

    public int getLevel() {
        return level;
    }

    public int getLinesCleared() {
        return linesCleared;
    }

    public int getDelay() {
        if (level <= 3)
            return 1000;
        if (level <= 6)
            return 700;
        return 400;
    }
}

// --- 2. VISÃO (Interface Gráfica com Swing) ---------------------------------
// Traduções e correções de layout aplicadas

/**
 * Desenha o menu (sem "Opções") e o jogo (sem peça flutuante no Game Over).
 */
class GamePanel extends JPanel {
    private static final int BLOCK_SIZE = 30;
    private final GameEngine engine;
    private static final Color BOARD_BACKGROUND = new Color(190, 205, 190);
    private static final Color TEXT_COLOR = new Color(30, 60, 30);
    private static final Color BORDER_COLOR = new Color(30, 60, 30);

    public GamePanel(GameEngine engine) {
        this.engine = engine;
        setBackground(BOARD_BACKGROUND);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        switch (engine.getCurrentState()) {
            case SPLASH:
                drawSplash(g2d);
                break;
            case MENU:
                drawMenu(g2d);
                break;
            case HIGH_SCORES:
                drawHighScores(g2d);
                break;
            default:
                drawGame(g2d);
                break;
        }
    }

    private void drawMenuBorder(Graphics2D g2d) {
        int width = getWidth();
        int height = getHeight();
        int padding = 20;
        int thickness = 4;
        g2d.setColor(BORDER_COLOR);
        g2d.fillRect(padding, padding, width - 2 * padding, thickness);
        g2d.fillRect(padding, height - padding - thickness, width - 2 * padding, thickness);
        g2d.fillRect(padding, padding, thickness, height - 2 * padding);
        g2d.fillRect(width - padding - thickness, padding, thickness, height - 2 * padding);
        int innerPadding = padding + 10;
        g2d.drawRect(innerPadding, innerPadding, width - 2 * innerPadding, height - 2 * innerPadding);
    }

    private void drawSplash(Graphics2D g2d) {
        g2d.setColor(BOARD_BACKGROUND);
        g2d.fillRect(0, 0, getWidth(), getHeight());
        drawMenuBorder(g2d);
        g2d.setColor(TEXT_COLOR);
        g2d.setFont(new Font("Monospaced", Font.BOLD, 80));
        String title = "TETRIS";
        FontMetrics fmTitle = g2d.getFontMetrics();
        int titleX = (getWidth() - fmTitle.stringWidth(title)) / 2;
        g2d.drawString(title, titleX, getHeight() / 3);
        g2d.setFont(new Font("Monospaced", Font.BOLD, 24));
        String message = "[ PRESSIONE ENTER ]";
        FontMetrics fmMsg = g2d.getFontMetrics();
        int msgX = (getWidth() - fmMsg.stringWidth(message)) / 2;
        g2d.drawString(message, msgX, getHeight() / 2 + 50);
    }

    /**
     * Remove "Opções"
     */
    private void drawMenu(Graphics2D g2d) {
        g2d.setColor(BOARD_BACKGROUND);
        g2d.fillRect(0, 0, getWidth(), getHeight());
        drawMenuBorder(g2d);
        g2d.setColor(TEXT_COLOR);
        g2d.setFont(new Font("Monospaced", Font.BOLD, 40));
        String title = "MENU PRINCIPAL";
        FontMetrics fmTitle = g2d.getFontMetrics();
        int titleX = (getWidth() - fmTitle.stringWidth(title)) / 2;
        g2d.drawString(title, titleX, getHeight() / 4);

        g2d.setFont(new Font("Monospaced", Font.BOLD, 30));
        // Opção "Opções" removida
        String[] options = { "1. NOVO JOGO", "2. HIGHSCORE", "3. SAIR" };

        int startY = getHeight() / 2 - 40;
        int lineSpacing = 50;
        for (int i = 0; i < options.length; i++) {
            String option = options[i];
            String fullLine = (engine.getSelectedMenuIndex() == i) ? "> " + option : "  " + option;
            FontMetrics fm = g2d.getFontMetrics();
            int optionX = (getWidth() - fm.stringWidth(fullLine)) / 2;
            g2d.drawString(fullLine, optionX, startY + i * lineSpacing);
        }
    }

    private void drawHighScores(Graphics2D g2d) {
        g2d.setColor(BOARD_BACKGROUND);
        g2d.fillRect(0, 0, getWidth(), getHeight());
        drawMenuBorder(g2d);
        g2d.setColor(TEXT_COLOR);
        g2d.setFont(new Font("Monospaced", Font.BOLD, 40));
        String title = "HIGHSCORES";
        FontMetrics fmTitle = g2d.getFontMetrics();
        int titleX = (getWidth() - fmTitle.stringWidth(title)) / 2;
        g2d.drawString(title, titleX, 100);
        g2d.setFont(new Font("Monospaced", Font.PLAIN, 20));
        int startY = 160;
        int lineSpacing = 30;
        String header = String.format("%-6s %-10s %s", "RANK", "NOME", "PONTOS");
        FontMetrics fmHeader = g2d.getFontMetrics();
        int xPos = (getWidth() - fmHeader.stringWidth(header)) / 2;
        g2d.drawString(header, xPos, startY - 20);

        List<HighscoreEntry> scores = engine.getHighscoreManager().getTopScores();
        if (scores.isEmpty()) {
            g2d.setFont(new Font("Monospaced", Font.ITALIC, 20));
            String msg = "Nenhuma pontuação registrada.";
            int msgX = (getWidth() - g2d.getFontMetrics().stringWidth(msg)) / 2;
            g2d.drawString(msg, msgX, startY + lineSpacing);
        } else {
            int rank = 1;
            for (HighscoreEntry entry : scores) {
                String rankStr = String.format("%-6s", rank + ".");
                String nameStr = String.format("%-10s", entry.getName());
                String scoreStr = String.valueOf(entry.getScore());
                String line = rankStr + nameStr + scoreStr;
                g2d.drawString(line, xPos, startY + (rank - 1) * lineSpacing);
                rank++;
            }
        }

        g2d.setFont(new Font("Monospaced", Font.PLAIN, 18));
        String msg = "Pressione ESC para voltar";
        int msgX = (getWidth() - g2d.getFontMetrics().stringWidth(msg)) / 2;
        g2d.drawString(msg, msgX, getHeight() - 60);
    }

    /**
     * Corrige o "Fim de Jogo".
     */
    private void drawGame(Graphics2D g2d) {
        // 1. Desenha a peça fantasma (somente se o jogo não tiver acabado)
        Tetromino ghostPiece = engine.getGhostPiece();
        if (ghostPiece != null && !engine.isGameOver()) {
            drawPiece(g2d, ghostPiece, new Color(30, 60, 30, 50), true);
        }

        // 2. Desenha as peças solidificadas no tabuleiro
        Board board = engine.getBoard();
        for (int r = 0; r < Board.HEIGHT; r++) {
            for (int c = 0; c < Board.WIDTH; c++) {
                if (board.getCellColor(r, c) != null) {
                    drawBlock(g2d, c, r, board.getCellColor(r, c));
                }
            }
        }

        // 3. Desenha a peça atual (somente se o jogo não tiver acabado)
        Tetromino currentPiece = engine.getCurrentPiece();
        if (currentPiece != null && !engine.isGameOver()) {
            drawPiece(g2d, currentPiece, currentPiece.getColor(), false);
        }

        // 4. Desenha as mensagens de overlay (PAUSADO / FIM DE JOGO)
        if (engine.isGameOver()) {
            drawMessage(g2d, "FIM DE JOGO", Color.RED);
        } else if (engine.isPaused()) {
            drawMessage(g2d, "PAUSADO", TEXT_COLOR);
        }
    }

    private void drawBlock(Graphics g, int x, int y, Color color) {
        int xPos = x * BLOCK_SIZE;
        int yPos = y * BLOCK_SIZE;
        g.setColor(color);
        g.fillRect(xPos, yPos, BLOCK_SIZE, BLOCK_SIZE);
        g.setColor(color.darker().darker());
        g.drawRect(xPos, yPos, BLOCK_SIZE - 1, BLOCK_SIZE - 1);
    }

    private void drawPiece(Graphics g, Tetromino piece, Color color, boolean isGhost) {
        int[][] shape = piece.getShape();
        for (int row = 0; row < shape.length; row++) {
            for (int col = 0; col < shape[0].length; col++) {
                if (shape[row][col] != 0) {
                    int gridX = piece.getX() + col;
                    int gridY = piece.getY() + row;
                    if (gridY >= 0) {
                        int xPos = gridX * BLOCK_SIZE;
                        int yPos = gridY * BLOCK_SIZE;
                        if (isGhost) {
                            g.setColor(color);
                            g.fillRect(xPos, yPos, BLOCK_SIZE, BLOCK_SIZE);
                            g.setColor(Color.BLACK);
                            g.drawRect(xPos, yPos, BLOCK_SIZE - 1, BLOCK_SIZE - 1);
                        } else {
                            drawBlock(g, gridX, gridY, color);
                        }
                    }
                }
            }
        }
    }

    private void drawMessage(Graphics g, String message, Color color) {
        g.setColor(new Color(BOARD_BACKGROUND.getRed(), BOARD_BACKGROUND.getGreen(), BOARD_BACKGROUND.getBlue(), 200));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(color);
        g.setFont(new Font("Monospaced", Font.BOLD, 50));
        FontMetrics fm = g.getFontMetrics();
        int x = (getWidth() - fm.stringWidth(message)) / 2;
        int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
        g.drawString(message, x, y);
    }
}

/**
 * Ajusta o espaçamento das caixas para evitar sobreposição.
 */
class ScorePanel extends JPanel {
    private static final int BLOCK_SIZE_MAIN = 25;
    private static final int BLOCK_SIZE_SMALL = 15;
    private final GameEngine engine;
    private static final Color PANEL_BACKGROUND = new Color(190, 205, 190);
    private static final Color TEXT_COLOR = new Color(30, 60, 30);
    private static final Color BORDER_COLOR = new Color(30, 60, 30);
    private static final Color GROUP_PANEL_BG = new Color(170, 185, 170);
    private static final Font FONT_TITLE = new Font("Monospaced", Font.BOLD, 16);
    private static final Font FONT_TEXT = new Font("Monospaced", Font.PLAIN, 14);
    private static final Font FONT_SCORE_VALUE = new Font("Monospaced", Font.BOLD, 18);
    private static final int MARGIN = 15;

    public ScorePanel(GameEngine engine) {
        this.engine = engine;
        setPreferredSize(new Dimension(220, Board.HEIGHT * 30));
        setBackground(PANEL_BACKGROUND);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (engine.getCurrentState() != GameState.PLAYING &&
                engine.getCurrentState() != GameState.PAUSED &&
                engine.getCurrentState() != GameState.GAME_OVER) {
            return;
        }

        int yOffset = MARGIN;

        // --- 1. Painel de Peças (Espaçamento corrigido) ---
        int piecePanelHeight = 200; // Aumentado de 180 para 200
        drawGroupPanel(g2d, yOffset, piecePanelHeight, "PEÇAS");
        g2d.setFont(FONT_TITLE);
        g2d.setColor(TEXT_COLOR);
        g2d.drawString("TROCAR (C)", MARGIN + 10, yOffset + 30);
        drawHeldPiece(g2d, yOffset + 40);
        g2d.drawString("PRÓXIMAS", MARGIN + 110, yOffset + 30);
        drawNextPiece(g2d, yOffset + 40, 0, BLOCK_SIZE_MAIN);
        drawNextPiece(g2d, yOffset + 120, 1, BLOCK_SIZE_SMALL); // yOffset de 110 -> 120
        drawNextPiece(g2d, yOffset + 160, 2, BLOCK_SIZE_SMALL); // yOffset de 155 -> 160
        yOffset += piecePanelHeight + MARGIN;

        // --- 2. Painel de Status ---
        int statusPanelHeight = 120;
        drawGroupPanel(g2d, yOffset, statusPanelHeight, "INFORMAÇÕES");
        int statusY = yOffset + 35;
        statusY = drawScoreInfo(g2d, statusY, "PONTUAÇÃO:", engine.getScore().getScore());
        statusY = drawScoreInfo(g2d, statusY, "NÍVEL:", engine.getScore().getLevel());
        drawScoreInfo(g2d, statusY, "LINHAS:", engine.getScore().getLinesCleared());
        yOffset += statusPanelHeight + MARGIN;

        // --- 3. Painel de Controles (Altura ajustada) ---
        // Ajustado para 235 para preencher os 600px de altura (200+120+235 + 15*3 = 555
        // + 45 = 600)
        int controlsPanelHeight = (int) (getPreferredSize().getHeight() - yOffset - MARGIN);
        drawGroupPanel(g2d, yOffset, controlsPanelHeight, "CONTROLES");
        int controlY = yOffset + 35;
        g2d.setFont(FONT_TEXT);
        g2d.setColor(TEXT_COLOR);
        String[] controls = { "Setas: Mover/Rodar", "Espaço: Queda Rápida", "C: Trocar Peça", "P: Pausa", "ESC: Menu" };
        for (String line : controls) {
            g2d.drawString(line, MARGIN + 10, controlY);
            controlY += 22;
        }
    }

    // Métodos helpers (drawGroupPanel, drawScoreInfo, drawHeldPiece, etc)
    private void drawGroupPanel(Graphics g, int y, int height, String title) {
        int panelWidth = getWidth() - (MARGIN * 2);
        g.setColor(GROUP_PANEL_BG);
        g.fillRect(MARGIN, y, panelWidth, height);
        g.setColor(BORDER_COLOR);
        g.drawRect(MARGIN, y, panelWidth - 1, height - 1);
        g.setFont(FONT_TITLE);
        FontMetrics fm = g.getFontMetrics();
        int titleWidth = fm.stringWidth(title);
        int titleX = MARGIN + (panelWidth - titleWidth) / 2;
        g.setColor(PANEL_BACKGROUND);
        g.fillRect(titleX - 5, y - (fm.getHeight() / 2), titleWidth + 10, fm.getHeight());
        g.setColor(TEXT_COLOR);
        g.drawString(title, titleX, y + (fm.getAscent() / 2) - 2);
    }

    private int drawScoreInfo(Graphics g, int yOffset, String title, int value) {
        g.setFont(FONT_TEXT);
        g.setColor(TEXT_COLOR);
        g.drawString(title, MARGIN + 10, yOffset);
        String valueStr = String.valueOf(value);
        g.setFont(FONT_SCORE_VALUE);
        FontMetrics fm = g.getFontMetrics();
        int valueX = getWidth() - MARGIN - 10 - fm.stringWidth(valueStr);
        g.drawString(valueStr, valueX, yOffset);
        return yOffset + 30;
    }

    private void drawHeldPiece(Graphics g, int yStart) {
        int boxSize = 80;
        int startX = MARGIN + 10;
        g.setColor(BORDER_COLOR);
        g.drawRect(startX, yStart, boxSize, boxSize);
        Tetromino heldPiece = engine.getHeldPiece();
        if (heldPiece != null) {
            int pieceWidth = heldPiece.getShape()[0].length * BLOCK_SIZE_MAIN;
            int pieceHeight = heldPiece.getShape().length * BLOCK_SIZE_MAIN;
            drawPiece(g, heldPiece, startX + (boxSize - pieceWidth) / 2, yStart + (boxSize - pieceHeight) / 2,
                    BLOCK_SIZE_MAIN);
        }
    }

    private void drawNextPiece(Graphics g, int yStart, int index, int blockSize) {
        Queue<Tetromino> nextPieces = engine.getNextPieces();
        if (index >= nextPieces.size())
            return;
        Tetromino piece = (Tetromino) nextPieces.toArray()[index];
        int boxWidth = 80;
        int startX = MARGIN + 110;
        int pieceWidth = piece.getShape()[0].length * blockSize;
        int pieceHeight = piece.getShape().length * blockSize;
        int areaHeight = (index == 0) ? 80 : 40;
        drawPiece(g, piece, startX + (boxWidth - pieceWidth) / 2, yStart + (areaHeight - pieceHeight) / 2, blockSize);
    }

    private void drawBlock(Graphics g, int xPos, int yPos, Color color, int blockSize) {
        g.setColor(color);
        g.fillRect(xPos, yPos, blockSize, blockSize);
        g.setColor(color.darker().darker());
        g.drawRect(xPos, yPos, blockSize - 1, blockSize - 1);
    }

    private void drawPiece(Graphics g, Tetromino piece, int xStart, int yStart, int blockSize) {
        int[][] shape = piece.getShape();
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[0].length; c++) {
                if (shape[r][c] != 0) {
                    drawBlock(g, xStart + c * blockSize, yStart + r * blockSize, piece.getColor(), blockSize);
                }
            }
        }
    }
}

// --- 3. CONTROLE (Motor do Jogo) --------------------------------------------
/**
 * Remove "Opções" do menu e gerencia o layout dinâmico.
 */
class GameEngine extends KeyAdapter implements ActionListener {
    private JFrame frame;
    private GamePanel gamePanel;
    private ScorePanel scorePanel;
    private JPanel mainPanel;

    private static final int GAME_PANEL_WIDTH = Board.WIDTH * 30; // 300
    private static final int SCORE_PANEL_WIDTH = 220;
    private static final int GAME_HEIGHT = Board.HEIGHT * 30; // 600
    private static final int MENU_WIDTH = GAME_PANEL_WIDTH + SCORE_PANEL_WIDTH; // 520

    private Board board;
    private Score score;
    private Tetromino currentPiece;
    private Queue<Tetromino> nextPieces;
    private Tetromino heldPiece = null;
    private boolean canHold = true;
    private HighscoreManager highscoreManager;

    private Timer gameTimer;
    private GameState currentState;
    private int selectedMenuIndex = 0;
    // Opção "Opções" removida
    private final String[] MENU_OPTIONS = { "NOVO JOGO", "HIGHSCORE", "SAIR" };

    public GameEngine() {
        this.board = new Board();
        this.score = new Score();
        this.nextPieces = new LinkedList<>();
        this.highscoreManager = new HighscoreManager();
        this.gamePanel = new GamePanel(this);
        this.scorePanel = new ScorePanel(this);
        this.currentState = GameState.SPLASH;

        for (int i = 0; i < 3; i++) {
            nextPieces.add(TetrominoFactory.createRandomPiece());
        }
        initializeFrame();
    }

    public void initializeFrame() {
        frame = new JFrame("Tetris em Java POO");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        mainPanel = new JPanel(new BorderLayout());

        gamePanel.setPreferredSize(new Dimension(MENU_WIDTH, GAME_HEIGHT));
        mainPanel.add(gamePanel, BorderLayout.CENTER);

        frame.add(mainPanel, BorderLayout.CENTER);
        frame.pack();

        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
        frame.addKeyListener(this);
        frame.setFocusable(true);
        frame.requestFocusInWindow();
    }

    public void startGame() {
        this.board = new Board();
        this.score = new Score();
        this.nextPieces.clear();
        for (int i = 0; i < 3; i++) {
            nextPieces.add(TetrominoFactory.createRandomPiece());
        }
        this.heldPiece = null;
        this.canHold = true;
        currentPiece = getNextPieceInQueue();
        if (gameTimer != null)
            gameTimer.stop();
        gameTimer = new Timer(score.getDelay(), this);
        currentState = GameState.SPLASH;

        mainPanel.remove(scorePanel);
        gamePanel.setPreferredSize(new Dimension(MENU_WIDTH, GAME_HEIGHT));
        frame.pack();
        frame.setLocationRelativeTo(null);

        updateView();
    }

    private Tetromino getNextPieceInQueue() {
        Tetromino next = nextPieces.poll();
        nextPieces.add(TetrominoFactory.createRandomPiece());
        return next;
    }

    private void startNewGame() {
        currentState = GameState.PLAYING;
        gameTimer.start();
        updateView();
    }

    private void navigateToMenu() {
        currentState = GameState.MENU;
        selectedMenuIndex = 0;
        if (gameTimer != null)
            gameTimer.stop();

        mainPanel.remove(scorePanel);
        gamePanel.setPreferredSize(new Dimension(MENU_WIDTH, GAME_HEIGHT));
        frame.pack();
        frame.setLocationRelativeTo(null);

        updateView();
    }

    private void restartGameLogic() {
        gamePanel.setPreferredSize(new Dimension(GAME_PANEL_WIDTH, GAME_HEIGHT));
        mainPanel.add(scorePanel, BorderLayout.EAST);
        frame.pack();
        frame.setLocationRelativeTo(null);

        board = new Board();
        score = new Score();
        nextPieces.clear();
        for (int i = 0; i < 3; i++) {
            nextPieces.add(TetrominoFactory.createRandomPiece());
        }
        heldPiece = null;
        canHold = true;
        currentPiece = getNextPieceInQueue();
        if (gameTimer != null)
            gameTimer.stop();
        gameTimer = new Timer(score.getDelay(), this);

        startNewGame();
    }

    private void togglePause() {
        if (currentState == GameState.PLAYING) {
            currentState = GameState.PAUSED;
            gameTimer.stop();
        } else if (currentState == GameState.PAUSED) {
            currentState = GameState.PLAYING;
            gameTimer.start();
        }
        updateView();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (currentState == GameState.PLAYING) {
            movePiece(0, 1);
            if (gameTimer.getDelay() != score.getDelay()) {
                gameTimer.setDelay(score.getDelay());
            }
            updateView();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        switch (currentState) {
            case SPLASH:
                if (keyCode == KeyEvent.VK_ENTER)
                    navigateToMenu();
                break;
            case MENU:
                handleMenuInput(keyCode);
                break;
            case HIGH_SCORES:
                if (keyCode == KeyEvent.VK_ESCAPE)
                    navigateToMenu();
                break;
            case PLAYING:
                handleGameInput(keyCode);
                // Fall-through
            case PAUSED:
                if (keyCode == KeyEvent.VK_P)
                    togglePause();
                if (keyCode == KeyEvent.VK_ESCAPE)
                    navigateToMenu();
                break;
            case GAME_OVER:
                if (keyCode == KeyEvent.VK_ENTER || keyCode == KeyEvent.VK_ESCAPE)
                    navigateToMenu();
                break;
        }
        updateView();
    }

    /**
     * Remove "Opções"
     */
    private void handleMenuInput(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_UP:
                selectedMenuIndex = (selectedMenuIndex - 1 + MENU_OPTIONS.length) % MENU_OPTIONS.length;
                break;
            case KeyEvent.VK_DOWN:
                selectedMenuIndex = (selectedMenuIndex + 1) % MENU_OPTIONS.length;
                break;
            case KeyEvent.VK_ENTER:
                executeMenuOption(selectedMenuIndex);
                break;
            case KeyEvent.VK_1:
                executeMenuOption(0);
                break; // Novo Jogo
            case KeyEvent.VK_2:
                executeMenuOption(1);
                break; // Highscore
            case KeyEvent.VK_3:
                executeMenuOption(2);
                break; // Sair (era 4)
        }
    }

    /**
     * Remove "Opções"
     */
    private void executeMenuOption(int index) {
        switch (index) {
            case 0:
                restartGameLogic();
                break; // Novo Jogo
            case 1:
                currentState = GameState.HIGH_SCORES; // Highscore
                mainPanel.remove(scorePanel);
                gamePanel.setPreferredSize(new Dimension(MENU_WIDTH, GAME_HEIGHT));
                frame.pack();
                frame.setLocationRelativeTo(null);
                break;
            case 2:
                System.exit(0);
                break; // Sair (era 3)
        }
    }

    private void handleGameInput(int keyCode) {
        if (currentState != GameState.PLAYING)
            return;
        switch (keyCode) {
            case KeyEvent.VK_LEFT:
                movePiece(-1, 0);
                break;
            case KeyEvent.VK_RIGHT:
                movePiece(1, 0);
                break;
            case KeyEvent.VK_DOWN:
                movePiece(0, 1);
                break;
            case KeyEvent.VK_UP:
                rotatePiece();
                break;
            case KeyEvent.VK_SPACE:
                fallPiece();
                break;
            case KeyEvent.VK_C:
                swapHeldPiece();
                break;
        }
    }

    private void swapHeldPiece() {
        if (!canHold || currentPiece == null)
            return;
        Tetromino pieceToHold = currentPiece.clone();
        if (heldPiece == null) {
            heldPiece = pieceToHold;
            currentPiece = getNextPieceInQueue();
        } else {
            Tetromino temp = heldPiece;
            heldPiece = pieceToHold;
            currentPiece = temp;
            currentPiece.setX(Board.WIDTH / 2 - 2);
            currentPiece.setY(0);
        }
        canHold = false;
        updateView();
    }

    /**
     * **MÉTODO ATUALIZADO** - Correção do bug `engine.isGameActive`
     */
    public Tetromino getGhostPiece() {
        // AQUI ESTAVA O BUG: trocado "engine.isGameActive()" por "isGameActive()"
        if (currentPiece == null || !isGameActive())
            return null;

        Tetromino ghost = currentPiece.clone();
        int y = ghost.getY();
        while (board.isValidPosition(ghost, ghost.getX(), y + 1, ghost.getShape())) {
            y++;
        }
        ghost.setY(y);
        return ghost;
    }

    private void movePiece(int dx, int dy) {
        if (currentPiece == null)
            return;
        int newX = currentPiece.getX() + dx;
        int newY = currentPiece.getY() + dy;
        if (board.isValidPosition(currentPiece, newX, newY, currentPiece.getShape())) {
            currentPiece.setX(newX);
            currentPiece.setY(newY);
        } else if (dy == 1) {
            solidifyAndSpawnNewPiece();
        }
    }

    private void rotatePiece() {
        if (currentPiece == null)
            return;
        int[][] originalShape = currentPiece.getShape();
        currentPiece.rotate();
        if (!board.isValidPosition(currentPiece, currentPiece.getX(), currentPiece.getY(), currentPiece.getShape())) {
            currentPiece.shape = originalShape;
        }
    }

    private void fallPiece() {
        if (currentPiece == null)
            return;
        while (board.isValidPosition(currentPiece, currentPiece.getX(), currentPiece.getY() + 1,
                currentPiece.getShape())) {
            currentPiece.setY(currentPiece.getY() + 1);
        }
        solidifyAndSpawnNewPiece();
    }

    private void solidifyAndSpawnNewPiece() {
        board.solidifyPiece(currentPiece);
        int linesCleared = board.clearLines();
        if (linesCleared > 0) {
            score.addScore(linesCleared);
        }

        // Pega a próxima peça
        currentPiece = getNextPieceInQueue();
        canHold = true;

        // Verifica o Game Over COM a nova peça
        if (!board.isValidPosition(currentPiece, currentPiece.getX(), currentPiece.getY(), currentPiece.getShape())) {
            currentState = GameState.GAME_OVER;
            gameTimer.stop();
            // Define currentPiece como null para que não seja desenhada na tela de Fim de
            // Jogo
            currentPiece = null;

            int finalScore = score.getScore();
            String playerName = JOptionPane.showInputDialog(frame,
                    "FIM DE JOGO! Pontuação: " + finalScore + "\nInsira seu nome para o Highscore:",
                    "Fim de Jogo", JOptionPane.QUESTION_MESSAGE);

            if (playerName != null && !playerName.trim().isEmpty()) {
                if (playerName.length() > 10) {
                    playerName = playerName.substring(0, 10);
                }
                highscoreManager.addScore(playerName, finalScore);
            }
        }
    }

    private void updateView() {
        gamePanel.repaint();
        scorePanel.repaint();
    }

    // Getters
    public Board getBoard() {
        return board;
    }

    public Tetromino getCurrentPiece() {
        return currentPiece;
    }

    public Queue<Tetromino> getNextPieces() {
        return nextPieces;
    }

    public Tetromino getHeldPiece() {
        return heldPiece;
    }

    public Score getScore() {
        return score;
    }

    public HighscoreManager getHighscoreManager() {
        return highscoreManager;
    }

    public GameState getCurrentState() {
        return currentState;
    }

    public boolean isPaused() {
        return currentState == GameState.PAUSED;
    }

    public boolean isGameOver() {
        return currentState == GameState.GAME_OVER;
    }

    // Helper para saber se o jogo está em um estado "ativo" (jogando ou pausado)
    public boolean isGameActive() {
        return currentState == GameState.PLAYING || currentState == GameState.PAUSED;
    }

    public int getSelectedMenuIndex() {
        return selectedMenuIndex;
    }
}