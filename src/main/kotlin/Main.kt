import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.SortedMap
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
import kotlin.math.PI
import kotlin.math.sin

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
    var ellapsedFrame = 0

    var period: Double //　周期(フレーム)
    val changeKeyMapBySec = mutableMapOf(0.1 to 440.0, 0.2 to 220.000, 0.3 to 110.000, 0.4 to 70.0, 0.5 to 60.0 )
    val changeKeyMapByFrameSorted :SortedMap<Int, Double>
    val changeKeyMapFrames : List<Int>
    var volume: Double
    var changeKeyMapIndex = 0;
    init {
        list = ArrayList()
        period = sample_rate / freq
        volume = Math.pow(2.0, (sample_size_byte * 8 - 1).toDouble()) - 1
        var i = 0
        val sortedKeys = changeKeyMapBySec.keys.sorted()
        val changeMapFrame = mutableMapOf<Int, Double>()

        for (kv in changeKeyMapBySec){
            val sec = sortedKeys[i]
            changeMapFrame[(sec * sample_rate).toInt()] = changeKeyMapBySec[sec]!!
            i++
        }
        changeKeyMapByFrameSorted = changeMapFrame.toSortedMap()
        changeKeyMapFrames = changeKeyMapByFrameSorted.keys.toList()
    }

    @Throws(IOException::class)
    override fun read(): Int {
        run {
            if (changeKeyMapIndex <= changeKeyMapFrames.lastIndex){
                val nextChangeFrame = changeKeyMapFrames[changeKeyMapIndex]

                if (ellapsedFrame == nextChangeFrame) {
                    freq = changeKeyMapByFrameSorted[nextChangeFrame]!!
                    period = sample_rate / freq
                    changeKeyMapIndex++
                }
            }
        }
        if (list.isEmpty()) {
            val phase = index / period
            val factor = sin(2 * phase * PI)
            val value: Long = (volume * factor).toLong()
            val buffer = ByteBuffer.allocate(8)
            buffer.putLong(value)
            val array = buffer.array()
            for (i in 8 - sample_size_byte..7) {
                list.add(array[i])
            }
            ellapsedFrame++
            index++
            index %= period
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
    val ss = SampleSquare(880.0, 3.0)
    val file = File("result/square_440_${System.currentTimeMillis()}.wav")
    file.createNewFile()
    AudioSystem.write(
        AudioInputStream(ss, ss.format, ss.length()),
        AudioFileFormat.Type.WAVE, file
    )
}