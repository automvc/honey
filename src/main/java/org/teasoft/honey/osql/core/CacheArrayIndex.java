/*
 * Copyright 2013-2019 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;


/**
 * Cache Array Index.
 * @author Kingstar
 * @since  1.4
 */
public class CacheArrayIndex {
	

	private int low;  //低对应的是较久的
	private int high;
	private int know;  //若确定know已超时，则low到know之间都超时
	
	private static int size;
	
	private static int startDeleteCacheRate;  //when timeout use
	private static int fullUsedRate;      //when add element in cache use
	
	private static int fullClearCacheSize; 
	
	static{
		startDeleteCacheRate=(int) (HoneyConfig.getHoneyConfig().cache_startDeleteRate*100);  //转成百分比
		fullUsedRate=(int) (HoneyConfig.getHoneyConfig().cache_fullUsedRate*100); //转成百分比
		size=HoneyConfig.getHoneyConfig().cache_mapSize;
		fullClearCacheSize=(int) (HoneyConfig.getHoneyConfig().cache_fullClearRate *size);
	}
	
	public synchronized int getLow() {
		return low;
	}
	public synchronized void setLow(int low) {
		this.low = low;
	}
	public int getHigh() {
		return high;
	}

	public int getKnow() {
		return know;
	}
/*	public synchronized void setKnow(int know) {
		
		 this.know = know;
//		if(low<=high)
//		  this.know = know;
//		else{ //循环的情况
//			if( low < know  || know < high   ){
//				this.know = know;
//			}
//		}
	}*/
	
	public int getUsedSize(){
		int t=getHigh()-getLow();
		if(t>=0) return t;
		else return t+size;
	}
	
	public int getEmptySize(){
		return size-getUsedSize();
	}
	
	/**
	 * @return used rate(0-100)  (eg: 80,  mean: 80%)
	 */
	public int getUsedRate(){
		return (getUsedSize()*100)/size;
	}
	
	public synchronized int getNext(){
//		return (high++)%size;
		if(high>=size){  //high是标识已使用的下一个元素(即第一个可用元素)
			high=1;  //下一个可以元素的下标为1
			return 0;
		}else{
		  return high++;
		}
	}
	
	public boolean isFull(){
		return getEmptySize()==0;
	}
	
	public boolean isWouldbeFull(){
		return getUsedRate() > fullUsedRate;
	}
	
	public int getDeleteCacheIndex(){
		return (getLow()+ fullClearCacheSize)%size;
	}
	
	public boolean isStartDelete(){
		return getUsedRate()>startDeleteCacheRate;
	}

}
