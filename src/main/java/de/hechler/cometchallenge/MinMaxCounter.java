package de.hechler.cometchallenge;

public class MinMaxCounter {

	private int min;
	private int max;
	private int count;
	private int total;

	public MinMaxCounter() {
		this(Integer.MAX_VALUE, Integer.MIN_VALUE, 0, 0);
	}

	public MinMaxCounter(int min, int max, int count, int total) {
		this.min = min;
		this.max = max;
		this.count = count;
		this.total = total;
	}

	public MinMaxCounter(MinMaxCounter other) {
		this(other.min, other.max, other.count, other.total);
	}

	public MinMaxCounter update(int value) {
		min = Math.min(min, value);
		max = Math.max(max, value);
		count += 1;
		total += value;
		return this;
	}

	public MinMaxCounter update(MinMaxCounter other) {
		min = Math.min(min, other.min);
		max = Math.max(max, other.max);
		count += other.count;
		total += other.total;
		return this;
	}
	
	public int getMin() {
		return min;
	}

	public int getMax() {
		return max;
	}

	public int getTotal() {
		return total;
	}

	public int getCount() {
		return count;
	}

	public int getAvg() {
		return count == 0 ? 0 : total / count;
	}
	
	public double getDAvg() {
		return count == 0 ? 0.0 : ((double)total)/count;
	}
	
	@Override public String toString() { 
		return "[min="+min+",max="+max+",avg="+getAvg()+",cnt="+count+"]"; 
	}

	
}
