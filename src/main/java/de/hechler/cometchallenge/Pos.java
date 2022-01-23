package de.hechler.cometchallenge;

import java.util.Objects;

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
	
	public int getSQDist(Pos other) {
		return (other.x-x)*(other.x-x)+(other.y-y)*(other.y-y);
	}
	
	public double getDist(Pos other) {
		return Math.sqrt(getSQDist(other));
	}
	
	public int getManhattenDist(Pos other) {
		return Math.abs(other.x-x)+Math.abs(other.y-y);
	}
	
	@Override public String toString() { return "("+x+","+y+")"; }
	@Override
	public int hashCode() {
		return Objects.hash(x, y);
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof Pos))
			return false;
		Pos other = (Pos) obj;
		return x == other.x && y == other.y;
	}
	public void add(Pos other) {
		x += other.x;
		y += other.y;
	}

	
	
}
