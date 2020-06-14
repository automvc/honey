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
 * 在一个workid内连续唯一的ID生成方法(绝对连续单调递增，全局唯一).Serial Unique Id in one workid.
 * 优点:连续唯一;不强依赖时钟. 在DB内实现,可达到分布式全局唯一ID在DB内自增长.
 * Advantages:continuous and unique;less clock dependent.Implemented in dB, it can achieve auto increment of distributed global unique ID in dB.
 * 缺点/Shortcoming:worker1's ID<worker2's ID...<worker1023's ID.
 * 
 * SerialUniqueId:绝对连续单调递增，全局唯一.
 * 连续单调递增ID生成算法SerialUniqueId：不依赖于时间，也不依赖于任何第三方组件，只是启动时，用一个时间作为第一个ID设置的种子，
 * 设置了初值ID后，就可获取并递增ID。在一台DB内与传统的一样，连续单调递增（而不只是趋势递增），而代表DB的workerid作为DB的区别放在高位，
 * 从所有DB节点看，则满足分布式DB生成全局唯一ID。本地（C8 I7 16g）1981ms可生成1亿个ID号,利用上批获取，分隔业务，每秒生成过10亿个ID号
 * 不成问题，能满足双11的峰值要求。可用作分布式DB内置生成64位long型ID自增主键。只要按本算法设置了记录的ID初值，然后默认让数据库表id主键自
 * 增就可以（如MYSQL）。
 * 绝对连续单调递增，全局唯一的方案，如下：
 * 只能是由DBA等角色，在新增一个库时，就分配一个库的workerid. 然后在初始化表时，设置初始ID开始用的值，以后由DB自动增长。Workerid的分配
 * 可统一放在一个配置文件，由工具检测到某个表是空表，且使用的主键对应的是Java的long型时，设置初始ID开始用的值，以后由DB自动增长。
 * 
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
		long initNum = (workerId << workerIdShift) | ((timestamp - twepoch) << timestampLeftShift) | (segment << segmentShift) | (sequence);
		sequenceNumber = new AtomicLong(initNum);
		
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
		
		//使用乐观锁的话,会浪费ID号
		
		return r;
	}
	
	private void setAtomicLong(long newNum){
		sequenceNumber.set(newNum);
	}

}
