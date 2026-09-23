import com.android.apksig.ApkSigner
import java.io.File
import java.io.FileInputStream
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate

/** Uso: Signer <in.apk> <out.apk> <keystore> <storepass> <alias> <keypass> */
fun main(a: Array<String>) {
    val ks = KeyStore.getInstance(KeyStore.getDefaultType())
    FileInputStream(a[2]).use { ks.load(it, a[3].toCharArray()) }
    val key = ks.getKey(a[4], a[5].toCharArray()) as PrivateKey
    val cert = ks.getCertificate(a[4]) as X509Certificate
    val cfg = ApkSigner.SignerConfig.Builder("CERT", key, listOf(cert)).build()
    ApkSigner.Builder(listOf(cfg))
        .setInputApk(File(a[0])).setOutputApk(File(a[1]))
        .setMinSdkVersion(26)
        .setV1SigningEnabled(true).setV2SigningEnabled(true).setV3SigningEnabled(true)
        .build().sign()
    println("Firmado: " + a[1])
}
