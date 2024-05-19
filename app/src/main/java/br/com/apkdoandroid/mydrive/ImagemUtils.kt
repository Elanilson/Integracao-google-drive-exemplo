package br.com.apkdoandroid.mydrive

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.graphics.decodeBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ImagemUtils {

    @RequiresApi(Build.VERSION_CODES.R)
    suspend fun decodeImage(uri: Uri, context : Context): File {
        val context = context
        return withContext(Dispatchers.IO){
            val contentResolver = context.contentResolver
            val file = File(context.cacheDir,"imagemTeste.png")
            val fileOutput = FileOutputStream(file)
            val btm = ImageDecoder.createSource(contentResolver,uri).decodeBitmap { info, source ->
            }
            btm.compress(Bitmap.CompressFormat.WEBP_LOSSY,80,fileOutput)
            file
        }
    }
}