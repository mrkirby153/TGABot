import org.json.JSONArray
import org.json.JSONObject
import java.io.File

fun main(args: Array<String>) {

    val rootJson = JSONObject()
    var id = 1
    val categoryIndex = JSONArray()
    val optionsIndex = JSONObject()
    outer@while (true) {
        print("Enter a name for the category: ")
        val categoryName = readLine() ?: ""
        if (categoryName == "exit") {
            break
        }
        print("Enter a channel: ")
        val channel = readLine() ?: ""
        categoryIndex.put(JSONObject().apply {
            put("id", id)
            put("channel", channel)
            put("name", categoryName)
        })
        print("Enter a list of nominees separated by a comma: ")
        val options = readLine() ?: ""
        if(options.isBlank()){
            println("No nominees entered...")
            continue
        }
        val arr = JSONArray()
        options.split(",").forEachIndexed { i, o ->
            arr.put(JSONObject().apply {
                put("emoji", "${i+1}⃣")
                put("name", o)
            })
        }
        optionsIndex.put(id.toString(), arr)
        id++
    }
    rootJson.put("categories", categoryIndex)
    rootJson.put("options", optionsIndex)

    val out = File("output.json")
    out.writeText(rootJson.toString(2))
    println("Final json written to `output.json`")
}