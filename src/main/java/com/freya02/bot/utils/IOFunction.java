package com.freya02.bot.utils;

import java.io.IOException;

public interface IOFunction<T, R> {
	R get(T t) throws IOException;
}
