/*
 * Copyright 2016-2020 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.distribution;

import org.teasoft.bee.distribution.GenId;
import org.teasoft.bee.distribution.Worker;

/**
 * @author Kingstar
 * @since  1.7.3
 */
public class PearFlowerId implements GenId {

	private Worker worker;

	private long timestamp; // second 31
	private long workerId = getWorker().getWorkerId(); //10
	private long segment = 0L; // 3
	private long sequence = 1L; // 19

	private final long workerIdBits = 10L;
	private final long segmentBits = 3L;
	private final long sequenceBits = 19L;

	private final long timestampShift = workerIdBits + segmentBits + sequenceBits;
	private final long workerIdShift = segmentBits + sequenceBits;
	private final long segmentShift = sequenceBits;
	
	private final long sequenceMask = ~(-1L << sequenceBits);
	
	private final long halfWorkid=1<<(workerIdBits-1);
	private final long fullWorkid=1<<workerIdBits;

	private long twepoch = 1483200000; // 单位：s    2017-01-01 (yyyy-MM-dd)
	private long lastTimestamp = -1L;

	private static boolean useHalfWorkId = true;
	private static long tolerateSecond=10;
	private static long switchWorkIdTimeThreshold=120;

	public Worker getWorker() {
		if (this.worker == null) return new DefaultWorker();
		return worker;
	}

	public void setWorker(Worker worker) {
		this.worker = worker;
	}

	@Override
	public synchronized long get() {
		return getNextId();
	}

	@Override
	public synchronized long[] getRangeId(int sizeOfIds) {
		long r[]=new long[2];
		r[0]=get();
		
//		sequence=sequence+sizeOfIds-1;
		sequence=sequence+sizeOfIds-1-1; //r[0]相当于已获取了第一个元素
		if ((sequence >> sequenceBits) > 0) { // 超过19位表示的最大值
			sequence = sequence & sequenceMask;
			if (segment >= 7) { // 已用完
				lastTimestamp++; //批获取时,提前消费1s
				segment = 0L;
			} else {
				segment++;
			}
		}
		r[1]=get();
		
		return r;
	}

	/**
	 * 返回id,当id<0时,表示异常的id
	 * @return
	 */
	private long getNextId() {
		timestamp = currentSecond();
		if (timestamp < lastTimestamp) {//分支1:回拨 
//			if(tolerateSecond<=0) return -1;  //不允许回拨
			if(tolerateSecond<=0) return tolerateSecond=1;  //处理润秒问题,至少容忍1秒
			long offset = lastTimestamp - timestamp;
			if (offset <= tolerateSecond) {
				try {
					wait(400*tolerateSecond);
					timestamp = currentSecond();
					if (timestamp < lastTimestamp) { // 等待1次后仍是回拨
						wait(600*tolerateSecond+10);
						timestamp = currentSecond();
						if (timestamp < lastTimestamp) { // 等待两次后还是回拨,则返回异常id
							return -1L;
						}// 否则 走分支2或3
					} // 否则 走分支2或3
				} catch (InterruptedException e) {
					return -2;
				}
			} else {// 回拨时，超过容忍的秒数误差

				if (offset > switchWorkIdTimeThreshold) {
					if (useHalfWorkId) {
						switchWorkerId();
						lastTimestamp = -1L;
					}else{
						return -1L;
					}
				} else {
					try {
						wait(tolerateSecond*500); // 等待一半容忍的秒数.循环调用时,可慢慢减小误差
						timestamp = currentSecond();
						if (timestamp < lastTimestamp) {
							return -3L;  //回拨太大
						}// 否则 走分支2或3
					} catch (InterruptedException e) {
						return -2;
					}
				}
			}
		}// 分支1:回拨  结束

		if (timestamp == lastTimestamp) { // 分支2
			sequence++;
			if ((sequence >> sequenceBits) > 0) { // 超过19位表示的最大值
				if (segment >= 7) { // 已用完，不能再派号
					try {
						wait(100);
					} catch (InterruptedException e) {
						return -2;
					}

					return getNextId();
				} else {
					sequence = 0L;
					segment++;
				}
			}
		} else if (timestamp > lastTimestamp) {// 分支3
			segment = 0L;
			sequence = 0L;
			lastTimestamp = timestamp; // 记录新的一秒重新开始
		}

		return ((timestamp - twepoch) << timestampShift) | (workerId << workerIdShift) | (segment << segmentShift) | (sequence);

	}

	private long currentSecond() {
		return (System.currentTimeMillis()) / 1000L;
	}

	// 系统运行时，检测到时钟回拨比较大时,将切换workerid
	private void switchWorkerId() {
//		                               +  512) % 1024; 
		this.workerId = (this.workerId + halfWorkid) % fullWorkid;// 将workerid切换到空闲部分
	}

}
