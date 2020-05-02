package massim.javaagents.utils;

//This class is used for block as well as dispensers
public class Block extends Thing {
	private String type;

	public Block(int x, int y, String type) {
		super(x, y);
		this.type = type;
	}

	public String getType() {
		return type;
	}
	
	@Override
	public boolean equals(Object o){
		if(o == this) return true;
        
        if(!(o instanceof Block || o instanceof IntegerPair)){
            return false;
		}
		
		if(o instanceof IntegerPair){
			IntegerPair t = (IntegerPair)o;
			return x == t.getX() && y == t.getY();
		}
        
        Block t = (Block)o;
        return x == t.x && y == t.y && type.equals(t.type);
	}
}