package cz.anty.purkynka.marks

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri

/**
 * Created by anty on 10/16/17.
 * @author anty
 */
class MarksProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        return true
    }

    override fun getType(uri: Uri?): String? {
        return null
    }

    override fun query(uri: Uri?, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        return null
    }

    override fun insert(uri: Uri?, values: ContentValues?): Uri? {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        return null
    }

    override fun delete(uri: Uri?, selection: String?, selectionArgs: Array<out String>?): Int {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        return 0
    }

    override fun update(uri: Uri?, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        return 0
    }
}