package com.example.accel_jellybean

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import androidx.core.app.NotificationCompat
import com.android.accel_jellybean.IAccel
import java.util.Locale

class AccelService : Service() {

    companion object {
        const val ACTION_READ_ACCEL = "com.example.accel_jellybean.ACCEL_UPDATE"
        private const val TAG = "AccelService"
    }

    private var mAccelService: IAccel? = null
    private var isRunning = false
    private val CHANNEL_ID = "AccelServiceChannel"
    private val NOTIFICATION_ID = 1

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification("Iniciando leitura...")
        startForeground(NOTIFICATION_ID, notification)

        if (!isRunning) {
            isRunning = true
            conectarAoDaemon()
        }
        return START_STICKY
    }

    private fun conectarAoDaemon() {
        Thread {
            var tentativas = 0
            while (tentativas < 15 && isRunning) {
                try {
                    val serviceManagerClass = Class.forName("android.os.ServiceManager")
                    val getServiceMethod = serviceManagerClass.getMethod("getService", String::class.java)
                    val binder = getServiceMethod.invoke(null, "accel_jellybean") as IBinder?
                    if (binder != null) {
                        mAccelService = IAccel.Stub.asInterface(binder)
                        updateNotification("Conectado ao daemon")
                        lerAccelContinuamente()
                        return@Thread
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Erro: ${e.message}")
                }

                tentativas++
                updateNotification("Tentativa $tentativas/15 - aguardando serviço...")
                Thread.sleep(1000)
            }
            updateNotification("Erro: serviço accel_jellybean não encontrado")
        }.start()
    }

    private fun lerAccelContinuamente() {
        while (isRunning && mAccelService != null) {
            try {
                val accelX = mAccelService?.getAccelX() ?: 0f
                val accelY = mAccelService?.getAccelY() ?: 0f
                val accelZ = mAccelService?.getAccelZ() ?: 0f

                val values = String.format(Locale.getDefault(),
                    "X: %.2f, Y: %.2f, Z: %.2f", accelX, accelY, accelZ)
                updateNotification(values)

                val intent = Intent(ACTION_READ_ACCEL)
                intent.setPackage(packageName)
                intent.putExtra("accelX", accelX)
                intent.putExtra("accelY", accelY)
                intent.putExtra("accelZ", accelZ)
                sendBroadcast(intent)

                Thread.sleep(1000)
            } catch (e: RemoteException) {
                Log.e(TAG, "RemoteException: ${e.message}")
                break
            } catch (e: InterruptedException) {
                break
            }
        }
    }

    private fun createNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Accel Service")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(content: String) {
        val notification = createNotification(content)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Accel Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        isRunning = false
        super.onDestroy()
    }
}