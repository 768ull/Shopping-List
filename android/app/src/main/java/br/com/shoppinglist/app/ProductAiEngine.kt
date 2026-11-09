package br.com.shoppinglist.app

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.extensions.OrtxPackage
import io.aatricks.llmedge.StableDiffusion
import io.aatricks.llmedge.huggingface.HuggingFaceHub
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale
import kotlin.math.sqrt

/**
 * Two-stage on-device product AI:
 * 1) multilingual semantic embedding decides whether a real Unicode emoji fits;
 * 2) only when no confident emoji exists, SDXS creates and caches a kawaii product sticker.
 *
 * Model weights are downloaded into app-private storage and stay available offline.
 */
object ProductAiEngine {
    private const val GENERATED_PREFIX = "generated:"
    private const val MODEL_DIR = "local_ai"
    private const val SEMANTIC_FILE = "distiluse-multilingual.onnx"
    private const val SEMANTIC_SIZE = 136_313_389L
    private const val SEMANTIC_SHA256 = "827d1a23e47f8c68a5788e58a06e137edf52aa568a6e4c852f4ce79f21b8a205"
    private const val SEMANTIC_URL =
        "https://huggingface.co/onnx-community/distiluse-base-multilingual-v2-merged-onnx/resolve/469261b351c8c1d9bca24b5f2f544ec264cfd2f3/combined_tokenizer_embedded_model.onnx?download=true"

    private const val IMAGE_MODEL_ID = "concedo/sdxs-512-tinySDdistilled-GGUF"
    private const val IMAGE_MODEL_REVISION = "3144d898d61492f8382ffcabec055733fc5b2a0e"
    private const val IMAGE_MODEL_FILE = "sdxs-512-tinySDdistilled_Q8_0.gguf"
    private const val VECTOR_CACHE_VERSION = 5
    private const val VECTOR_DIMENSION = 768
    private const val MIN_SIMILARITY = 0.78f
    private const val GIB = 1024L * 1024L * 1024L

    interface Callback {
        fun onStatus(message: String)
        fun onResult(value: String)
        fun onError(message: String)
    }

    private data class Candidate(val description: String, val emoji: String)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val processingLock = Mutex()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val environment: OrtEnvironment by lazy { OrtEnvironment.getEnvironment() }

    @Volatile private var semanticSession: OrtSession? = null
    @Volatile private var cachedCandidateVectors: List<FloatArray>? = null

