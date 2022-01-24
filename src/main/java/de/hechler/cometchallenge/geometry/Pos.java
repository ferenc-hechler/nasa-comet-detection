package de.hechler.cometchallenge.geometry;

import java.util.Objects;

public class Pos {

	private double x;
	private double y;

	public Pos() {
		this(0, 0);
	}
	public Pos(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public double getX() { return x; }
	public double getY() { return y; }
	
	public double getSQDist(Pos other) {
		return (other.x-x)*(other.x-x)+(other.y-y)*(other.y-y);
	}
	
	public double getDist(Pos other) {
		return Math.sqrt(getSQDist(other));
	}
	
	public double getManhattenDist(Pos other) {
		return Math.abs(other.x-x)+Math.abs(other.y-y);
	}
	
	public double getSQDistFromLine(Pos lineP1, Pos lineP2) {
		double x0 = getX();
		double x1 = lineP1.getX();
		double x2 = lineP2.getX();
		double y0 = getY();
		double y1 = lineP1.getY();
		double y2 = lineP2.getY();
		// see: https://en.wikipedia.org/wiki/Distance_from_a_point_to_a_line#Line_defined_by_two_points
		double numerator = (x2-x1)*(y1-y0) - (x1-x0)*(y2-y1);
		double denominatorSQ = (x2-x1)*(x2-x1) + (y2-y1)*(y2-y1);  
		if (denominatorSQ==0) { // lineP1 is equal to lineP2, return distance to this point 
			return getSQDist(lineP1);
		}
		return numerator*numerator/denominatorSQ;
	}
	
	public double getDistFromLine(Pos lineP1, Pos lineP2) {
		return Math.sqrt(getSQDistFromLine(lineP1, lineP2));
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
