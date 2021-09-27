/*
 * Copyright 2016-2021 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * InputStream流转字符串的工具
 * @author Kingstar
 * @since  1.9.8
 */
public class StreamUtil {
	
	public static String stream2String(InputStream in) {
		
		return stream2String(in, "UTF-8");
		
	}
	
	public static String stream2String(InputStream in,String charsetName) {
		
		if(in==null) return null;
		BufferedReader bfReader = null;
		StringBuffer sb = null;
		try {
			sb = new StringBuffer();
			bfReader = new BufferedReader(new InputStreamReader(in, charsetName));
			
			String line = bfReader.readLine();
			while (line != null) {
				sb.append(line);
				line = bfReader.readLine();
				if (line != null) {
					sb.append("\n");
				}
			}
		}catch(Exception e) {
			
		} finally {
			try {
				bfReader.close();
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
		
		return sb.toString();
		
	}
	
//	public static void main(String[] args) throws Exception{
//		InputStream in=new FileInputStream("D:\\temp\\user2.txt");
//		System.out.println(stream2String(in));
//	}

}
