package eu.tutorials.mybizz

import android.app.Application
import com.google.firebase.FirebaseApp

class MyBizApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
