package cz.anty.purkynka.marks.data

import android.content.Context
import com.google.common.collect.ImmutableList
import eu.codetopic.utils.ui.container.items.custom.CustomItem

/**
 * Created by anty on 6/20/17.
 * @author anty
 */
data class Lesson(val fullName: String, val shortName: String, val marks: ImmutableList<Mark>): CustomItem() {

    fun getDiameter(): Double {
        var tempMark = 0.0
        var tempWeight = 0
        for (mark in marks) {
            if (mark.value == 0.0) continue
            tempMark += mark.value * mark.weight.toDouble()
            tempWeight += mark.weight
        }
        return tempMark / tempWeight.toDouble()
    }

    override fun onBindViewHolder(holder: ViewHolder?, itemPosition: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getItemLayoutResId(context: Context?): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}