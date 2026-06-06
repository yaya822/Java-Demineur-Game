package game;
import model.Board;
import model.Cellule;

public class Game {

    private  Board board;
    private boolean isGameover;
    private boolean firstClick = true; // pour ne pas cliquer  la premiere click sur une mine 

    public Board getBoard() {
        return board;
    }
    public void setBoard(Board board) {
        this.board = board;
    }
    public boolean isGameover() {
        return isGameover;
    }
    public void setGameover(boolean isGameover) {
        this.isGameover = isGameover;
    }

    // methode pour demarer le jeu 
    public void start(int rows, int cols, int mines) {
    board = new Board(rows, cols);
    board.generateMins(mines);
    board.initializeAdjacency();
    isGameover = false;
    firstClick = true;
}
    // methode qui verifier est ce que tous les cellules nom mines sont  revelees
    public boolean checkWin(){
        int revealedCount = 0;

            for(int i = 0; i < this.board.getRows(); i++) {
                for(int j = 0; j < this.board.getCols(); j++) {
                    Cellule cell = this.board.getGrid()[i][j];

                    if(!cell.isMine() && !cell.isReveald())
                        return false;

                    if(cell.isReveald() && !cell.isMine())
                        revealedCount++;
                }
            }

            return revealedCount > 0;
    }
    // methode qui changer l'etat d'une cellule en toggle 
    public void toggleFlage(int x,int y){
        if(this.board.inBounds(x, y)){
            Cellule cell =this.board.getGrid()[x][y];
            if(!cell.isReveald()){
                if(cell.isFlagged())
                    cell.setFlagged(false);
                else 
                    cell.setFlagged(true);
            }
        }
    }
    // methode qui verifier est ce qu'une mine est revelee 
     public boolean  checkGameOver(){
        for(int i=0;i<this.board.getRows();i++){
            for(int j=0;j<this.board.getCols();j++){
                Cellule cell =this.board.getGrid()[i][j];
                if(cell.isMine() && cell.isReveald()){
                    return true;
                }
            }
        }
        return false ;
    }
    // methode pour que le premier click ne soit pas une mine 
    private void makeFirstClickSafe(int row, int col) {
        Cellule clickedCell = board.getCellule(row, col);

        if (!clickedCell.isMine()) {
            return;
        }
        clickedCell.setMine(false);
        int newRow;
        int newCol;

        do {
            newRow = (int)(Math.random() * board.getRows());
            newCol = (int)(Math.random() * board.getCols());
        } while (
            (newRow == row && newCol == col) ||
            board.getCellule(newRow, newCol).isMine()
        );

        board.getCellule(newRow, newCol).setMine(true);
        board.initializeAdjacency();
    }
    // methode qui releve une cellule (si une cellule cliquer est vide il relleve son entourage d'une maniere recurcive )
    public void revealCell(int row, int col){
        
        if(!board.inBounds(row, col)) return;

        Cellule cell = board.getCellule(row, col);
        if (cell.isReveald() || cell.isFlagged()) return;

        if (firstClick) {
            makeFirstClickSafe(row, col);
            firstClick = false;
            cell = board.getCellule(row, col);
        }

        cell.setReveald(true);
        if(cell.isEmpty()){

            for(int i=-1;i<=1;i++){
                for(int j=-1;j<=1;j++){
                    if(i==0 && j==0) continue;   
                        revealCell(row+i, col+j);
                }
            }
        }
        if(cell.isMine()){
            this.setGameover(true);
            return;
        }
        
    }

    public Cellule getCellule(int row,int col){
         return board.getCellule(row, col);
    }
    
    public int getAdjCells(int row,int col){
        return board.calculateAdj(row, col);
    }
    
}
