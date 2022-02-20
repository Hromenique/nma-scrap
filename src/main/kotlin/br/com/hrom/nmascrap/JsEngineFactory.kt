package br.com.hrom.nmascrap

import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

class JsEngineFactory {

    companion object {
        fun newEngine(): ScriptEngine {
            val scriptEngineManager = ScriptEngineManager()
            return scriptEngineManager.getEngineByName("nashorn")
        }
    }
}