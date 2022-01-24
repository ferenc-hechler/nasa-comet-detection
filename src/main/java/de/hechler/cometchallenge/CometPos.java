package de.hechler.cometchallenge;

import java.text.SimpleDateFormat;
import java.util.Date;

public class CometPos {

	private Date timestamp;
	private Pos position;

	public CometPos(Date timestamp, Pos position) {
		this.timestamp = timestamp;
		this.position = position;
	}
	
	public Pos getPosition() {
		return position;
	}
	
	public Date getTimestamp() {
		return timestamp;
	}
	
	@Override
	public String toString() {
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		return position.toString()+"-"+sdf.format(timestamp);
	}
	
}
