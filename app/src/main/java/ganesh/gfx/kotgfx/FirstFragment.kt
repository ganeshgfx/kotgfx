package ganesh.gfx.kotgfx

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import ganesh.gfx.kotgfx.databinding.FragmentFirstBinding
import ganesh.gfx.kotgfx.main.DashboardFragment


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */

class FirstFragment : Fragment() {

    private var viewBinding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = viewBinding!!

    //gs
    private val RC_SIGN_IN = 89
    private lateinit var googleSignInClient: GoogleSignInClient
    //

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        viewBinding = FragmentFirstBinding.inflate(inflater, container, false)

        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
           .requestIdToken("726998173956-9scleenut2j9auf4oisqgvtl56gcdfjs.apps.googleusercontent.com")
            .requestEmail()
            .build()

        val user = FirebaseAuth.getInstance().currentUser

//        Log.d("TAG", "onViewCreated: "+user.toString())

        if(user != null) {
            login()
        }

       // login()
        googleSignInClient = GoogleSignIn.getClient(view.context, gso)
        viewBinding?.signup?.setOnClickListener{
            if(user== null) {
                singin()
            }
            else {
                Log.d("TAG", "onViewCreated: Already lodged - " + user.displayName)

                //val fragment = DashboardFragment();
               login()
            }
        }
    }

    private fun login() {
        Log.d("TAG", "login: ")
        val fragment = SecondFragment();
        parentFragmentManager.beginTransaction().replace(R.id.nav_host_fragment_content_main,fragment).commit()
    }

    private fun singin() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    //gs
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.w("TAG", "Google sign in failed", e)
            }
        }
    }
    private fun firebaseAuthWithGoogle(idToken: String) {
        val auth = FirebaseAuth.getInstance()
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener() { task ->
                if (task.isSuccessful) {
                    Log.d("TAG", "signInWithCredential:success")
                    Log.d("TAG", "firebaseAuthWithGoogle: ${auth.currentUser?.displayName}")
                    login()
                } else {
                    Log.w("TAG", "signInWithCredential:failure", task.exception)
                }
            }
    }
    //gs

    override fun onDestroyView() {
        super.onDestroyView()
        viewBinding = null
    }
}