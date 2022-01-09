package com.freya02.bot.docs;

import java.util.HashMap;
import java.util.Map;

public class CachedClassMetadata {
	//Use specialised map type, otherwise Gson will use its own horrible linked hash map implementation, which would take 2x more memory
	//method signature to sha3-256 + .json
	private final HashMap<String, String> methodSignatureToFileNameMap = new HashMap<>();

	public Map<String, String> getMethodSignatureToFileNameMap() {
		return methodSignatureToFileNameMap;
	}
}
