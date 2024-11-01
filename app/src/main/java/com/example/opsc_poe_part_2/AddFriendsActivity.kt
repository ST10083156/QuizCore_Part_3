package com.example.opsc_poe_part_2

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.opsc_poe_part_2.Models.FriendRequestListModel
import com.example.opsc_poe_part_2.databinding.ActivityAddFriendsBinding
import com.example.opsc_quizcore.Models.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.auth.User

class AddFriendsActivity : AppCompatActivity() {
    private lateinit var binding : ActivityAddFriendsBinding
    private lateinit var auth : FirebaseAuth
    private lateinit var db : FirebaseFirestore
    private lateinit var allUsers : MutableList<UserModel>
    private lateinit var userFriendsIDs : MutableList<String>
    private lateinit var userRequestsIDs : MutableList<String>
    private lateinit var filteredUsers : MutableList<UserModel>
    private lateinit var usersAdapter: UsersAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAddFriendsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        binding.toolbar.pageNameTxt.text = getString(R.string.add_friends)
        binding.toolbar.backBtn.setOnClickListener {
            val intent = Intent(this,FriendsActivity::class.java)
            startActivity(intent)
            finish()
        }
        binding.toolbar.backBtn.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white))
        binding.toolbar.loginStatusBtn.text = getString(R.string.sign_out)
        binding.toolbar.loginStatusBtn.setOnClickListener {
            auth.signOut()
            Toast.makeText(this,"Signed Out",Toast.LENGTH_SHORT).show()
            val intent = Intent(this,MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        allUsers = mutableListOf()
        filteredUsers = mutableListOf()
        userFriendsIDs = mutableListOf()
        userRequestsIDs= mutableListOf()

        usersAdapter = UsersAdapter(filteredUsers)
        binding.usersRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.usersRecyclerView.adapter = usersAdapter

        getUsersRequests()
        getUsersFriends()
        getUsers()

        binding.searchEditText.addTextChangedListener(object :TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterList(s.toString())

            }

            override fun afterTextChanged(s: Editable?) {}

        })
    }

    private fun filterList(query: String) {
        val userID = auth.uid
        filteredUsers.clear()
        if (query.isEmpty()) {
            filteredUsers.addAll(allUsers)
        } else {

            val filtered = allUsers.filter { user ->
                user.username.contains(query, ignoreCase = true)&& user.id != userID && !userFriendsIDs.contains(user.id) && !userRequestsIDs.contains(user.id)
            }

            filteredUsers.addAll(filtered)
        }
        usersAdapter.notifyDataSetChanged()
    }

    private fun getUsersRequests(){
        val userID = auth.uid
        db.collection("FriendRequests").get().addOnSuccessListener { snapshot ->
            if (!snapshot.isEmpty){
                for(document in snapshot){
                  val  userRequestsList = document.get("friendrequests") as? List<Map<String, Any>>
                    val currentListID = document.get("userid") as? String
                    if (userRequestsList!=null){
                        for(userRequest in userRequestsList){
                            val requestID = userRequest["id"] as String
                            if(requestID == userID){
                                userRequestsIDs.add(currentListID.toString())
                            }
                        }
                    }
                }
            }
        }

        db.collection("FriendRequests").limit(1).whereEqualTo("userid",userID).get().addOnSuccessListener {
            snapshot ->
            if(!snapshot.isEmpty){
                for(document in snapshot) {
                    val userRequestsList = document.get("friendrequests") as? List<Map<String, Any>>
                    if(userRequestsList!=null){
                        for(userRequest in userRequestsList){

                            val requestID = userRequest["id"] as String

                                userRequestsIDs.add(requestID)

                        }
                    }
                }
            }
                }
        }


    private fun getUsersFriends(){
        val userID = auth.uid
        db.collection("Friends").whereEqualTo("userid",userID).limit(1).get().addOnSuccessListener { snapshot ->
            if (!snapshot.isEmpty){
                for(document in snapshot){
                    val usersFriends = document.get("friendslist") as? List<Map<String, Any>>
                    if(usersFriends!=null){
                        for(userFriend in usersFriends){
                            val friendID = userFriend["id"] as String
                            userFriendsIDs.add(friendID)
                        }
                    }

                }
            }
        }
    }


    private fun getUsers() {
        val userID = auth.uid
        db.collection("Users").get().addOnSuccessListener { snapshot ->
            if (!snapshot.isEmpty) {
                for (document in snapshot.documents) {
                    val user = document.toObject(UserModel::class.java) as UserModel

                    if (user.id != userID && !userFriendsIDs.contains(user.id) && !userRequestsIDs.contains(user.id)) {
                        allUsers.add(user)
                        filteredUsers.add(user)
                        Log.d("AddFriendsActivity", "User retrieved: ${user.username}")
                    }
                }
                usersAdapter.notifyDataSetChanged()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this,"{e.message}",Toast.LENGTH_LONG).show()
        }
    }
}