    // Cured meats deliberately bypass semantic matching: salame must not become bacon.
    private val candidates = listOf(
        Candidate("maçã, maçã vermelha, fruta fresca", "🍎"),
        Candidate("maçã verde, fruta verde", "🍏"),
        Candidate("banana, fruta amarela", "🍌"),
        Candidate("uvas, cacho de uvas", "🍇"),
        Candidate("morango, fruta vermelha", "🍓"),
        Candidate("laranja, tangerina, mexerica, cítrico", "🍊"),
        Candidate("melancia, fruta grande", "🍉"),
        Candidate("abacaxi, ananás", "🍍"),
        Candidate("manga, fruta tropical", "🥭"),
        Candidate("pera, fruta verde", "🍐"),
        Candidate("limão, lima, fruta cítrica", "🍋"),
        Candidate("abacate, avocado", "🥑"),
        Candidate("tomate, tomate vermelho", "🍅"),
        Candidate("cenoura, raiz laranja", "🥕"),
        Candidate("batata inglesa, batata comum", "🥔"),
        Candidate("batata doce", "🍠"),
        Candidate("cebola", "🧅"),
        Candidate("alho", "🧄"),
        Candidate("brócolis, brócolos", "🥦"),
        Candidate("alface, couve, folhas verdes, verduras", "🥬"),
        Candidate("milho, espiga de milho", "🌽"),
        Candidate("pepino", "🥒"),
        Candidate("pimentão, pimento", "🫑"),
        Candidate("pimenta ardida, chili", "🌶️"),
        Candidate("cogumelo, champignon", "🍄"),
        Candidate("pão, pão de forma, pão francês", "🍞"),
        Candidate("queijo, queijo amarelo, queijo mussarela", "🧀"),
        Candidate("ovo, ovos, ovo com casca", "🥚"),
        Candidate("bacon, fatias de bacon defumado", "🥓"),
        Candidate("carne bovina, bife, carne vermelha", "🥩"),
        Candidate("frango assado, coxa de frango, galinha", "🍗"),
        Candidate("peixe, peixe fresco, tilápia", "🐟"),
        Candidate("camarão, camarões", "🍤"),
        Candidate("arroz cozido, arroz branco", "🍚"),
        Candidate("feijão, feijões, grãos de feijão", "🫘"),
        Candidate("macarrão, espaguete, massa", "🍝"),
        Candidate("sopa, miojo, macarrão instantâneo", "🍜"),
        Candidate("sal, saleiro", "🧂"),
        Candidate("mel, pote de mel", "🍯"),
        Candidate("biscoito, bolacha, cookie", "🍪"),
        Candidate("chocolate, barra de chocolate", "🍫"),
        Candidate("bala, doce, pirulito", "🍬"),
        Candidate("bolo, torta doce", "🍰"),
        Candidate("sorvete, casquinha de sorvete", "🍦"),
        Candidate("pizza", "🍕"),
        Candidate("hambúrguer, cheeseburger", "🍔"),
        Candidate("batata frita, fritas", "🍟"),
        Candidate("pipoca", "🍿"),
        Candidate("suco, suco de fruta, suco natural, suco integral, suco de caixinha", "🧃"),
        Candidate("néctar de fruta, refresco, polpa de fruta, bebida de fruta", "🧃"),
        Candidate("suco em pó e sucos prontos: Tang, Fresh, Mid, Del Valle, Maguary, Dafruta e Kapo", "🧃"),
        Candidate("refrigerante, bebida gaseificada, bebida gelada, copo com canudo", "🥤"),
        Candidate("refrigerante de cola, Coca Cola, Coca-Cola, Pepsi, Guaraná, Guaraná Antarctica", "🥤"),
        Candidate("Fanta, Sprite, 7 Up, Schweppes, água tônica, soda", "🥤"),
        Candidate("água mineral, água potável, água com gás, água sem gás, garrafa de água", "💧"),
        Candidate("água mineral de marcas Bonafont, Minalba, Crystal, Indaiá, São Lourenço, Perrier e Evian", "💧"),
        Candidate("água de coco e bebidas de coco, Kero Coco e Sococo", "🥥"),
        Candidate("água de coco, bebida de coco", "🥥"),
        Candidate("bebida energética, energético, Red Bull, Monster, Baly, TNT, Fusion, Gatorade, Powerade, isotônico", "🥤"),
        Candidate("café, café em pó, café solúvel, cappuccino, espresso, xícara de café", "☕"),
        Candidate("chá, chá de ervas, chá mate, infusão, xícara de chá", "🍵"),
        Candidate("chá gelado, ice tea, chá pronto para beber", "🧋"),
        Candidate("leite, leite integral, leite desnatado, copo de leite", "🥛"),
        Candidate("achocolatado, Nescau, Toddy, Yakult, leite fermentado, bebida láctea, iogurte líquido, Ninho, Itambé, Piracanjuba, Parmalat, Italac, Activia, Danone", "🥛"),
        Candidate("cerveja, cerveja pilsen, lager, IPA, chope, chopp", "🍺"),
        Candidate("vinho tinto, vinho branco, vinho rosé, espumante, champanhe", "🍷"),
        Candidate("bebida destilada, whisky, uísque, vodka, cachaça, rum, gin, tequila", "🥃"),
        Candidate("manteiga, margarina", "🧈"),
        Candidate("papel higiênico, rolo de papel", "🧻"),
        Candidate("sabonete, barra de sabão", "🧼"),
        Candidate("esponja de limpeza", "🧽"),
        Candidate("vassoura de limpeza", "🧹"),
        Candidate("balde de limpeza", "🪣"),
        Candidate("escova de dente, escova dental", "🪥"),
        Candidate("ração para cachorro, ração para gato, petisco para animais, osso para cachorro", "🦴"),
        Candidate("planta em vaso, planta doméstica", "🪴"),
        Candidate("celular, telefone, smartphone", "📱"),
        Candidate("televisão, TV", "📺"),
        Candidate("computador, notebook, laptop", "💻"),
        Candidate("tablet", "📱"),
        Candidate("mouse de computador", "🖱️"),
        Candidate("teclado de computador", "⌨️"),
        Candidate("impressora", "🖨️"),
        Candidate("videogame, console de jogos, controle de videogame", "🎮"),
        Candidate("fone de ouvido, headphone, headset", "🎧"),
        Candidate("rádio, caixa de som portátil, alto-falante Bluetooth", "📻"),
        Candidate("câmera fotográfica, máquina fotográfica", "📷"),
        Candidate("carregador, cabo USB, plugue elétrico", "🔌"),
        Candidate("pilha, bateria", "🔋"),
        Candidate("lâmpada, lâmpada LED", "💡"),
        Candidate("relógio de pulso", "⌚"),
        Candidate("óculos de sol", "🕶️"),
        Candidate("óculos de grau", "👓"),
        Candidate("camiseta, camisa", "👕"),
        Candidate("calça jeans, calças", "👖"),
        Candidate("vestido", "👗"),
        Candidate("sapato, tênis, calçado", "👟"),
        Candidate("meia", "🧦"),
        Candidate("bolsa, mochila", "🎒"),
        Candidate("lápis", "✏️"),
        Candidate("caneta", "🖊️"),
        Candidate("caderno", "📓"),
        Candidate("livros", "📚"),
        Candidate("tesoura", "✂️"),
        Candidate("martelo", "🔨"),
        Candidate("chave de fenda", "🪛"),
        Candidate("chave inglesa, alicate", "🔧"),
        Candidate("serrote, serra manual", "🪚"),
        Candidate("trena, fita métrica", "📏"),
        Candidate("vaso sanitário, privada", "🚽"),
        Candidate("chuveiro", "🚿"),
        Candidate("banheira", "🛁"),
        Candidate("cama, colchão", "🛏️"),
        Candidate("cadeira", "🪑"),
        Candidate("caixa de papelão, pacote", "📦"),
        Candidate("presente, pacote de presente", "🎁"),
        Candidate("chave, chaveiro", "🔑"),
        Candidate("carro, automóvel", "🚗"),
        Candidate("motocicleta, moto", "🏍️"),
        Candidate("bicicleta", "🚲"),
        Candidate("pneu", "🛞"),
        Candidate("bola de futebol", "⚽"),
        Candidate("ursinho de pelúcia, brinquedo de pelúcia", "🧸"),
        Candidate("salgadinho de pacote, batata chips, Doritos, Cheetos, Ruffles e Pringles", "🍟")
    )

