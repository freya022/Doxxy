package com.freya02.bot.docs;

import java.util.HashMap;

public class ConstantMap extends HashMap<String, ConstantMap.Constant> {
	public record Constant(ClassReference type, String value) {}
}
