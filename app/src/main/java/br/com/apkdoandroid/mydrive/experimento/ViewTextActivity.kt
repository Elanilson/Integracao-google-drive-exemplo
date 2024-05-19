package br.com.apkdoandroid.mydrive.experimento

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import br.com.apkdoandroid.mydrive.R
import java.io.File

class ViewTextActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_text)

        // Obter o caminho do arquivo passado através do Intent
        val filePath = intent.getStringExtra("FILE_PATH")

        // Ler o conteúdo do arquivo
        val file = File(filePath)
        val text = file.readText()

        // Exibir o texto em um TextView
        val textView = findViewById<TextView>(R.id.textView)
        textView.text = text
    }
}