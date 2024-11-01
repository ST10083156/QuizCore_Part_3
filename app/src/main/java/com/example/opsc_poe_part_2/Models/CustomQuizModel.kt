package com.example.opsc_quizcore.Models

import android.os.Parcel
import android.os.Parcelable

data class CustomQuizModel(
    val id: Int = 0,
    val userid: String,
    val quizname: String,
    val category: String,
    val questions: List<QuestionModel>
) : Parcelable {
    constructor(parcel: Parcel) : this(
        id = parcel.readInt(),
        userid = parcel.readString() ?: "",
        quizname = parcel.readString() ?: "",
        category = parcel.readString() ?: "",
        questions = parcel.createTypedArrayList(QuestionModel.CREATOR) ?: emptyList()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(userid)
        parcel.writeString(quizname)
        parcel.writeString(category)
        parcel.writeTypedList(questions)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<CustomQuizModel> {
        override fun createFromParcel(parcel: Parcel): CustomQuizModel {
            return CustomQuizModel(parcel)
        }

        override fun newArray(size: Int): Array<CustomQuizModel?> {
            return arrayOfNulls(size)
        }
    }
}
