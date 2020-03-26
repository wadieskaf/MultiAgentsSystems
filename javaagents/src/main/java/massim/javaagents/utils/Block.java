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
}