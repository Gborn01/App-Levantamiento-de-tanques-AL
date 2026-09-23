import com.android.apksig.ApkVerifier
import java.io.File
fun main(a: Array<String>) {
    val r = ApkVerifier.Builder(File(a[0])).build().verify()
    println("verificada=${r.isVerified} v1=${r.isVerifiedUsingV1Scheme} v2=${r.isVerifiedUsingV2Scheme} v3=${r.isVerifiedUsingV3Scheme}")
    r.errors.forEach { println("ERROR: $it") }
    r.signerCertificates.forEach { println("cert: " + it.subjectX500Principal) }
    if (!r.isVerified) System.exit(1)
}
