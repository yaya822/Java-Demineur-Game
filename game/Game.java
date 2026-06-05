package game;
import model.Board;
import model.Cellule;

public class Game {
    private  Board board;
    private boolean isGameover;

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

    public   void start(int rows,int cols,int mines){
        board=new Board(rows,cols);
        board.generateMins(mines);
        board.initializeAdjacency();
    }
    
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
    public void revealCell(int row, int col){
        if(!board.inBounds(row, col)) return;

        Cellule cell = board.getCellule(row, col);
        if(cell.isReveald() || cell.isFlagged()) return;
        
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