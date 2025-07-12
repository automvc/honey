/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.util;

import java.util.concurrent.atomic.AtomicInteger;

import org.teasoft.honey.osql.core.HoneyConfig;
import org.teasoft.honey.osql.core.Logger;
import org.teasoft.honey.osql.util.DateUtil;

/**
 * 
 * 组成:1位符号位+22位时间(分)+9位序列号
 * 每分钟可出512个int序列号,可透支两分钟(共512*3). 约可用7.98年
 * 本身只支持单点出号.
 * 
 * @author Kingstar
 * @since  1.17
 */
public class IntSerialId {

	private AtomicInteger sequenceNumber = null;
	private static final long sequence = 1L; // 19

	private long startTime; // second 31

	private static final long shift = 9;

	private long startSecond = getStartSecond();

	private int initNum;

	/**
	 * !注意:每次新建都会重置开始号码.
	 */
	public IntSerialId() {
		startTime = _curSecond();
		long ll = (startTime - startSecond) / 60; // 单位：分钟
		initNum = (int) ((ll << shift) | sequence);
		sequenceNumber = new AtomicInteger(initNum);
	}

	private long _curSecond() {
		return (System.currentTimeMillis()) / 1000L;
	}

	public synchronized int get() {
		int id = sequenceNumber.getAndIncrement();

		testSpeedLimit(id);
		return id;
	}

	/**
	 * 批量获取int类型id.sizeOfIds通常不应该大于1536
	 * @param sizeOfIds
	 * @return
	 */
	public synchronized int[] getRangeId(int sizeOfIds) {
		int r[] = new int[2];
		int a = sequenceNumber.getAndIncrement();
		r[0] = a;
		r[1] = r[0] + sizeOfIds - 1;
		setSequenceNumber(a + sizeOfIds);

		// 使用乐观锁的话,会浪费ID号

		testSpeedLimit(r[1]);

		return r;
	}

	private void setSequenceNumber(int newNum) {
		sequenceNumber.set(newNum);
	}

	private synchronized void testSpeedLimit(long currentLong) {

		long spentTime = (_curSecond() - startTime) / 60 + 1 + 2; // 可透支两分钟

		if (spentTime > 0) {
			if ((spentTime << shift) > (currentLong - initNum)) return;
		}
		try {
			wait(10);
			testSpeedLimit(currentLong);
		} catch (InterruptedException e) {
			Logger.warn(e.getMessage(), e);
			Thread.currentThread().interrupt();
		} catch (Exception e) {
			Logger.warn(e.getMessage());
		}
	}

	private static long defaultStart = 1640966400; // 单位：s 2022-01-01 (yyyy-MM-dd)

	private long getStartSecond() {
		int startYear = HoneyConfig.getHoneyConfig().genid_startYear;
		if (startYear < 1970) return defaultStart;

		long newTime = DateUtil.toTimestamp(startYear + "-01-01 00:00:00").getTime();
		return newTime / 1000;
	}

}
