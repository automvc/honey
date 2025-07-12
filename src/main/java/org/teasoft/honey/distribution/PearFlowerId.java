/*
 * Copyright 2016-2020 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.distribution;

import java.util.concurrent.ThreadLocalRandom;

import org.teasoft.bee.distribution.GenId;
import org.teasoft.bee.distribution.Worker;
import org.teasoft.honey.osql.core.HoneyConfig;
import org.teasoft.honey.osql.core.Logger;

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

	private ThreadLocalRandom random = ThreadLocalRandom.current();
	private boolean isBatch = false;

	private Worker worker;

	private long timestamp; // second 31 bits
	private long segment = 0L;// 一般会从分支3 获取初值
	private long workerId = getWorker().getWorkerId();
	private long sequence = 0L; // 一般会从分支3 获取初值

	// 以下三部分加起来要等于32位.
	private static final long segmentBits = 9L;
	private static final long workerIdBits = 10L;
	private static final long sequenceBits = 13L;

	private static final long timestampShift = workerIdBits + segmentBits + sequenceBits;
	private static final long segmentShift = workerIdBits + sequenceBits;
	private static final long workerIdShift = sequenceBits;

//	private static final long sequenceMask = ~(-1L << sequenceBits);
	private static final long maxSegment = (1L << segmentBits) - 1L;
	private static final long maxSequence = 1L << sequenceBits;

	private static final long halfWorkid = 1 << (workerIdBits - 1);
	private static final long fullWorkid = 1 << workerIdBits;

//	private long startSecond = 1483200000; // 2017-01-01 (yyyy-MM-dd) ,   单位 unit (s)    
	private long startSecond = Start.getStartSecond();
	private long lastTimestamp = -1L;

	private static volatile boolean useHalfWorkId;
	private static long tolerateSecond = 10;
	private static long switchWorkIdTimeThreshold = 120;
	private static int randomNumBound;

	{
		boolean t_useHalfWorkId = HoneyConfig.getHoneyConfig().pearFlowerId_useHalfWorkId;
		long t_tolerateSecond = HoneyConfig.getHoneyConfig().pearFlowerId_tolerateSecond;
		long t_switchWorkIdTimeThreshold = HoneyConfig.getHoneyConfig().pearFlowerId_switchWorkIdTimeThreshold;

		useHalfWorkId = t_useHalfWorkId;
		if (t_tolerateSecond > 0) tolerateSecond = t_tolerateSecond;
		if (t_switchWorkIdTimeThreshold > 0) switchWorkIdTimeThreshold = t_switchWorkIdTimeThreshold;

		int t_randomNumBound = HoneyConfig.getHoneyConfig().pearFlowerId_randomNumBound;
		if (t_randomNumBound < 1 || t_randomNumBound > 512)
			randomNumBound = 2;
		else
			randomNumBound = t_randomNumBound;
	}

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
		if (sizeOfIds > maxSequence) {
			Logger.warn("parameter sizeOfIds(" + sizeOfIds + ") greate maxSequence(" + maxSequence + ") will cause range Id do not continue!");
			return null;
		}
		long r[] = new long[2];
		r[0] = getNextId();
//		sequence=sequence+sizeOfIds-1;
		sequence = sequence + sizeOfIds - 1 - 1; // r[0]相当于已获取了第一个元素
//		if ((sequence >> sequenceBits) > 0) { // 超过序列位表示的最大值
		if ((sequence + 1 >> sequenceBits) > 0) { // 超过序列位表示的最大值 提前将结束的那个数也计算入内 fixed V2.1
//			sequence = sequence & sequencreMask;
			if (segment >= maxSegment) { // 已用完
				lastTimestamp++; // 批获取时,提前消费1s
				segment = 0L;
			} else {
				segment++;
			}

			sequence = 0;
			// 取max时,超过序列位表示的最大值,不连续,要重新获取
			return getRangeId(sizeOfIds);
		}
		isBatch = true;
		r[1] = getNextId(); // 要去组装一个id
		return r;
	}

	/**
	 * 返回id,当id<0时,表示异常的id
	 * @return
	 */
	private synchronized long getNextId() {
		timestamp = currentSecond();
		if (timestamp < lastTimestamp) {// 分支1:回拨
//			if(tolerateSecond<=0) return -1;  //不允许回拨
//			if(tolerateSecond<=0) return tolerateSecond=1;  //处理润秒问题,至少容忍1秒  bug
			if (tolerateSecond <= 0) tolerateSecond = 1; // 处理润秒问题,至少容忍1秒
			long offset = lastTimestamp - timestamp;
			if (offset <= tolerateSecond) {
				try {
					wait(400 * tolerateSecond);
					timestamp = currentSecond();
					if (timestamp < lastTimestamp) { // 等待1次后仍是回拨
						wait(600 * tolerateSecond + 10);
						timestamp = currentSecond();
						if (timestamp < lastTimestamp) { // 等待两次后还是回拨,则返回异常id
							return -1L;
						} // 否则 走分支2或3
					} // 否则 走分支2或3
				} catch (InterruptedException e) {
					Logger.warn(e.getMessage(), e);
					Thread.currentThread().interrupt();
					return -2;
				} catch (Exception e) {
					Logger.warn(e.getMessage());
					return -2;
				}
			} else {// 回拨时，超过容忍的秒数误差

				if (offset > switchWorkIdTimeThreshold) {
					if (useHalfWorkId) {
						switchWorkerId();
						lastTimestamp = -1L;
					} else {
						return -1L;
					}
				} else {
					try {
						wait(tolerateSecond * 500); // 等待一半容忍的秒数.循环调用时,可慢慢减小误差
						timestamp = currentSecond();
						if (timestamp < lastTimestamp) {
							return -3L; // 回拨太大
						} // 否则 走分支2或3
					} catch (InterruptedException e) {
						Logger.warn(e.getMessage(), e);
						Thread.currentThread().interrupt();
						return -2;
					} catch (Exception e) {
						Logger.warn(e.getMessage());
						return -2;
					}
				}
			}
		} // 分支1:回拨 结束

		if (timestamp == lastTimestamp) { // 分支2 新的if
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
						Logger.warn(e.getMessage(), e);
						Thread.currentThread().interrupt();
						return -2;
					} catch (Exception e) {
						Logger.warn(e.getMessage());
						return -2;
					}

					return getNextId();
				} else {
//					sequence = 0L;
					setStartSequence();
					segment++;
				}
			}
		} else if (timestamp > lastTimestamp) {// 分支3
			segment = 0L;
//			sequence = 0L;
			setStartSequence();
			lastTimestamp = timestamp; // 记录新的一秒重新开始
		}

//		return ((timestamp - twepoch) << timestampShift) | (workerId << workerIdShift) | (segment << segmentShift) | (sequence);
		return ((timestamp - startSecond) << timestampShift) | (segment << segmentShift) | (workerId << workerIdShift) | (sequence);
	}

	private void setStartSequence() { // 批获取时,是否为引起数量不够 ?????? 不会
		if (isBatch) {
			isBatch = false;
			sequence = 0L;
		} else if (randomNumBound == 1) {
			sequence = 0L;
		} else {
			sequence = random.nextInt(randomNumBound);
		}
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