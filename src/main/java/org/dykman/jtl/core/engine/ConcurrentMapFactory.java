package org.dykman.jtl.core.engine;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.dykman.jtl.core.JSON;

public class ConcurrentMapFactory implements MapFactory<String,JSON> {

	@Override
	public Map<String, JSON> createMap() {
		return new ConcurrentHashMap<String, JSON>();
	}
	@Override
	public Map<String, JSON> createMap(int cap) {
		return new ConcurrentHashMap<String, JSON>(cap);
	}

	public Map<String, JSON> copyMap(Map<String, JSON> c) {
		return new ConcurrentHashMap<String, JSON>(c);
	}
}
