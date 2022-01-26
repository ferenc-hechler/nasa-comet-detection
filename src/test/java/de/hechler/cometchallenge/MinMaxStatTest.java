package de.hechler.cometchallenge;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MinMaxStatTest {

	@Test
	void testGetSigma() {
		MinMaxStat mms = new MinMaxStat();
		mms.update(12);
		mms.update(55);
		mms.update(74);
		mms.update(79);
		mms.update(90);
		assertTrue(Math.abs(mms.getSigma()-27.444)<0.001);
	}

	@Test
	void testGetStdErr() {
		MinMaxStat mms = new MinMaxStat();
		mms.update(12);
		mms.update(55);
		mms.update(74);
		mms.update(79);
		mms.update(90);
		assertTrue(Math.abs(mms.getStdErr()-12.273)<0.001);
	}

}
