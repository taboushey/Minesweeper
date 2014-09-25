package minesweeper;

/*
 * @author Tabetha Boushey
 * version: 1/20/2012
 */

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class Minesweeper extends JPanel implements AWTEventListener, ActionListener {
    
    private static final int totalBombs = 10;
    private int rows = 9, columns = 9, total = (rows * columns);
    private JPanel board = new JPanel(new GridLayout(rows, columns));
    private JLabel bombCount = new JLabel(totalBombs + "");
    private boolean setColors = false;
    private JButton resetButton = new JButton("Reset");
   
     public Minesweeper() {
        setLayout(new BorderLayout());
        add(board, BorderLayout.CENTER);
        newButtons();
        addControlPanel();
        Toolkit.getDefaultToolkit().addAWTEventListener(this,AWTEvent.KEY_EVENT_MASK);
    }
     
    public static enum Status {                                                 // Enum used to see the stage of the 
        clicked, checked, initial, markedWrong                                  // game you are in.
    }

    public static enum GameState {
        haventStarted, playing, finished
    }
    private GameState state = GameState.haventStarted;
    
    private void restartGame() {
        state = GameState.haventStarted;
        board.removeAll();
        newButtons();
        board.updateUI();
        bombCount.setText("" + totalBombs);
        bombCount.updateUI();
    }
    
    private void startThread() {
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                while (state == GameState.playing) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        th.start();
    }
    
private void newButtons() {
        List<Point> newBombLocation = new ArrayList<>();
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                JButton btn = getButton(newBombLocation, total, new Point(row, column) {
                    @Override
                    public String toString() {
                        return (int) getX() + ", " + (int) getY();
                    }
                    @Override
                    public boolean equals(Object obj) {
                        return ((Point) obj).getX() == getX() && ((Point) obj).getY() == getY();
                    }
                    @Override
                    public int hashCode() {
                        int hash = 7;
                        return hash;
                    }
                });
                board.add(btn);
            }
        }
        while (newBombLocation.size() < totalBombs) {
            refreshBombs(newBombLocation, board.getComponents());
        }
        for (Component c : board.getComponents()) {
            updateBombCount((GameButton) c, board.getComponents());
        }
    }
   
    private void refreshBombs(List<Point> newBombLocation, Component[] components) {
        Random newRandom = new Random();
        for (Component c : components) {
            Point location = ((GameButton) c).getPosition();
            int currentPosition = new Double(((location.x) * columns) + location.getY()).intValue();
            int bombLocation = newRandom.nextInt(total);
            if (bombLocation == currentPosition) {
                ((GameButton) c).setBomb(true);
                newBombLocation.add(((GameButton) c).getPosition());
                return;
            }
        }
    }

    private GameButton getButton(List<Point> lstBombsLocation, int totalLocations, Point location) {
        GameButton button = new GameButton(location);
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setFocusable(false);
        if (lstBombsLocation.size() < totalBombs) {
            if (isBomb()) {
                button.setBomb(true);
                lstBombsLocation.add(location);
            }
        }
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                if (state != GameState.playing) {
                    state = GameState.playing;
                    startThread();
                }
                if (((GameButton) mouseEvent.getSource()).isEnabled() == false) {
                    return;
                }
                if (mouseEvent.getButton() == MouseEvent.BUTTON1) {
                    if (((GameButton) mouseEvent.getSource()).getState() == Status.checked) {
                        ((GameButton) mouseEvent.getSource()).setState(Status.initial);
                        bombCount.setText((Long.parseLong(bombCount.getText()) + 1) + "");
                        ((GameButton) mouseEvent.getSource()).updateUI();
                        return;
                    }
                    ((GameButton) mouseEvent.getSource()).setState(Status.clicked);
                    if (((GameButton) mouseEvent.getSource()).isBomb()) {
                        blastBombs();
                        return;
                    } else {
                        if (((GameButton) mouseEvent.getSource()).totalBombCount() == 0) {
                            surroundingZeros(((GameButton) mouseEvent.getSource()).getPosition());
                        }
                    }
                    if (!checkForWin()) {
                        ((GameButton) mouseEvent.getSource()).setEnabled(false);
                    }
                } else if (mouseEvent.getButton() == MouseEvent.BUTTON3) {
                    if (((GameButton) mouseEvent.getSource()).getState() == Status.checked) {
                        ((GameButton) mouseEvent.getSource()).setState(Status.initial);
                        bombCount.setText((Long.parseLong(bombCount.getText()) + 1) + "");
                    } else {
                        ((GameButton) mouseEvent.getSource()).setState(Status.checked);
                        bombCount.setText((Long.parseLong(bombCount.getText()) - 1) + "");
                    }
                }
                ((GameButton) mouseEvent.getSource()).updateUI();
            }
        });
        return button;
    }

    private boolean checkForWin() {
        boolean win = false;
        for (Component c : board.getComponents()) {
            GameButton b = (GameButton) c;
            if (b.getState() != Status.clicked) {
                if (b.isBomb()) {
                    win = true;
                } else {
                    return false;
                }
            }
        }
        if (win) {
            state = GameState.finished;
            for (Component c : board.getComponents()) {
                GameButton b = (GameButton) c;
                if (b.isBomb()) {
                    b.setState(Status.checked);
                }
                b.setEnabled(false);
            }
            JOptionPane.showMessageDialog(this, "You win the game ", "Congrats", JOptionPane.INFORMATION_MESSAGE, null);
        }
        return win;
    }

    private void surroundingZeros(Point currentPoint) {
        Point[] points = getSurroundings(currentPoint);
        for (Point p : points) {
            GameButton b = buttonLocation(board.getComponents(), p);
            if (b != null && b.totalBombCount() == 0 && b.getState() != Status.clicked && b.getState() != Status.checked && b.isBomb() == false) {
                b.setState(Status.clicked);
                surroundingZeros(b.getPosition());
                b.updateUI();
            }
            if (b != null && b.totalBombCount() > 0 && b.getState() != Status.clicked && b.getState() != Status.checked && b.isBomb() == false) {
                b.setEnabled(false);
                b.setState(Status.clicked);
                b.updateUI();
            }
        }
    }

    private void blastBombs() {
        int blastCount = 0;
        for (Component c : board.getComponents()) {
            ((GameButton) c).setEnabled(false);
            ((GameButton) c).transferFocus();
            if (((GameButton) c).isBomb() && ((GameButton) c).getState() != Status.checked) {
                ((GameButton) c).setState(Status.clicked);
                ((GameButton) c).updateUI();
                blastCount++;
            }
            if (((GameButton) c).isBomb() == false && ((GameButton) c).getState() == Status.checked) {
                ((GameButton) c).setState(Status.markedWrong);
            }
        }
        bombCount.setText("" + blastCount);
        bombCount.updateUI();
        state = GameState.finished;
        JOptionPane.showMessageDialog(this, "You loose the game ", "Game Over", JOptionPane.ERROR_MESSAGE, null);
        for (Component c : board.getComponents()) {
            GameButton b = (GameButton) c;
            b.setEnabled(false);
        }
    }

    private boolean isBomb() {
        Random r = new Random();
        return r.nextInt(rows) == 1;
    }

    public static void main(String... args) {
        JFrame fr = new JFrame("MineSweeper");
        fr.setLayout(new BorderLayout());
        fr.add(new Minesweeper());
        fr.setResizable(false);
        fr.setSize(250, 350);
        fr.setLocationRelativeTo(null);
        fr.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        fr.setVisible(true);
    }

    class GameButton extends JButton {

        private boolean isBomb = false;
        private Point position = null;
        private int bombCount = 0;
        private Status state = Status.initial;

        public void setState(Status state) {
            this.state = state;
            if (totalBombCount() == 0 && !isBomb) {
                setEnabled(false);
            }
        }

        public Status getState() {
            return state;
        }

        public int totalBombCount() {
            return bombCount;
        }

        public GameButton(Point position) {
            setPosition(position);
            setText(position.toString());
        }

        public Point getPosition() {
            return position;
        }

        public void setPosition(Point position) {
            this.position = position;
        }

        public void setBombCount(int bombCount) {
            this.bombCount = bombCount;
        }
        
        public boolean isBomb() {
            return isBomb;
        }

        public void setBomb(boolean isBomb) {
            this.isBomb = isBomb;
        }

        @Override
        public String getText() {
            if (state == Status.initial) {
                return "";
            }
            if (state == Status.checked) {
                return "\u00B6";
            }
            if (state == Status.clicked) {
                if (isBomb) {
                    return "<html><font size='20'><b>*</b></font></html>";
                } else {
                    if (totalBombCount() > 0) {
                        return totalBombCount() + "";
                    } else {
                        return "";
                    }
                }
            }
            if (state == Status.markedWrong) {
                return "X";
            }
            return super.getText();
        }

        @Override
        public Color getBackground() {
            if (setColors && isBomb) {
                return Color.red;
            }
            if (state == Status.clicked) {
                if (isBomb) {
                    return Color.blue;
                }
                if (totalBombCount() > 0) {
                    return Color.black;
                }
            }
            if (isEnabled()) {
                return Color.gray.brighter();
            } else {
                return super.getBackground();
            }
        }
    }

    private Point[] getSurroundings(Point cPoint) {
        int cX = (int) cPoint.getX();
        int cY = (int) cPoint.getY();
        Point[] points = {new Point(cX - 1, cY - 1), new Point(cX - 1, cY), new Point(cX - 1, cY + 1), new Point(cX, cY - 1), new Point(cX, cY + 1), new Point(cX + 1, cY - 1), new Point(cX + 1, cY), new Point(cX + 1, cY + 1)};
        return points;
    }

    private void updateBombCount(GameButton btn, Component[] components) {
        Point[] points = getSurroundings(btn.getPosition());
        for (Point p : points) {
            GameButton b = buttonLocation(components, p);
            if (b != null && b.isBomb()) {
                btn.setBombCount(btn.totalBombCount() + 1);
            }
        }
        btn.setText(btn.totalBombCount() + "");
    }

    private GameButton buttonLocation(Component[] components, Point position) {
        for (Component btn : components) {
            if ((((GameButton) btn).getPosition().equals(position))) {
                return (GameButton) btn;
            }
        }
        return null;
    }

    @Override
    public void eventDispatched(AWTEvent event) {
        if (KeyEvent.class.isInstance(event) && ((KeyEvent) (event)).getID() == KeyEvent.KEY_RELEASED) {
            if (((KeyEvent) (event)).getKeyCode() == KeyEvent.VK_F1) {
            }
            if (((KeyEvent) (event)).getKeyCode() == KeyEvent.VK_F2) {
                restartGame();
            }
            if (((KeyEvent) (event)).getKeyCode() == KeyEvent.VK_F3) {
                setColors = !setColors;
                if (state == GameState.playing) {
                    board.updateUI();
                }
            }
            if (((KeyEvent) (event)).getKeyCode() == KeyEvent.VK_F12) {
                for (Component c : board.getComponents()) {
                    GameButton b = (GameButton) c;
                    if (b.isBomb() == false) {
                        b.setState(Status.clicked);
                    } else {
                        b.setState(Status.checked);
                    }
                    b.setEnabled(false);
                }
                checkForWin();
            }
        }
    }
private void addControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.add(bombCount);
        panel.add(resetButton);
        JPanel newPanel = new JPanel(new GridLayout(1, 3));
        newPanel.add(bombCount);
        newPanel.add(panel);
        add(newPanel, BorderLayout.NORTH);
        resetButton.addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if (actionEvent.getSource() == resetButton) {
            restartGame();
        }
    }
}
