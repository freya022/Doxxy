package com.freya02.bot.utils;

import java.io.IOException;

public interface IOSupplier<R> {
	R get() throws IOException;
}
