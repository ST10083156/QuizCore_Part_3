package com.example.opsc_poe_part_2

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
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
import com.example.opsc_poe_part_2.Models.FriendsListModel
import com.example.opsc_poe_part_2.UsersAdapter.UserCallback
import com.example.opsc_poe_part_2.databinding.ActivityAddFriendsBinding
import com.example.opsc_poe_part_2.databinding.ActivityRequestsBinding
import com.example.opsc_quizcore.Models.UserModel
import com.google.api.Distribution.BucketOptions.Linear
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.auth.User

class RequestsActivity : AppCompatActivity() , FriendsRequestAdapter.OnRequestActionListener{
    private lateinit var binding : ActivityRequestsBinding
    private lateinit var auth : FirebaseAuth
    private lateinit var db : FirebaseFirestore
    private lateinit var allRequests : MutableList<UserModel>
    private lateinit var requestsAdapter: FriendsRequestAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRequestsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        binding.toolbar.backBtn.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white))
        binding.toolbar.pageNameTxt.text = getString(R.string.requests)
        binding.toolbar.backBtn.setOnClickListener {
            val intent = Intent(this,FriendsActivity::class.java)
            startActivity(intent)
            finish()
        }
        binding.toolbar.loginStatusBtn.text = getString(R.string.sign_out)
        binding.toolbar.loginStatusBtn.setOnClickListener {
            auth.signOut()
            Toast.makeText(this,"Signed Out",Toast.LENGTH_SHORT).show()
            val intent = Intent(this,MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        allRequests = mutableListOf()

        binding.requestsRecyclerView.layoutManager = LinearLayoutManager(this)
        requestsAdapter = FriendsRequestAdapter(allRequests,this)
        binding.requestsRecyclerView.adapter = requestsAdapter


        getUserRequests()
        Log.e("RequestsActivivty","${allRequests.size}")
        requestsAdapter.notifyDataSetChanged()
    }
    private fun getUserRequests() {
        val userID = auth.uid.toString()
        db.collection("FriendRequests").whereEqualTo("userid", userID).get().addOnSuccessListener { snapshot ->
            Log.d("RequestsActivity", "Snapshot Size: ${snapshot.size()}") // Log the number of documents retrieved
            if (!snapshot.isEmpty) {
                Log.d("RequestsActivity", "User ID: $userID")
                allRequests.clear()
                for (document in snapshot.documents) {
                    val userRequestsList = document.get("friendrequests") as? List<Map<String, Any>>
                    Log.d("RequestsActivity", "Document ID: ${document.id}, Data: ${document.data}")
                    Log.d("RequestsActivity","userRequestsList size = : ${userRequestsList?.size}")
                    if (userRequestsList != null) {
                        for (userRequestMap in userRequestsList) {

                            val request = UserModel(
                                id = userRequestMap["id"] as String,
                                name = userRequestMap["name"] as String,
                                username = userRequestMap["username"] as String,
                                image = userRequestMap["image"] as? String,
                                score = (userRequestMap["score"] as Number).toInt()
                            )
                            if (request != null) {
                                allRequests.add(request)
                            }
                        }
                    }
                }
                requestsAdapter.notifyDataSetChanged()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed to get requests: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onAcceptRequest(position: Int) {
        addFriend(allRequests[position])
        removeRequest(allRequests[position])
        allRequests.remove(allRequests[position])
        requestsAdapter.notifyDataSetChanged()


    }

    override fun onRejectRequest(position: Int) {
        removeRequest(allRequests[position])
       allRequests.remove(allRequests[position])
        Toast.makeText(this,"Friend Request Rejected",Toast.LENGTH_SHORT).show()
        requestsAdapter.notifyDataSetChanged()


    }

    private fun removeRequest(request : UserModel){
        val userID = auth.uid

        db.collection("FriendRequests")
            .whereEqualTo("userid", userID)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    for (document in documents) {
                        // Remove the UserModel object from the 'friendrequests' array
                        document.reference.update("friendrequests", FieldValue.arrayRemove(request))
                            .addOnSuccessListener {
                                Log.d("RemoveRequest", "Request for ${request.username} removed successfully.")
                            }
                            .addOnFailureListener { e ->
                                Log.e("RemoveRequest", "Error removing request: ${e.message}")
                            }
                    }
                } else {
                    Log.d("RemoveRequest", "No matching documents found.")
                }
            }
            .addOnFailureListener { e ->
                Log.e("RemoveRequest", "Error getting documents: ${e.message}")
            }
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


    private fun addFriend(friend: UserModel){
        getCurrentUser(object : UserCallback {
            override fun onUserRetrieved(user: UserModel) {
                val userID = auth.uid.toString()
                db.collection("Friends").whereEqualTo("userid", userID).limit(1).get()
                    .addOnSuccessListener { documents ->
                        if (!documents.isEmpty) {
                            val snapshot = documents.documents[0]

                            db.collection("Friends").document(snapshot.id)
                                .update("friendslist", FieldValue.arrayUnion(friend))
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        this@RequestsActivity,
                                        "Friend Request Accepted",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        } else {
                            createNewFriendDocument(friend, user)
                        }
                    }

                db.collection("Friends").whereEqualTo("userid", friend.id).limit(1).get()
                    .addOnSuccessListener { documents ->
                        if (!documents.isEmpty) {
                            val snapshot = documents.documents[0]

                            db.collection("Friends").document(snapshot.id)
                                .update("friendslist", FieldValue.arrayUnion(user))
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        this@RequestsActivity,
                                        "Friend Request Accepted",
                                        Toast.LENGTH_SHORT
                                    )
                                        .show()
                                }
                        }
                        else
                        {
                            createNewFriendDocument(user,friend)
                        }
                    }
            }




            override fun onFailure(exception: Exception) {
                Toast.makeText(this@RequestsActivity,"Failed to accept request",Toast.LENGTH_SHORT).show()
            }
        })


    }

    private fun createNewFriendDocument(friend:UserModel,user : UserModel){
        val newFriendsList = FriendsListModel(
            userid = user.id,
            friendslist = mutableListOf(friend)
        )
        db.collection("Friends").add(newFriendsList).addOnSuccessListener {
            Toast.makeText(this,"Friend Request Accepted",Toast.LENGTH_SHORT).show()

        }
    }
}


