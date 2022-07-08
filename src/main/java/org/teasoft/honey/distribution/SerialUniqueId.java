/*
 * Copyright 2016-2020 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.distribution;

import java.util.concurrent.atomic.AtomicLong;

import org.teasoft.bee.distribution.GenId;
import org.teasoft.bee.distribution.Worker;
import org.teasoft.honey.osql.core.Logger;

/**
 * 在一个workerid内连续唯一的ID生成方法(绝对连续单调递增，全局唯一).Serial Unique Id in one workerid.
 * 优点:连续唯一;不依赖时钟. 在DB内实现,可达到分布式全局唯一ID在DB内自增长;在同一个workerid内,获取的ID号,可以满足连续单调递增唯一.
 * Advantages:continuous and unique;less clock dependent.Implemented in dB, it can achieve auto increment 
 * of distributed global unique ID in dB. The ID number get in the same workerid can satisfy continuous
 * monotonic increasing uniqueness.
 * 缺点/Shortcoming:worker1's ID<worker2's ID...<worker1023's ID.
 * 
 * SerialUniqueId:绝对连续单调递增，全局唯一.
 * 分布式环境下生成连续单调递增(在一个workerid内),且全局唯一数字id.
 * 连续单调递增ID生成算法SerialUniqueId：不依赖于时间，也不依赖于任何第三方组件，只是启动时，用一个时间作为第一个ID设置的种子，
 * 设置了初值ID后，就可获取并递增ID。在一台DB内与传统的一样，连续单调递增（而不只是趋势递增），而代表DB的workerid作为DB的区别放在高位，
 * 从所有DB节点看，则满足分布式DB生成全局唯一ID。本地（C8 I7 16g）1981ms可生成1亿个ID号,利用上批获取，分隔业务，每秒生成过亿ID号
 * 不成问题。可用作分布式DB内置生成64位long型ID自增主键。只要按本算法设置了记录的ID初值，然后默认让数据库表id主键自增就可以（如MYSQL）。
 * 绝对连续单调递增，全局唯一的方案(可用于DB表主键)，如下：
 * 只能是在新增一个库时，就分配一个库的workerid. 然后在初始化表时，设置初始ID开始用的值，以后由DB自动增长。Workerid的分配可统一放在一个
 * 配置文件，由工具检测到某个表是空表，且使用的主键对应的是Java的long型时，设置初始ID开始用的值。
 * 
 * 考虑到2019年双11的峰值不超过55万笔/秒, 因此419w/s这个值已可以满足此苛刻要求;采用testSpeedLimit()检测平均值不超过419w/s这个
 * 值即可,而且在空闲时段省下的ID号,还可以在高峰期时使用。
 
 * 这个都被大家忽略了:
 * DB表自增ID，也是可以改为具有分布式特性的，SerialUniqueId就是！

 * 
 * @author Kingstar
 * @since  1.8
 */
public class SerialUniqueId implements GenId{
	
	private AtomicLong sequenceNumber= null;
	
	private Worker worker;

	private long workerId = getWorker().getWorkerId(); //10
	private long timestamp; // second 31
	private static final long segment = 0L; // 3
	private static final long sequence = 1L; // 19
	
//	private final long workerIdBits = 10L;
	private static final long timeBits=31L;
	private static final long segmentBits = 3L;
	private static final long sequenceBits = 19L;
	
	private static final long workerIdShift = sequenceBits+segmentBits+timeBits;
	private static final long timestampLeftShift = sequenceBits+segmentBits;
	private static final long segmentShift = sequenceBits;


//	private long startSecond = 1483200000; // 单位：s    2017-01-01 (yyyy-MM-dd)
	private long startSecond = Start.getStartSecond();
	private long initNum;
	
	/**
	 * !注意:每次新建都会重置开始号码.
	 */
	public SerialUniqueId() {

		timestamp = _curSecond();
		initNum = (workerId << workerIdShift) | ((timestamp - startSecond) << timestampLeftShift) | (segment << segmentShift) | (sequence);
		sequenceNumber = new AtomicLong(initNum);
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
		 long id=sequenceNumber.getAndIncrement();
		 
		 testSpeedLimit(id);
		 return id;
	}

	@Override
	public synchronized long[] getRangeId(int sizeOfIds) {
		long r[]=new long[2];
		r[0]=sequenceNumber.getAndIncrement();
		r[1]=r[0]+sizeOfIds-1;
		setSequenceNumber(r[0]+sizeOfIds);
		
		//使用乐观锁的话,会浪费ID号
		
		testSpeedLimit(r[1]);
		
		return r;
	}
	
	private void setSequenceNumber(long newNum){
		sequenceNumber.set(newNum);
	}
	
	private synchronized void testSpeedLimit(long currentLong) {

		long spentTime = _curSecond() - timestamp + 1;

		if (spentTime > 0) {
			if ((spentTime << timestampLeftShift) > (currentLong - initNum)) return;
		}
		try {
			wait(10);
			testSpeedLimit(currentLong);
		} catch (Exception e) {
		  Logger.error(e.getMessage());
		}
	}

}
