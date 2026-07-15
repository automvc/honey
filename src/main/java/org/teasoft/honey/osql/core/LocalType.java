package org.teasoft.honey.osql.core;

public enum LocalType {
//	SESSION, TRANSACTION, USER,
	NORMAL,   //要使用者负责回收； 一般存放不用回收的信息。
	REQUEST,  //一次请求；本次请求完成后会回收；
	MORETABLE_INSERT,
	SHARDING //Sharding模式下使用；clearContext时会回收。
}