class FriendsRequestAdapter(
    private val users: List<UserModel>,
    private val listener: OnRequestActionListener
) : RecyclerView.Adapter<FriendsRequestAdapter.FriendRequestViewHolder>() {

    inner class FriendRequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userImage: ImageView = itemView.findViewById(R.id.userImage)
        val usernameTextView: TextView = itemView.findViewById(R.id.usernameTV)
        val acceptButton: Button = itemView.findViewById(R.id.acceptRequestBtn)
        val rejectButton: Button = itemView.findViewById(R.id.rejectRequestBtn)

        init {
            acceptButton.setOnClickListener {
                listener.onAcceptRequest(adapterPosition)
            }
            rejectButton.setOnClickListener {
                listener.onRejectRequest(adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendRequestViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.user_request_list_item, parent, false)
        return FriendRequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendRequestViewHolder, position: Int) {
        val user = users[position]

        holder.usernameTextView.text = user.username
        if(user.image!=null){
        holder.userImage.setImageResource(R.drawable.profile_user)
        }
        val originalBitmapAccept = BitmapFactory.decodeResource(holder.itemView.context.resources, R.drawable.check_mark)
        val scaledBitmapAccept = Bitmap.createScaledBitmap(originalBitmapAccept, 100, 100, true)
        val buttonDrawableAccept = BitmapDrawable(holder.itemView.context.resources, scaledBitmapAccept)
        holder.acceptButton.setCompoundDrawablesWithIntrinsicBounds(buttonDrawableAccept,null,null,null)
        val originalBitmapReject = BitmapFactory.decodeResource(holder.itemView.context.resources, R.drawable.close)
        val scaledBitmapReject = Bitmap.createScaledBitmap(originalBitmapReject, 100, 100, true)
        val buttonDrawableReject = BitmapDrawable(holder.itemView.context.resources, scaledBitmapReject)
        holder.rejectButton.setCompoundDrawablesWithIntrinsicBounds(buttonDrawableReject,null,null,null)

    }

    override fun getItemCount(): Int = users.size

    interface OnRequestActionListener {
        fun onAcceptRequest(position: Int)
        fun onRejectRequest(position: Int)
    }

}