    @JvmStatic
    fun process(context: Context, productName: String, callback: Callback) {
        val appContext = context.applicationContext
        scope.launch {
            try {
                processingLock.withLock {
                    val normalized = normalize(productName)
                    if (normalized.length < 2) return@withLock

                    val key = productKey(normalized)
                    val existing = generatedStickerFile(appContext, key)
                    if (existing.isFile && existing.length() > 4096L) {
                        deliverResult(callback, GENERATED_PREFIX + key)
                        return@withLock
                    }

                    val forceImage = mustGenerateSpecificSticker(normalized)
                    if (!forceImage) {
                        deliverStatus(callback, "Entendendo o produto localmente...")
                        val match = semanticMatch(appContext, productName, callback)
                        if (match != null) {
                            deliverResult(callback, match)
                            return@withLock
                        }
                    }

                    releaseSemanticSession()
                    deliverStatus(callback, "O produto não tem um emoji adequado. Preparando a imagem kawaii...")
                    ensureImageGenerationMemory(appContext)
                    createProductSticker(appContext, productName, key, callback)
                    deliverResult(callback, GENERATED_PREFIX + key)
                }
            } catch (error: Throwable) {
                val message = error.message?.takeIf { it.isNotBlank() }
                    ?: error.javaClass.simpleName
                deliverError(callback, message)
            }
        }
    }

