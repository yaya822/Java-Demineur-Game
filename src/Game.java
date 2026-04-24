public class Game {
    private Board board;
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

    public void start(int x,int y){
        this.board=new Board(x,y);
        this.board.generateMins(8);
        this.board.calculateAdj(0, 0);
    }
    
    public boolean chekWin(){
        for(int i=0;i<this.board.getRows();i++){
            for(int j=0;j<this.board.getCols();j++){
                Cellule cell =this.board.getGrid()[i][j];
                    if(!cell.isMine() && !cell.isReveald())
                            return false;
            }
        }
        return true;
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

}