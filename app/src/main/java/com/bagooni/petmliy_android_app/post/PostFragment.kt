package com.bagooni.petmliy_android_app.post

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bagooni.petmliy_android_app.MainActivity
import com.bagooni.petmliy_android_app.R
import com.bagooni.petmliy_android_app.databinding.FragmentPostBinding
import com.bagooni.petmliy_android_app.post.Retrofit.Like
import com.bagooni.petmliy_android_app.post.Retrofit.LikeRetrofitService
import com.bagooni.petmliy_android_app.walk.WalkFragment.Companion.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION_AND_WRITE_EXTERNAL_STORAGE_PERMISSION
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PostFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {
    var client: OkHttpClient? =
        httpLoggingInterceptor()?.let { OkHttpClient.Builder().addInterceptor(it).build() }

    private var _binding: FragmentPostBinding?=null
    private val binding get() = _binding!!
    private var personEmailInput : String = ""
    private var userImgUri : String = ""

    lateinit var retrofitService: RetrofitService
    lateinit var likeRetrofitService: LikeRetrofitService
    lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPostBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkSign()
        swipeRefreshLayout = view.findViewById(R.id.swipeLayout)
        swipeRefreshLayout.setOnRefreshListener(this)
        val feedListView = view.findViewById<RecyclerView>(R.id.feedList)

        binding.uploadButton.setOnClickListener {
            getPermissions()
            findNavController().navigate(R.id.action_postFragment_to_postUploadFragment)
        }

        var gson = GsonBuilder().setLenient().create()
        val retrofit = Retrofit.Builder()
            .baseUrl("http://ec2-54-180-166-236.ap-northeast-2.compute.amazonaws.com:8080/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        retrofitService = retrofit.create(RetrofitService::class.java)
        likeRetrofitService = retrofit.create(LikeRetrofitService::class.java)
        getPost()
    }

    class PostRecyclerViewAdapter(
        val postList: ArrayList<Post>,
        val inflater: LayoutInflater,
        val glide: RequestManager,
        val postFragment: PostFragment,
        val activity: MainActivity
    ): RecyclerView.Adapter<PostRecyclerViewAdapter.ViewHolder>(){

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
            val userImg : ImageView
            val userName : TextView
            val postUserName : TextView
            val postImg : ImageView
            val postContent : TextView
            val favoriteBtn : ImageButton
            val favoriteColorBtn : ImageButton
            val postLayer : ImageView
            val postHeart : ImageView
            val commentBtn : ImageButton

            init{
                userImg = itemView.findViewById(R.id.userImg)
                userName = itemView.findViewById(R.id.userEmail)
                postUserName = itemView.findViewById(R.id.postUserName)
                postImg = itemView.findViewById(R.id.postImg)
                postContent = itemView.findViewById(R.id.postContent)
                favoriteBtn = itemView.findViewById(R.id.favoriteBtn) //좋아요 버튼
                favoriteColorBtn = itemView.findViewById(R.id.favoriteColorBtn) //좋아요 색 버튼
                postLayer = itemView.findViewById(R.id.postLayer)
                postHeart = itemView.findViewById(R.id.postHeart)
                commentBtn = itemView.findViewById(R.id.commentBtn)

                favoriteBtn.setOnClickListener {
                    postFragment.postLike(postList[adapterPosition].postId)
                    Thread {
                        activity.runOnUiThread {
                            favoriteColorBtn.visibility = VISIBLE
                            postLayer.visibility = VISIBLE
                            postHeart.visibility = VISIBLE
                        }
                        Thread.sleep(1000)
                        activity.runOnUiThread {
                            postLayer.visibility = INVISIBLE
                            postHeart.visibility = INVISIBLE
                        }
                    }.start()
                }
                favoriteColorBtn.setOnClickListener {
                    postFragment.deleteData(postList[adapterPosition].postId)
                    Thread{
                        activity.runOnUiThread {
                            favoriteColorBtn.visibility = INVISIBLE
                        }
                    }.start()
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(inflater.inflate(R.layout.post_recyclerview_item,parent,false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val post = postList.get(position)

            post.userImg.let{
                glide.load(it).centerCrop().circleCrop().into(holder.userImg)
            }

            if (!post.postImg.isNullOrEmpty() ){
                val byte = Base64.decode(post.postImg, Base64.DEFAULT)
                val img:Bitmap = BitmapFactory.decodeByteArray(byte, 0, byte.size)
                holder.postImg.setImageBitmap(img)
            }
            holder.postUserName.text = post.email.split("@")[0]
            holder.userName.text = post.email.split("@")[0]
            holder.postContent.text = post.postContent

            postFragment.likeRetrofitService.aboutLike(postFragment.personEmailInput,post.postId)
                .enqueue(object : Callback<Int>{
                override fun onResponse(call: Call<Int>, response: Response<Int>) {
                    Log.d("getPostLike",response.body().toString())
                    if (response.body() == 1){
                        Thread{
                            activity.runOnUiThread {
                                holder.favoriteColorBtn.visibility = VISIBLE
                            }
                        }.start()
                    }else{
                        Thread{
                            activity.runOnUiThread {
                                holder.favoriteColorBtn.visibility = INVISIBLE
                            }
                        }.start()
                    }
                }
                override fun onFailure(call: Call<Int>, t: Throwable) {
                }
            })

            holder.commentBtn.setOnClickListener {
                postFragment.postToComment(post.postId)
            }
        }
        override fun getItemCount(): Int {
            return postList.size
        }
    }

    private fun getPost(){
        retrofitService.getPost().enqueue(object : Callback<ArrayList<Post>>{
            override fun onResponse(
                call: Call<ArrayList<Post>>,
                response: Response<ArrayList<Post>>
            ) {
                val postList = response.body()
                val postRecyclerView = view?.findViewById<RecyclerView>(R.id.feedList)
                postRecyclerView?.adapter = postList?.let {
                    PostRecyclerViewAdapter(
                        it,
                        LayoutInflater.from(activity),
                        Glide.with(activity!!),
                        this@PostFragment,
                        activity as (MainActivity)
                    )
                }
            }
            override fun onFailure(call: Call<ArrayList<Post>>, t: Throwable) {
                Log.d("log",t.message.toString())
            }
        })
    }

    //좋아요 구현
    private fun postLike(postId: Long){
        likeRetrofitService.postLike(personEmailInput,postId,userImgUri).enqueue(object : Callback<Like>{
            override fun onResponse(call: Call<Like>, response: Response<Like>) {
            }
            override fun onFailure(call: Call<Like>, t: Throwable) {
            }

        })
    }

    //좋아요 취소
    private fun deleteData(postId: Long){
        likeRetrofitService.deleteLike(personEmailInput,postId).enqueue(object : Callback<Void>{
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.d("log",t.message.toString())
            }
        })
    }

    private fun checkSign(){
        val acct = GoogleSignIn.getLastSignedInAccount(activity as MainActivity)
        if (acct != null) {
            val personEmail = acct.email
            val userImg = acct.photoUrl
            personEmailInput = personEmail.toString()
            userImgUri = userImg.toString()
            Log.d("google",personEmailInput)
            Log.d("google",userImg.toString())
        }
    }

    fun postToComment(postId : Long){
        var action = PostFragmentDirections.actionPostFragmentToCommentFragment(postId)
        findNavController().navigate(action)
    }

    private fun httpLoggingInterceptor(): HttpLoggingInterceptor? {
        val interceptor = HttpLoggingInterceptor { message ->
            Log.e(
                "MyGitHubData :",
                message + ""
            )
        }
        return interceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
    }

    private fun getPermissions() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ), PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION_AND_WRITE_EXTERNAL_STORAGE_PERMISSION
            )
        }
    }

    override fun onRefresh() {
        swipeRefreshLayout.isRefreshing = false
    }
}
