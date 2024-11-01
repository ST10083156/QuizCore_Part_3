package com.example.opsc_poe_part_2.Models

import com.example.opsc_quizcore.Models.UserModel
import com.google.firebase.firestore.auth.User

data class FriendsListModel(
    val userid : String,
    val friendslist : MutableList<UserModel> = mutableListOf()
)
