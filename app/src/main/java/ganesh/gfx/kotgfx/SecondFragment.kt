package ganesh.gfx.kotgfx

import android.animation.ValueAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnticipateOvershootInterpolator
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import ganesh.gfx.kotgfx.data.Box
import ganesh.gfx.kotgfx.data.Move
import ganesh.gfx.kotgfx.databinding.FragmentSecondBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*


/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var database: DatabaseReference
    val user = FirebaseAuth.getInstance().currentUser

    private val TAG = "TAG"
    var room = ""
    var userId = user?.uid;
    var roomCreatedByMe = false
    var waitForOtherPlayer = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        database = FirebaseDatabase.getInstance("https://jaga-fae3a-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference().child("tiktak");

        val arguments = arguments
        val reset = arguments?.getString("reset")
        val resetBy = arguments?.getString("resetBy")
        if (reset != null) {
            room = reset
        }
        Log.d(TAG, "onCreateView: "+resetBy)
        if(reset!=null && !room.equals("")){
            if(resetBy.equals(userId)) {
                roomCreatedByMe = true
                waitForOtherPlayer = false
                database.child("rooms").child(room).child("moves").removeValue()
            }else{

                database.child("rooms").child(room).child("status").setValue("playing").addOnCompleteListener{
                    listenerForReset()
                }

                roomCreatedByMe = false
                waitForOtherPlayer = true
            }
            _binding!!.createRoomView.visibility = View.GONE
            _binding!!.RoomView.visibility = View.VISIBLE
            _binding!!.roomCode.text = room
            listenerForUpdates()
        } else{
            _binding!!.createRoom.setOnClickListener {
                var Xroom: String = _binding!!.roomInput.text.toString().trim();

                if (!Xroom.equals("")) {
                    room = Xroom
                    database.child("rooms").child(room).get().addOnSuccessListener {
                        if (it.value == null) {

                            database.child("rooms").child(room).removeValue().addOnCompleteListener {
                                database.child("rooms").child(room).child("players").child(userId + "").setValue(userId + "").addOnCompleteListener {
                                    listenerForUpdates()
                                    _binding!!.createRoomView.visibility = View.GONE
                                    _binding!!.RoomView.visibility = View.VISIBLE
                                    _binding!!.roomCode.text = room
                                    roomCreatedByMe = true
                                    waitForOtherPlayer = false
                                }
                            }

                        } else {
                            // Log.d(TAG, "onCreateView: ${it.child("players")}")
                            if (it.child("players").childrenCount == 1L) {
                                database.child("rooms").child(room).child("players").push().setValue(userId + "").addOnCompleteListener {
                                    listenerForUpdates()
                                    _binding!!.createRoomView.visibility = View.GONE
                                    _binding!!.RoomView.visibility = View.VISIBLE
                                    _binding!!.roomCode.text = room
                                    waitForOtherPlayer = true

                                    database.child("rooms").child(room).child("status").setValue("playing").addOnCompleteListener{
                                        listenerForReset()
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Room Not Avilable...!!!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        //Log.d(TAG, "Got value ${it.value}")
                    }.addOnFailureListener {
                        Log.e("firebase", "Error getting data at room id making", it)
                    }
                }
            }
        }

//        database.child("rooms").child(room).removeValue().addOnCompleteListener{
//            database.child("rooms").child(room).child("players").child(userId+"").setValue(userId+"").addOnCompleteListener{
//                listenerForUpdates()
//            }
//        }

        return binding.root

    }

    lateinit var boxs: List<Box>
    lateinit var v: View

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        v = view

        boxs = listOf(
            Box(binding.aa, " ", "00"), Box(binding.ab, " ", "01"), Box(binding.ac, " ", "02"),
            Box(binding.ba, " ", "10"), Box(binding.bb, " ", "11"), Box(binding.bc, " ", "12"),
            Box(binding.ca, " ", "20"), Box(binding.cb, " ", "21"), Box(binding.cc, " ", "22"),
        )

        for (box in boxs) {
            box.button.setOnClickListener {
                if(!waitForOtherPlayer) {
                    changeSymbol(box, true)
                    waitForOtherPlayer = true
                }
                vibratePhone()
            }
        }

        binding.resetGrid.setOnClickListener {
            if(roomCreatedByMe) {
                database.child("rooms").child(room).child("status").setValue("reset").addOnCompleteListener{
                    reset()
                }
            }
        }
    }

    private fun sendOnline(move: Move) {
        var id = user?.uid;
        database.child("rooms").child(room).child("moves").push().setValue(move).addOnCompleteListener {
            //  Log.d(TAG, "sendOnline: DONE")
        }.addOnFailureListener {
            Log.d(TAG, "sendOnline: Error")
        }
    }

    fun listenerForReset() {
        val listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                Log.d(TAG, "onDataChange reset: "+dataSnapshot.value)
                if(dataSnapshot.value.toString() == "reset"){

                    GlobalScope.launch {
                        val handler = Handler(Looper.getMainLooper())
                        handler.post {
                            reset("ahh")
                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(TAG, "Error at read reset:onCancelled", databaseError.toException())
            }
        }
        database.child("rooms").child(room).child("status").addValueEventListener(listener)
    }

    fun listenerForUpdates() {
        val listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (data in dataSnapshot.children) {
                    val move: Move? = data.getValue(Move::class.java)
                    if (move != null) {
                        if (!move.userId.equals(userId)) {
                            // Log.d(TAG, move.userId+" | " + userId)
                            // Log.d(TAG, "" + move?.move)
                            for (box in boxs) {
                                if (box.pos.equals(move.move)) {
                                    //box.button.performClick()

                                    GlobalScope.launch {
                                        val handler = Handler(Looper.getMainLooper())
                                        handler.post {
                                            changeSymbol(box, false)
                                        }
                                    }

                                    waitForOtherPlayer = false
                                }
                            }
                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(TAG, "Error at send:onCancelled", databaseError.toException())
            }
        }
        database.child("rooms").child(room).child("moves").limitToLast(1).addValueEventListener(listener)
    }

    val duration: Long = 600

    //var turn = true
    fun changeSymbol(paxlu: Box, turn: Boolean) {

        //1
        val box = paxlu.button
        val valueAnimator = ValueAnimator.ofFloat(0f, 180f, 360f)
        valueAnimator.addUpdateListener {
            val value = it.animatedValue as Float
            box.rotation = value
        }
        valueAnimator.interpolator = AnticipateOvershootInterpolator()
        valueAnimator.duration = duration
        valueAnimator.start()
        var time: Long = (duration / 2)
        GlobalScope.launch {
            delay(time)
            val handler = Handler(Looper.getMainLooper())
            handler.post {
                if (box.drawable == null) {
                    if (turn) {
                        box.setImageDrawable(resources.getDrawable(R.drawable.o))
                        paxlu.symbol = "o"

                        val move = Move(userId, paxlu.pos)
                        sendOnline(move)

                    } else {
                        box.setImageDrawable(resources.getDrawable(R.drawable.x))
                        paxlu.symbol = "x"
                    }
                    // turn = !turn
                    checkWin(boxs)
                }
            }
        }

    }

    fun vibratePhone() {
//        val vibrator = context?.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
//        if (Build.VERSION.SDK_INT >= 26) {
//            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
//        } else {
//            vibrator.vibrate(50)
//        }
    }

    fun checkWin(boxs: List<Box>) {

        GlobalScope.launch {
            delay(100)
            val handler = Handler(Looper.getMainLooper())
            handler.post {
                var lines: List<String> = listOf(
                    /*
                    0  1  2
                    3  4  5
                    6  7  8
                     */
                    ("" + boxs[0].symbol + boxs[1].symbol + boxs[2].symbol).replace(" ", ""),
                    ("" + boxs[3].symbol + boxs[4].symbol + boxs[5].symbol).replace(" ", ""),
                    ("" + boxs[6].symbol + boxs[7].symbol + boxs[8].symbol).replace(" ", ""),

                    ("" + boxs[0].symbol + boxs[4].symbol + boxs[8].symbol).replace(" ", ""),
                    ("" + boxs[2].symbol + boxs[4].symbol + boxs[6].symbol).replace(" ", ""),

                    ("" + boxs[0].symbol + boxs[3].symbol + boxs[6].symbol).replace(" ", ""),
                    ("" + boxs[1].symbol + boxs[4].symbol + boxs[7].symbol).replace(" ", ""),
                    ("" + boxs[2].symbol + boxs[5].symbol + boxs[8].symbol).replace(" ", ""),
                )

                for (line in lines) {
                    if (line.length == 3) {
                        //Log.d("TAG", "checkWin: " + line)
                        if (line.equals("xxx")) {
                            Toast.makeText(context, "X Won!!!", Toast.LENGTH_LONG).show()
                        }
                        if (line.equals("ooo")) {
                            Toast.makeText(context, "O Won!!!", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                //Toast.makeText(context, lines.toString(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun reset(rBy:String = userId.toString()) {
        val thisFragment = SecondFragment()
        val arguments = Bundle()
        arguments.putString(
            "reset",
            room)
        arguments.putString(
            "resetBy",
            rBy)
        thisFragment.arguments = arguments

        parentFragmentManager
            .beginTransaction()
            .replace(
                R.id.nav_host_fragment_content_main,
                thisFragment
            )
            .commit()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d(TAG, "onDestroyView: Exited")
        //database.child("rooms").child(room).setValue("")
    }

    fun rand(start: Int, end: Int): Int {
        require(start <= end) { "Illegal Argument" }
        return (start..end).random()
    }
}