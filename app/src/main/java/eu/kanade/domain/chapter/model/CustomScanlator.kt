package eu.kanade.domain.chapter.model

import tachiyomi.domain.chapter.model.Chapter

/**
 * A locally created "scanlator" that matches chapters by their name instead of by the scanlator the source reports,
 * e.g. every chapter whose name contains "raw".
 *
 * A rule is excluded exactly like a regular scanlator: it is stored in the manga's excluded scanlator set, encoded
 * with [PREFIX] so it can never clash with a real scanlator. Because of that it is persisted and backed up together
 * with the other excluded scanlators.
 */
object CustomScanlator {

    private const val PREFIX = "@@chapter_name_contains@@"

    fun isCustom(value: String): Boolean = value.startsWith(PREFIX)

    /** Encodes [keyword] so it can be stored in the excluded scanlator set. */
    fun encode(keyword: String): String = PREFIX + keyword.trim()

    /** The chapter name keyword of an encoded rule. */
    fun keyword(value: String): String = value.removePrefix(PREFIX)
}

/** The chapter name keywords of the custom scanlators contained in this collection of excluded scanlators. */
fun Collection<String>.customScanlatorKeywords(): List<String> {
    return filter(CustomScanlator::isCustom)
        .map(CustomScanlator::keyword)
        .filter { it.isNotBlank() }
}

fun String.matchesAnyKeyword(keywords: List<String>): Boolean {
    return keywords.any { contains(it, ignoreCase = true) }
}

/** Removes the chapters whose name matches one of the excluded custom scanlators. */
fun List<Chapter>.withoutCustomExcluded(excludedScanlators: Collection<String>): List<Chapter> {
    val keywords = excludedScanlators.customScanlatorKeywords()
    if (keywords.isEmpty()) return this
    return filterNot { it.name.matchesAnyKeyword(keywords) }
}
