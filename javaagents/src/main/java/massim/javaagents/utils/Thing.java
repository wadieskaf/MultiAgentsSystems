package massim.javaagents.utils;

public class Thing{
    protected int x, y;
    
    public Thing(int x, int y){
        this.x = x;
        this.y = y;
    }
    
    public int getX(){
        return x;
    }
    
    public int getY(){
        return y;
    }
    
    @Override
    public boolean equals(Object o){
        if(o == this) return true;
        
        if(!(o instanceof Thing)){
            return false;
        }
        
        Thing t = (Thing)o;
        return x == t.x && y == t.y;
    }
}
