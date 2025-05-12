package dev.freya02.doxxy.bot.docs.render

internal object SubSuperScripts {

    // fun
    // https://lingojam.com/SuperscriptGenerator
    internal val superscripts = "abcdefghijklmnopqrstuvwxyz0123456789+-=()".zip("ᵃᵇᶜᵈᵉᶠᵍʰᶦʲᵏˡᵐⁿᵒᵖᑫʳˢᵗᵘᵛʷˣʸᶻ⁰¹²³⁴⁵⁶⁷⁸⁹⁺⁻⁼⁽⁾") //uhh ok
        .associate { (k, v) -> k to v } + mapOf(
        // https://en.wikipedia.org/wiki/Unicode_subscripts_and_superscripts#Superscripts_and_subscripts_block
        '2' to '\u00B2',
        '3' to '\u00B3',
        '1' to '\u00B9',

        '0' to '\u2070',
        'i' to '\u2071',
        '4' to '\u2074',
        '5' to '\u2075',
        '6' to '\u2076',
        '7' to '\u2077',
        '8' to '\u2078',
        '9' to '\u2079',
        '+' to '\u207A',
        '-' to '\u207B',
        '=' to '\u207C',
        '(' to '\u207D',
        ')' to '\u207E',
        'n' to '\u207F',

        'x' to '\u02E3',
        's' to '\u02E2',
        'c' to '\u1D9C',
    )

    // even funnier
    // https://lingojam.com/SubscriptGenerator
    internal val subscripts = "abcdefghijklmnopqrstuvwxyz0123456789+-=()".zip("ₐ₆꜀ₔₑբ₉ₕᵢⱼₖₗₘₙₒₚqᵣₛₜᵤᵥᵥᵥₓᵧ₂₀₁₂₃₄₅₆₇₈₉₊₋₌₍₎") //wtf
        .associate { (k, v) -> k to v } + mapOf(
        // https://en.wikipedia.org/wiki/Unicode_subscripts_and_superscripts#Superscripts_and_subscripts_block
        '0' to '\u2080',
        '1' to '\u2081',
        '2' to '\u2082',
        '3' to '\u2083',
        '4' to '\u2084',
        '5' to '\u2085',
        '6' to '\u2086',
        '7' to '\u2087',
        '8' to '\u2088',
        '9' to '\u2089',
        '+' to '\u208A',
        '-' to '\u208B',
        '=' to '\u208C',
        '(' to '\u208D',
        ')' to '\u208E',

        'a' to '\u2090',
        'e' to '\u2091',
        'o' to '\u2092',
        'x' to '\u2093',
        'ə' to '\u2094',
        'h' to '\u2095',
        'k' to '\u2096',
        'l' to '\u2097',
        'm' to '\u2098',
        'n' to '\u2099',
        'p' to '\u209A',
        's' to '\u209B',
        't' to '\u209C',
    )

    internal fun String.replaceScriptCharactersOrNull(mappings: Map<Char, Char>): String? =
        lowercase()
            .toCharArray()
            .map { c -> mappings[c] ?: return null } //If you can't convert everything, abort
            .joinToString("")
}