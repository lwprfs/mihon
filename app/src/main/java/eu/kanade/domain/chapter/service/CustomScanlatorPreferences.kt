package eu.kanade.domain.chapter.service

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

/**
 * Keeps the locally created scanlators (chapter name keywords) of each manga, so they stay in the filter dialog
 * after being un-excluded. Which of them are excluded is stored with the manga's excluded scanlators.
 */
@Inject
@SingleIn(AppScope::class)
class CustomScanlatorPreferences(
    private val preferenceStore: PreferenceStore,
) {

    fun keywords(mangaId: Long): Preference<Set<String>> {
        return preferenceStore.getStringSet("custom_name_scanlators_$mangaId", emptySet())
    }
}
