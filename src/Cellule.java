public class Cellule {
        private boolean isMine;
        private boolean isReveald;
        private boolean isFlagged;
        private int adjacentMines;

        public boolean isMine() {
            return isMine;
        }
        public void setMine(boolean isMine) {
            this.isMine = isMine;
        }
        public boolean isReveald() {
            return isReveald;
        }
        public void setReveald(boolean isReveald) {
            this.isReveald = isReveald;
        }
        public boolean isFlagged() {
            return isFlagged;
        }
        public void setFlagged(boolean isFlagged) {
            this.isFlagged = isFlagged;
        }
        public int getAdjacentMines() {
            return adjacentMines;
        }
        public void setAdjacentMines(int adjacentMines) {
            this.adjacentMines = adjacentMines;
        }
        
        public void reveal(){
            if(!isReveald)
                this.isReveald=true;
        }
        public void toggleFlage(){
            if(!isFlagged)
                this.isFlagged=true;
        }


}
