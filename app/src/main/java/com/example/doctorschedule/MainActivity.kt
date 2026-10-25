package com.example.doctorschedule

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: DoctorViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var fabRefresh: FloatingActionButton
    private lateinit var fabSpeaker: FloatingActionButton
    private lateinit var officialMessageView: TextView
    private lateinit var floatingDove: TextView

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        recyclerView = findViewById(R.id.recycler_doctors)
        progressBar = findViewById(R.id.progress_bar)
        fabRefresh = findViewById(R.id.fab_refresh)
        fabSpeaker = findViewById(R.id.fab_speaker)
        officialMessageView = findViewById(R.id.tv_official_message)
        floatingDove = findViewById(R.id.floating_dove)

        recyclerView.layoutManager = LinearLayoutManager(this)

        viewModel = ViewModelProvider(this)[DoctorViewModel::class.java]

        viewModel.isLoading.observe(this) { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.doctors.observe(this) { doctors ->
            recyclerView.adapter = DoctorAdapter(doctors)
            if (doctors.isEmpty()) {
                Toast.makeText(this, "اطلاعاتی یافت نشد", Toast.LENGTH_SHORT).show()
            } else {
                // ذخیرهٔ خودکار لیست پزشکان در فایل JSON
                JsonStorage.saveDoctors(this, doctors)
                // دریافت خودکار و ترتیبی جزئیات همهٔ نوبت‌ها (پس‌زمینه)
                autoFetchAllTurnInfos(doctors)
            }
        }

        // پیام رسمی فقط یک‌بار در شروع برنامه (بعد از ۵ ثانیه ناپدید می‌شود)
        showOfficialMessage("حتماً پیش از مراجعه، برنامه را بروزرسانی کنید")
        Handler(Looper.getMainLooper()).postDelayed({
            officialMessageView.visibility = View.GONE
        }, 5_000)

        fabRefresh.setOnClickListener {
            viewModel.loadDoctors()
        }

        fabSpeaker.setOnClickListener {
            showMessagesDialog()
        }

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (!recyclerView.canScrollVertically(1)) {
                    fabRefresh.hide()
                    fabSpeaker.hide()
                } else {
                    fabRefresh.show()
                    fabSpeaker.show()
                }
            }
        })

        // شروع انیمیشن کبوتر (اولین اجرا بعد از ۱۰ ثانیه، سپس هر ۲۰ تا ۳۰ ثانیه)
        scheduleDoveAnimation()
    }

    /**
     * دریافت خودکار و ترتیبی جزئیات نوبت‌ها در پس‌زمینه.
     * هر نوبت یکی‌یکی گرفته می‌شود و در JSON ذخیره می‌شود.
     */
    private fun autoFetchAllTurnInfos(doctors: List<Doctor>) {
        CoroutineScope(Dispatchers.IO).launch {
            for (doctor in doctors) {
                for (schedule in doctor.schedules) {
                    try {
                        val info = TurnInfoService.getTurnInfo(schedule.showId)
                        if (info != null) {
                            JsonStorage.saveTurnInfo(
                                this@MainActivity,
                                schedule.showId,
                                info.appoint,
                                info.telAppoint,
                                info.webAppoint
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    // تأخیر کوتاه برای جلوگیری از فشار به سرور
                    delay(300)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                showJsonDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showJsonDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_json, null)
        val tvJson = dialogView.findViewById<TextView>(R.id.tv_json)
        val tvSize = dialogView.findViewById<TextView>(R.id.tv_size)
        val btnCopy = dialogView.findViewById<android.widget.Button>(R.id.btn_copy_json)

        val json = JsonStorage.loadJson(this)
        tvJson.text = json
        val sizeKb = JsonStorage.getFileSizeBytes(this) / 1024.0
        tvSize.text = "حجم فایل: %.2f کیلوبایت".format(sizeKb)

        val dialog = AlertDialog.Builder(this)
            .setTitle("JSON ذخیره شده")
            .setView(dialogView)
            .setPositiveButton("بستن", null)
            .create()

        btnCopy.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("json", json))
            Toast.makeText(this, "کپی شد", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    private fun scheduleDoveAnimation() {
        val doveRunnable = object : Runnable {
            override fun run() {
                animateDove()
                handler.postDelayed(this, Random.nextLong(20_000, 30_000))
            }
        }
        handler.postDelayed(doveRunnable, 10_000)
    }

    private fun animateDove() {
        floatingDove.visibility = View.VISIBLE
        floatingDove.alpha = 0f

        val parentWidth = findViewById<View>(android.R.id.content).width
        val parentHeight = findViewById<View>(android.R.id.content).height
        val startX = Random.nextFloat() * (parentWidth - 100)
        val endX = Random.nextFloat() * (parentWidth - 100)
        val startY = Random.nextFloat() * (parentHeight / 2)
        val endY = Random.nextFloat() * (parentHeight - 200)

        floatingDove.x = startX
        floatingDove.y = startY

        val moveX = ObjectAnimator.ofFloat(floatingDove, "x", startX, endX).apply {
            duration = 4000
            interpolator = AccelerateDecelerateInterpolator()
        }
        val moveY = ObjectAnimator.ofFloat(floatingDove, "y", startY, endY).apply {
            duration = 4000
            interpolator = AccelerateDecelerateInterpolator()
        }
        val fadeIn = ObjectAnimator.ofFloat(floatingDove, "alpha", 0f, 0.8f).apply {
            duration = 1000
        }
        val fadeOut = ObjectAnimator.ofFloat(floatingDove, "alpha", 0.8f, 0f).apply {
            duration = 1000
            startDelay = 3000
        }

        val set = android.animation.AnimatorSet()
        set.playTogether(moveX, moveY, fadeIn)
        set.start()

        fadeOut.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                floatingDove.visibility = View.INVISIBLE
            }
        })
        handler.postDelayed({ fadeOut.start() }, 3000)
    }

    private fun showMessagesDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_messages, null)
        val recyclerMessages = dialogView.findViewById<RecyclerView>(R.id.recycler_messages)
        val btnRefreshMessages = dialogView.findViewById<android.widget.Button>(R.id.btn_refresh_messages)

        recyclerMessages.layoutManager = LinearLayoutManager(this)
        val adapter = MessageAdapter(emptyList())
        recyclerMessages.adapter = adapter

        loadMessages(adapter)

        btnRefreshMessages.setOnClickListener {
            loadMessages(adapter)
        }

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("بستن", null)
            .show()
    }

    private fun loadMessages(adapter: MessageAdapter) {
        MessageService.fetchMessages { messages ->
            runOnUiThread {
                adapter.updateMessages(messages)
            }
        }
    }

    private fun showOfficialMessage(text: String) {
        officialMessageView.text = text
        officialMessageView.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
