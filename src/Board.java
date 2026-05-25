import java.util.ArrayList;

public class Board {
    private Cellule[][] grid;
    private int rows;
    private int cols;
    private int totalMines;
    

    public Board(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.grid=new Cellule[rows][cols] ;
        for(int i=0;i<rows;i++){
            for(int j=0;j<cols;j++){
                this.grid[i][j]=new Cellule();
            }
        }
    }

    public Cellule[][] getGrid() {
        return grid;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public int getCols() {
        return cols;
    }

    public void setCols(int cols) {
        this.cols = cols;
    }

    public int getTotalMines() {
        return totalMines;
    }

    public void setTotalMines(int totalMines) {
        this.totalMines = totalMines;
    }

    //methode qui verifier une cellule est ce qu'elle est a l'interieur du board ou pas 
    public boolean inBounds(int row,int col){
        if(row<0 || row>=this.rows 
            || col<0 || col>=this.cols) return false;
        return true;
    }
    // methode qui genere les mines d'une maniere aleatoire 
    public void generateMins(int nbrMines){
        int i=0;
        while(i<nbrMines){
            int randomRow = (int)(Math.random() * this.rows);
            int randomCol = (int)(Math.random() * this.cols);
            if(!this.grid[randomRow][randomCol].isMine()){
                    this.getCellule(randomRow, randomCol).setMine(true);
                    i++;

            }
        }
         for(int r = 0; r < this.rows; r++) {
        for(int c = 0; c < this.cols; c++) {
            this.grid[r][c].setAdjacentMines(calculateAdj(r, c));
        }
    }
    }
    //methode qui calcule les nombres des mines qui se trouve a l'entourage d'une cellule
    public int  calculateAdj(int row,int col){
            int somme=0;
            for(int i=-1;i<=1;i++){
                for(int j=-1;j<=1;j++){
                    if(i==0 && j==0) continue;
                    
                    int newRow=row+i;
                    int newCol=col+j;
                    if(inBounds(newRow,newCol)){
                        if(grid[newRow][newCol].isMine()){
                            somme++;

                        }
                    }
                }

            }
            return somme;
    }
    //methode qui initialise chaque cellule en calculant le nombre des mine adjacent
    public void initializeAdjacency(){
        for(int i=0;i<this.rows;i++){
            for(int j=0;j<this.cols;j++){
                if(!this.grid[i][j].isMine()){
                    grid[i][j].setAdjacentMines(calculateAdj(i, j));
                }
            }
        }
    }

    public Cellule getCellule(int i,int j){
        return grid[i][j];
    }

      

    public boolean isMine(int row,int col){
        return this.grid[row][col].isMine();
    }

    public boolean checkAllCellaRevealed(){
        for (int i=0;i<rows;i++){
            for (int j=0;j<cols;j++){
                if(!grid[i][j].isReveald() && !grid[i][j].isMine())
                    return false;
            }
        }
        return true;
    }
    
}
