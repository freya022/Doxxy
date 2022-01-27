package com.freya02.bot.docs.cached;

import com.freya02.docs.SeeAlso;

import java.util.List;

public interface CachedDoc {
	List<SeeAlso.SeeAlsoReference> getSeeAlsoReferences();
}
