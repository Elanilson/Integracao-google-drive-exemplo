package br.com.apkdoandroid.mydrive.experimento

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.graphics.decodeBitmap
import br.com.apkdoandroid.mydrive.R
import br.com.apkdoandroid.mydrive.databinding.ActivityMainBinding
import br.com.apkdoandroid.mydrive.experimento.storage.StorageManager
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.FileList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.io.IOException
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.Collections

/** documentação
 *  //https://developers.google.com/identity/sign-in/android/legacy-sign-in?hl=pt-br
 *https://console.cloud.google.com/cloud-resource-manager?hl=pt-br&_ga=2.66907566.1488628390.1715903146-1390533128.1715896753
 */

class MainActivity : AppCompatActivity() {
    private val fields = "nextPageToken, files(id,name)"
   // private val scopes = listOf(Scope(DriveScopes.DRIVE_FILE))
    private val accessDriveEscope = Scope(Scopes.DRIVE_FILE)
    private val scopesEmail = Scope(Scopes.EMAIL)
    private var isFileRead = false
    private val receivedText : String = "Hello world, \n Hi guys, I am Elanilson!"
    private lateinit var storageManager : StorageManager
    private var uriImagem : Uri? = null

    val abrirGaleria = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ){uri ->

        if(uri != null){
            uriImagem = uri
            Toast.makeText(this," imagem selecionada",Toast.LENGTH_SHORT).show()
        }else{
            Toast.makeText(this,"Nenhuma imagem selecionada",Toast.LENGTH_SHORT).show()
        }

    }




    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        storageManager = StorageManager(this)

        binding.buttonLogin.setOnClickListener { signIn() }
        binding.buttonVerificarLogin.setOnClickListener { verificarUsuarioLogadoSimplificado() }
        binding.buttonUpload.setOnClickListener {

            CoroutineScope(Dispatchers.IO).launch {
                val mAccount = GoogleSignIn.getLastSignedInAccount(this@MainActivity)
                val credential = GoogleAccountCredential.usingOAuth2(this@MainActivity, Collections.singleton(Scopes.DRIVE_FILE))

                credential.selectedAccount = mAccount?.account

                val googleDriveService = Drive.Builder(
                    NetHttpTransport(),
                    GsonFactory(),
                    credential
                ).setApplicationName(MY_WALLET).build()
               // upLoadArquivo(googleDriveService)
                uploadImagem(googleDriveService)
            }

           // buttonUpLoad()
        }
        binding.buttonLer.setOnClickListener {

            CoroutineScope(Dispatchers.IO).launch {
                val mAccount = GoogleSignIn.getLastSignedInAccount(this@MainActivity)
                val credential = GoogleAccountCredential.usingOAuth2(this@MainActivity, Collections.singleton(Scopes.DRIVE_FILE))

                credential.selectedAccount = mAccount?.account

                val googleDriveService = Drive.Builder(
                    NetHttpTransport(),
                    GsonFactory(),
                    credential
                ).setApplicationName(MY_WALLET).build()
                lerArquivo(googleDriveService)

            }
        }
        binding.buttonListar.setOnClickListener {

            CoroutineScope(Dispatchers.IO).launch {
                val mAccount = GoogleSignIn.getLastSignedInAccount(this@MainActivity)
                val credential = GoogleAccountCredential.usingOAuth2(this@MainActivity, Collections.singleton(Scopes.DRIVE_FILE))

                credential.selectedAccount = mAccount?.account

                val googleDriveService = Drive.Builder(
                    NetHttpTransport(),
                    GsonFactory(),
                    credential
                ).setApplicationName(MY_WALLET).build()

                getAllBackupFiles(googleDriveService)
            }
        }
        binding.buttonLogout.setOnClickListener { deslogar() }
        binding.buttonAbrirGaleria.setOnClickListener {
            abrirGaleria()

           /* CoroutineScope(Dispatchers.IO).launch {
                val mAccount = GoogleSignIn.getLastSignedInAccount(this@MainActivity)
                val credential = GoogleAccountCredential.usingOAuth2(this@MainActivity, Collections.singleton(Scopes.DRIVE_FILE))

                credential.selectedAccount = mAccount?.account

                val googleDriveService = Drive.Builder(
                    NetHttpTransport(),
                    GsonFactory(),
                    credential
                ).setApplicationName(MY_WALLET).build()
               val itens =  listDriveFiles(googleDriveService)

                Log.d("MyDrive", "Total de itens: ${itens.size}")
            }*/

        }
       /* binding.buttonAbrirGaleria.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                val mAccount = GoogleSignIn.getLastSignedInAccount(this@MainActivity)
                val credential = GoogleAccountCredential.usingOAuth2(this@MainActivity, Collections.singleton(Scopes.DRIVE_FILE))

                credential.selectedAccount = mAccount?.account

                val googleDriveService = Drive.Builder(
                    NetHttpTransport(),
                    GsonFactory(),
                    credential
                ).setApplicationName(MY_WALLET).build()
                uploadImagem(googleDriveService)
            }
        }*/




    }

    private fun abrirGaleria(){

        abrirGaleria.launch("image/*")
    }






    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun uploadImagem(googleDriveService : Drive){
        withContext(Dispatchers.Default){
            val googleDriveFileHolderJob =async { createFolder(googleDriveService, MY_APP,null) }
            val googleDriveFileHolder = googleDriveFileHolderJob.await()
            val createFileJob = async { createFileInternalStorage(receivedText) }

            val file = uriImagem?.let { decodeImage(it) }

            if(file !== null){

                val uploadJob = async { uploadFile(
                    googleDriveService,
                    file,
                    IMAGE,
                    googleDriveFileHolder?.id
                ) }

                uploadJob.await()

                withContext(Dispatchers.Main){
                    Toast.makeText(this@MainActivity,"Upload imagem com Sucesso", Toast.LENGTH_LONG).show()
                }

            }else{

            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    suspend fun decodeImage(uri: Uri, ):File{
        val context = this@MainActivity
        return withContext(Dispatchers.IO){
            val contentResolver = context.contentResolver
            val file = File(context.cacheDir,"testImage.png")
            val fileOutput = FileOutputStream(file)
            val btm = ImageDecoder.createSource(contentResolver,uri).decodeBitmap { info, source ->
            }
            btm.compress(Bitmap.CompressFormat.WEBP_LOSSY,80,fileOutput)
            file
        }
    }

    private fun buttonUpLoad(){
        isFileRead = false
        val gso = GoogleSignInOptions
            .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()


        val googleSingClient = GoogleSignIn.getClient(this,gso)

        val signInIntent = googleSingClient?.signInIntent
        if (signInIntent != null) {
            laucher.launch(signInIntent)
        }
    }
    // Quando o usuário clicar no botão de login, inicie o intent de login
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
            }
        }


    }


    private fun verificarUsuarioLogadoSimplificado(){
        val account = GoogleSignIn.getLastSignedInAccount(this)

        if(account != null){
            binding.textViewEmail.text = "${account?.email}"
            Log.d("MyDrive", "Usuário  está logado")
        }else{
            Log.d("MyDrive", "Usuário não está logado")
        }
    }


    private var laucherLogin = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
        Log.d("MyDrive", "resultCode: $result.resultCode}")
        if(result.resultCode == Activity.RESULT_OK){
            val data : Intent? = result.data
            try{
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                binding.textViewEmail.text = "${task?.getResult()?.email}"
                checkForGooglePermissions()
                Log.d("MyDrive", "Login com sucesso")
                task.getResult(ApiException::class.java)
            }catch (e : Exception){
                Log.d("MyDrive", "laucher ${e.message}")
            }
        }else if(result.resultCode == Activity.RESULT_CANCELED){
            Log.d("MyDrive", "Tentativa de login cancelada")
        }
    }


    private var laucher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
        if(result.resultCode == Activity.RESULT_OK){
            val data : Intent? = result.data
            try{
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                Log.d("MyDrive", "laucher ${data?.`package`}")
                task.getResult(ApiException::class.java)
                checkForGooglePermissions()
            }catch (e : Exception){

            }
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

    private fun createTempFileInternalStorage() : File {
        var privateDir = filesDir
        privateDir = File(privateDir, MY_RC_RECOVERED_DIR)
        privateDir.mkdir()
        privateDir = File(privateDir, MY_RC_RECOVERED_TEXT_FILE)
        return  privateDir;

    }

    private fun readDataFromFile(file : File) : String?{
        try{
            val br = BufferedReader(FileReader(file))
            val line = br.readLines().joinToString()
            br.close()
            return  line
        }catch (e : Exception){

        }
        return null
    }

    private fun uploadFile(mDriveService: Drive, localFile: File, mimeType : String?, folderId : String?) : GoogleDriveFileHolder {
        val root = folderId?.let { listOf(it) } ?: listOf(ROOT)

        val metadata = com.google.api.services.drive.model.File()
            .setParents(root)
            .setMimeType(mimeType)
            .setName(localFile.name)

        val fileContent = FileContent(mimeType,localFile)

        val fileMeta = mDriveService.files().create(
            metadata,
            fileContent
        ).execute()

        val googleDriveFileHolder = GoogleDriveFileHolder()
        googleDriveFileHolder.id = fileMeta.id
        googleDriveFileHolder.name = fileMeta.name


        return googleDriveFileHolder

    }

    private fun createFileInternalStorage(text: String) : File?{
        var privateDir = filesDir
        privateDir = File(privateDir, MY_APP)
        privateDir.mkdir()
        privateDir = File(privateDir,"$MY_APP_LOWER.text")

        try{
            val fileOutStream = FileOutputStream(privateDir)
            fileOutStream.write(text.toByteArray())
            fileOutStream.close()
            return  privateDir
        }catch (e : Exception){

        }

        return null
    }

    private fun downloadFile(mDriveService: Drive, targetFile : File?, fileId : String?){
        val outputStream : OutputStream = FileOutputStream(targetFile)
        mDriveService.files()[fileId].executeAndDownloadTo(outputStream)
    }

    private fun abriFiles(file: File){
        // Abrir a ViewTextActivity para exibir o conteúdo do arquivo
        val intent = Intent(this@MainActivity, ViewTextActivity::class.java)
        intent.putExtra("FILE_PATH", file?.path)
        startActivity(intent)
    }

    private suspend fun driveSetUp(){
        val mAccount = GoogleSignIn.getLastSignedInAccount(this)
        val credential = GoogleAccountCredential.usingOAuth2(this, Collections.singleton(Scopes.DRIVE_FILE))

        credential.selectedAccount = mAccount?.account

        val googleDriveService = Drive.Builder(
            NetHttpTransport(),
            GsonFactory(),
            credential
        ).setApplicationName(MY_WALLET).build()

        if(isFileRead){
            //read from drive
            lerArquivo(googleDriveService)

        }else{

            upLoadArquivo(googleDriveService)

        }
    }

    private suspend fun lerArquivo(googleDriveService : Drive){
        withContext(Dispatchers.Default){
            try {
                val fileListJob = async { listDriveFiles(googleDriveService)  }
                val fileList = fileListJob.await()
                val cealAppEnv = "$MY_APP_LOWER.text"

                if(fileList.isNotEmpty()){
                    if(fileList.any { fileListItem ->
                            (fileListItem.name == MY_TEXT_FILE || fileListItem.name == cealAppEnv)
                        }){

                        for (item in fileList){
                            Log.d("MyDrive", "nomes: ${item.name}")
                            if(item.name == cealAppEnv || item.name == MY_TEXT_FILE){

                                val createFileJob = async { createTempFileInternalStorage() }
                              //  Log.d("MyDrive", "nomes: ${createFileJob.await().path}")
                                val file = createFileJob.await()

                                val downalodFileJob = async { downloadFile(googleDriveService,file,fileList[0].id) }

                                downalodFileJob.await()

                                val readJob = async { file?.let { readDataFromFile(it) } }
                                val text = readJob.await()
                                withContext(Dispatchers.Main){
                                    Toast.makeText(this@MainActivity,"$text",
                                        Toast.LENGTH_LONG).show()


                                }
                                Log.d("MyDrive", "conteudo $text")
                                break
                            }
                        }

                    }else{
                        Log.d("MyDrive", "Arquivo não encontrado")
                    }

                }else{
                    Log.d("MyDrive", "Nenhum arquivo encontrado")
                }
            }catch (e : Exception){
                Log.e("MyDrive", "Erro ao ler arquivo do Google Drive", e)
            }
        }
    }

    private suspend fun upLoadArquivo(googleDriveService : Drive){

        withContext(Dispatchers.Default){
            val googleDriveFileHolderJob =async { createFolder(googleDriveService, MY_APP,null) }
            val googleDriveFileHolder = googleDriveFileHolderJob.await()
            val createFileJob = async { createFileInternalStorage(receivedText) }

            val file = createFileJob.await()

            if(file !== null){

                val uploadJob = async { uploadFile(
                    googleDriveService,
                    file,
                    TEXT,
                    googleDriveFileHolder?.id
                ) }

                uploadJob.await()

                withContext(Dispatchers.Main){
                    Toast.makeText(this@MainActivity,"Sucesso", Toast.LENGTH_LONG).show()
                }

            }else{

            }
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

        }

        return null
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
                    if(false){
                        val outputStream = ByteArrayOutputStream()
                        drive.files().get(item.id).executeMediaAndDownloadTo(outputStream)

                        // Converter o fluxo de saída para uma string
                        val content = String(outputStream.toByteArray(), Charset.forName("UTF-8"))

                        Log.e("MyDrive", "content ${content} ")

                    }else if(item.name.equals("testImage.png")){
                        Log.e("MyDrive", "content ${item.thumbnailLink} ")
                       withContext(Dispatchers.Main){
                           dialogExibirFoto(item.thumbnailLink)
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

    private fun listDriveFiles(mDriveService : Drive) : List<com.google.api.services.drive.model.File>{
        var result : FileList
        var pageToken : String? = null

        do{
            result = mDriveService.files().list()
               // .setQ("mimeType='${TEXT}'")
                .setSpaces(DRIVE)
                .setFields(fields)
                .setPageToken(pageToken)
                .execute()
            pageToken = result.nextPageToken

        }while (pageToken != null)

        return result.files
    }

    companion object{
        const val RC_AUTHORIZE_DRIVE = 1
        const val MY_RC_RECOVERED_DIR = "CealRecovered"
        const val MY_RC_RECOVERED_TEXT_FILE = "CealRecovered.txt"
        const val TEXT = "text/plain"
        const val IMAGE = "image/*"
        const val MY_APP = "APP"
        const val MY_APP_LOWER = "app"
        const val DRIVE = "drive"
        const val ROOT = "root"
        const val MY_WALLET = "MY  WHALLET"
        const val MY_TEXT_FILE = "MEU.text"
        const val GOOGLE_DRIVE = "application/vnd.google-apps.folder"
        val RC_SIGN_IN = 9001 // Identificador para onActivityResult
    }

    private fun dialogExibirFoto(url : String){
        val dialogCustom = Dialog(this, com.google.android.material.R.style.MaterialAlertDialog_Material3)
        dialogCustom.setContentView(R.layout.layout_custom)
        val image = dialogCustom.findViewById<ImageView>(R.id.imageView)
        val btn = dialogCustom.findViewById<Button>(R.id.buttonVoltar)

        Glide.with(this).load(url).into(image);
        btn.setOnClickListener { dialogCustom.dismiss() }
        dialogCustom.create()
        dialogCustom.show()
    }

}






private fun politica(){
   /* var texto = "<string name=\"apkdoandroid_terms_privacy_text\">Ao enviar os dados," +
            " você concorda com o <a href=\"https://www.google.com.br/\">" +
            "termo de uso do serviço</a> e <a href=\"https://www.google.com.br/\"> " +
            "nossa política de privacidade</a>.</string>"




    val spannedText = HtmlCompat.fromHtml(texto, HtmlCompat.FROM_HTML_MODE_LEGACY)
    val textView = findViewById<TextView>(R.id.textViewEmail)
    textView.setText(spannedText)
    textView.movementMethod = LinkMovementMethod.getInstance()
    textView.setAutoLinkMask(Linkify.WEB_URLS);
    textView.setLinksClickable(true);*/
}