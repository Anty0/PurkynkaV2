package cz.anty.purkynka.marks

import android.content.Context
import android.content.SharedPreferences
import cz.anty.purkynka.PrefNames.FILE_NAME_MARKS_DATA
import eu.codetopic.utils.data.preferences.provider.BasicSharedPreferencesProvider
import eu.codetopic.utils.data.preferences.provider.ISharedPreferencesProvider
import eu.codetopic.utils.data.preferences.support.VersionedContentProviderPreferences

/**
 * @author anty
 */
class MarksProvider : VersionedContentProviderPreferences<SharedPreferences>(AUTHORITY, MarksData.SAVE_VERSION) {

    companion object {
        const val AUTHORITY = "cz.anty.purkynka.marks.data"
    }

    override fun onPreparePreferencesProvider(): ISharedPreferencesProvider<SharedPreferences> {
        return BasicSharedPreferencesProvider(context, FILE_NAME_MARKS_DATA, Context.MODE_PRIVATE)
    }

    override fun onUpgrade(editor: SharedPreferences.Editor, from: Int, to: Int) {
        MarksData.onUpgrade(editor, from, to)
    }
}