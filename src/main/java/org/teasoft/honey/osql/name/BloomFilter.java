/*
 * Copyright 2020-2025 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.name;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
/**
 * @author Kingstar
 * @since  2.5.2
 */
import java.util.BitSet;

import org.teasoft.honey.logging.Logger;

public class BloomFilter {
	private static final String MD5 = "MD5";
	private final int bitSize; // 位数组长度
	private final int hashCount; // 哈希函数个数
	private final BitSet bitSet; // 位数组

	/**
	 * 构造布隆过滤器
	 * @param expectedSize 预期元素数量
	 * @param falsePositiveRate 最大误判率
	 * @param hashCount 哈希函数个数
	 */
	public BloomFilter(int expectedSize, double falsePositiveRate, int hashCount) {
		// 计算位数组长度
		this.bitSize = (int) (-expectedSize * Math.log(falsePositiveRate) / (Math.pow(Math.log(2), 2)));
//		System.err.println(this.bitSize);
		this.hashCount = hashCount;
		this.bitSet = new BitSet(bitSize);
	}

	/**
	 * 向布隆过滤器中添加元素
	 * @param str 待添加的字符串
	 */
	public void add(String str) {
		if (str == null) return;
		// 生成3个不同的哈希值
		int[] hashes = getHashes(str);
		for (int hash : hashes) {
			// 计算哈希值在位数组中的索引（确保非负）
			int index = Math.abs(hash % bitSize);
			bitSet.set(index);
		}
	}

	/**
	 * 判断元素是否可能存在于布隆过滤器中
	 * @param str 待判断的字符串
	 * @return true：可能存在；false：一定不存在
	 */
	public boolean contains(String str) {
		if (str == null) return false;
		int[] hashes = getHashes(str);
		for (int hash : hashes) {
			int index = Math.abs(hash % bitSize);
			if (!bitSet.get(index)) {
				return false; // 只要有一个位为0，一定不存在
			}
		}
		return true; // 所有位都为1，可能存在（有误判风险）
	}

	/**
	 * 生成3个不同的哈希值（基于MD5和hashCode）
	 * @param str 输入字符串
	 * @return 3个哈希值组成的数组
	 */
	private int[] getHashes(String str) {
		int[] hashes = new int[hashCount];
		byte[] data = str.getBytes(StandardCharsets.UTF_8);
		try {
			// 哈希
			int a = str.hashCode();
			a = a >= 0 ? a : -a;
			hashes[0] = a % bitSize;
//			hashes[0] = Math.abs(str.hashCode()) % bitSize;//TODO Bad attempt to compute absolute value of signed 32-bit hashcode

			// 哈希函数1：基于MD5的前4字节
			MessageDigest md5 = MessageDigest.getInstance(MD5);
			byte[] md5Hash = md5.digest(data);
			hashes[1] = bytesToInt(md5Hash, 0);

			// 哈希函数2：基于MD5的后4字节?
			hashes[2] = bytesToInt(md5Hash, 4);

		} catch (NoSuchAlgorithmException e) {
//			hashes[1] = hashes[0];
//			hashes[2] = hashes[0];
			Logger.warn("Have Exception when generate MD5. " + e.getMessage());
			return new int[] { hashes[0] };
		}
		return hashes;
	}

	/**
	 * 将字节数组转换为整数（取指定起始位置的4个字节）
	 */
	private int bytesToInt(byte[] bytes, int start) {
		int result = 0;
		for (int i = 0; i < 4; i++) {
			result <<= 8;
			result |= (bytes[start + i] & 0xFF);
		}
		return result;
	}

	// 测试示例
//	public static void main(String[] args) {
//		int n = 800;
//		double p = 0.0001;
//		int k = 3;
//		BloomFilter filter = new BloomFilter(n, p, k);
//
//		// 添加800个测试字符串
//		for (int i = 0; i < n; i++) {
//			filter.add("test_string_" + i);
//		}

	// 测试已添加的元素（应全部返回true）
//		int correctCount = 0;
//		for (int i = 0; i < n; i++) {
//			if (filter.contains("test_string_" + i)) {
//				correctCount++;
//			}
//		}
//		System.out.println("已添加元素的识别率：" + (correctCount * 1.0 / n));
//
//		// 测试未添加的元素（统计误判率）
//		int falsePositive = 0;
//		int testCount = 100000; // 测试10万个随机字符串
//		for (int i = n; i < n + testCount; i++) {
//			if (filter.contains("random_string_" + i)) {
//				falsePositive++;
//			}
//		}
//		System.out.println("误判率：" + (falsePositive * 1.0 / testCount));
//	}
}
