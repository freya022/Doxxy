package dev.freya02.doxxy.bot.docs.render

import dev.freya02.doxxy.bot.docs.render.SubSuperScripts.replaceScriptCharactersOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

object SubSuperScriptsTest {

    @Test
    fun `Characters are replaced`() {
        assertEquals("ᵃᵇᶜᵈᵉᶠᵍʰⁱʲᵏˡᵐⁿᵒᵖᑫʳˢᵗᵘᵛʷˣʸᶻ⁰¹²³⁴⁵⁶⁷⁸⁹⁺⁻⁼⁽⁾", "abcdefghijklmnopqrstuvwxyz0123456789+-=()".replaceScriptCharactersOrNull(SubSuperScripts.superscripts))
        assertEquals("ₐ₆꜀ₔₑբ₉ₕᵢⱼₖₗₘₙₒₚqᵣₛₜᵤᵥᵥₓₓᵧ₀₁₂₃₄₅₆₇₈₉₊₋₌₍₎", "abcdefghijklmnopqrstuvwxyz0123456789+-=()".replaceScriptCharactersOrNull(SubSuperScripts.subscripts))
    }

    @Test
    fun `Incomplete chains are ignored`() {
        assertNull("−w".replaceScriptCharactersOrNull(SubSuperScripts.superscripts))
        assertNull("−w".replaceScriptCharactersOrNull(SubSuperScripts.subscripts))
    }
}
