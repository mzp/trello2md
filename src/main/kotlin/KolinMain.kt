import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okhttp3.OkHttpClient
import okhttp3.Request
import java.lang.reflect.Type

object KotlinMain {
    @JvmStatic
    fun main(args: Array<String>) {
        val boardId = args[0]
        val trello = Trello(args[1], args[2])

        val lists = trello.getLists(boardId)
        val cards = trello.getCards(boardId)

        val list_with_card = lists.map { list ->
            val id = list.id
            val cards = cards.filter { it.idList == id }.sortedBy { it.pos }
            list to cards
        }.sortedBy { (list, _) -> list.pos }

        val result = list_with_card.map { (list, cards) ->
"""
# ${list.name}

${cards.map { "- ${it.name}\n" }.joinToString(separator = "") { it }}
"""
        }.joinToString(separator = "\n") { it }

        println(result)
    }

}

class Trello(val key: String, val token: String) {
    fun getLists(boardId: String): List<TList> {
        val json = get(boardId, "lists", "name,pos")
        return parse(json, TList::class.java)
    }

    fun getCards(boardId: String): List<Card> {
        val json = get(boardId, "cards", "name,idList,pos")
        return parse(json, Card::class.java)
    }

    private fun get(boardId: String, path: String, fields: String): String {
        val url = "https://api.trello.com/1/boards/${boardId}/${path}?fields=${fields}&key=${key}&token=${token}"

        val client = OkHttpClient()
        val request = Request.Builder()
                .url(url)
                .build()

        val response = client.newCall(request).execute()
        return response.body().string()
    }

    private fun <T>parse(body: String, type: Type): List<T> {
        val moshi = Moshi.Builder().build()
        val type: Type = Types.newParameterizedType(List::class.java, type)
        val adapter: JsonAdapter<List<T>> = moshi.adapter(type)

        return adapter.fromJson(body)
    }
}