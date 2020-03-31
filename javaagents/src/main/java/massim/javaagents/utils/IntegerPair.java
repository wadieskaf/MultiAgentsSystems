package massim.javaagents.utils;

public class IntegerPair {
    private int x;
    private int y;

    public IntegerPair(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
    
    @Override
    public boolean equals(Object o){
        if(o == this) return true;
        
        if(!(o instanceof IntegerPair)){
            return false;
        }
        
        IntegerPair t = (IntegerPair)o;
        return x == t.x && y == t.y;
    }

}
