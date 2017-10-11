package cz.anty.purkynka.marks.data

import android.content.Context
import eu.codetopic.utils.ui.container.items.custom.CustomItem
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by anty on 6/20/17.
 * @author anty
 */
data class Mark(val date: Date, val shortLesson: String, val longLesson: String, val valueToShow: String,
                val value: Double, val type: String, val weight: Int, val note: String, val teacher: String): CustomItem() {

    override fun onBindViewHolder(holder: CustomItem.ViewHolder?, itemPosition: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getItemLayoutResId(context: Context?): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}