    @JvmStatic
    fun isGenerated(value: String?): Boolean = value?.startsWith(GENERATED_PREFIX) == true

    @JvmStatic
    fun generatedFile(context: Context, value: String): File {
        val key = value.removePrefix(GENERATED_PREFIX)
        return generatedStickerFile(context, key)
    }

    private fun generatedStickerFile(context: Context, key: String): File {
        return File(File(context.filesDir, "generated_stickers"), "$key.png")
    }

    private suspend fun semanticMatch(
        context: Context,
        productName: String,
        callback: Callback
    ): String? {
        val session = ensureSemanticSession(context, callback)
        val vectors = ensureCandidateVectors(context, session, callback)
        val query = embed(session, arrayOf("Produto de compra: $productName"))[0]
        val normalizedQuery = normalizeVector(query)

        var bestIndex = -1
        var bestScore = -1.0f
        for (index in vectors.indices) {
            val score = dot(normalizedQuery, vectors[index])
            if (score > bestScore) {
                bestScore = score
                bestIndex = index
            }
        }
        return if (bestIndex >= 0 && bestScore >= MIN_SIMILARITY) {
            candidates[bestIndex].emoji
        } else null
    }

    private suspend fun ensureSemanticSession(
        context: Context,
        callback: Callback
    ): OrtSession {
        semanticSession?.let { return it }
        val modelFile = ensureSemanticModelFile(context, callback)
        val options = OrtSession.SessionOptions()
        try {
            options.registerCustomOpLibrary(OrtxPackage.getLibraryPath())
            options.setIntraOpNumThreads(2)
            options.setInterOpNumThreads(1)
            val created = environment.createSession(modelFile.absolutePath, options)
            semanticSession = created
            return created
        } finally {
            options.close()
        }
    }

