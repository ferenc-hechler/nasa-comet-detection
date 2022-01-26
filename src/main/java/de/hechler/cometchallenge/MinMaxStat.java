package de.hechler.cometchallenge;

import java.util.ArrayList;
import java.util.List;

public class MinMaxStat {

	private double min;
	private double max;
	private List<Double> values;
	private double total;

	public MinMaxStat() {
		this(Integer.MAX_VALUE, Integer.MIN_VALUE, new ArrayList<>(), 0);
	}

	public MinMaxStat(double min, double max, List<Double> values, double total) {
		this.min = min;
		this.max = max;
		this.values = values;
		this.total = total;
	}

	public MinMaxStat(MinMaxStat other) {
		this(other.min, other.max, other.values, other.total);
	}

	public MinMaxStat update(double value) {
		min = Math.min(min, value);
		max = Math.max(max, value);
		values.add(value);
		total += value;
		return this;
	}

	public MinMaxStat update(MinMaxStat other) {
		min = Math.min(min, other.min);
		max = Math.max(max, other.max);
		values.addAll(other.values);
		total += other.total;
		return this;
	}
	
	public double getMin() {
		return min;
	}

	public double getMax() {
		return max;
	}

	public double getTotal() {
		return total;
	}

	public double getCount() {
		return values.size();
	}

	public double getAvg() {
		return values.isEmpty() ? 0.0 : total / values.size();
	}
	
	public double getSQErr() {
		double avg = getAvg();
		double sqErr = 0;
		for (double value:values) {
			sqErr += (value-avg)*(value-avg);
		}
		return sqErr;
	}
	
	public double getErr() {
		return Math.sqrt(getSQErr());
	}
	
	public double getSQSigma() {
		return getSQErr() / values.size();
	}
	
	public double getSigma() {
		return Math.sqrt(getSQSigma());
	}
	
	public double getStdErr() {
		return getErr()/getCount();
	}
	
	@Override public String toString() { 
		return "[min="+min+",max="+max+",avg="+getAvg()+",sigma="+getSigma()+",cnt="+getCount()+"]"; 
	}

	
}
