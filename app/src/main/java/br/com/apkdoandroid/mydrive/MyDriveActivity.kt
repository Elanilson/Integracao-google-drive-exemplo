package br.com.apkdoandroid.mydrive

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.lifecycle.coroutineScope
import br.com.apkdoandroid.mydrive.databinding.ActivityMainBinding
import br.com.apkdoandroid.mydrive.databinding.ActivityMyDriveBinding
import br.com.apkdoandroid.mydrive.experimento.DriveFileInfo
import br.com.apkdoandroid.mydrive.experimento.GoogleDriveFileHolder
import br.com.apkdoandroid.mydrive.experimento.MainActivity.Companion.RC_AUTHORIZE_DRIVE

import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.Charset
import java.util.Collections

/**   dica - eu criei o projeto no firebase e procurei o mesmo projeto no console.cloud.google.com e ativei o drive api
 *  projeto firebase + drive
 *
 * documentação
 *  //https://developers.google.com/identity/sign-in/android/legacy-sign-in?hl=pt-br
 *https://console.cloud.google.com/cloud-resource-manager?hl=pt-br&_ga=2.66907566.1488628390.1715903146-1390533128.1715896753
 */


class MyDriveActivity : AppCompatActivity() {
    companion object{
        const val RC_AUTHORIZE_DRIVE = 1
        const val IMAGE = "image/*"
        const val MY_APP = "APP"
        const val ROOT = "root"
        const val MY_WALLET = "MY  WHALLET"
        const val GOOGLE_DRIVE = "application/vnd.google-apps.folder"
    }