    private suspend fun ensureSemanticModelFile(context: Context, callback: Callback): File =
        withContext(Dispatchers.IO) {
            val directory = File(context.filesDir, MODEL_DIR)
            if (!directory.exists() && !directory.mkdirs()) {
                throw IOException("Não foi possível preparar o armazenamento dos modelos.")
            }
            val destination = File(directory, SEMANTIC_FILE)
            val marker = File(directory, "$SEMANTIC_FILE.sha256")
            if (destination.isFile && destination.length() == SEMANTIC_SIZE &&
                marker.isFile && marker.readText().trim().equals(SEMANTIC_SHA256, ignoreCase = true)
            ) {
                return@withContext destination
            }

            ensureUnmeteredNetwork(context, "136 MB para o modelo de entendimento")
            deliverStatus(callback, "Baixando IA de entendimento (136 MB) pelo Wi-Fi...")
            val temporary = File(directory, "$SEMANTIC_FILE.part")
            temporary.delete()
            val connection = (URL(SEMANTIC_URL).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 30_000
                readTimeout = 60_000
                useCaches = false
                setRequestProperty("Accept-Encoding", "identity")
                setRequestProperty("User-Agent", "ShoppingList-LocalAI/1.0")
            }
            try {
                val response = connection.responseCode
                if (response != HttpURLConnection.HTTP_OK) {
                    throw IOException("Não foi possível baixar a IA de entendimento (HTTP $response).")
                }
                val announcedSize = connection.contentLengthLong
                if (announcedSize > 0L && announcedSize != SEMANTIC_SIZE) {
                    throw IOException("O arquivo do modelo não tem o tamanho esperado.")
                }
                val digest = MessageDigest.getInstance("SHA-256")
                var copied = 0L
                connection.inputStream.use { input ->
                    FileOutputStream(temporary).use { output ->
                        val buffer = ByteArray(1024 * 256)
                        var lastProgress = 0
                        while (true) {
                            val count = input.read(buffer)
                            if (count == -1) break
                            copied += count.toLong()
                            if (copied > SEMANTIC_SIZE) {
                                throw IOException("O modelo de entendimento excedeu o tamanho esperado.")
                            }
                            digest.update(buffer, 0, count)
                            output.write(buffer, 0, count)
                            val percent = (copied * 100L / SEMANTIC_SIZE).toInt()
                            if (percent >= lastProgress + 10) {
                                lastProgress = percent
                                deliverStatus(callback, "Baixando IA de entendimento: $percent%")
                            }
                        }
                        output.fd.sync()
                    }
                }
                if (copied != SEMANTIC_SIZE) {
                    throw IOException("Download incompleto da IA de entendimento.")
                }
                val actualSha = digest.digest().joinToString("") {
                    "%02x".format(Locale.ROOT, it.toInt() and 0xff)
                }
                if (!actualSha.equals(SEMANTIC_SHA256, ignoreCase = true)) {
                    throw IOException("A verificação SHA-256 do modelo de entendimento falhou.")
                }
                if (destination.exists() && !destination.delete()) {
                    throw IOException("Não foi possível substituir o modelo antigo.")
                }
                if (!temporary.renameTo(destination)) {
                    throw IOException("Não foi possível guardar o modelo de entendimento.")
                }
                marker.writeText(SEMANTIC_SHA256)
                destination
            } finally {
                connection.disconnect()
                temporary.delete()
            }
        }

    private suspend fun ensureCandidateVectors(
        context: Context,
        session: OrtSession,
        callback: Callback
    ): List<FloatArray> {
        cachedCandidateVectors?.let { return it }
        val cacheFile = File(File(context.filesDir, MODEL_DIR), "emoji-semantic-index-v$VECTOR_CACHE_VERSION.bin")
        val restored = readVectorCache(cacheFile)
        if (restored != null) {
            cachedCandidateVectors = restored
            return restored
        }

        deliverStatus(callback, "Preparando o catálogo semântico de produtos...")
        val descriptions = candidates.map { "Emoji de produto: ${it.description}" }.toTypedArray()
        val rawVectors = embed(session, descriptions)
        if (rawVectors.size != candidates.size || rawVectors.any { it.size != VECTOR_DIMENSION }) {
            throw IOException("O modelo semântico retornou vetores incompatíveis.")
        }
        val vectors = rawVectors.map { normalizeVector(it) }
        writeVectorCache(cacheFile, vectors)
        cachedCandidateVectors = vectors
        return vectors
    }

    private fun embed(session: OrtSession, texts: Array<String>): List<FloatArray> {
        val input = OnnxTensor.createTensor(environment, texts, longArrayOf(texts.size.toLong()))
        try {
            session.run(mapOf("text" to input)).use { result ->
                val output = result.get(0).value
                val rows = output as? Array<*>
                    ?: throw IOException("A saída da IA de entendimento não é uma matriz.")
                return rows.map { row ->
                    (row as? FloatArray)?.copyOf()
                        ?: throw IOException("A saída da IA de entendimento não contém vetores válidos.")
                }
            }
        } finally {
            input.close()
        }
    }

