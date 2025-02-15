/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.teasoft.bee.osql.Serializer;

/**
 * Jdk序列化工具.JdkSerializer.
 * @author Kingstar
 * @since  1.11
 */
public class JdkSerializer implements Serializer {

	@Override
	public byte[] serialize(Object obj) throws IOException {
		byte b[] = null;
		if (obj == null) return b; // V2.1.8
		try (ByteArrayOutputStream out = new ByteArrayOutputStream();
				ObjectOutputStream objOut = new ObjectOutputStream(out)) {// V2.1.10
			objOut.writeObject(obj);
			b = out.toByteArray();
			return b;
		}
	}

	@Override
	public Object unserialize(byte[] bytes) throws IOException {
		if (bytes == null) {
			return null;
		}
		Object obj = null;
		try (ByteArrayInputStream input = new ByteArrayInputStream(bytes);
				ObjectInputStream objInput = new ObjectInputStream(input)) {
			obj = objInput.readObject();
		} catch (ClassNotFoundException e) {
			Logger.warn(e.getMessage(), e);
		}
		return obj;
	}

}