    private  var credential: GoogleAccountCredential? = null
    private  var mAccount: GoogleSignInAccount? = null
    private  var googleDriveService: Drive? = null
    private val accessDriveEscope = Scope(Scopes.DRIVE_FILE)
    private val scopesEmail = Scope(Scopes.EMAIL)
    private var uriImagem : Uri? = null
    private val binding by lazy { ActivityMyDriveBinding.inflate(layoutInflater) }
    @RequiresApi(Build.VERSION_CODES.R)
    val abrirGaleria = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ){uri ->

        if(uri != null){
            binding.progressBar.visibility = View.VISIBLE
            uriImagem = uri
            lifecycle.coroutineScope.launch {
                uploadImagem(googleDriveService!!)
            }
        }else{
            Toast.makeText(this,"Nenhuma imagem selecionada", Toast.LENGTH_SHORT).show()
        }

    }



    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        config()
        onClick()

    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun onClick(){
        binding.buttonLogin.setOnClickListener { signIn() }
        binding.buttonLogout.setOnClickListener {deslogar()  }
        binding.buttonVisualizarImagem.setOnClickListener {
            googleDriveService?.let { it1 ->
                lifecycle.coroutineScope.launch {
                    binding.progressBar.visibility = View.VISIBLE
                    getAllBackupFiles(it1)
                }
            }
        }
        binding.buttonUpload.setOnClickListener {
            abrirGaleria()
        }

    }

    private fun config(){
         mAccount = GoogleSignIn.getLastSignedInAccount(this@MyDriveActivity)
         credential = GoogleAccountCredential.usingOAuth2(this@MyDriveActivity, Collections.singleton(
            Scopes.DRIVE_FILE))

        credential?.selectedAccount = mAccount?.account

         googleDriveService = Drive.Builder(
            NetHttpTransport(),
            GsonFactory(),
            credential
        ).setApplicationName(MY_WALLET).build()
    }

    fun signIn() {

        // Configure o Google Sign-In antes de fazer login
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()


        val googleSignInClient = GoogleSignIn.getClient(this, gso)

        val signInIntent = googleSignInClient.signInIntent
        laucherLogin.launch(signInIntent)
    }

    fun deslogar() {
        val gso = GoogleSignInOptions
            .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN) // Define o método de login padrão do Google
            .requestEmail() // Solicita permissão para acessar o endereço de e-mail do usuário
            .build() // Constrói as opções de login

        // Criando um cliente para o Google Sign-In usando as opções configuradas acima
        val googleSignInClient = GoogleSignIn.getClient(this, gso)

        if(googleSignInClient != null){
            googleSignInClient!!.signOut().addOnCompleteListener {
                // Logout bem-sucedido
                Log.d("MyDrive", "Logout bem-sucedido")
                binding.layoutLogin.visibility = View.VISIBLE
                binding.layout.visibility = View.GONE
                binding.linearLayout.visibility = View.GONE
                binding.buttonLogout.visibility = View.GONE
            }
        }


    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun abrirGaleria(){
        binding.imageView4.visibility = View.GONE
        abrirGaleria.launch("image/*")
    }

    suspend fun getAllBackupFiles(drive: Drive): List<DriveFileInfo> {
        val files = mutableListOf<DriveFileInfo>()
        return withContext(Dispatchers.IO) {
            val result =
                drive.files().list().setSpaces("drive").setFields("*")
                    .execute()
            result.files

            if(result != null){
                for (item in result.files){
                    //  if(item.name.equals("app.text")){
                    if(item.name.equals("imagemTeste.png")){
                        Log.e("MyDrive", "content ${item.thumbnailLink} ")
                        withContext(Dispatchers.Main){
                            binding.progressBar.visibility = View.GONE
                            binding.imageView4.visibility = View.VISIBLE
                            Glide.with(this@MyDriveActivity).load(item.thumbnailLink).into( binding.imageView4);

                        }
                    }
                    Log.e("MyDrive", "item ${item.name} ")
                }
            }else{
                Log.e("MyDrive", "item null")
            }

            files
        }
    }




    private fun verificarUsuarioLogdo(){
        val account = GoogleSignIn.getLastSignedInAccount(this)

        if(account != null){
            binding.textViewEmail.text = "${account?.email}"
            binding.layoutLogin.visibility = View.GONE
            binding.layout.visibility = View.VISIBLE
            binding.linearLayout.visibility = View.VISIBLE
            binding.buttonLogout.visibility = View.VISIBLE
            Log.d("MyDrive", "Usuário  está logado")
        }else{
            Log.d("MyDrive", "Usuário não está logado")
        }
    }

    override fun onStart() {
        super.onStart()
        verificarUsuarioLogdo()
    }


    private var laucherLogin = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
        Log.d("MyDrive", "resultCode: $result.resultCode}")
        if(result.resultCode == Activity.RESULT_OK){
            val data : Intent? = result.data
            try{
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                binding.textViewEmail.text = "${task?.getResult()?.email}"
                checkForGooglePermissions()
                binding.layoutLogin.visibility = View.GONE
                binding.layout.visibility = View.VISIBLE
                binding.linearLayout.visibility = View.VISIBLE
                binding.buttonLogout.visibility = View.VISIBLE
                Log.d("MyDrive", "Login com sucesso")
                task.getResult(ApiException::class.java)
            }catch (e : Exception){
                Log.d("MyDrive", "laucher ${e.message}")
            }
        }else if(result.resultCode == Activity.RESULT_CANCELED){
            Log.d("MyDrive", "Tentativa de login cancelada")
        }
    }

    private fun checkForGooglePermissions() {
        if(!GoogleSignIn.hasPermissions(
                GoogleSignIn.getLastSignedInAccount(this),
                accessDriveEscope,
                scopesEmail
            )){
            GoogleSignIn.requestPermissions(
                this,
                RC_AUTHORIZE_DRIVE,
                GoogleSignIn.getLastSignedInAccount(this),
                accessDriveEscope,
                scopesEmail
            )
        }else{
            // lifecycle.coroutineScope.launch { driveSetUp()  }
        }
    }

    private fun createFolder(mDriveService: Drive, foldeName: String?, folderId: String?): GoogleDriveFileHolder?{
        try {

            val googleDriveFileHolder = GoogleDriveFileHolder()
            val root = folderId?.let { listOf(it) } ?: listOf(ROOT)

            val metadata = com.google.api.services.drive.model.File()
                .setParents(root)
                .setMimeType(GOOGLE_DRIVE)
                .setName(foldeName)



            val googleFile = mDriveService.files().create(metadata).execute() ?: throw IOException("IOException")


            googleDriveFileHolder.id = googleFile.id


            return googleDriveFileHolder
        }catch (e : Exception){
            Log.e("MyDrive", "createFolder ${e.message}")
        }

        return null
    }

    private fun uploadFile(mDriveService: Drive, localFile: File, mimeType : String?, folderId : String?) : GoogleDriveFileHolder? {
        try {
            val root = folderId?.let { listOf(it) } ?: listOf(ROOT)
            //val name = if(localFile.name.isNullOrEmpty()) "sem nome" else  localFile.name
            val metadata = com.google.api.services.drive.model.File()
                .setParents(root)
                .setMimeType(mimeType)
                .setName( localFile.name)

            val fileContent = FileContent(mimeType,localFile)

            val fileMeta = mDriveService.files().create(
                metadata,
                fileContent
            ).execute()

            val googleDriveFileHolder = GoogleDriveFileHolder()
            googleDriveFileHolder.id = fileMeta.id
            googleDriveFileHolder.name = fileMeta.name


            return googleDriveFileHolder
        }catch (e : Exception){
            e.stackTrace
            Log.e("MyDrive", e.message.toString())
        }finally {

            return  null
        }

    }

    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun uploadImagem(googleDriveService : Drive){
        withContext(Dispatchers.Default){
            val googleDriveFileHolderJob = async { createFolder(googleDriveService, MY_APP,null) }
            val googleDriveFileHolder = googleDriveFileHolderJob.await()

                var file = uriImagem?.let { ImagemUtils.decodeImage(it,this@MyDriveActivity) }

                if(file !== null){
                    val uploadJob = async { uploadFile(
                        googleDriveService,
                        file,
                        IMAGE,
                        googleDriveFileHolder?.id
                    ) }

                    uploadJob.await()

                    withContext(Dispatchers.Main){
                        binding.progressBar.visibility = View.GONE
                        if(uploadJob != null){
                            Toast.makeText(this@MyDriveActivity,"Upload imagem com Sucesso", Toast.LENGTH_LONG).show()
                        }else{
                            Toast.makeText(this@MyDriveActivity,"Falha no Upload imagem" , Toast.LENGTH_LONG).show()
                        }


                    }


            }
        }
    }




}