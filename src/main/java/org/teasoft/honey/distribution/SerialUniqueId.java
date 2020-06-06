/*
 * Copyright 2016-2020 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.distribution;

import java.util.concurrent.atomic.AtomicLong;

import org.teasoft.bee.distribution.GenId;
import org.teasoft.bee.distribution.Worker;

/**
 * 在一个workid内连续唯一的ID生成方法.Serial Unique Id in one workid.
 * 优点:连续唯一;不强依赖时钟. 在DB内实现,可达到分布式全局唯一ID在DB内自增长.
 * Advantages:continuous and unique;less clock dependent.Implemented in dB, it can achieve auto increment of distributed global unique ID in dB.
 * 缺点/Shortcoming:worker1's ID<worker2's ID...<worker1023's ID.
 * @author Kingstar
 * @since  1.7.3
 */
public class SerialUniqueId implements GenId{
	
	private AtomicLong sequenceNumber= null;
	
	private Worker worker;

	private long workerId = getWorker().getWorkerId(); //10
	private long timestamp; // second 31
	private final long segment = 0L; // 3
	private final long sequence = 1L; // 19
	
//	private final long workerIdBits = 10L;
	private final long timeBits=31L;
	private final long segmentBits = 3L;
	private final long sequenceBits = 19L;
	
	private final long workerIdShift = sequenceBits+segmentBits+timeBits;
	private final long timestampLeftShift = sequenceBits+segmentBits;
	private final long segmentShift = sequenceBits;


	private long twepoch = 1483200000; // 单位：s    2017-01-01 (yyyy-MM-dd)
	
	/**
	 * !注意:每次新建都会重置开始号码.
	 */
	public SerialUniqueId() {

		timestamp = _curSecond();
		long num = (workerId << workerIdShift) | ((timestamp - twepoch) << timestampLeftShift) | (segment << segmentShift) | (sequence);
		sequenceNumber = new AtomicLong(num);
		
//		sequenceNumber = new AtomicLong(1);
	}
	
	private long _curSecond() {
		return (System.currentTimeMillis()) / 1000L;
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
		 return sequenceNumber.getAndIncrement();
	}

	@Override
	public synchronized long[] getRangeId(int sizeOfIds) {
		long r[]=new long[2];
		r[0]=sequenceNumber.getAndIncrement();
		r[1]=r[0]+sizeOfIds-1;
		setAtomicLong(r[0]+sizeOfIds);
		
		return r;
	}
	
	private void setAtomicLong(long newNum){
		sequenceNumber.set(newNum);
	}

}