class UsersAdapter (private val users : MutableList<UserModel>) : RecyclerView.Adapter<UsersAdapter.UserViewHolder>(){
    private var db = FirebaseFirestore.getInstance()
    private var auth = FirebaseAuth.getInstance()


    class UserViewHolder(itemView: View):RecyclerView.ViewHolder(itemView){
        val userImage: ImageView = itemView.findViewById(R.id.userImage)
        val usernameTxt : TextView = itemView.findViewById(R.id.usernameTV)
        val addButton : Button = itemView.findViewById(R.id.addFriendBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsersAdapter.UserViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.user_list_item,parent,false)
        return UserViewHolder(itemView)

    }

    override fun onBindViewHolder(holder: UsersAdapter.UserViewHolder, position: Int) {

        val user = users[position]
        holder.usernameTxt.text = user?.username.toString()
        holder.usernameTxt.setTextColor(ContextCompat.getColor(holder.itemView.context,R.color.black))
        if (user.image != null) {
            holder.userImage.setImageURI(getImageUri(user.image.toString()))
        }
        val originalBitmap = BitmapFactory.decodeResource(holder.itemView.context.resources, R.drawable.check_mark)
        val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, 100, 100, true)
        val buttonDrawable = BitmapDrawable(holder.itemView.context.resources, scaledBitmap)
        holder.addButton.setOnClickListener{
            holder.addButton.text = holder.itemView.context.getString(R.string.request_sent)
            holder.addButton.setCompoundDrawablesWithIntrinsicBounds(buttonDrawable,null,null,null)
            holder.addButton.isEnabled = false
            addRequest(user.id)
        }
    }

    override fun getItemCount(): Int {
        return users.size
    }

    fun getImageUri(imagePath: String): Uri? {

        return try {
            Uri.parse(imagePath)
        } catch (e: Exception) {
            null
        }

    }

    interface UserCallback {
        fun onUserRetrieved(user: UserModel)
        fun onFailure(exception: Exception)
    }

    private fun getCurrentUser(callback: UserCallback) {
        val userID = auth.uid
        if (userID != null) {
            db.collection("Users").whereEqualTo("id", userID).get()
                .addOnSuccessListener { snapshot ->
                    if (!snapshot.isEmpty) {
                        val user = snapshot.documents[0].toObject(UserModel::class.java)
                        if (user != null) {
                            callback.onUserRetrieved(user)
                        } else {
                            callback.onFailure(Exception("User not found"))
                        }
                    } else {
                        callback.onFailure(Exception("No matching user"))
                    }
                }
                .addOnFailureListener { e ->
                    callback.onFailure(e)
                }
        } else {
            callback.onFailure(Exception("User ID is null"))
        }
    }


    private fun addRequest(id: String) {
        db.collection("FriendRequests").whereEqualTo("userid", id).limit(1).get().addOnSuccessListener { documents ->
            if (!documents.isEmpty) {
                val snapshot = documents.documents[0]
                getCurrentUser(object : UserCallback {
                    override fun onUserRetrieved(user: UserModel) {
                        db.collection("FriendRequests").document(snapshot.id)
                            .update("friendrequests", FieldValue.arrayUnion(user))
                            .addOnSuccessListener {
                                Log.e("AddFriendsActivity", "Friend request sent successfully")
                            }
                            .addOnFailureListener { e ->
                                Log.e("AddFriendsActivity", "Error updating friend requests: ${e.message}")
                            }
                    }

                    override fun onFailure(exception: Exception) {
                        Log.e("AddFriendsActivity", "Failed to retrieve current user: ${exception.message}")
                    }
                })
            } else {
                getCurrentUser(object : UserCallback {
                    override fun onUserRetrieved(user: UserModel) {
                        val newRequest = FriendRequestListModel(
                            userid = id,
                            friendrequests = mutableListOf(user)
                        )
                        db.collection("FriendRequests").add(newRequest).addOnSuccessListener {
                            Log.e("AddFriendsActivity", "New friend request document created")
                        }.addOnFailureListener { e ->
                            Log.e("AddFriendsActivity", "Error creating new friend request document: ${e.message}")
                        }
                    }

                    override fun onFailure(exception: Exception) {
                        Log.e("AddFriendsActivity", "Failed to retrieve current user: ${exception.message}")
                    }
                })
            }
        }.addOnFailureListener { e ->
            Log.e("AddFriendsActivity", "Error retrieving friend requests: ${e.message}")
        }
    }




}