import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.sql.Time
import java.time.LocalDateTime
import java.util.Random
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import kotlin.Array
import kotlin.Double
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Throws

/**
 *
 * @author minaberger
 */
class SampleSquare(var freq: Double, var seconds: Double) : InputStream() {
    var signed = true
    var big_endian = true
    var sample_rate = 48000f
    var sample_size_byte = 2
    var channels = 1
    var list: ArrayList<Byte>
    var index = 0.0
    var period: Double
    var volume: Double
    var i = 0
    init {
        list = ArrayList()
        period = sample_rate / freq
        volume = Math.pow(2.0, (sample_size_byte * 8 - 1).toDouble()) - 1
    }

    @Throws(IOException::class)
    override fun read(): Int {
//        val result = when ((i / 220) % 2){
//            0 -> 255
//            else -> -255
//        }
//        i++
//        return result

        if (list.isEmpty()) {
            val value: Long
            value = if (index / period < 0.5) {
                volume.toLong()
            } else {
                -volume.toLong()
            }
            index++
            index %= period
            val buffer = ByteBuffer.allocate(8)
            buffer.putLong(value)
            val array = buffer.array()
            for (i in 8 - sample_size_byte..7) {
                list.add(array[i])
            }
        }
        val current = list.removeAt(0)
        return current.toUByte().toInt()
    }

    fun length(): Long {
        return (sample_rate * channels * seconds).toLong()
    }

    val format: AudioFormat
        get() = AudioFormat(sample_rate, sample_size_byte * 8, channels, signed, big_endian)

}
fun main(arg: Array<String>) {
    val ss = SampleSquare(440.0, 3.0)
    val file = File("result/square_440_${System.currentTimeMillis()}.wav")
    file.createNewFile()
    AudioSystem.write(
        AudioInputStream(ss, ss.format, ss.length()),
        AudioFileFormat.Type.WAVE, file
    )
}