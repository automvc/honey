/*
 * Copyright 2013-2018 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.honey.osql.core;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.bee.osql.ObjSQLException;
import org.bee.osql.SQL;

/**
 * 直接操作数据库，并返回结果.在该类中的sql字符串要是DB能识别的SQL语句
 * @author Kingstar
 * Create on 2013-6-30 下午10:32:53
 * @since  1.0
 */
public class SqlLib implements SQL{
	
	public SqlLib(){}

	private Connection getConn() throws SQLException {
		Connection conn = null;
		
		conn=HoneyContext.getCurrentConnection();  
		if(conn==null){
			try {
				conn=SessionFactory.getConnection(); //不开户事务时
			} catch (Exception e) {
				// TODO: handle exception
				Logger.print("Have Error when get the Connection: ", e.getMessage());
			}
		}
		
		return conn;
	}
	
	
	//要是写的sql对应的结构与entity的结构不一致,将会有问题
	@Override
	public <T> List<T> select(String sql,T entity ){
		
		Connection conn = null;
		PreparedStatement pst = null;
		T targetObj = null;
		List<T> rsList =null;
		try{
		conn = getConn();
		pst = conn.prepareStatement(sql); 
		
		setPreparedValues(pst,sql);

		ResultSet rs = pst.executeQuery();
		rsList= new ArrayList<T>();
		
		Field field[] = entity.getClass().getDeclaredFields();
		int columnCount=field.length;
		
		while (rs.next()) {
			
			targetObj = (T)entity.getClass().newInstance();
			for (int i = 0; i < columnCount; i++) {

				field[i].setAccessible(true); 
				field[i].set(targetObj,rs.getObject(transformStr(field[i].getName())));
			}
			rsList.add(targetObj);
		 } 
	   }
		catch(SecurityException se){
			System.err.println("---------the SqlLib.Select  -----------SecurityException:  "+se.getMessage());
		}catch(SQLException e){
			System.err.println("---------the SqlLib.Select -----------SQLException:  "+e.getMessage());
		}catch(IllegalAccessException e){
			System.err.println("---------the SqlLib.Select -----------IllegalAccessException:  "+e.getMessage());
		}catch(InstantiationException e){
			System.err.println("---------the SqlLib.Select -----------InstantiationException:  "+e.getMessage());
		}finally{
			checkClose(pst, conn);
		}

		entity=null;
		targetObj=null;

		return rsList;
	}

	@Override
	public <T> List<T> selectSomeField(String sql,T entity ){
		
		Connection conn = null;
		PreparedStatement pst = null;
		T targetObj = null;
		List<T> rsList =null;
		try{
		conn = getConn();
		pst = conn.prepareStatement(sql); 
		
		setPreparedValues(pst,sql);

		ResultSet rs = pst.executeQuery();
		ResultSetMetaData rmeta = rs.getMetaData();
		int columnCount = rmeta.getColumnCount();
		rsList= new ArrayList<T>();
		
		Field field[] = entity.getClass().getDeclaredFields();
		
		while (rs.next()) {
			
			targetObj = (T)entity.getClass().newInstance();
			for (int i = 0; i < columnCount; i++) {
				try{
				field[i] = entity.getClass().getDeclaredField(transformField(rmeta.getColumnName(i + 1))); 
				}catch(NoSuchFieldException e){
					System.err.println(e.getMessage());
				}
				field[i].setAccessible(true); 
				field[i].set(targetObj, rs.getObject(i + 1));  //对相应Field设置
				
				
			}
			rsList.add(targetObj);
		} 
		
		}
		catch(SecurityException se){
			System.err.println("---------the SqlLib.selectSomeField  -----------SecurityException:  "+se.getMessage());
		}catch(SQLException e){
			System.err.println("---------the SqlLib.selectSomeField  -----------SQLException:  "+e.getMessage());
		}catch(IllegalAccessException e){
			System.err.println("---------the SqlLib.selectSomeField  -----------IllegalAccessException:  "+e.getMessage());
		}catch(InstantiationException e){
			System.err.println("---------the SqlLib.selectSomeField  -----------InstantiationException:  "+e.getMessage());
		}finally{
			checkClose(pst, conn);
		}

		entity=null;
		targetObj=null;
		
		return rsList;
	}
	
	/**
	 * SQL function: max,min,avg,sum,count. 如果统计的结果集为空,除了count返回0,其它都返回空字符.
	 */
	@Override
	public String selectFun(String sql) throws ObjSQLException {
		String result=null;
		Connection conn = null;
		PreparedStatement pst = null;
		ResultSet rs=null;
		try{
		conn = getConn();
		pst = conn.prepareStatement(sql);
		
		setPreparedValues(pst,sql);
		
		 rs = pst.executeQuery();
		 if(rs.next()) {      //if
//				result= rs.getString(1); 
				if(rs.getObject(1)==null) result="";
				else result= rs.getObject(1).toString(); 
		 } 
			
			rs.last();
			if(rs.getRow()>1) {
				throw new ObjSQLException("ObjSQLException:The size of ResultSet more than 1.");
//				throw new ObjSQLException("ObjSQLException:Miss The Field! The entity("+tableName+") don't contain the field:"+ FieldForFun);
			}
			
			
		} catch (SQLException e) {
			System.err.println("===============selectFun: "+ e.getMessage());
		}finally{
			checkClose(pst, conn);
		}
		
		return result;
	}
	
	
    @Override
	public List<String[]> select(String sql) {
		List<String[]> list = new ArrayList<String[]>();
		Connection conn = null;
		PreparedStatement pst = null;
		ResultSet rs = null;

		try {
			conn = getConn();
			pst = conn.prepareStatement(sql);
			
			setPreparedValues(pst,sql);
			
			rs = pst.executeQuery();
			ResultSetMetaData rmeta = rs.getMetaData();
			int columnCount = rmeta.getColumnCount();
			String str[]=null;
			while(rs.next()) {
				str=new String[columnCount];
				for (int i = 0; i < columnCount; i++) {
					str[i]=rs.getString(i+1);
				}
				list.add(str);
			}

		} catch (SQLException e) {
			System.err.println("========= List<String> select: " + e.getMessage());
		} finally {
			checkClose(pst, conn);
		}

		return list;
	}



