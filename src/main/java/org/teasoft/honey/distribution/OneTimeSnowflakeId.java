/*
 * Copyright 2016-2020 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.distribution;

import org.teasoft.bee.distribution.GenId;
import org.teasoft.bee.distribution.Worker;
import org.teasoft.honey.osql.core.Logger;

/**
 * OneTimeSnowflakeId，进一步改进了梨花算法。
 * 不依赖时间的梨花算法，Workerid应放在序号sequence的上一段，且应用SerialUniqueId算法，使ID不依赖于时间自动递增。
 * 使用不依赖时间的梨花算法OneTimeSnowflakeId，应保证各节点大概均衡轮流出号，这样入库的ID会比较有序，因此每个段号内的序列号不能太多。
 * 支持批获取ID号。可以一次取一批ID（即一个范围内的ID一次就可以获取了）。可以代替依赖DB的号段模式。
 * 应用订单号等有安全要求的场景,可随机不定时获取一些批的号码不用即可。
 * 
 * [<delete> 可间隔时间缓存时间值到文件，供重启时设置回初值用。若不缓存，则重启时，又用目前的时间点，重设开始的ID。只要平均不超过419w/s，重启时造成的时
 * 钟回拨都不会有影响（但却浪费了辛苦攒下的每秒没用完的ID号）。要是很多时间都超过419w/s，证明你有足够的能力做这件事：间隔时间缓存时间值到文件。</delete>]
 * 考虑到2019年双11的峰值不超过55万笔/秒, 因此419w/s这个值已可以满足此苛刻要求;采用testSpeedLimit()检测平均值不超过419w/s这个值即可,而且在空闲时
 * 段省下的ID号,还可以在高峰时使用。
 * 
 * 
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
public class OneTimeSnowflakeId implements GenId {

	private Worker worker;
	private long startTime;
	
	private long time; //second 31 bits   just start need the time.
	private long segment = 0L;
	private long workerId = getWorker().getWorkerId();
	private long sequence = 0L; 

	//以下三部分加起来要等于32位.
	private static final long segmentBits = 9L;
	private static final long workerIdBits = 10L;
	private static final long sequenceBits = 13L;

	private static final long timestampShift = workerIdBits + segmentBits + sequenceBits;
	private static final long segmentShift = workerIdBits + sequenceBits;
	private static final long workerIdShift = sequenceBits;

//	private static final long sequenceMask = ~(-1L << sequenceBits);
	private static final long maxSegment = (1L << segmentBits) - 1L;
	private static final long maxSequence = 1L<<sequenceBits;

	private long twepoch = 1483200000; // 单位：s    2017-01-01 (yyyy-MM-dd)
	private long _counter=0;

	public OneTimeSnowflakeId() {
		startTime=_curSecond();
		time = startTime - twepoch;
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
		long id=getNextId();
		testSpeedLimit();
		return id;
	}

	@Override
	public synchronized long[] getRangeId(int sizeOfIds) {
		
		if(sizeOfIds>maxSequence) {
			Logger.error("parameter sizeOfIds("+sizeOfIds+") greate maxSequence("+maxSequence+") will cause range Id do not continue!");
			return null;
		}
		
		long r[] = new long[2];
		r[0] = getNextId();

		sequence = sequence + sizeOfIds - 1 - 1; //r[0]相当于已获取了第一个元素
		_counter=_counter + sizeOfIds - 1 - 1;
		if ((sequence >> sequenceBits) > 0) { // 超过序列位表示的最大值
//			sequence = sequence & sequenceMask;
			if (segment >= maxSegment) { // 已用完
				time++;
				segment = 0L;
			} else {
				segment++;
			}
			
			sequence=0;
			//取max时,超过序列位表示的最大值,不连续,要重新获取
			return getRangeId(sizeOfIds); 
		}
		r[1] = getNextId(); //r[0]到r[1]是加1递增的吗? 不是. 因workid在太低位,会跳跃. v1.9,在往上两行重新设置segment和sequence,让其在segment内连续
		testSpeedLimit();
		return r;
	}

	/**
	 * 返回id
	 * @return id number.
	 */
	private long getNextId() {
		sequence++; 
		_counter++; 
		if ((sequence >> sequenceBits) > 0) { // 超过序列位表示的最大值
			if (segment >= maxSegment) { // 已用完,自动用下一秒的
				time++;
			} else {
				sequence = 0L;
				segment++;
			}
		}
		return (time << timestampShift) | (segment << segmentShift) | (workerId << workerIdShift) | (sequence);
	}

	private long _curSecond() {
		return (System.currentTimeMillis()) / 1000L;
	}
	
//	private void testSpeedLimit() {
	private synchronized void testSpeedLimit() {
		long spentTime=_curSecond() - startTime + 1;
		if (spentTime > 0) {
			if ((spentTime << (segmentBits + sequenceBits)) > _counter) return; //check some one workerid.
		}
		try {
			wait(10);
			testSpeedLimit();
		} catch (Exception e) {
			Logger.error(e.getMessage());
		}
	}

}