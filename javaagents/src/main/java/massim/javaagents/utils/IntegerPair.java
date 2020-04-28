package massim.javaagents.utils;

import java.util.Objects;

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
    public int hashCode() {
        return Objects.hash(x,y);
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

    public IntegerPair add(IntegerPair pair){
        return new IntegerPair(this.x + pair.getX(), this.y + pair.getY());
    }
    public IntegerPair subtract(IntegerPair pair) {
        return new IntegerPair(this.x - pair.getX(), this.y - pair.getY());
    }
    public IntegerPair inverse(){
        return new IntegerPair(-1 * this.x, -1 * this.y);
    }

    public Boolean check(int x, int y){
        return this.x > 0 && this.x < x && this.y>0 && this.y <y;
    }
    
    @Override
    public String toString(){
        return "(" + x + "," + y + ")";
    }

}