    private fun readVectorCache(file: File): List<FloatArray>? {
        if (!file.isFile || file.length() < 16L) return null
        return try {
            DataInputStream(BufferedInputStream(FileInputStream(file))).use { input ->
                if (input.readInt() != 0x534C4149) return null
                if (input.readInt() != VECTOR_CACHE_VERSION) return null
                val count = input.readInt()
                val dimension = input.readInt()
                if (count != candidates.size || dimension != VECTOR_DIMENSION) return null
                List(count) { FloatArray(dimension) { input.readFloat() } }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun writeVectorCache(file: File, vectors: List<FloatArray>) {
        file.parentFile?.mkdirs()
        val temporary = File(file.parentFile, file.name + ".tmp")
        DataOutputStream(BufferedOutputStream(FileOutputStream(temporary))).use { output ->
            output.writeInt(0x534C4149)
            output.writeInt(VECTOR_CACHE_VERSION)
            output.writeInt(vectors.size)
            output.writeInt(VECTOR_DIMENSION)
            vectors.forEach { vector -> vector.forEach(output::writeFloat) }
            output.flush()
        }
        if (file.exists()) file.delete()
        if (!temporary.renameTo(file)) temporary.delete()
    }

    private fun normalizeVector(source: FloatArray): FloatArray {
        val result = source.copyOf()
        var normSquared = 0.0
        result.forEach { normSquared += it.toDouble() * it.toDouble() }
        val norm = sqrt(normSquared).toFloat().coerceAtLeast(1e-12f)
        for (index in result.indices) result[index] /= norm
        return result
    }

    private fun dot(left: FloatArray, right: FloatArray): Float {
        var result = 0.0f
        for (index in left.indices) result += left[index] * right[index]
        return result
    }

    private fun mustGenerateSpecificSticker(normalized: String): Boolean {
        val phrases = listOf(
            "salame", "salami", "linguica", "linguica calabresa", "calabresa",
            "linguica toscana", "salsicha", "presunto", "apresuntado",
            "mortadela", "chorizo", "pepperoni", "paio", "chourico"
        )
        return phrases.any { phrase -> (" $normalized ").contains(" $phrase ") }
    }

    private suspend fun createProductSticker(
        context: Context,
        productName: String,
        key: String,
        callback: Callback
    ): File {
        val outputFile = generatedStickerFile(context, key)
        if (outputFile.isFile && outputFile.length() > 4096L) return outputFile

        val storage = StatFs(context.filesDir.absolutePath)
        if (storage.availableBytes < 950L * 1024L * 1024L) {
            throw IOException("A IA de imagens precisa de aproximadamente 950 MB livres no armazenamento interno.")
        }

        val cachedImageModel = File(
            File(context.filesDir, "hf-models"),
            IMAGE_MODEL_ID.replace("/", "_") + "/$IMAGE_MODEL_REVISION/$IMAGE_MODEL_FILE"
        )
        if (!cachedImageModel.isFile) {
            ensureUnmeteredNetwork(context, "aproximadamente 683 MB para o modelo de imagens")
        }
        deliverStatus(callback, "Baixando IA de criação de imagens (aprox. 683 MB)...")
        val imageModel = HuggingFaceHub.ensureModelOnDisk(
            context = context,
            modelId = IMAGE_MODEL_ID,
            revision = IMAGE_MODEL_REVISION,
            preferredQuantizations = emptyList(),
            filename = IMAGE_MODEL_FILE,
            preferSystemDownloader = true,
            onProgress = { downloaded, total ->
                if (total != null && total > 0L) {
                    val percent = (downloaded * 100L / total).toInt().coerceIn(0, 100)
                    deliverStatus(callback, "Baixando IA de imagens: $percent%")
                } else {
                    deliverStatus(callback, "Baixando IA de imagens...")
                }
            }
        ).file

        if (!imageModel.isFile || imageModel.length() < 500L * 1024L * 1024L) {
            throw IOException("O arquivo da IA de imagens está incompleto.")
        }
        ensureImageGenerationMemory(context)
        deliverStatus(callback, "Criando adesivo kawaii de $productName...")

        val runtime = StableDiffusion.load(
            context = context,
            modelPath = imageModel.absolutePath,
            nThreads = Runtime.getRuntime().availableProcessors().coerceIn(2, 4),
            offloadToCpu = true,
            keepClipOnCpu = true,
            keepVaeOnCpu = true,
            flashAttn = false,
            sequentialLoad = true,
            preferPerformanceMode = false
        )
        try {
            val cleanName = productName.trim().take(90)
            val prompt = "single isolated kawaii emoji-style sticker illustration of $cleanName, " +
                "recognizable accurate real-world product, cute rounded shape, friendly tiny face, " +
                "soft pastel colors, polished smooth 3D clay and vector art, centered composition, " +
                "simple clean white background, one object only, no text, no letters, no watermark"
            val negative = "words, typography, letters, logo, watermark, multiple objects, collage, " +
                "photograph, realistic photo, blurry, low quality, cropped object, extra limbs"
            val seed = key.take(8).toLongOrNull(16) ?: 42L
            val rgb = runtime.txt2img(
                prompt = prompt,
                negative = negative,
                width = 512,
                height = 512,
                steps = 1,
                cfg = 1.0f,
                seed = seed
            ) ?: throw IOException("O gerador local não retornou uma imagem.")
            val bitmap = rgbToBitmap(rgb, 512, 512)
            try {
                outputFile.parentFile?.mkdirs()
                val temporary = File(outputFile.parentFile, outputFile.name + ".tmp")
                FileOutputStream(temporary).use { stream ->
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                        throw IOException("Não foi possível salvar o adesivo gerado.")
                    }
                    stream.fd.sync()
                }
                if (outputFile.exists()) outputFile.delete()
                if (!temporary.renameTo(outputFile)) {
                    temporary.delete()
                    throw IOException("Não foi possível guardar o adesivo gerado.")
                }
            } finally {
                bitmap.recycle()
            }
        } finally {
            runtime.close()
        }
        return outputFile
    }

    private fun rgbToBitmap(bytes: ByteArray, width: Int, height: Int): Bitmap {
        val expected = width * height * 3
        if (bytes.size < expected) throw IOException("A imagem gerada está incompleta.")
        val pixels = IntArray(width * height)
        var source = 0
        for (index in pixels.indices) {
            val red = bytes[source++].toInt() and 0xff
            val green = bytes[source++].toInt() and 0xff
            val blue = bytes[source++].toInt() and 0xff
            pixels[index] = (0xff shl 24) or (red shl 16) or (green shl 8) or blue
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun ensureImageGenerationMemory(context: Context) {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(info)
        if (manager.isLowRamDevice || info.totalMem < 5L * GIB) {
            throw IOException("A criação local de imagens requer um aparelho ARM64 com aproximadamente 6 GB de RAM.")
        }
        if (info.availMem < 1_300L * 1024L * 1024L) {
            throw IOException("Há pouca memória livre para criar a imagem. Feche outros aplicativos e tente novamente.")
        }
    }

    private fun ensureUnmeteredNetwork(context: Context, description: String) {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (manager.activeNetwork == null || manager.isActiveNetworkMetered) {
            throw IOException("Conecte o celular ao Wi-Fi para baixar $description. Depois o modelo funcionará offline.")
        }
    }

    private fun releaseSemanticSession() {
        try {
            semanticSession?.close()
        } catch (_: Exception) {
        } finally {
            semanticSession = null
        }
    }

    private fun productKey(normalized: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(normalized.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }
    }

    private fun normalize(value: String): String {
        val decomposed = Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        return decomposed.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
    }

    private fun deliverStatus(callback: Callback, message: String) {
        mainHandler.post { callback.onStatus(message) }
    }

    private fun deliverResult(callback: Callback, result: String) {
        mainHandler.post { callback.onResult(result) }
    }

    private fun deliverError(callback: Callback, message: String) {
        mainHandler.post { callback.onError(message) }
    }
}
