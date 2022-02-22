package br.com.hrom.nmascrap

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import jdk.nashorn.api.scripting.ScriptObjectMirror
import javax.script.ScriptEngineManager

private val jsEngine = ScriptEngineManager().getEngineByName("nashorn")

fun <T> ObjectMapper.readJSObject(rawJsObject: String, type: TypeReference<T>): T {
    val scriptObject = jsEngine.eval(rawJsObject)
    val obj = convertToObject(scriptObject)
    val json = this.writeValueAsString(obj)
    return this.readValue(json, type)
}

private fun ObjectMapper.convertToObject(scriptObj: Any): Any {
    if (scriptObj is ScriptObjectMirror) {
        return if (scriptObj.isArray) {
            val list: MutableList<Any> = mutableListOf()
            for ((_, value) in scriptObj) {
                list.add(convertToObject(value))
            }
            list
        } else {
            val map: MutableMap<String, Any> = mutableMapOf()
            for ((key, value) in scriptObj) {
                map[key] = convertToObject(value)
            }
            map
        }
    } else {
        return scriptObj
    }
}