/*
 * Copyright 2016-2020 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.distribution;

import org.teasoft.bee.distribution.GenId;
import org.teasoft.bee.distribution.Worker;

/**
 * <p>改进的雪花算法——姑且称为梨花算法(PearFlowerId)吧  （忽如一夜春风来，千树万树梨花开）。
 * <p>改进目标：解决雪花算法的时钟回拨问题；部分避免机器id重复时，号码冲突问题。

 * <pre>{@code
 * +------+----------------------+----------+-----------+-----------+
 * | sign |     time(second)     | segment  | workerid  | sequence  |
 * +------+----------------------+----------+-----------+-----------+
 *   1 bit        31 bits           9 bits     10 bits     13 bits
 * }</pre>
 * 
 * @author Kingstar
 * @since  1.8
 */
public class PearFlowerId implements GenId {

	private Worker worker;

	private long timestamp; // second 31 bits
	private long segment = 0L;//一般会从分支3 获取初值
	private long workerId = getWorker().getWorkerId(); 
	private long sequence = 0L;  //一般会从分支3 获取初值

	//以下三部分加起来要等于32位.
	private final long segmentBits = 9L;
	private final long workerIdBits = 10L;
	private final long sequenceBits = 13L;

	private final long timestampShift = workerIdBits + segmentBits + sequenceBits;
	private final long segmentShift =   workerIdBits + sequenceBits;
	private final long workerIdShift =  sequenceBits;
	
	private final long sequenceMask = ~(-1L << sequenceBits);
	private final long maxSegment=(1<<segmentBits)-1;
	
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
		r[0]=getNextId();
		
//		sequence=sequence+sizeOfIds-1;
		sequence=sequence+sizeOfIds-1-1; //r[0]相当于已获取了第一个元素
		if ((sequence >> sequenceBits) > 0) { // 超过序列位表示的最大值
			sequence = sequence & sequenceMask;
			if (segment >= maxSegment) { // 已用完
				lastTimestamp++; //批获取时,提前消费1s
				segment = 0L;
			} else {
				segment++;
			}
		}
		r[1]=getNextId();
		
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
//			sequence=sequence+524288 ; //TEST  19
//			sequence=sequence+262144 ; //TEST  18
//			sequence=sequence+131072 ; //TEST  17
//			sequence=sequence+8192 ; //TEST  13
			if ((sequence >> sequenceBits) > 0) { // 超过序列位表示的最大值
				if (segment >= maxSegment) { // 已用完，不能再派号
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

//		return ((timestamp - twepoch) << timestampShift) | (workerId << workerIdShift) | (segment << segmentShift) | (sequence);
		return ((timestamp - twepoch) << timestampShift) | (segment << segmentShift) | (workerId << workerIdShift)  | (sequence);

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
