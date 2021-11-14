package com.freya02.bot.utils;

import org.jsoup.nodes.Node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

public record TextAttributes(Node node, List<MdFlag2> flags) {
	public TextAttributes(Node node, Deque<MdFlag2> flags) {
		this(node, new ArrayList<>(flags));
	}

	public boolean contains(MdFlag2... flags) {
		return this.flags.containsAll(Arrays.asList(flags));
	}
}
