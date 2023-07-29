package com.hmy

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

// data class设计的初衷是不能更改 immutable 如果要更改数据一定要copy一遍，好处是前端知道data是consistent的
@Serializable
data class Song (
    val name: String,
    val lyric: String,
    val src: String,
    val length: String
)

@Serializable
data class PlayList (
    val id: Long,
    val songs: List<Song>
)


fun main() {
    // Application::module 和Java一样 在Call一个方法的时候通过::做reference
    // 这里是server的配置
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)


    // https://www.geeksforgeeks.org/double-colon-operator-in-java/
    val l : List<Int> = listOf(1,2,3,4,5)
    l.forEach({ num: Int -> println(num) })
    // forEach和println都是传一个int没有返回，那么就可以省掉signature
    l.forEach(System.out::println)

    // lambda function的作用域是在String里面
    noname(5) {
        true
    }

    noname(5, String::isEmpty)
}

// 做了一个限制，限制好了传进来的东西是什么
fun noname(x: Int, y: String.() -> Boolean) {}

// extension的使用 在Application这个已经存在的class上面加一个叫module的方法
// 如果是Java的话可能会用继承

// 想启动一个module 可以理解为一个server 对back service的配置
fun Application.module() {

    // 传了两个参数 ContentNegotiation 和 Lambda函数 {}, 当Lambda是最后一个参数的时候，可以把括号去掉
    // install了一个json，目的是server传递数据的方式是json，serialize和deserialize的library
    // install了一个解析json的能力
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
        })
    }
    // routing 是一个function 唯一已经参数是Lambda
    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        // 作用是把feed.json文件返回给前端作为response
        get("/feed") {
            // find resource固定写法
            val jsonString = this::class.java.classLoader.getResource("feed.json").readText()
            val json = Json.parseToJsonElement(jsonString)
            // response
            call.respondText(json.toString(), ContentType.Application.Json)
        }

        get("/playlists") {
            // find resource固定写法
            val jsonString = this::class.java.classLoader.getResource("playlists.json").readText()
            val json = Json.parseToJsonElement(jsonString)
            // response
            call.respondText(json.toString(), ContentType.Application.Json)
        }

        // host mp3 on the website 静态的 可以理解为下载 目录一定要叫static
        static("/") {
            staticBasePackage = "static"
            static("songs") {
                resources("songs")
            }
        }

        get("playlist/{id}") {
            val jsonString = this::class.java.classLoader.getResource("playlists.json").readText()
            val playlists = Json.decodeFromString(ListSerializer(PlayList.serializer()), jsonString)
            val id = call.parameters["id"]
            // 只有传一个参数的时候 可以用it这个特殊的站位符 类似于 num: int
            val playlist : PlayList? = playlists.firstOrNull { it.id.toString() == id }
            call.respondNullable(playlist)
        }
    }
}
