/*
 * Copyright 2016-2020 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.distribution;

import org.teasoft.bee.distribution.GenId;
import org.teasoft.bee.distribution.Worker;

/**
 * OneTimeSnowflakeId，进一步改进了梨花算法。
 * 不依赖时间的梨花算法，Workerid应放在序号sequence的上一段，且应用SerialUniqueId算法，使ID不依赖于时间自动递增。
 * 使用不依赖时间的梨花算法OneTimeSnowflakeId，应保证各节点大概均衡轮流出号，这样入库的ID会比较有序，因此每个段号内的序列号不能太多。
 * 支持批获取ID号。可以一次取一批ID（即一个范围内的ID一次就可以获取了）。可以代替依赖DB的号段模式。
 * 应用订单号等有安全要求的场景,可随机不定时获取一些批的号码不用即可。
 * 
 * <pre>{@code
 * +------+----------------------+----------+-----------+-----------+
 * | sign |     time(second)     | segment  | workerid  | sequence  |
 * +------+----------------------+----------+-----------+-----------+
 *   1 bit        31 bits           9 bits     10 bits     13 bits
 * }</pre>
 * 
 * @author Kingstar
 * @since  1.7.3
 */
public class OneTimeSnowflakeId implements GenId {

	private Worker worker;

	//	private long timestamp; // second 312 bits   just start need the time.
	private long time; //second 312 bits
	private long segment = 0L;//一般会从分支3 获取初值
	private long workerId = getWorker().getWorkerId();
	private long sequence = 0L; //一般会从分支3 获取初值

	//以下三部分加起来要等于32位.
	private final long segmentBits = 9L;
	private final long workerIdBits = 10L;
	private final long sequenceBits = 13L;

	private final long timestampShift = workerIdBits + segmentBits + sequenceBits;
	private final long segmentShift = workerIdBits + sequenceBits;
	private final long workerIdShift = sequenceBits;

	private final long sequenceMask = ~(-1L << sequenceBits);
	private final long maxSegment = (1 << segmentBits) - 1;

	private long twepoch = 1483200000; // 单位：s    2017-01-01 (yyyy-MM-dd)

	public OneTimeSnowflakeId() {
		time = _curSecond() - twepoch;
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
		long r[] = new long[2];
		r[0] = getNextId();

//		sequence=sequence+sizeOfIds-1;
		sequence = sequence + sizeOfIds - 1 - 1; //r[0]相当于已获取了第一个元素
		if ((sequence >> sequenceBits) > 0) { // 超过序列位表示的最大值
			sequence = sequence & sequenceMask;
			if (segment >= maxSegment) { // 已用完
				time++;
				segment = 0L;
			} else {
				segment++;
			}
		}
		r[1] = getNextId();

		return r;
	}

	/**
	 * 返回id
	 * @return id number.
	 */
	private long getNextId() {
		sequence++;
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

}
