package com.freya02.bot.docs.cached;

import com.freya02.docs.data.SeeAlso;

import java.util.List;

public interface CachedDoc {
	List<SeeAlso.SeeAlsoReference> getSeeAlsoReferences();
}
