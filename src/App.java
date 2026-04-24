public class App {
    public static void main(String[] args) throws Exception {
        Board board=new Board(5, 5);
        
        
        board.generateMins(10);

        //System.out.println(board.getCellule(3, 4).isMine());
        for(int i = 0; i < 5; i++){
            for(int j = 0; j < 5; j++){
                System.out.print(board.getGrid()[i][j].isMine() ? "M " : "0 " );
            }
            System.out.println();
        }
    }
}
