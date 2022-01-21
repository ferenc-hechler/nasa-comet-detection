package de.hechler.cometchallenge;

public class Pos {

	private int x;
	private int y;

	public Pos() {
		this(0, 0);
	}
	public Pos(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public int getX() { return x; }
	public int getY() { return y; }
	
	@Override public String toString() { return "("+x+","+y+")"; }
	
}
