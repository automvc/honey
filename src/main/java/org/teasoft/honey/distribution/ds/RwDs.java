/*
 * Copyright 2016-2020 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.distribution.ds;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.teasoft.bee.distribution.ds.Route;
import org.teasoft.bee.osql.SuidType;
import org.teasoft.bee.osql.exception.NoConfigException;
import org.teasoft.honey.osql.core.HoneyConfig;
import org.teasoft.honey.osql.core.HoneyContext;
import org.teasoft.honey.osql.core.StringConst;

/**
 * 一个写数据库,多个读数据库.One Write DB,more read DB.
 * @author Kingstar
 * @since  1.8
 */
public class RwDs implements Route{

	private String writeDd;
	private List<String> readDsList;

	private static int count = 0;
	private static int max_cout = Integer.MAX_VALUE - 1000000;
	private static Random r = new Random();
	private byte lock[] = new byte[0];
	
	private int r_routeWay;
	
	{  //非static,每次new都会执行.若writer有更新,这样可以刷新.
		init();
	}
	
	private void init(){
		String wDB=HoneyConfig.getHoneyConfig().multiDS_writeDB;
		String rDB=HoneyConfig.getHoneyConfig().multiDS_readDB;
		//要判断从配置文件拿来的信息不能为空。
		if( (wDB==null || "".equals(wDB.trim()))  ||  (rDB==null || "".equals(rDB.trim()))){
			throw new NoConfigException("Error: bee.dosql.multiDS.writeDB and bee.dosql.multiDS.readDB can not be null or empty when bee.dosql.multiDS.type=1! ");
		}
		
		wDB=wDB.trim();//v1.11
		setWriteDs(wDB);  
		setReadDsList(parseRDb(rDB));
		getReadDsList().remove(wDB); //写库不能放在只读库列表   若需要在主库中读取数据,可特指
		r_routeWay=HoneyConfig.getHoneyConfig().multiDS_rDbRouteWay; 
	}
	
	private List<String> parseRDb(String rDB_str){
		String s[]=rDB_str.split(",");
		List<String> rList=new ArrayList<>();
		for (int i = 0; i < s.length; i++) {
			rList.add(s[i].trim()); //v1.11
		}
		return rList;
	}
	
	@Override
	public String getDsName() {
		RouteStruct routeStruct = HoneyContext.getCurrentRoute();
		
		//V1.11 同一连接,默认走写库.  (前面有指定会用指定的)
		if (StringConst.tRue.equals(HoneyContext.getSameConnctionDoing())
		 || StringConst.tRue.equals(HoneyContext.getJdbcTranWriterDs())) {
			return getWriteDs();
		}
		
		if (routeStruct!=null && SuidType.SELECT == routeStruct.getSuidType()) {
			return getReadDs(r_routeWay);
		} else {
			return getWriteDs();
		}
	}

	public String getWriteDs() {
		return writeDd;
	}

	public void setWriteDs(String writeDs) {  //todo if master change, need update
		this.writeDd = writeDs;
	}
	
//	public String getReadDs() {
//		
//		return getReadDs(0); //rand
//	}

	public String getReadDs(int type) {

		int index = 0;
		if (type == 1)
			index = poll();
		else
			index = rand();
		
		return getReadDsList().get(index);
	}

	private int poll() {

		int size = readDsList.size();
		if (count > max_cout) {
			synchronized (lock) {
				if (count > max_cout) count = 0;
			}
		}
		int index = count % size;
		count++;

		return index;
	}

	private int rand() {
		int size = readDsList.size();
		int index = r.nextInt(size);

		return index;
	}

	public List<String> getReadDsList() {
		return readDsList;
	}

	public void setReadDsList(List<String> readDsList) {
		this.readDsList = readDsList;
	}

}