	/*
	 * include insert,delete and update.
	 */
	//对应jdbc的executeUpdate方法
	@Override	
	public int modify(String sql){ 
		int num = 0;
		Connection conn = null;
		PreparedStatement pst = null;
		try{
		conn = getConn();
		pst = conn.prepareStatement(sql);
		
		setPreparedValues(pst,sql);
		
		num = pst.executeUpdate(); //该语句必须是一个 SQL 数据操作语言（Data Manipulation Language，DML）语句
		                            //，比如 INSERT、UPDATE 或 DELETE 语句；或者是无返回内容的 SQL 语句，比如 DDL 语句。
		} catch (SQLException e) {
			System.err.println("===============SqlLib.modify: "+ e.getMessage());
		} finally{
		  checkClose(pst, conn);
		}
		return num;
	}
	
	 @Override
	public String selectJson(String sql) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public int[] batch(String sql[]){
		int len=sql.length;
		int batchSize=HoneyConfig.getHoneyConfig().getBatchSize();
				
		if(len>batchSize)
			return batch(sql,batchSize);
		else 
			return batch(sql,0,len);
	}
  @Override		
  public int[] batch(String insertSql[],int batchSize){
		int len = insertSql.length;
		int total[]=new int[len];
		int part[]=new int[batchSize];
		if (len <= batchSize)
		 return	batch(insertSql);
		else {
			for (int i = 0; i < len/batchSize; i++) {
				part=batch(insertSql,i*batchSize,(i+1)*batchSize);  
				total=HoneyUtil.mergeArray(total,part,i*batchSize,(i+1)*batchSize);
			}
			
		}
		if(len%batchSize !=0){  //尾数不成批
			int t2[]=batch(insertSql,len-(len%batchSize),len);
			total=HoneyUtil.mergeArray(total,t2,len-(len%batchSize),len);
		}
		
		return total;
	}
	
	private static String index1="[index";
	private static String index2="]";
	private int[] batch(String sql[] , int start,int end) {
		int a[]=new int[end-start];
		Connection conn = null;
		PreparedStatement pst = null;
		try{
		conn = getConn();
		conn.setAutoCommit(false);
		
		pst = conn.prepareStatement(sql[0]);
		for(int i=start;i<end;i++){ //start... (end-1)
			
			setPreparedValues(pst,sql[0]+index1+i+index2);
			pst.addBatch();
		}
		a=pst.executeBatch(); //一次性提交      若两条数据,有一条插入不成功,返回0,0.  但实际上又有一第成功插入数据库(mysql测试,在自动提交的时候会有问题,不是自动提交不会)
		conn.commit();
		
		}catch(SQLException e){
			System.err.println("==================SqlLib.batch=============:"+e.getMessage());
		}finally{
			checkClose(pst, conn);
		}
		
		return a;
	}
	
	protected void checkClose(Statement stmt, Connection conn) {

		if (stmt != null) {
			try {
				stmt.close();
			} catch (SQLException e) {
				e.printStackTrace();
				System.err.println("-----------SQLException in checkClose------"+e.getMessage());
			}
		}
		try {
		  if (conn != null  && conn.getAutoCommit()) {//自动提交时才关闭.如果开启事务,则由事务负责
				conn.close();
		  }
		} catch (SQLException e) {
			System.err.println("-----------SQLException in checkClose------"+e.getMessage());
		}
	}
	
	private void setPreparedValues(PreparedStatement pst,String sql) throws SQLException{
		 List<PreparedValue> list=HoneyContext.getPreparedValue(sql);
			if(null!=list && list.size()>0)
				   _setPreparedValues(pst, list);
	 }
	 
	 private  void _setPreparedValues(PreparedStatement pst, List<PreparedValue> list) throws SQLException{
		int size=list.size();
	    for (int i = 0; i < size; i++) {
			int k=HoneyUtil.getJavaTypeIndex(list.get(i).getType());
			HoneyUtil.setPreparedValues(pst,k,i,list.get(i).getValue()); //i from 0
	 }
	}
	 
	//to java naming
	// 转成java命名规范  
	private String transformField(String field) {
		return HoneyUtil.transformField(field);
	}
    //to db naming
	// 转成带下画线的
	private String transformStr(String str) {
		return HoneyUtil.transformStr(str);
	} 
	 
}
