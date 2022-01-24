package de.hechler.cometchallenge;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hechler.cometchallenge.analyze.ImageAnalyzer;
import de.hechler.cometchallenge.analyze.SequenceAnalyzer;
import de.hechler.cometchallenge.geometry.Pos;

public class CometPath {

	private List<CometPos> cometPositions;
	
	public CometPath(Date timestamp, Pos pos) {
		cometPositions = new ArrayList<>();
		cometPositions.add(new CometPos(timestamp, pos));
	}

	public CometPath(CometPath other) {
		cometPositions = new ArrayList<>(other.cometPositions);
	}

	public CometPath createNewIfInRange(Date timestamp, Pos pos) {
		if (cometPositions.size() > 1) {
			// TODO: handle rotation (e.g. comet0001)
			CometPos firstCP = cometPositions.get(0);
			CometPos lastCP = cometPositions.get(cometPositions.size()-1);
			double DT = lastCP.getTimestamp().getTime()-firstCP.getTimestamp().getTime();
			double DX = lastCP.getPosition().getX()-firstCP.getPosition().getX();
			double DY = lastCP.getPosition().getY()-firstCP.getPosition().getY();
			double dt = timestamp.getTime()-firstCP.getTimestamp().getTime();
			double dx = DX*dt/DT;
			double dy = DY*dt/DT;
			Pos expectedPos = new Pos(firstCP.getPosition().getX()+dx,firstCP.getPosition().getY()+dy);
			double mhDist = firstCP.getPosition().getManhattenDist(expectedPos);
			double mhWrong = pos.getManhattenDist(expectedPos);
			double mhTolerated = 5.0; // Math.min(5, 1+mhDist/2); 
			if (mhWrong > mhTolerated) {
				return null;
			}
			double sqDistFromLine = pos.getSQDistFromLine(firstCP.getPosition(), expectedPos);
			double sqTolerated = 2.0;
			if (sqDistFromLine > sqTolerated) {
				return null;
			}
		}
		CometPath result = new CometPath(this); 
		result.add(timestamp, pos);
		return result;
	}
	

	public void add(Date timestamp, Pos cometPos) {
		cometPositions.add(new CometPos(timestamp, cometPos));
	}
	
	public int getLength() {
		return cometPositions.size();
	}
	
	public CometPos getCometPosition(int i) {
		return cometPositions.get(i);
	}

	public double getDistError() {
		if (cometPositions.size() <= 2) {
			return 0;
		}
		CometPos firstCP = cometPositions.get(0);
		CometPos lastCP = cometPositions.get(cometPositions.size()-1);
		double DT = lastCP.getTimestamp().getTime() - firstCP.getTimestamp().getTime();
		double DX = lastCP.getPosition().getX() - firstCP.getPosition().getX();
		double DY = lastCP.getPosition().getY() - firstCP.getPosition().getY();
		double result = 0;
		for (int i=1; i<cometPositions.size()-2; i++) {
			CometPos testCP = cometPositions.get(i);
			double dt = testCP.getTimestamp().getTime() - firstCP.getTimestamp().getTime();
			double dx = DX*dt/DT;
			double dy = DY*dt/DT;
			Pos expectedPos = new Pos(firstCP.getPosition().getX()+dx, firstCP.getPosition().getY()+dy);
			result += expectedPos.getSQDist(testCP.getPosition());
		}
		return result/(cometPositions.size()-2);
	}

	public double getLineError() {
		if (cometPositions.size() <= 2) {
			return 0;
		}
		CometPos firstCP = cometPositions.get(0);
		CometPos lastCP = cometPositions.get(cometPositions.size()-1);
		double DT = lastCP.getTimestamp().getTime() - firstCP.getTimestamp().getTime();
		double DX = lastCP.getPosition().getX() - firstCP.getPosition().getX();
		double DY = lastCP.getPosition().getY() - firstCP.getPosition().getY();
		double result = 0;
		for (int i=1; i<cometPositions.size()-2; i++) {
			CometPos testCP = cometPositions.get(i);
			result += testCP.getPosition().getSQDistFromLine(firstCP.getPosition(), lastCP.getPosition());
		}
		return result/(cometPositions.size()-2);
	}

	
	@Override
	public String toString() {
		return cometPositions.toString();
	}

	public String toSubmissionText(SequenceAnalyzer analyzer) {
		StringBuilder result = new StringBuilder();
		String folderName = analyzer.getImageAnalyzer(0).getPath().getParent().getFileName().toString();
		result.append(folderName);
		for (CometPos cometPos:cometPositions) {
			ImageAnalyzer ia = analyzer.getImageAnalyzerForTimestamp(cometPos.getTimestamp());
			String xStr = String.format(Locale.ROOT, "%.2f", cometPos.getPosition().getX());
			String yStr = String.format(Locale.ROOT, "%.2f", cometPos.getPosition().getY());
			result.append(",").append(ia.getFilename())
					.append(",").append(xStr)
					.append(",").append(yStr);
		}
		double confidence = 1.0/(1.0+getDistError()+getLineError());
		String confStr = String.format(Locale.ROOT, "%.2f", confidence);
		result.append(",").append(confStr);
		return result.toString();
	}
	
}
