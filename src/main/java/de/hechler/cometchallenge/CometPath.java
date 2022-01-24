package de.hechler.cometchallenge;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CometPath {

	private List<CometPos> cometPositions;
	private long lastDX;
	private long lastDY;
	private long lastDT;
	
	public CometPath(Date timestamp, Pos pos) {
		cometPositions = new ArrayList<>();
		cometPositions.add(new CometPos(timestamp, pos));
		lastDX = 0;
		lastDY = 0;
		lastDT = 0;
	}

	public CometPath(CometPath other) {
		cometPositions = new ArrayList<>(other.cometPositions);
		lastDX = other.lastDX;
		lastDY = other.lastDY;
		lastDT = other.lastDT;
	}

	public CometPath createNewIfInRange(Date timestamp, Pos pos) {
		CometPos lastCP = cometPositions.get(cometPositions.size()-1);
		if (cometPositions.size() > 1) {
			long dt = timestamp.getTime()-lastCP.getTimestamp().getTime();
			int dx = (int)(lastDX*dt/lastDT);
			int dy = (int)(lastDY*dt/lastDT);
			Pos expectedPos = new Pos(lastCP.getPosition().getX()+dx,lastCP.getPosition().getY()+dy);
			int mhDist = lastCP.getPosition().getManhattenDist(expectedPos);
			int mhWrong = pos.getManhattenDist(expectedPos);
			int mhTolerated = 5; // Math.min(5, 1+mhDist/2); 
			if (mhWrong > mhTolerated) {
				return null;
			}
		}
		CometPath result = new CometPath(this); 
		result.cometPositions.add(new CometPos(timestamp, pos));
		result.lastDX = pos.getX()-lastCP.getPosition().getX();
		result.lastDY = pos.getY()-lastCP.getPosition().getY();
		result.lastDT = timestamp.getTime()-lastCP.getTimestamp().getTime();
		return result;
	}
	
	public int getLength() {
		return cometPositions.size();
	}

	@Override
	public String toString() {
		return cometPositions.toString();
	}
	
}
