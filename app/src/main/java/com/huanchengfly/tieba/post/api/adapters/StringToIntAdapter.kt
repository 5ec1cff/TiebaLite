package com.huanchengfly.tieba.post.api.adapters

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

class StringToIntAdapter : JsonDeserializer<Int> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Int {
        if (json.isJsonPrimitive) return json.asString.toInt()
        throw IllegalArgumentException("$json is not String!")
    }
}