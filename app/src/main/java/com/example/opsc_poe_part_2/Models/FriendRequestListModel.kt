package com.example.opsc_poe_part_2.Models

import com.example.opsc_quizcore.Models.UserModel

data class FriendRequestListModel(
    val userid : String,
    val friendrequests : MutableList<UserModel> = mutableListOf()
)
